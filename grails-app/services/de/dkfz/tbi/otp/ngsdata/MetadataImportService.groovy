package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import grails.util.*
import groovy.transform.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*
import org.springframework.security.access.prepost.*

import java.util.logging.*
import java.util.regex.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static de.dkfz.tbi.otp.utils.StringUtils.*

/**
 * Metadata import 2.0 (OTP-34)
 */
class MetadataImportService {

    ExecutionService executionService

    LsdfFilesService lsdfFilesService

    static int MAX_ILSE_NUMBER_RANGE_SIZE = 20

    static final String AUTO_DETECT_DIRECTORY_STRUCTURE_NAME = ''
    static final String DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME = 'dataFilesInSameDirectory'
    static final String MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME = 'dataFilesOnGpcfMidTerm'

    @Autowired
    ApplicationContext applicationContext

    SampleIdentifierService sampleIdentifierService
    SeqTrackService seqTrackService
    TrackingService trackingService

    /**
     * @return A collection of descriptions of the validations which are performed
     */
    Collection<String> getImplementedValidations() {
        return (Collection<String>) metadataValidators.sum { it.descriptions }
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
    MetadataValidationContext validateWithAuth(File metadataFile, String directoryStructureName) {
        return validate(metadataFile, directoryStructureName)
    }

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
    ValidateAndImportResult validateAndImportWithAuth(File metadataFile, String directoryStructureName, boolean align, boolean ignoreWarnings, String previousValidationMd5sum, String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        return validateAndImport(metadataFile, directoryStructureName, align, RunSegment.ImportMode.MANUAL, ignoreWarnings, previousValidationMd5sum, ticketNumber, seqCenterComment, automaticNotification)
    }

    ValidateAndImportResult validateAndImport(File metadataFile, String directoryStructureName, boolean align, RunSegment.ImportMode importMode, boolean ignoreWarnings, String previousValidationMd5sum, String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        MetadataValidationContext context = validate(metadataFile, directoryStructureName)
        MetaDataFile metadataFileObject = null
        if (mayImport(context, ignoreWarnings, previousValidationMd5sum)) {
            metadataFileObject = importMetadataFile(context, align, importMode, ticketNumber, seqCenterComment, automaticNotification)
            copyMetaDataFileIfMidterm(context)
        }
        return new ValidateAndImportResult(context, metadataFileObject)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateAutomaticNotificationFlag(OtrsTicket otrsTicket, boolean automaticNotification) {
        otrsTicket.automaticNotification = automaticNotification
        assert otrsTicket.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateFinalNotificationFlag(OtrsTicket otrsTicket, boolean finalNotificationSent) {
        otrsTicket.finalNotificationSent = finalNotificationSent
        assert otrsTicket.save(flush: true)
    }

    protected void copyMetaDataFileIfMidterm(MetadataValidationContext context) {
        if (context.directoryStructure instanceof DataFilesOnGpcfMidTerm) {
            File source = context.metadataFile
            try {
                String ilse = context.spreadsheet.dataRows[0].getCellByColumnTitle(MetaDataColumn.ILSE_NO.name()).text
                File targetDirectory = lsdfFilesService.getIlseFolder(ilse)
                File targetFile = new File(targetDirectory, source.name)
                if (!targetFile.exists()) {
                    Realm realm = Realm.findByNameAndEnv(Realm.LATEST_DKFZ_REALM, Environment.getCurrent().getName())
                    assert realm

                    lsdfFilesService.createDirectory(targetDirectory, realm)
                    executionService.executeCommandReturnProcessOutput(realm, "cp ${source} ${targetDirectory}").assertExitCodeZeroAndStderrEmpty()
                    LsdfFilesService.ensureFileIsReadableAndNotEmpty(targetFile)
                }

                assert targetFile.bytes == context.content
            } catch (Throwable t) {
                throw new RuntimeException("Copying of metadatafile ${source} failed", t)
            }
        }
    }

    List<ValidateAndImportResult> validateAndImportMultiple(String otrsTicketNumber, String ilseNumbers) {
        return validateAndImportMultiple(otrsTicketNumber,
                parseIlseNumbers(ilseNumbers).collect { getMetadataFilePathForIlseNumber(it) },
                MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME)
    }

    List<ValidateAndImportResult> validateAndImportMultiple(String otrsTicketNumber, Collection<File> metadataFiles, String directoryStructureName) {
        List<ValidateAndImportResult> results = metadataFiles.collect {
            validateAndImport(it, directoryStructureName, true, RunSegment.ImportMode.AUTOMATIC, false, null, otrsTicketNumber, null, true)
        }
        List<MetadataValidationContext> failedValidations = results.findAll { it.metadataFile == null }*.context
        if (failedValidations.isEmpty()) {
            return results
        } else {
            throw new MultiImportFailedException(failedValidations)
        }
    }

    protected static File getMetadataFilePathForIlseNumber(int ilseNumber) {
        String ilseNumberString = Integer.toString(ilseNumber)
        return new File(LsdfFilesService.midtermStorageMountPoint.first(),
                "${ilseNumberString.padLeft(6, '0')}/data/${ilseNumberString}_meta.tsv")
    }

    protected static List<Integer> parseIlseNumbers(String ilseNumbers) {
        List<Integer> result = []
        ilseNumbers.split(/\s*[,+&]\s*/).each {
            if (it ==~ /^\d{4,6}$/) {
                result.add(Integer.parseInt(it))
            } else {
                Matcher matcher = it =~ /^(\d{4,6})\s*-\s*(\d{4,6})$/
                if (matcher) {
                    int min = Integer.parseInt(matcher.group(1))
                    int max = Integer.parseInt(matcher.group(2))
                    if (min >= max) {
                        throw new IllegalArgumentException("Illegal range of ILSe numbers: '${it}'")
                    }
                    if (max - min - 1 > MAX_ILSE_NUMBER_RANGE_SIZE) {
                        throw new IllegalArgumentException("Range of ILSe numbers is too large: '${it}'")
                    }
                    for (int i = min; i <= max; i++) {
                        result.add(i)
                    }
                } else {
                    throw new IllegalArgumentException("Cannot parse '${it}' as an ILSe number or a range of ILSe numbers.")
                }
            }
        }
        return result
    }

    protected Collection<MetadataValidator> getMetadataValidators() {
        return applicationContext.getBeansOfType(MetadataValidator).values().sort { it.getClass().name }
    }

    protected static String getDirectoryStructureBeanName(String directoryStructureName, File metadataFile) {
        if (directoryStructureName == AUTO_DETECT_DIRECTORY_STRUCTURE_NAME) {
            // TODO: Really do auto-detection based on the metadata file path
            return DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME
        } else {
            return directoryStructureName
        }
    }

    protected DirectoryStructure getDirectoryStructure(String directoryStructureBeanName) {
        return applicationContext.getBean(directoryStructureBeanName, DirectoryStructure)
    }

    public static boolean mayImport(AbstractMetadataValidationContext context, boolean ignoreWarnings, String previousValidationMd5sum) {
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

    protected MetaDataFile importMetadataFile(MetadataValidationContext context, boolean align, RunSegment.ImportMode importMode, String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        RunSegment runSegment = new RunSegment(
                align: align,
                otrsTicket: ticketNumber ? trackingService.createOrResetOtrsTicket(ticketNumber, seqCenterComment, automaticNotification) : null,
                importMode: importMode,
        )
        assert runSegment.save()

        importRuns(context, runSegment, context.spreadsheet.dataRows)

        MetaDataFile metaDataFile = new MetaDataFile(
                fileName: context.metadataFile.name,
                filePath: context.metadataFile.parent,
                md5sum: context.metadataFileMd5sum,
                runSegment: runSegment,
        )
        assert metaDataFile.save()

        List<SamplePair> samplePairs = SamplePair.findMissingDiseaseControlSamplePairs()
        samplePairs*.save()

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

            importSeqTracks(context, runSegment, run, rows)
        }
    }

    private void importSeqTracks(MetadataValidationContext context, RunSegment runSegment, Run run, Collection<Row> runRows) {
        runRows.groupBy { MultiplexingService.combineLaneNumberAndBarcode(it.getCellByColumnTitle(LANE_NO.name()).text,
                extractBarcode(it).value) }.each { String laneId, List<Row> rows ->
            String ilseNumber = uniqueColumnValue(rows, ILSE_NO)
            String tagmentation = uniqueColumnValue(rows, TAGMENTATION_BASED_LIBRARY)?.toLowerCase()
            SeqType seqType = exactlyOneElement(SeqType.findAllWhere(
                    name: uniqueColumnValue(rows, SEQUENCING_TYPE) + ((tagmentation && ["1", "true"].contains(tagmentation)) ? '_TAGMENTATION' : ''),
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
            Map properties = [
                    laneId: laneId,
                    ilseSubmission: ilseNumber ? IlseSubmission.findOrSaveWhere(ilseNumber: Integer.parseInt(ilseNumber)) : null,
                    // TODO OTP-2050: Use a different fallback value?
                    insertSize: tryParseInt(uniqueColumnValue(rows, INSERT_SIZE), 0),
                    run: run,
                    sample: (atMostOneElement(SampleIdentifier.findAllWhere(name: sampleIdString)) ?:
                            sampleIdentifierService.parseAndFindOrSaveSampleIdentifier(sampleIdString)).sample,
                    seqType: seqType,
                    pipelineVersion: SoftwareToolService.getBaseCallingTool(pipelineVersionString).softwareTool,
                    kitInfoReliability: kitInfoReliability,
                    libraryPreparationKit: libraryPreparationKit,
                    libraryName: libraryName,
                    normalizedLibraryName: normalizedLibraryName,
            ]
            if (seqTypeName == SeqTypeNames.CHIP_SEQ) {
                properties['antibodyTarget'] = exactlyOneElement(AntibodyTarget.findAllByNameIlike(
                        escapeForSqlLike(uniqueColumnValue(rows, ANTIBODY_TARGET))))
                properties['antibody'] = uniqueColumnValue(rows, ANTIBODY) ?: null
            }

            SeqTrack seqTrack = (seqTypeName?.factory ?: SeqTrack.FACTORY).call(properties)
            assert seqTrack.save()

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
            assert dataFile.save()

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
            ).save()
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
        } else {
            Cell filenameCell = row.getCellByColumnTitle(FASTQ_FILE.name())
            if (filenameCell) {
                String barcodeFromFilename = MultiplexingService.barcode(filenameCell.text)
                cells.add(filenameCell)
                if (barcodeFromFilename) {
                    barcode = barcodeFromFilename
                }
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
        String tagmentation = tuple.getValue(TAGMENTATION_BASED_LIBRARY.name())?.toLowerCase()
        return tuple.getValue(SEQUENCING_TYPE.name()) + ((tagmentation && ["1", "true"].contains(tagmentation)) ? SeqType.TAGMENTATION_SUFFIX : '')
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

@TupleConstructor
class MultiImportFailedException extends RuntimeException {
    final List<MetadataValidationContext> failedValidations
}
