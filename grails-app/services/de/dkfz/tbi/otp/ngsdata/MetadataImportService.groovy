package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import groovy.transform.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*
import org.springframework.security.access.prepost.*

import java.util.logging.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static de.dkfz.tbi.otp.utils.StringUtils.*

/**
 * Metadata import 2.0 (OTP-34)
 */
class MetadataImportService {

    static final String AUTO_DETECT_DIRECTORY_STRUCTURE_NAME = ''
    static final String DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME = 'dataFilesInSameDirectory'

    @Autowired
    ApplicationContext applicationContext

    SampleIdentifierService sampleIdentifierService
    SeqTrackService seqTrackService
    TrackingService trackingService

    /**
     * @return A collection of descriptions of the validations which are performed
     */
    Collection<String> getImplementedValidations() {
        return metadataValidators.sum { it.descriptions }
    }

    /**
     * @return Names (keys) and descriptions (values) of directory structures
     */
    Map<String, String> getSupportedDirectoryStructures() {
        Map<String, String> directoryStructures = new TreeMap<String, String>()
        directoryStructures.put(AUTO_DETECT_DIRECTORY_STRUCTURE_NAME, 'detect automatically')
        applicationContext.getBeansOfType(DirectoryStructure).each { String name, DirectoryStructure directoryStructure ->
            directoryStructures.put(name, directoryStructure.description)
        }
        return directoryStructures
    }

    /**
     * @param directoryStructureName As returned by {@link #getSupportedDirectoryStructures()}
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")  // TODO: OTP-1908: Relax this restriction
    MetadataValidationContext validate(File metadataFile, String directoryStructureName) {
        MetadataValidationContext context = MetadataValidationContext.createFromFile(metadataFile,
                getDirectoryStructure(getDirectoryStructureBeanName(directoryStructureName, metadataFile)))
        if (context.spreadsheet) {
            metadataValidators*.validate(context)
        }
        return context
    }

    /**
     * @param directoryStructureName As returned by {@link #getSupportedDirectoryStructures()}
     * @param previousValidationMd5sum May be {@code null}
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ValidateAndImportResult validateAndImport(File metadataFile, String directoryStructureName, boolean align, boolean ignoreWarnings, String previousValidationMd5sum, String ticketNumber) {
        MetadataValidationContext context = validate(metadataFile, directoryStructureName)
        MetaDataFile metadataFileObject = null
        if (mayImport(context, ignoreWarnings, previousValidationMd5sum)) {
            metadataFileObject = importMetadataFile(context, align, ticketNumber)
        }
        return new ValidateAndImportResult(context, metadataFileObject)
    }

    protected Collection<MetadataValidator> getMetadataValidators() {
        return applicationContext.getBeansOfType(MetadataValidator).values().sort { it.getClass().name }
    }

    protected static String getDirectoryStructureBeanName(String directoryStructureName, File metadataFile) {
        if (directoryStructureName == AUTO_DETECT_DIRECTORY_STRUCTURE_NAME) {
            // TODO: Really do auto-detection based on the metadata file path when more directories structures are supported
            return DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME
        } else {
            return directoryStructureName
        }
    }

    protected DirectoryStructure getDirectoryStructure(String directoryStructureBeanName) {
        return applicationContext.getBean(directoryStructureBeanName, DirectoryStructure)
    }

    protected static boolean mayImport(MetadataValidationContext context, boolean ignoreWarnings, String previousValidationMd5sum) {
        Level maxLevel = context.maximumProblemLevel
        if (maxLevel.intValue() < Level.WARNING.intValue()) {
            return true
        } else if (maxLevel == Level.WARNING && ignoreWarnings) {
            if (context.metadataFileMd5sum.equalsIgnoreCase(previousValidationMd5sum)) {
                return true
            } else {
                context.addProblem(Collections.emptySet(), Level.INFO, 'Not ignoring warnings, because the metadata file has changed since the previous validation.')
            }
        }
        return false
    }

    protected MetaDataFile importMetadataFile(MetadataValidationContext context, boolean align, String ticketNumber) {
        RunSegment runSegment = new RunSegment(
                metaDataStatus: RunSegment.Status.COMPLETE,
                allFilesUsed: true,
                align: align,
                filesStatus: RunSegment.FilesStatus.NEEDS_INSTALLATION,
                initialFormat: RunSegment.DataFormat.FILES_IN_DIRECTORY,
                currentFormat: RunSegment.DataFormat.FILES_IN_DIRECTORY,
                mdPath: context.metadataFile.parentFile.parent,
                otrsTicket: ticketNumber ? trackingService.createOrResetOtrsTicket(ticketNumber) : null,
        )
        // TODO OTP-1952: un-comment
        //assert runSegment.save(flush: true)

        importRuns(context, runSegment, context.spreadsheet.dataRows)

        MetaDataFile metaDataFile = new MetaDataFile(
                fileName: context.metadataFile.name,
                filePath: context.metadataFile.parent,
                used: true,
                md5sum: context.metadataFileMd5sum,
                runSegment: runSegment,
        )
        assert metaDataFile.save(flush: true)

        return metaDataFile
    }

    private void importRuns(MetadataValidationContext context, RunSegment runSegment, Collection<Row> metadataFileRows) {
        metadataFileRows.groupBy { it.getCellByColumnTitle(RUN_ID.name()).text }.each { String runName, List<Row> rows ->
            Run run = Run.findOrSaveWhere(
                    name: runName,
                    seqCenter: exactlyOneElement(SeqCenter.findAllWhere(name: uniqueColumnValue(rows, CENTER_NAME))),
                    seqPlatform: SeqPlatformService.findSeqPlatform(
                            uniqueColumnValue(rows, INSTRUMENT_PLATFORM),
                            uniqueColumnValue(rows, INSTRUMENT_MODEL),
                            uniqueColumnValue(rows, SEQUENCING_KIT) ?: null),
                    dateExecuted: Objects.requireNonNull(
                            RunDateParserService.parseDate('yyyy-MM-dd', uniqueColumnValue(rows, RUN_DATE))),
            )

            // TODO OTP-1952: delete the following 3 lines
            assert runSegment.run == null
            runSegment.run = run
            assert runSegment.save(flush: true)

            importSeqTracks(context, runSegment, run, rows)
        }
    }

    private void importSeqTracks(MetadataValidationContext context, RunSegment runSegment, Run run, Collection<Row> runRows) {
        runRows.groupBy { MultiplexingService.combineLaneNumberAndBarcode(it.getCellByColumnTitle(LANE_NO.name()).text,
                extractBarcode(it).value) }.each { String laneId, List<Row> rows ->
            SeqType seqType = exactlyOneElement(SeqType.findAllWhere(
                    name: uniqueColumnValue(rows, SEQUENCING_TYPE) + (uniqueColumnValue(rows, TAGMENTATION_BASED_LIBRARY) ? '_TAGMENTATION' : ''),
                    libraryLayout: uniqueColumnValue(rows, LIBRARY_LAYOUT),
            ))
            SeqTypeNames seqTypeName = seqType.seqTypeName
            String pipelineVersionString = uniqueColumnValue(rows, PIPELINE_VERSION) ?: 'unknown'
            String sampleIdString = uniqueColumnValue(rows, SAMPLE_ID)
            String libPrepKitString = uniqueColumnValue(rows, LIB_PREP_KIT)
            InformationReliability kitInfoReliability
            LibraryPreparationKit libraryPreparationKit = null
            if (!libPrepKitString) {
                assert seqTypeName != SeqTypeNames.EXOME
                kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED
            } else if (libPrepKitString == InformationReliability.UNKNOWN_VERIFIED.rawValue) {
                kitInfoReliability = InformationReliability.UNKNOWN_VERIFIED
            } else {
                kitInfoReliability = InformationReliability.KNOWN
                libraryPreparationKit = Objects.requireNonNull(
                        LibraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(libPrepKitString))
            }
            String libraryName = uniqueColumnValue(rows, CUSTOMER_LIBRARY) ?: ""
            String normalizedLibraryName = SeqTrack.normalizeLibraryName(libraryName)
            String adapterFileName = uniqueColumnValue(rows, ADAPTER_FILE)
            AdapterFile adapterFile = adapterFileName ? CollectionUtils.exactlyOneElement(AdapterFile.findAllByFileName(adapterFileName)) : null
            Map properties = [
                    laneId: laneId,
                    ilseId: uniqueColumnValue(rows, ILSE_NO) ?: null,
                    // TODO OTP-2050: Use a different fallback value?
                    insertSize: tryParseInt(uniqueColumnValue(rows, INSERT_SIZE), 0),
                    run: run,
                    sample: (atMostOneElement(SampleIdentifier.findAllWhere(name: sampleIdString)) ?:
                            sampleIdentifierService.parseAndFindOrSaveSampleIdentifier(sampleIdString)).sample,
                    seqType: seqType,
                    seqPlatform: run.seqPlatform,
                    pipelineVersion: SoftwareToolService.getBaseCallingTool(pipelineVersionString).softwareTool,
                    kitInfoReliability: kitInfoReliability,
                    libraryPreparationKit: libraryPreparationKit,
                    libraryName: libraryName,
                    normalizedLibraryName: normalizedLibraryName,
                    adapterFile: adapterFile,
            ]
            if (seqTypeName == SeqTypeNames.CHIP_SEQ) {
                properties['antibodyTarget'] = exactlyOneElement(AntibodyTarget.findAllByNameIlike(
                        escapeForSqlLike(uniqueColumnValue(rows, ANTIBODY_TARGET))))
                properties['antibody'] = uniqueColumnValue(rows, ANTIBODY) ?: null
            }

            SeqTrack seqTrack = (seqTypeName?.factory ?: SeqTrack.FACTORY).call(properties)
            assert seqTrack.save(flush: true)

            importDataFiles(context, runSegment, seqTrack, rows)

            boolean willBeAligned = seqTrackService.decideAndPrepareForAlignment(seqTrack)
            seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, willBeAligned)
        }
    }

    private static void importDataFiles(MetadataValidationContext context, RunSegment runSegment, SeqTrack seqTrack, Collection<Row> seqTrackRows) {
        Map<Integer, Collection<Row>> seqTrackRowsByMateNumber =
                seqTrackRows.groupBy { Integer.valueOf(extractMateNumber(it).value) }
        assert seqTrackRows.size() == (
                LibraryLayout.values().find { it.name() == seqTrack.seqType.libraryLayout }?.mateCount
                        ?: seqTrackRowsByMateNumber.keySet().max()
        )
        seqTrackRowsByMateNumber.each { Integer mateNumber, List<Row> rows ->
            Row row = exactlyOneElement(rows)
            File file = context.directoryStructure.getDataFilePath(context, row)
            DataFile dataFile = new DataFile(
                    pathName: '',
                    fileName: file.name,
                    initialDirectory: file.parent,
                    vbpFileName: file.name,
                    md5sum: row.getCellByColumnTitle(MD5.name()).text.toLowerCase(Locale.ENGLISH),
                    project: seqTrack.project,
                    dateExecuted: seqTrack.run.dateExecuted,
                    used: true,
                    mateNumber: mateNumber,
                    run: seqTrack.run,
                    runSegment: runSegment,
                    seqTrack: seqTrack,
                    fileType: FileTypeService.getFileType(file.name, FileType.Type.SEQUENCE),
            )
            assert dataFile.save(flush: true)

            assert new File(LsdfFilesService.getFileInitialPath(dataFile)) == file

            importMetadataEntries(context, dataFile, row)
        }
    }

    private static void importMetadataEntries(MetadataValidationContext context, DataFile dataFile, Row row) {
        for (Cell it : context.spreadsheet.header.cells) {
            assert new MetaDataEntry(
                    dataFile: dataFile,
                    key: MetaDataKey.findOrSaveWhere(name: it.text),
                    value: row.cells[it.columnIndex].text,
                    source: MetaDataEntry.Source.MDFILE,
            ).save(flush: true)
        }
    }

    private static Integer tryParseInt(String string, Integer fallbackValue) {
        try {
            return Integer.valueOf(string?.trim())
        } catch (NumberFormatException e) {
            return fallbackValue
        }
    }

    private static Long tryParseLong(String string, Long fallbackValue) {
        try {
            return Long.valueOf(string?.trim())
        } catch (NumberFormatException e) {
            return fallbackValue
        }
    }

    private static uniqueColumnValue(Collection<Row> rows, MetaDataColumn column) {
        Column col = rows.first().spreadsheet.getColumn(column.name())
        return uniqueColumnValue(rows, col)
    }

    private static uniqueColumnValue(Collection<Row> rows, Column column) {
        return exactlyOneElement(rows*.getCell(column)*.text.unique())
    }

    public static ExtractedValue extractBarcode(Row row) {
        String barcode = null
        Set<Cell> cells = [] as Set

        Cell barcodeCell = row.getCellByColumnTitle(BARCODE.name())
        if (barcodeCell) {
            cells.add(barcodeCell)
            barcode = barcodeCell.text ?: null
        }

        Cell filenameCell = row.getCellByColumnTitle(FASTQ_FILE.name())
        if (filenameCell) {
            String barcodeFromFilename = MultiplexingService.barcode(filenameCell.text)
            if (barcodeFromFilename) {
                if (!barcode?.contains(barcodeFromFilename)) {
                    cells.add(filenameCell)
                    if (!barcode) {
                        barcode = barcodeFromFilename
                    } else {
                        // Yes, this is what the metadata import currently does
                        barcode = MultiplexingService.combineLaneNumberAndBarcode(barcode, barcodeFromFilename)
                    }
                }
            } else if (!barcode) {
                cells.add(filenameCell)
            }
        }

        if (cells) {
            return new ExtractedValue(barcode, cells)
        } else {
            return null
        }
    }

    public static ExtractedValue extractMateNumber(Row row) {
        Cell libraryLayoutCell = row.getCellByColumnTitle(LIBRARY_LAYOUT.name())
        Cell mateNumberCell = row.getCellByColumnTitle(MATE.name())
        int mateNumber

        if (mateNumberCell) {
            try {
                mateNumber = (mateNumberCell.text).toInteger()
            } catch (NumberFormatException ignored) {
                return null
            }
            return new ExtractedValue(Integer.toString(mateNumber), [mateNumberCell] as Set)
        }

        if (libraryLayoutCell && LibraryLayout.values().find { it.name() == libraryLayoutCell.text }?.mateCount == 1) {
            return new ExtractedValue('1', [libraryLayoutCell] as Set)
        }
        Cell filenameCell = row.getCellByColumnTitle(FASTQ_FILE.name())
        if (!filenameCell) {
            return null
        }
        try {
            mateNumber = MetaDataService.findOutMateNumber(filenameCell.text)
        } catch (RuntimeException e) {
            if (e.message == "cannot find mateNumber for ${filenameCell.text}".toString()) {
                return null
            } else {
                throw e
            }
        }
        return new ExtractedValue(Integer.toString(mateNumber), [filenameCell] as Set)
    }

    public static String getSeqTypeNameFromMetadata(ValueTuple tuple) {
        return tuple.getValue(SEQUENCING_TYPE.name()) + (tuple.getValue(TAGMENTATION_BASED_LIBRARY.name()) ? '_TAGMENTATION' : '')
    }

}

@TupleConstructor
class ExtractedValue {

    final String value

    /**
     * The cells the value has been extracted from
     */
    final Set<Cell> cells

}

@TupleConstructor
class ValidateAndImportResult {

    final MetadataValidationContext context

    /**
     * {@code null} if the import has been rejected
     */
    final MetaDataFile metadataFile
}
