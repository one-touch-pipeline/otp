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
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
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
    @Autowired
    RemoteShellHelper remoteShellHelper
    AntibodyTargetService antibodyTargetService
    ConfigService configService
    FileService fileService
    FileSystemService fileSystemService
    LibraryPreparationKitService libraryPreparationKitService
    LsdfFilesService lsdfFilesService
    MailHelperService mailHelperService
    MergingCriteriaService mergingCriteriaService
    OtrsTicketService otrsTicketService
    ProcessingOptionService processingOptionService
    ProcessingThresholdsService processingThresholdsService
    SampleIdentifierService sampleIdentifierService
    SamplePairDeciderService samplePairDeciderService
    SampleTypeService sampleTypeService
    SeqPlatformService seqPlatformService
    SeqTrackService seqTrackService
    SeqTypeService seqTypeService


    static int MAX_ILSE_NUMBER_RANGE_SIZE = 20

    static final String MATE_NUMBER_EXPRESSION = /^(?<index>i|I)?(?<number>[1-9]\d*)$/


    /**
     * @return A collection of descriptions of the validations which are performed
     */
    Collection<String> getImplementedValidations() {
        return (Collection<String>) metadataValidators.sum { it.descriptions }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    // TODO: OTP-1908: Relax this restriction
    MetadataValidationContext validateWithAuth(File metadataFile, DirectoryStructureBeanName directoryStructure) {
        FileSystem fs = fileSystemService.filesystemForFastqImport
        return validate(fs.getPath(metadataFile.path), directoryStructure)
    }

    MetadataValidationContext validate(Path metadataFile, DirectoryStructureBeanName directoryStructure) {
        MetadataValidationContext context = MetadataValidationContext.createFromFile(
                metadataFile,
                getDirectoryStructure(directoryStructure),
                directoryStructure.displayName,
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
     * @param previousValidationMd5sum May be {@code null}
     */
    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<ValidateAndImportResult> validateAndImportWithAuth(List<PathWithMd5sum> metadataPaths, DirectoryStructureBeanName directoryStructure, boolean align,
                                                            boolean ignoreWarnings, String ticketNumber, String seqCenterComment,
                                                            boolean automaticNotification) {
        try {
            Map<MetadataValidationContext, String> contexts = metadataPaths.collectEntries { PathWithMd5sum pathWithMd5sum ->
                return [(validate(pathWithMd5sum.path, directoryStructure)): pathWithMd5sum.md5sum]
            }
            List<ValidateAndImportResult> results = contexts.collect { context, md5sum ->
                return importHelperMethod(context, align, FastqImportInstance.ImportMode.MANUAL, ignoreWarnings, md5sum, ticketNumber, seqCenterComment,
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

    private ValidateAndImportResult importHelperMethod(MetadataValidationContext context, boolean align, FastqImportInstance.ImportMode importMode,
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

    static List<SeqCenter> getSeqCenters(MetadataValidationContext context) {
        List<String> centerNames = context.spreadsheet.dataRows*.getCellByColumnTitle(CENTER_NAME.name())?.text
        return centerNames ? SeqCenter.findAllByNameInList(centerNames) : []
    }

    List<ValidateAndImportResult> validateAndImportMultiple(String otrsTicketNumber, String ilseNumbers) {
        FileSystem fs = fileSystemService.filesystemForFastqImport
        return validateAndImportMultiple(
                otrsTicketNumber,
                parseIlseNumbers(ilseNumbers).collect { getMetadataFilePathForIlseNumber(it, fs) },
                DirectoryStructureBeanName.GPCF_SPECIFIC
        )
    }

    List<ValidateAndImportResult> validateAndImportMultiple(String otrsTicketNumber, List<Path> metadataFiles, DirectoryStructureBeanName directoryStructure) {
        List<MetadataValidationContext> contexts = metadataFiles.collect {
            return validate(it, directoryStructure)
        }
        List<ValidateAndImportResult> results = contexts.collect {
            return importHelperMethod(it, true, FastqImportInstance.ImportMode.AUTOMATIC, false, null, otrsTicketNumber, null, true)
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
        if (!(ilseId =~ /^\d{1,6}$/)) {
            return null
        }
        String ilse = ilseId.padLeft(6, '0')
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(configService.getDefaultRealm())

        return fileSystem.getPath("${configService.getSeqCenterInboxPath()}/${seqCenter.dirName}/${ilse[0..2]}/${ilse}")
    }

    protected static Path getMetadataFilePathForIlseNumber(int ilseNumber, FileSystem fileSystem) {
        String ilseNumberString = Integer.toString(ilseNumber)
        SeqCenter seqCenter = exactlyOneElement(SeqCenter.findAllByAutoImportable(true))
        return fileSystem.getPath(
                seqCenter.autoImportDir,
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

    protected DirectoryStructure getDirectoryStructure(DirectoryStructureBeanName name) {
        DirectoryStructure directoryStructure = applicationContext.getBean(name.beanName, DirectoryStructure)
        directoryStructure.setFileSystem(fileSystemService?.filesystemForFastqImport)
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

    protected MetaDataFile importMetadataFile(MetadataValidationContext context, boolean align, FastqImportInstance.ImportMode importMode, String ticketNumber,
                                              String seqCenterComment, boolean automaticNotification) {
        Long timeImportStarted = System.currentTimeMillis()
        log.debug('import started')
        FastqImportInstance fastqImportInstance = new FastqImportInstance(
                otrsTicket: ticketNumber ? otrsTicketService.createOrResetOtrsTicket(ticketNumber, seqCenterComment, automaticNotification) : null,
                importMode: importMode,
        )
        assert fastqImportInstance.save(flush: false)
        Long timeStarted = System.currentTimeMillis()
        log.debug('runs stared')
        importRuns(context, fastqImportInstance, context.spreadsheet.dataRows, align)
        log.debug("runs stopped took: ${System.currentTimeMillis() - timeStarted}")

        fastqImportInstance.refresh()
        notifyAboutUnsetConfig(fastqImportInstance.dataFiles*.seqTrack as List, fastqImportInstance.otrsTicket)

        MetaDataFile metaDataFile = new MetaDataFile(
                fileName           : context.metadataFile.fileName.toString(),
                filePath           : context.metadataFile.parent.toString(),
                md5sum             : context.metadataFileMd5sum,
                fastqImportInstance: fastqImportInstance,
        )
        assert metaDataFile.save(flush: true)

        log.debug("import stopped ${metaDataFile.fileName}:  ${System.currentTimeMillis() - timeImportStarted}")
        return metaDataFile
    }

    protected void notifyAboutUnsetConfig(List<SeqTrack> seqTracks, OtrsTicket ticket) {
        List<SeqTrack> configured = getSeqTracksWithConfiguredAlignment(seqTracks.unique())

        List<SeqTrack> withoutCategory = sampleTypeService.getSeqTracksWithoutSampleCategory(configured)
        List<SeqTrack> withoutThreshold = processingThresholdsService.getSeqTracksWithoutProcessingThreshold(configured)

        if (withoutCategory || withoutThreshold) {
            StringBuilder subject = new StringBuilder()
            if (ticket) {
                subject.append("[${ticket.prefixedTicketNumber}] ")
            }
            subject.append("Configuration missing for ")
            subject.append([withoutCategory ? "category" : "", withoutThreshold ? "threshold" : ""].findAll().join(" and "))

            String body = ""
            if (withoutCategory) {
                body += "\nNo category set for:\n"
                body += "${withoutCategory.collect { "${it.project} - ${it.sampleType.displayName}" }.unique().join(";\n")}\n"
            }
            if (withoutThreshold) {
                body += "\nNo threshold set for:\n"
                body += "${withoutThreshold.collect { "${it.project} - ${it.sampleType.displayName} - ${it.seqType.displayName}" }.unique().join(";\n")}\n"
            }

            mailHelperService.sendEmail(
                    subject.toString(),
                    body,
                    processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_ERRORS)
            )
        }
    }

    protected List<SeqTrack> getSeqTracksWithConfiguredAlignment(List<SeqTrack> seqTracks) {
        seqTracks.findAll { SeqTrack seqTrack ->
            ConfigPerProjectAndSeqType.findAllByProjectAndSeqTypeAndPipelineInListAndObsoleteDateIsNull(
                    seqTrack.project,
                    seqTrack.seqType,
                    Pipeline.findAllByTypeInList(Pipeline.Type.values().findAll { it == Pipeline.Type.ALIGNMENT })
            )
        }
    }

    private void importRuns(MetadataValidationContext context, FastqImportInstance fastqImportInstance, Collection<Row> metadataFileRows, boolean align) {
        metadataFileRows.groupBy { it.getCellByColumnTitle(RUN_ID.name()).text }.each { String runName, List<Row> rows ->
            Run run = getOrCreateRun(runName, rows)

            Long timeStarted = System.currentTimeMillis()
            log.debug('seqTracks started')
            importSeqTracks(context, fastqImportInstance, run, rows, align)
            log.debug("seqTracks stopped took: ${System.currentTimeMillis() - timeStarted}")
        }
    }

    protected Run getOrCreateRun(String runName, List<Row> rows) {
        SeqCenter seqCenter =  exactlyOneElement(SeqCenter.findAllWhere(name: uniqueColumnValue(rows, CENTER_NAME)))
        SeqPlatform seqPlatform = seqPlatformService.findSeqPlatform(
                uniqueColumnValue(rows, INSTRUMENT_PLATFORM),
                uniqueColumnValue(rows, INSTRUMENT_MODEL),
                uniqueColumnValue(rows, SEQUENCING_KIT) ?: null)
        String dateString = uniqueColumnValue(rows, RUN_DATE)
        Date dateExecuted = dateString ? RunDateParserService.parseDate('yyyy-MM-dd', dateString) : null

        Run run = atMostOneElement(Run.findAllByName(runName))
        if (run) {
            assert run.seqCenter == seqCenter : "The center of run (${run.seqCenter}) differ from center in sheet (${seqCenter})"
            assert run.seqPlatform == seqPlatform : "The seqPlatform of run (${run.seqPlatform}) differ from seqPlatform in sheet (${seqPlatform})"
            assert run.dateExecuted == dateExecuted : "The dateExecuted of run (${run.dateExecuted}) differ from dateExecuted in sheet (${dateExecuted})"
            return run
        }

        Run newRun = new Run(
                name: runName,
                seqCenter: seqCenter,
                seqPlatform: seqPlatform,
                dateExecuted: dateExecuted,
        )
        newRun.save(flush: true)
        return newRun
    }

    private void importSeqTracks(MetadataValidationContext context, FastqImportInstance fastqImportInstance, Run run, Collection<Row> runRows, boolean align) {
        Map<String, List<Row>> runsGroupedByLane = runRows.groupBy {
            MultiplexingService.combineLaneNumberAndBarcode(it.getCellByColumnTitle(LANE_NO.name()).text, extractBarcode(it).value)
        }
        int amountOfRows = runsGroupedByLane.size()
        runsGroupedByLane.eachWithIndex { String laneId, List<Row> rows, int index ->
            String projectName = uniqueColumnValue(rows, PROJECT)
            Project project = Project.getByNameOrNameInMetadataFiles(projectName)
            String ilseNumber = uniqueColumnValue(rows, ILSE_NO)
            String seqTypeRaw = uniqueColumnValue(rows, SEQUENCING_TYPE)
            String tagmentationRaw = uniqueColumnValue(rows, TAGMENTATION_BASED_LIBRARY)?.toLowerCase()
            String baseMaterial = uniqueColumnValue(rows, BASE_MATERIAL)
            boolean isSingleCell = SeqTypeService.isSingleCell(baseMaterial)
            LibraryLayout libLayout = LibraryLayout.findByName(uniqueColumnValue(rows, SEQUENCING_READ_TYPE))

            SeqType seqType = seqTypeService.findByNameOrImportAlias(
                    seqTypeMaybeTagmentationName(seqTypeRaw, tagmentationRaw),
                    [libraryLayout: libLayout, singleCell: isSingleCell],
            )
            String pipelineVersionString = uniqueColumnValue(rows, FASTQ_GENERATOR) ?: 'unknown'
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
                    laneId               : laneId,
                    ilseSubmission       : ilseSubmission,
                    // TODO OTP-2050: Use a different fallback value?
                    insertSize           : tryParseInt(uniqueColumnValue(rows, FRAGMENT_SIZE), 0),
                    run                  : run,
                    sample               : (atMostOneElement(SampleIdentifier.findAllWhere(name: sampleIdString)) ?:
                            sampleIdentifierService.parseAndFindOrSaveSampleIdentifier(sampleIdString, project)).sample,
                    seqType              : seqType,
                    pipelineVersion      : SoftwareToolService.getBaseCallingTool(pipelineVersionString).softwareTool,
                    kitInfoReliability   : kitInfoReliability,
                    libraryPreparationKit: libraryPreparationKit,
                    libraryName          : libraryName,
                    normalizedLibraryName: normalizedLibraryName,
            ]
            if (seqType.hasAntibodyTarget) {
                properties['antibodyTarget'] = antibodyTargetService.findByNameOrImportAlias(uniqueColumnValue(rows, ANTIBODY_TARGET))
                properties['antibody'] = uniqueColumnValue(rows, ANTIBODY) ?: null
            }

            if (isSingleCell) {
                properties['singleCellWellLabel'] = uniqueColumnValue(rows, SINGLE_CELL_WELL_LABEL) ?:
                        sampleIdentifierService.parseCellPosition(sampleIdString, project)
            }

            SeqTrack seqTrack = new SeqTrack(properties)
            assert seqTrack.save(flush: false)

            Long timeStarted = System.currentTimeMillis()
            log.debug("dataFiles started ${index}/${amountOfRows}")
            importDataFiles(context, fastqImportInstance, seqTrack, rows)
            log.debug("dataFiles stopped took: ${System.currentTimeMillis() - timeStarted}")
            assert seqTrack.save(flush: true) //needs to flush the session, so seqTrackService.decideAndPrepareForAlignment can work

            Collection<MergingWorkPackage> mergingWorkPackages = []
            if (align) {
                mergingWorkPackages = seqTrackService.decideAndPrepareForAlignment(seqTrack)
            }
            seqTrackService.determineAndStoreIfFastqFilesHaveToBeLinked(seqTrack, !mergingWorkPackages.empty)
            samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages)
            mergingCriteriaService.createDefaultMergingCriteria(project, seqType)
        }
    }

    private static void importDataFiles(MetadataValidationContext context, FastqImportInstance fastqImportInstance, SeqTrack seqTrack, Collection<Row> seqTrackRows) {
        Map<String, Collection<Row>> seqTrackRowsByMateNumber = seqTrackRows.groupBy {
            extractMateNumber(it).value
        }
        assert seqTrackRowsByMateNumber.findAll {
            !it.key.toUpperCase(Locale.ENGLISH).startsWith('I')
        }.size() == seqTrack.seqType.libraryLayout.mateCount

        seqTrackRowsByMateNumber.each { String mateNumber, List<Row> rows ->
            Matcher matcher = mateNumber =~ MATE_NUMBER_EXPRESSION
            assert matcher
            Row row = exactlyOneElement(rows)

            Path file = context.directoryStructure.getDataFilePath(context, row)
            String mate = matcher.group('number')
            boolean indexFile = matcher.group('index')

            DataFile dataFile = new DataFile(
                    pathName           : '',
                    fileName           : file.fileName.toString(),
                    initialDirectory   : file.parent.toString(),
                    vbpFileName        : file.fileName.toString(),
                    md5sum             : row.getCellByColumnTitle(MD5.name()).text.toLowerCase(Locale.ENGLISH),
                    project            : seqTrack.project,
                    dateExecuted       : seqTrack.run.dateExecuted,
                    used               : true,
                    mateNumber         : mate,
                    indexFile          : indexFile,
                    run                : seqTrack.run,
                    fastqImportInstance: fastqImportInstance,
                    seqTrack           : seqTrack,
                    fileType           : FileTypeService.getFileType(file.fileName.toString(), FileType.Type.SEQUENCE),
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

        Cell barcodeCell = row.getCellByColumnTitle(INDEX.name())
        if (barcodeCell) {
            cells.add(barcodeCell)
            barcode = barcodeCell.text ? barcodeCell.text.replace(',', '-') : null
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
        Cell mateNumberCell = row.getCellByColumnTitle(READ.name())
        if (mateNumberCell) {
            return new ExtractedValue(mateNumberCell.text, [mateNumberCell] as Set)
        }
        return null
    }

    static String getSeqTypeNameFromMetadata(ValueTuple tuple) {
        String tagmentation = tuple.getValue(TAGMENTATION_BASED_LIBRARY.name())?.toLowerCase()
        String seqType = tuple.getValue(SEQUENCING_TYPE.name())
        return seqTypeMaybeTagmentationName(seqType, tagmentation)
    }

    static Project getProjectFromMetadata(ValueTuple tuple) {
        String sampleId = tuple.getValue(SAMPLE_ID.name())
        String projectName = tuple.getValue(PROJECT.name()) ?: ''
        SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllByName(sampleId))
        if (sampleIdentifier) {
            return sampleIdentifier.project
        } else {
            Project projectFromProjectColumn = atMostOneElement(Project.findAllByNameOrNameInMetadataFiles(projectName, projectName))
            if (projectFromProjectColumn) {
                return projectFromProjectColumn
            }
        }
        return null
    }

    SeqType getSeqTypeFromMetadata(ValueTuple tuple) {
        boolean isSingleCell = seqTypeService.isSingleCell(tuple.getValue(BASE_MATERIAL.name()))
        LibraryLayout libLayout = LibraryLayout.findByName(tuple.getValue(SEQUENCING_READ_TYPE.name()))
        if (!libLayout) {
            return null
        }

        return seqTypeService.findByNameOrImportAlias(
                getSeqTypeNameFromMetadata(tuple),
                [libraryLayout: libLayout, singleCell: isSingleCell],
        )
    }

    /** small helper to parse seqtypes and boolean-like uservalues into a proper OTP name */
    static String seqTypeMaybeTagmentationName(String seqType, String tagmentationRawValue) {
        boolean isTagmentation = tagmentationRawValue in ["1", "true"]
        return seqType + (isTagmentation && !seqType.endsWith(SeqType.TAGMENTATION_SUFFIX) ? SeqType.TAGMENTATION_SUFFIX : '')
    }
}

@TupleConstructor
@ToString(includePackage=false, includeNames=true)
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
