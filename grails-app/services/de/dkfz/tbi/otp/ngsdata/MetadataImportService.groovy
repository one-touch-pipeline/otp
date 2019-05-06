/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.ngsdata

import com.jcraft.jsch.JSchException
import grails.gorm.transactions.Transactional
import groovy.transform.ToString
import groovy.transform.TupleConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.TrackingService
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple

import java.nio.file.*
import java.util.regex.Matcher

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * Metadata import 2.0 (OTP-34)
 */
@Transactional
class MetadataImportService {

    @TupleConstructor
    @ToString
    static class PathWithMd5sum {
        final Path path
        final String md5sum
    }

    @Autowired
    ApplicationContext applicationContext

    SampleIdentifierService sampleIdentifierService
    SeqTrackService seqTrackService
    TrackingService trackingService
    FileSystemService fileSystemService
    SeqPlatformService seqPlatformService
    @Autowired
    RemoteShellHelper remoteShellHelper
    LsdfFilesService lsdfFilesService
    LibraryPreparationKitService libraryPreparationKitService
    SeqTypeService seqTypeService
    ConfigService configService
    MailHelperService mailHelperService
    ProcessingOptionService processingOptionService
    FileService fileService
    AntibodyTargetService antibodyTargetService

    static int MAX_ILSE_NUMBER_RANGE_SIZE = 20

    static final String AUTO_DETECT_DIRECTORY_STRUCTURE_NAME = ''
    static final String DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME = 'dataFilesInSameDirectory'
    static final String MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME = 'dataFilesOnGpcfMidTerm'


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
        FileSystem fs = fileSystemService.getFilesystemForFastqImport()
        return validate(fs.getPath(metadataFile.path), directoryStructureName)
    }

    MetadataValidationContext validate(Path metadataFile, String directoryStructureName) {
        MetadataValidationContext context = MetadataValidationContext.createFromFile(
                metadataFile,
                getDirectoryStructure(getDirectoryStructureBeanName(directoryStructureName)),
        )
        if (context.spreadsheet) {
            Long hash = System.currentTimeMillis()
            metadataValidators.each {
                Long startTime = System.currentTimeMillis()
                it.validate(context)
                log.debug("finished ${it.getClass()} took ${System.currentTimeMillis() - startTime}ms validation started : ${hash}")

            }
        }
        return context
    }

    /**
     * @param directoryStructureName As returned by {@link #getSupportedDirectoryStructures()}
     * @param previousValidationMd5sum May be {@code null}
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<ValidateAndImportResult> validateAndImportWithAuth(List<PathWithMd5sum> metadataPaths, String directoryStructureName, boolean align,
                                                            boolean ignoreWarnings, String ticketNumber, String seqCenterComment,
                                                            boolean automaticNotification) {
        try {
            Map<MetadataValidationContext, String> contexts = metadataPaths.collectEntries { PathWithMd5sum pathWithMd5sum ->
                return [(validate(pathWithMd5sum.path, directoryStructureName)): pathWithMd5sum.md5sum]
            }
            List<ValidateAndImportResult> results = contexts.collect { context, md5sum ->
                return importHelperMethod(context, align, RunSegment.ImportMode.MANUAL, ignoreWarnings, md5sum, ticketNumber, seqCenterComment,
                        automaticNotification)
            }
            return results
        } catch (Exception e) {
            if (!e.message.startsWith('Copying of metadata file')) {
                String recipientsString = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_ERRORS)
                if (recipientsString) {
                    mailHelperService.sendEmail("Error: while importing metadata file", "Metadata paths: ${metadataPaths*.path.join('\n')}" +
                            "${e.getLocalizedMessage()}\n${e.getCause()}", recipientsString)
                }
            }
            throw new RuntimeException("Error while importing metadata file with paths: ${metadataPaths*.path.join('\n')}", e)
        }
    }

    private ValidateAndImportResult importHelperMethod(MetadataValidationContext context, boolean align, RunSegment.ImportMode importMode,
                                                       boolean ignoreWarnings, String previousValidationMd5sum, String ticketNumber, String seqCenterComment,
                                                       boolean automaticNotification) {
        MetaDataFile metadataFileObject = null
        if (mayImport(context, ignoreWarnings, previousValidationMd5sum)) {
            metadataFileObject = importMetadataFile(context, align, importMode, ticketNumber, seqCenterComment, automaticNotification)
            copyMetadataFileIfRequested(context)
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

    protected void copyMetadataFileIfRequested(MetadataValidationContext context) {
        List<SeqCenter> seqCenters = getSeqCenters(context)
        seqCenters.findAll { it?.copyMetadataFile }.unique().each { SeqCenter seqCenter ->
            Path source = context.metadataFile
            try {
                String ilse = context.spreadsheet.dataRows[0].getCellByColumnTitle(ILSE_NO.name()).text
                Path targetDirectory = getIlseFolder(ilse, seqCenter)
                Path targetFile = targetDirectory.resolve(source.fileName.toString())
                if (!Files.exists(targetFile)) {
                    fileService.createFileWithContent(targetFile, context.content)
                }

                assert Files.readAllBytes(targetFile) == context.content
            } catch (Throwable t) {
                String recipientsString = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_ERRORS)
                if (recipientsString) {
                    mailHelperService.sendEmail("Error: Copying of metadatafile ${source} failed",
                            "${t.getLocalizedMessage()}\n${t.getCause()}", recipientsString)
                }
                throw new RuntimeException("Copying of metadata file ${source} failed", t)
            }
        }
    }

    static  List<SeqCenter> getSeqCenters(MetadataValidationContext context) {
        List<String> centerNames = context.spreadsheet.dataRows*.getCellByColumnTitle(CENTER_NAME.name())?.text
        return centerNames ? SeqCenter.findAllByNameInList(centerNames) : []
    }

    List<ValidateAndImportResult> validateAndImportMultiple(String otrsTicketNumber, String ilseNumbers) {
        FileSystem fs = fileSystemService.getFilesystemForFastqImport()
        return validateAndImportMultiple(otrsTicketNumber,
                parseIlseNumbers(ilseNumbers).collect { getMetadataFilePathForIlseNumber(it, fs) },
                MIDTERM_ILSE_DIRECTORY_STRUCTURE_BEAN_NAME
        )
    }

    List<ValidateAndImportResult> validateAndImportMultiple(String otrsTicketNumber, List<Path> metadataFiles, String directoryStructureName) {
        List<MetadataValidationContext> contexts = metadataFiles.collect {
            return validate(it, directoryStructureName)
        }
        List<ValidateAndImportResult> results = contexts.collect {
            return importHelperMethod(it, true, RunSegment.ImportMode.AUTOMATIC, false, null, otrsTicketNumber,
                    null, true)
        }
        List<MetadataValidationContext> failedValidations = results.findAll { it.metadataFile == null }*.context
        if (failedValidations.isEmpty()) {
            return results
        } else {
            throw new MultiImportFailedException(failedValidations, metadataFiles)
        }
    }

    /**
     * Returns the absolute path to an ILSe Folder inside the sequencing center inbox
     */
    Path getIlseFolder(String ilseId, SeqCenter seqCenter) throws JSchException {
        assert seqCenter
        if (!(ilseId =~ /^\d{4,6}$/)) {
            return null
        }
        String ilse = ilseId.padLeft(6, '0')
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(configService.getDefaultRealm())

        return fileSystem.getPath("${configService.getSeqCenterInboxPath()}/${seqCenter.dirName}/${ilse[0..2]}/${ilse}")

    }

    protected static Path getMetadataFilePathForIlseNumber(int ilseNumber, FileSystem fileSystem) {
        String ilseNumberString = Integer.toString(ilseNumber)
        SeqCenter seqCenter = exactlyOneElement(SeqCenter.findAllByAutoImportable(true))
        return fileSystem.getPath(seqCenter.autoImportDir,
                ilseNumberString.padLeft(6, '0'),
                "data",
                "${ilseNumberString}_meta.tsv"
        )
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

    protected static String getDirectoryStructureBeanName(String directoryStructureName) {
        if (directoryStructureName == AUTO_DETECT_DIRECTORY_STRUCTURE_NAME) {
            return DATA_FILES_IN_SAME_DIRECTORY_BEAN_NAME
        } else {
            return directoryStructureName
        }
    }

    protected DirectoryStructure getDirectoryStructure(String directoryStructureBeanName) {
        DirectoryStructure directoryStructure = applicationContext.getBean(directoryStructureBeanName, DirectoryStructure)
        directoryStructure.setFileSystem(fileSystemService?.getFilesystemForFastqImport())
        return directoryStructure
    }

    static boolean mayImport(AbstractMetadataValidationContext context, boolean ignoreWarnings, String previousValidationMd5sum) {
        java.util.logging.Level maxLevel = context.maximumProblemLevel
        if (maxLevel.intValue() < Level.WARNING.intValue()) {
            return true
        } else if (maxLevel == Level.WARNING && ignoreWarnings) {
            if (context.metadataFileMd5sum.equalsIgnoreCase(previousValidationMd5sum)) {
                return true
            } else {
                context.addProblem(Collections.emptySet(), Level.INFO,
                        'Not ignoring warnings, because the metadata file has changed since the previous validation.')
            }
        }
        return false
    }

    protected MetaDataFile importMetadataFile(MetadataValidationContext context, boolean align, RunSegment.ImportMode importMode, String ticketNumber,
                                              String seqCenterComment, boolean automaticNotification) {
        Long timeImportStarted = System.currentTimeMillis()
        log.debug('import started')
        RunSegment runSegment = new RunSegment(
                align: align,
                otrsTicket: ticketNumber ? trackingService.createOrResetOtrsTicket(ticketNumber, seqCenterComment, automaticNotification) : null,
                importMode: importMode,
        )
        assert runSegment.save(flush: false)
        Long timeStarted = System.currentTimeMillis()
        log.debug('runs stared')
        importRuns(context, runSegment, context.spreadsheet.dataRows)
        log.debug("runs stopped took: ${System.currentTimeMillis() - timeStarted}")

        MetaDataFile metaDataFile = new MetaDataFile(
                fileName: context.metadataFile.fileName.toString(),
                filePath: context.metadataFile.parent.toString(),
                md5sum: context.metadataFileMd5sum,
                runSegment: runSegment,
        )
        assert metaDataFile.save(flush: true)

        Long timeSamplePairCreationStarted = System.currentTimeMillis()
        log.debug('sample pair stared')
        List<SamplePair> samplePairs = SamplePair.findMissingDiseaseControlSamplePairs()
        samplePairs*.save(flush: true)
        log.debug("sample pair stopped:  ${System.currentTimeMillis() - timeSamplePairCreationStarted}")

        log.debug("import stopped ${metaDataFile.fileName}:  ${System.currentTimeMillis() - timeImportStarted}")
        return metaDataFile
    }

    private void importRuns(MetadataValidationContext context, RunSegment runSegment, Collection<Row> metadataFileRows) {
        metadataFileRows.groupBy { it.getCellByColumnTitle(RUN_ID.name()).text }.each { String runName, List<Row> rows ->
            Run run = Run.findWhere(
                    name: runName,
                    seqCenter: exactlyOneElement(SeqCenter.findAllWhere(name: uniqueColumnValue(rows, CENTER_NAME))),
                    seqPlatform: seqPlatformService.findSeqPlatform(
                            uniqueColumnValue(rows, INSTRUMENT_PLATFORM),
                            uniqueColumnValue(rows, INSTRUMENT_MODEL),
                            uniqueColumnValue(rows, SEQUENCING_KIT) ?: null),
                    dateExecuted: Objects.requireNonNull(
                            RunDateParserService.parseDate('yyyy-MM-dd', uniqueColumnValue(rows, RUN_DATE))),
            )
            if (!run) {
                run = new Run(
                        name: runName,
                        seqCenter: exactlyOneElement(SeqCenter.findAllWhere(name: uniqueColumnValue(rows, CENTER_NAME))),
                        seqPlatform: seqPlatformService.findSeqPlatform(
                                uniqueColumnValue(rows, INSTRUMENT_PLATFORM),
                                uniqueColumnValue(rows, INSTRUMENT_MODEL),
                                uniqueColumnValue(rows, SEQUENCING_KIT) ?: null),
                        dateExecuted: Objects.requireNonNull(
                                RunDateParserService.parseDate('yyyy-MM-dd', uniqueColumnValue(rows, RUN_DATE))),
                )
                run.save(flush: true)
            }

            Long timeStarted = System.currentTimeMillis()
            log.debug('seqTracks started')
            importSeqTracks(context, runSegment, run, rows)
            log.debug("seqTracks stopped took: ${System.currentTimeMillis() - timeStarted}")
        }
    }

    private void importSeqTracks(MetadataValidationContext context, RunSegment runSegment, Run run, Collection<Row> runRows) {
        int amountOfRows = runRows.size() / 2
        runRows.groupBy {
            MultiplexingService.combineLaneNumberAndBarcode(it.getCellByColumnTitle(LANE_NO.name()).text, extractBarcode(it).value)
        }.eachWithIndex { String laneId, List<Row> rows, int index ->
            String projectName = uniqueColumnValue(rows, PROJECT)
            Project project = Project.getByNameOrNameInMetadataFiles(projectName)
            String ilseNumber = uniqueColumnValue(rows, ILSE_NO)
            String seqTypeRaw = uniqueColumnValue(rows, SEQUENCING_TYPE)
            String tagmentationRaw = uniqueColumnValue(rows, TAGMENTATION_BASED_LIBRARY)?.toLowerCase()
            String baseMaterial = uniqueColumnValue(rows, BASE_MATERIAL)
            boolean isSingleCell = SeqTypeService.isSingleCell(baseMaterial)
            LibraryLayout libLayout = LibraryLayout.findByName(uniqueColumnValue(rows, LIBRARY_LAYOUT))

            SeqType seqType = seqTypeService.findByNameOrImportAlias(
                    seqTypeMaybeTagmentationName(seqTypeRaw, tagmentationRaw),
                    [libraryLayout: libLayout, singleCell: isSingleCell]
            )
            String pipelineVersionString = uniqueColumnValue(rows, PIPELINE_VERSION) ?: 'unknown'
            String sampleIdString = uniqueColumnValue(rows, SAMPLE_ID)
            String libPrepKitString = uniqueColumnValue(rows, LIB_PREP_KIT)
            InformationReliability kitInfoReliability
            LibraryPreparationKit libraryPreparationKit = null
            if (!libPrepKitString) {
                assert !seqType.isExome()
                kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED
            } else if (libPrepKitString == InformationReliability.UNKNOWN_VERIFIED.rawValue) {
                kitInfoReliability = InformationReliability.UNKNOWN_VERIFIED
            } else {
                kitInfoReliability = InformationReliability.KNOWN
                libraryPreparationKit = Objects.requireNonNull(
                        libraryPreparationKitService.findByNameOrImportAlias(libPrepKitString))
            }
            String libraryName = uniqueColumnValue(rows, CUSTOMER_LIBRARY) ?: ""
            String normalizedLibraryName = SeqTrack.normalizeLibraryName(libraryName)
            IlseSubmission ilseSubmission
            if (ilseNumber) {
                ilseSubmission = IlseSubmission.findWhere(ilseNumber: Integer.parseInt(ilseNumber))
                if (!ilseSubmission) {
                    ilseSubmission = new IlseSubmission(ilseNumber: Integer.parseInt(ilseNumber))
                    ilseSubmission.save(flush: true)
                }
            } else {
                ilseSubmission = null
            }

            Map properties = [
                    laneId: laneId,
                    ilseSubmission: ilseSubmission,
                    // TODO OTP-2050: Use a different fallback value?
                    insertSize: tryParseInt(uniqueColumnValue(rows, INSERT_SIZE), 0),
                    run: run,
                    sample: (atMostOneElement(SampleIdentifier.findAllWhere(name: sampleIdString)) ?:
                            sampleIdentifierService.parseAndFindOrSaveSampleIdentifier(sampleIdString, project)).sample,
                    seqType: seqType,
                    pipelineVersion: SoftwareToolService.getBaseCallingTool(pipelineVersionString).softwareTool,
                    kitInfoReliability: kitInfoReliability,
                    libraryPreparationKit: libraryPreparationKit,
                    libraryName: libraryName,
                    normalizedLibraryName: normalizedLibraryName,
            ]
            if (seqType.hasAntibodyTarget) {
                properties['antibodyTarget'] = antibodyTargetService.findByNameOrImportAlias(uniqueColumnValue(rows, ANTIBODY_TARGET))
                properties['antibody'] = uniqueColumnValue(rows, ANTIBODY) ?: null
            }

            if (isSingleCell) {
                properties['cellPosition'] = sampleIdentifierService.parseCellPosition(sampleIdString, project)
            }

            SeqTrack seqTrack = seqType.isExome() ? new ExomeSeqTrack(properties) :
                    seqType.hasAntibodyTarget ? new ChipSeqSeqTrack(properties) :
                            new SeqTrack(properties)
            assert seqTrack.save(flush: false)

            Long timeStarted = System.currentTimeMillis()
            log.debug("dataFiles started ${index}/${amountOfRows}")
            importDataFiles(context, runSegment, seqTrack, rows)
            log.debug("dataFiles stopped took: ${System.currentTimeMillis() - timeStarted}")
            assert seqTrack.save(flush: true) //needs to flush the session, so seqTrackService.decideAndPrepareForAlignment can work

            boolean willBeAligned = seqTrackService.decideAndPrepareForAlignment(seqTrack)
            seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, willBeAligned)
        }
    }

    private static void importDataFiles(MetadataValidationContext context, RunSegment runSegment, SeqTrack seqTrack, Collection<Row> seqTrackRows) {
        Map<Integer, Collection<Row>> seqTrackRowsByMateNumber = seqTrackRows.groupBy { Integer.valueOf(extractMateNumber(it).value) }
        assert seqTrackRows.size() == seqTrack.seqType.libraryLayout.mateCount

        seqTrackRowsByMateNumber.each { Integer mateNumber, List<Row> rows ->
            Row row = exactlyOneElement(rows)
            Path file = context.directoryStructure.getDataFilePath(context, row)
            DataFile dataFile = new DataFile(
                    pathName: '',
                    fileName: file.fileName.toString(),
                    initialDirectory: file.parent.toString(),
                    vbpFileName: file.fileName.toString(),
                    md5sum: row.getCellByColumnTitle(MD5.name()).text.toLowerCase(Locale.ENGLISH),
                    project: seqTrack.project,
                    dateExecuted: seqTrack.run.dateExecuted,
                    used: true,
                    mateNumber: mateNumber,
                    run: seqTrack.run,
                    runSegment: runSegment,
                    seqTrack: seqTrack,
                    fileType: FileTypeService.getFileType(file.fileName.toString(), FileType.Type.SEQUENCE),
            )
            assert dataFile.save(flush: false)

            assert new File(LsdfFilesService.getFileInitialPath(dataFile)) == new File(file.toString())

            importMetadataEntries(context, dataFile, row)
        }
    }

    private static void importMetadataEntries(MetadataValidationContext context, DataFile dataFile, Row row) {
        for (Cell it : context.spreadsheet.header.cells) {
            MetaDataKey metaDataKey = MetaDataKey.findWhere(name: it.text)
            if (!metaDataKey) {
                metaDataKey = new MetaDataKey(name: it.text)
                metaDataKey.save(flush: true)
            }
            assert new MetaDataEntry(
                    dataFile: dataFile,
                    key: metaDataKey,
                    value: row.cells[it.columnIndex].text,
                    source: MetaDataEntry.Source.MDFILE,
            ).save(flush: false)
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

    static ExtractedValue extractBarcode(Row row) {
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

    static ExtractedValue extractMateNumber(Row row) {
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
        return null
    }

    static String getSeqTypeNameFromMetadata(ValueTuple tuple) {
        String tagmentation = tuple.getValue(TAGMENTATION_BASED_LIBRARY.name())?.toLowerCase()
        String seqType = tuple.getValue(SEQUENCING_TYPE.name())
        return seqTypeMaybeTagmentationName(seqType, tagmentation)
    }

    /** small helper to parse seqtypes and boolean-like uservalues into a proper OTP name */
    static String seqTypeMaybeTagmentationName(String seqType, String tagmentationRawValue) {
        boolean isTagmentation = tagmentationRawValue in ["1", "true"]
        return seqType + (isTagmentation && !seqType.endsWith(SeqType.TAGMENTATION_SUFFIX) ? SeqType.TAGMENTATION_SUFFIX : '')
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

    final List<Path> allPaths

}
