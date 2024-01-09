/*
 * Copyright 2011-2023 The OTP authors
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

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import groovy.transform.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfigurationService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructureBeanName
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrainService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.exceptions.CopyingOfFileFailedException
import de.dkfz.tbi.otp.utils.exceptions.MetadataFileImportException
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationInitializationService
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel

import java.nio.file.*
import java.util.logging.Level
import java.util.regex.Matcher

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

/**
 * Metadata import 2.0 (OTP-34)
 */
@CompileDynamic
@Transactional
class MetadataImportService {

    @Autowired
    ApplicationContext applicationContext

    @Autowired
    LinkGenerator linkGenerator

    @Autowired
    RemoteShellHelper remoteShellHelper
    AntibodyTargetService antibodyTargetService
    DataInstallationInitializationService dataInstallationInitializationService

    CellRangerConfigurationService cellRangerConfigurationService
    ConfigService configService
    FileService fileService
    FileSystemService fileSystemService
    LibraryPreparationKitService libraryPreparationKitService
    LsdfFilesService lsdfFilesService
    MailHelperService mailHelperService
    MergingCriteriaService mergingCriteriaService
    TicketService ticketService
    ProcessingThresholdsService processingThresholdsService
    SampleIdentifierService sampleIdentifierService
    SamplePairDeciderService samplePairDeciderService
    SampleTypeService sampleTypeService
    SeqPlatformService seqPlatformService
    SeqTrackService seqTrackService
    SeqTypeService seqTypeService
    SpeciesWithStrainService speciesWithStrainService
    FastqMetadataValidationService fastqMetadataValidationService

    static final int MAX_ILSE_NUMBER_RANGE_SIZE = 20

    static final String MATE_NUMBER_EXPRESSION = /^(?<index>i|I)?(?<number>[1-9]\d*)$/

    /**
     * @return A collection of descriptions of the validations which are performed
     */
    Collection<String> getImplementedValidations() {
        return (Collection<String>) metadataValidators.sum { it.descriptions }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    MetadataValidationContext validateWithAuth(ContentWithPathAndProblems contentWithPathAndProblems,
                                               DirectoryStructureBeanName directoryStructure, boolean ignoreAlreadyKnownMd5sum = false) {
        MetadataValidationContext context = fastqMetadataValidationService.createFromContent(
                contentWithPathAndProblems,
                getDirectoryStructure(directoryStructure),
                directoryStructure.displayName,
                ignoreAlreadyKnownMd5sum
        )
        return validate(context)
    }

    MetadataValidationContext validatePath(Path metadataPath, DirectoryStructureBeanName directoryStructure, boolean ignoreAlreadyKnownMd5sum = false) {
        MetadataValidationContext context = fastqMetadataValidationService.createFromFile(
                metadataPath,
                getDirectoryStructure(directoryStructure),
                directoryStructure.displayName,
                ignoreAlreadyKnownMd5sum
        )
        return validate(context)
    }

    private MetadataValidationContext validate(MetadataValidationContext context) {
        if (context.spreadsheet) {
            Long hash = System.currentTimeMillis()
            Long startTimeAll = System.currentTimeMillis()
            int dataCount = context.spreadsheet.dataRows.size()
            log.debug("start validation of ${dataCount} lines of ${context.metadataFile}, validation started : ${hash}")
            metadataValidators.each {
                Long startTime = System.currentTimeMillis()
                it.validate(context)
                log.debug("finished ${it.class} took ${System.currentTimeMillis() - startTime}ms for ${dataCount} lines, validation started : ${hash}")
            }
            log.debug("finished all ${metadataValidators.size()} validators for ${dataCount} lines took " +
                    "${System.currentTimeMillis() - startTimeAll}ms, validation started : ${hash}")
        }
        return context
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    List<ValidateAndImportResult> validateAndImport(List<ContentWithProblemsAndPreviousMd5sum> metadataPaths,
                                                    DirectoryStructureBeanName directoryStructure,
                                                    boolean ignoreWarnings, String ticketNumber, String seqCenterComment,
                                                    boolean automaticNotification,
                                                    boolean ignoreAlreadyKnownMd5sum = false) {
        try {
            Long startTime = System.currentTimeMillis()
            Map<MetadataValidationContext, String> contexts = metadataPaths.collectEntries { ContentWithProblemsAndPreviousMd5sum pathWithMd5sum ->
                MetadataValidationContext context = validateWithAuth(pathWithMd5sum.contentWithPathAndProblems, directoryStructure, ignoreAlreadyKnownMd5sum)
                return [(context): pathWithMd5sum.previousMd5sum]
            }
            contexts.collect { context, previousMd5Sum ->
                mayImport(context, ignoreWarnings, previousMd5Sum)
            }

            List<ValidateAndImportResult> results = contexts.collect { context, md5sum ->
                return importHelperMethod(context, FastqImportInstance.ImportMode.MANUAL,
                        ticketNumber, seqCenterComment, automaticNotification)
            }
            int lines = (results*.context*.spreadsheet*.dataRows*.size().sum() ?: 0) as int
            log.debug("finished validate and import took ${System.currentTimeMillis() - startTime}ms for ${lines}")
            return results
        } catch (Exception e) {
            throw new MetadataFileImportException("Error while importing metadata file with paths: " +
                    "${metadataPaths*.path.join('\n')}\n${e.localizedMessage}\n${e.cause}", e)
        }
    }

    private ValidateAndImportResult importHelperMethod(MetadataValidationContext context, FastqImportInstance.ImportMode importMode,
                                                       String ticketNumber, String seqCenterComment, boolean automaticNotification) {
        Path filePathTarget = createPathTargetForMetadataFile(context, ticketNumber)
        MetaDataFile metadataFileObject = importMetadataFile(context, importMode, ticketNumber, seqCenterComment, automaticNotification, filePathTarget)
        String copiedFile = copyMetadataFile(context, filePathTarget)
        return new ValidateAndImportResult(context, metadataFileObject, copiedFile)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateAutomaticNotificationFlag(Ticket ticket, boolean automaticNotification) {
        ticket.automaticNotification = automaticNotification
        assert ticket.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateFinalNotificationFlag(Ticket ticket, boolean finalNotificationSent) {
        ticket.finalNotificationSent = finalNotificationSent
        assert ticket.save(flush: true)
    }

    protected Path createPathTargetForMetadataFile(MetadataValidationContext context, String ticketNumber) {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        String oldName = context.metadataFile.fileName

        Date date = new Date()
        String yearMonth = TimeFormats.YEAR_MONTH_SLASH.getFormattedDate(date)
        String timeStamp = TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(date)

        Path metadataStorage = fileSystem.getPath("${configService.metadataStoragePath}")
        Path targetDir = metadataStorage.resolve(yearMonth).resolve(ticketNumber)

        int position = oldName.lastIndexOf('.')
        String newName = "${oldName.substring(0, position)}-${timeStamp}${oldName.substring(position)}"

        return targetDir.resolve(newName)
    }

    protected String copyMetadataFile(MetadataValidationContext context, Path targetFile) {
        Path source = context.metadataFile

        try {
            if (!Files.exists(targetFile)) {
                // create the directory and set the permission with owner and group access (setgid bit) explicitly
                fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(targetFile.parent,
                        "", FileService.OWNER_AND_GROUP_DIRECTORY_PERMISSION_STRING)
                fileService.createFileWithContent(targetFile, context.content)
            }
            assert Files.readAllBytes(targetFile) == context.content

            return targetFile.toString()
        } catch (Throwable t) {
            mailHelperService.sendEmailToTicketSystem("Error: Copying of metadatafile ${source} failed",
                    "${t.localizedMessage}\n${t.cause}")
            throw new CopyingOfFileFailedException("Copying of metadata file ${source} failed", t)
        }
    }

    List<ValidateAndImportResult> validateAndImportMultiple(String ticketNumber, String ilseNumbers, boolean ignoreAlreadyKnownMd5sum) {
        FileSystem fs = fileSystemService.remoteFileSystem
        return validateAndImportMultiple(
                ticketNumber,
                parseIlseNumbers(ilseNumbers).collect { getMetadataFilePathForIlseNumber(it, fs) },
                DirectoryStructureBeanName.GPCF_SPECIFIC,
                ignoreAlreadyKnownMd5sum
        )
    }

    List<ValidateAndImportResult> validateAndImportMultiple(String ticketNumber, List<Path> metadataFiles, DirectoryStructureBeanName directoryStructure,
                                                            boolean ignoreAlreadyKnownMd5sum) {
        List<MetadataValidationContext> failedValidations = []
        List<MetadataValidationContext> contexts = metadataFiles.collect {
            return validatePath(it, directoryStructure, ignoreAlreadyKnownMd5sum)
        }
        List<ValidateAndImportResult> results = contexts.collect { context ->
            try {
                mayImport(context, false, null)
                return importHelperMethod(context, FastqImportInstance.ImportMode.AUTOMATIC, ticketNumber, null, true)
            } catch (MetadataFileImportException e) {
                failedValidations.push(context)
                return new ValidateAndImportResult()
            }
        }
        if (failedValidations.isEmpty()) {
            return results
        }
        throw new MultiImportFailedException(failedValidations, metadataFiles)
    }

    protected Path getMetadataFilePathForIlseNumber(int ilseNumber, FileSystem fileSystem) {
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
        return applicationContext.getBeansOfType(MetadataValidator).values().sort { it.class.name }
    }

    protected DirectoryStructure getDirectoryStructure(DirectoryStructureBeanName name) {
        DirectoryStructure directoryStructure = applicationContext.getBean(name.beanName, DirectoryStructure)
        directoryStructure.fileSystem = fileSystemService?.remoteFileSystem
        return directoryStructure
    }

    static void mayImport(AbstractMetadataValidationContext context, boolean ignoreWarnings, String previousValidationMd5sum)
            throws MetadataFileImportException {
        Level maxLevel = context.maximumProblemLevel
        if (maxLevel.intValue() > LogLevel.WARNING.intValue()) {
            throw new MetadataFileImportException("The file with path ${context.metadataFile} problems exceeding the warning level.")
        } else if (maxLevel == LogLevel.WARNING && !ignoreWarnings) {
            throw new MetadataFileImportException("The file with path ${context.metadataFile} has problems with warning level. " +
                    "To import anyway ignore these warnings.")
        } else if (maxLevel == LogLevel.WARNING && ignoreWarnings && !context.metadataFileMd5sum.equalsIgnoreCase(previousValidationMd5sum)) {
            throw new MetadataFileImportException("The file with path ${context.metadataFile} has changed" +
                    " its md5 sum between validation and import. Please revalidate your metadata file(s) and import again.")
        }
    }

    protected MetaDataFile importMetadataFile(MetadataValidationContext context, FastqImportInstance.ImportMode importMode, String ticketNumber,
                                              String seqCenterComment, boolean automaticNotification, Path filePathTarget) {
        Long timeImportStarted = System.currentTimeMillis()
        log.debug("import started ${context.metadataFile.fileName} ${timeImportStarted}")
        FastqImportInstance fastqImportInstance = new FastqImportInstance(
                ticket: ticketNumber ? ticketService.createOrResetTicket(ticketNumber, seqCenterComment, automaticNotification) : null,
                importMode: importMode,
                state: WorkflowCreateState.WAITING,
        ).save(flush: true)

        Long timeStarted = System.currentTimeMillis()
        log.debug("  import runs of file  ${context.metadataFile.fileName} started")
        importRuns(context, fastqImportInstance, context.spreadsheet.dataRows)
        log.debug("  import runs of file  ${context.metadataFile.fileName} stopped took: ${System.currentTimeMillis() - timeStarted}")

        MetaDataFile metaDataFile = new MetaDataFile(
                fileNameSource: context.metadataFile.fileName.toString(),
                // If the file is passed via drag and drop there is no parent directory so we pass just a point
                filePathSource: context.metadataFile?.parent?.toString() ?: '',
                filePathTarget: filePathTarget.toString(),
                md5sum: context.metadataFileMd5sum,
                fastqImportInstance: fastqImportInstance,
        ).save(flush: true)

        fastqImportInstance.refresh()

        Long timeGeneratedThresholds = System.currentTimeMillis()
        log.debug("  generatedThresholds started")
        List<SeqTrack> analysableSeqTracks = SeqTrackService.getAnalysableSeqTracks((fastqImportInstance.sequenceFiles*.seqTrack as List).unique())
        List<ProcessingThresholds> generatedThresholds = processingThresholdsService.generateDefaultThresholds(analysableSeqTracks)
        notifyAboutUnsetConfig(analysableSeqTracks, generatedThresholds, fastqImportInstance.ticket)
        log.debug("  generatedThresholds stopped took: ${System.currentTimeMillis() - timeGeneratedThresholds}")

        metaDataFile.save(flush: true)

        log.debug("import stopped ${metaDataFile.fileNameSource} (lines: ${context.spreadsheet.dataRows.size()}) ${timeImportStarted}: " +
                "${System.currentTimeMillis() - timeImportStarted}")
        return metaDataFile
    }

    /**
     * Send an email notification with a list of the unset categories
     * and the generated default thresholds.
     */
    protected void notifyAboutUnsetConfig(List<SeqTrack> seqTracks, List<ProcessingThresholds> defaultThresholds, Ticket ticket) {
        List<SeqTrack> withoutCategory = sampleTypeService.getSeqTracksWithoutSampleCategory(seqTracks)

        if (withoutCategory || defaultThresholds) {
            StringBuilder subject = new StringBuilder()
            if (ticket) {
                subject.append("[${ticketService.getPrefixedTicketNumber(ticket)}] ")
            }
            subject.append("Configuration missing for ")
            subject.append([withoutCategory ? "category" : "", defaultThresholds ? "threshold" : ""].findAll().join(" and "))

            String body = ""
            if (withoutCategory) {
                body += "\nNo category set for:\n"
                body += "${withoutCategory.collect { "${it.project} - ${it.sampleType.displayName}" }.unique().join(";\n")}\n"
            }
            if (defaultThresholds) {
                body += "\nThese thresholds have been generated automatically:\n"
                body += defaultThresholds.collect {
                    "${it.project} - ${it.sampleType.displayName} - ${it.seqType.displayName}, min. Lanes: ${it.numberOfLanes}"
                }.unique().join("\n")
                body += "\n"
            }

            body += "\n\n Link to the Processing Thresholds Page:\n"
            List<Project> projects = seqTracks*.project.unique()
            projects.each { Project project ->
                body += getNoThresholdSetLink(project) + "\n"
            }

            mailHelperService.sendEmailToTicketSystem(subject.toString(), body)
        }
    }

    private String getNoThresholdSetLink(Project project) {
        return linkGenerator.link(
                controller: "processingThreshold",
                action: "index",
                absolute: true,
                params: [(ProjectSelectionService.PROJECT_SELECTION_PARAMETER): project.name]
        )
    }

    private void importRuns(MetadataValidationContext context, FastqImportInstance fastqImportInstance, Collection<Row> metadataFileRows) {
        Map<String, List<Row>> seqTrackPerRun = metadataFileRows.groupBy {
            it.getCellByColumnTitle(RUN_ID.name()).text
        }
        int amountOfRows = seqTrackPerRun.size()
        seqTrackPerRun.eachWithIndex { String runName, List<Row> rows, int index ->
            Run run = getOrCreateRun(runName, rows)

            Long timeStarted = System.currentTimeMillis()
            log.debug("    seqTracks of run ${run.name} started ${index}/${amountOfRows}")
            importSeqTracks(context, fastqImportInstance, run, rows)
            log.debug("    seqTracks of run ${run.name} stopped took: ${System.currentTimeMillis() - timeStarted}")
        }

        // Now that all rows are processed, we can clean up.
        // flush=false, because we don't care when it's cleaned up; it can just fade away together with the context.
        context.usedSampleIdentifiers*.delete(flush: false)
    }

    protected Run getOrCreateRun(String runName, List<Row> rows) {
        SeqCenter seqCenter = exactlyOneElement(SeqCenter.findAllWhere(name: uniqueColumnValue(rows, CENTER_NAME)))
        SeqPlatform seqPlatform = seqPlatformService.findSeqPlatform(
                uniqueColumnValue(rows, INSTRUMENT_PLATFORM),
                uniqueColumnValue(rows, INSTRUMENT_MODEL),
                uniqueColumnValue(rows, SEQUENCING_KIT) ?: null)
        String dateString = uniqueColumnValue(rows, RUN_DATE)
        Date dateExecuted = dateString ? TimeUtils.tryParseDate(TimeFormats.DATE, dateString) : null

        Run run = atMostOneElement(Run.findAllByName(runName))
        if (run) {
            assert run.seqCenter == seqCenter: "The center of run (${run.seqCenter}) differ from center in sheet (${seqCenter})"
            assert run.seqPlatform == seqPlatform: "The seqPlatform of run (${run.seqPlatform}) differ from seqPlatform in sheet (${seqPlatform})"
            assert run.dateExecuted == dateExecuted: "The dateExecuted of run (${run.dateExecuted}) differ from dateExecuted in sheet (${dateExecuted})"
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

    private void importSeqTracks(MetadataValidationContext context, FastqImportInstance fastqImportInstance, Run run, Collection<Row> runRows) {
        Map<String, List<Row>> runsGroupedByLane = runRows.groupBy {
            MultiplexingService.combineLaneNumberAndBarcode(it.getCellByColumnTitle(LANE_NO.name()).text, extractBarcode(it).value)
        }
        int amountOfRows = runsGroupedByLane.size()
        Set<Sample> samples = [] as Set
        runsGroupedByLane.eachWithIndex { String laneId, List<Row> rows, int index ->
            String projectName = uniqueColumnValue(rows, PROJECT)
            Project project = ProjectService.findByNameOrNameInMetadataFiles(projectName)
            String ilseNumber = uniqueColumnValue(rows, ILSE_NO)
            String seqTypeRaw = uniqueColumnValue(rows, SEQUENCING_TYPE)
            String baseMaterial = uniqueColumnValue(rows, BASE_MATERIAL)
            boolean isSingleCell = SeqTypeService.isSingleCell(baseMaterial)
            SequencingReadType libLayout = SequencingReadType.getByName(uniqueColumnValue(rows, SEQUENCING_READ_TYPE))
            List<String> speciesList = uniqueColumnValue(rows, SPECIES).split('[+]')*.trim()
            SpeciesWithStrain individualSpecies = speciesWithStrainService.getByAlias(speciesList.first())
            List<SpeciesWithStrain> sampleSpecies = []
            if (speciesList.size() > 1) {
                speciesList.removeAt(0)
                speciesList.each { String s ->
                    sampleSpecies.add(speciesWithStrainService.getByAlias(s))
                }
            }

            SeqType seqType = seqTypeService.findByNameOrImportAlias(seqTypeRaw,
                    [libraryLayout: libLayout, singleCell: isSingleCell],
            )
            String pipelineVersionString = uniqueColumnValue(rows, FASTQ_GENERATOR) ?: 'unknown'
            String sampleIdString = uniqueColumnValue(rows, SAMPLE_NAME)
            String libPrepKitString = uniqueColumnValue(rows, LIB_PREP_KIT)
            InformationReliability kitInfoReliability
            LibraryPreparationKit libraryPreparationKit = null
            if (!libPrepKitString) {
                assert !seqType.needsBedFile
                kitInfoReliability = InformationReliability.UNKNOWN_UNVERIFIED
            } else if (libPrepKitString == InformationReliability.UNKNOWN_VERIFIED.rawValue) {
                kitInfoReliability = InformationReliability.UNKNOWN_VERIFIED
            } else {
                kitInfoReliability = InformationReliability.KNOWN
                libraryPreparationKit = Objects.requireNonNull(
                        libraryPreparationKitService.findByNameOrImportAlias(libPrepKitString))
            }
            String libraryName = uniqueColumnValue(rows, TAGMENTATION_LIBRARY) ?: ""
            String normalizedLibraryName = SeqTrack.normalizeLibraryName(libraryName)
            IlseSubmission ilseSubmission
            if (ilseNumber) {
                ilseSubmission = CollectionUtils.atMostOneElement(IlseSubmission.findAllWhere(ilseNumber: Integer.parseInt(ilseNumber)))
                if (!ilseSubmission) {
                    ilseSubmission = new IlseSubmission(ilseNumber: Integer.parseInt(ilseNumber))
                    ilseSubmission.save(flush: true)
                }
            } else {
                ilseSubmission = null
            }

            SampleIdentifier sampleIdentifier = atMostOneElement(SampleIdentifier.findAllWhere(name: sampleIdString)) ?:
                    sampleIdentifierService.parseAndFindOrSaveSampleIdentifier(sampleIdString, project)
            context.usedSampleIdentifiers.add(sampleIdentifier)

            Map properties = [
                    laneId               : laneId,
                    ilseSubmission       : ilseSubmission,
                    insertSize           : tryParseInt(uniqueColumnValue(rows, FRAGMENT_SIZE), 0),
                    run                  : run,
                    sample               : sampleIdentifier.sample,
                    sampleIdentifier     : sampleIdentifier.name,
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
                        sampleIdentifierService.parseSingleCellWellLabel(sampleIdString, project)
            }

            SeqTrack seqTrack = new SeqTrack(properties)
            seqTrack.save(flush: false)

            if (seqTrack.individual.species) {
                assert seqTrack.individual.species == individualSpecies: "Individual contains value (${seqTrack.individual.species}) " +
                        "that differs from sheet (${individualSpecies})"
            } else {
                seqTrack.individual.species = individualSpecies
                seqTrack.individual.save(flush: false)
            }

            if (seqTrack.sample.mixedInSpecies) {
                assert seqTrack.sample.mixedInSpecies.size() == sampleSpecies.size() &&
                        seqTrack.sample.mixedInSpecies.containsAll(sampleSpecies): "Sample contains value " +
                        "(${seqTrack.sample.mixedInSpecies}) that differs from sheet (${sampleSpecies})"
            } else {
                seqTrack.sample.mixedInSpecies = []
                sampleSpecies.each { SpeciesWithStrain species ->
                    seqTrack.sample.mixedInSpecies.add(species)
                    seqTrack.sample.save(flush: false)
                }
            }

            Long timeStarted = System.currentTimeMillis()
            log.debug("      dataFiles of seqtrack ${seqTrack.laneId} started ${index}/${amountOfRows}")
            importDataFiles(context, fastqImportInstance, seqTrack, rows)
            log.debug("      dataFiles of seqtrack ${seqTrack.laneId} stopped took: ${System.currentTimeMillis() - timeStarted}")
            seqTrack.save(flush: true) // needs to flush the session, so seqTrackService.decideAndPrepareForAlignment can work

            mergingCriteriaService.createDefaultMergingCriteria(sampleIdentifier.project, seqType)
            Collection<MergingWorkPackage> mergingWorkPackages = seqTrackService.decideAndPrepareForAlignment(seqTrack)
            samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages)
            samples.add(sampleIdentifier.sample)
        }
        samples.each {
            cellRangerConfigurationService.runOnImport(it.individual.project, it)
        }
    }

    private static void importDataFiles(MetadataValidationContext context, FastqImportInstance fastqImportInstance, SeqTrack seqTrack,
                                        Collection<Row> seqTrackRows) {
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

            RawSequenceFile dataFile = new FastqFile(
                    pathName: '',
                    fileName: file.fileName.toString(),
                    initialDirectory: file.parent.toString(),
                    vbpFileName: file.fileName.toString(),
                    fastqMd5sum: row.getCellByColumnTitle(MD5.name()).text.toLowerCase(Locale.ENGLISH),
                    project: seqTrack.project,
                    dateExecuted: seqTrack.run.dateExecuted,
                    used: true,
                    mateNumber: mate,
                    indexFile: indexFile,
                    run: seqTrack.run,
                    fastqImportInstance: fastqImportInstance,
                    seqTrack: seqTrack,
                    fileType: FileTypeService.getFileType(file.fileName.toString(), FileType.Type.SEQUENCE),
            )
            dataFile.save(flush: false)

            assert new File(LsdfFilesService.getFileInitialPath(dataFile)) == new File(file.toString())

            importMetadataEntries(context, dataFile, row)
        }
    }

    private static void importMetadataEntries(MetadataValidationContext context, RawSequenceFile dataFile, Row row) {
        for (Cell it : context.spreadsheet.header.cells) {
            MetaDataKey metaDataKey = CollectionUtils.atMostOneElement(MetaDataKey.findAllWhere(name: it.text))
            if (!metaDataKey) {
                metaDataKey = new MetaDataKey(name: it.text)
                metaDataKey.save(flush: true)
            }
            new MetaDataEntry(
                    sequenceFile: dataFile,
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
        }
        return null
    }

    static ExtractedValue extractMateNumber(Row row) {
        Cell mateNumberCell = row.getCellByColumnTitle(READ.name())
        if (mateNumberCell) {
            return new ExtractedValue(mateNumberCell.text, [mateNumberCell] as Set)
        }
        return null
    }

    Path getMetaDataFileFullPath(MetaDataFile metaDataFile) {
        return Paths.get([metaDataFile.filePathSource, metaDataFile.fileNameSource].findAll().join(FileSystems.default.separator))
    }

    MetaDataFile findById(long id) {
        return MetaDataFile.get(id)
    }

    List<MetaDataFile> findAllByFastqImportInstance(FastqImportInstance importInstance) {
        return MetaDataFile.findAllByFastqImportInstance(importInstance, [sort: "dateCreated", order: "desc"])
    }
}

@TupleConstructor
@ToString(includePackage = false, includeNames = true)
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
    final String copiedFile
}

@TupleConstructor
class MultiImportFailedException extends RuntimeException {

    final List<MetadataValidationContext> failedValidations
    final List<Path> allPaths
}

@TupleConstructor
class ContentWithProblemsAndPreviousMd5sum {
    ContentWithPathAndProblems contentWithPathAndProblems
    String previousMd5sum

    Path getPath() {
        return this.contentWithPathAndProblems.path
    }

    byte[] getContent() {
        return this.contentWithPathAndProblems.content
    }
}
