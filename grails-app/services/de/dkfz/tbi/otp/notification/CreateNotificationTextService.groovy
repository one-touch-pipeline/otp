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
package de.dkfz.tbi.otp.notification

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep
import de.dkfz.tbi.otp.utils.MessageSourceService

import static de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep.*
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.ALL_DONE
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO

@Transactional
class CreateNotificationTextService {

    @Autowired
    LinkGenerator linkGenerator

    AlignmentInfoService alignmentInfoService
    LsdfFilesService lsdfFilesService
    MessageSourceService messageSourceService
    ProcessingOptionService processingOptionService

    /**
     * Helper function to create the notification for exactly one ProcessingStep.
     */
    String notification(OtrsTicket otrsTicket, ProcessingStatus status, ProcessingStep processingStep, Project project) {
        assert processingStep
        return multiStepNotification(otrsTicket, status, [processingStep], project)
    }

    /**
     * Prepares the entire body of a processing step update mail for the given steps.
     *
     * The body is structured as follows: First a greeting to the customer, followed by the custom
     * notification of all provided steps, followed by the seqCenterComment, followed by a common
     * conclusion and ended with a reference to the FAQs.
     */
    String multiStepNotification(OtrsTicket otrsTicket, ProcessingStatus status, List<ProcessingStep> processingSteps, Project project) {
        assert otrsTicket
        assert status
        assert project

        String stepInformation = buildStepNotifications(processingSteps, status)

        if (!stepInformation) {
            return ''
        }

        return messageSourceService.createMessage('notification.template.base', [
                stepInformation      : stepInformation,
                seqCenterComment     : buildSeqCenterComment(otrsTicket),
                emailSenderSalutation: processingOptionService.findOptionAsString(OptionName.EMAIL_SENDER_SALUTATION),
                faq                  : faq,
        ])
    }

    /**
     * Builds the notification body for multiple steps.
     *
     * If the notification is done for more than one ProcessingStep it will add a header to each
     * section like: '# Installation'
     *
     * Only creates the notification for finished steps and omits others, even if provided.
     */
    String buildStepNotifications(List<ProcessingStep> processingSteps, ProcessingStatus status) {
        Map<ProcessingStep, String> validNotifications = processingSteps.collectEntries { ProcessingStep processingStep ->
            [(processingStep): "${processingStep}Notification"(status).trim()]
        }.findAll { Map.Entry entry ->
            entry.value
        }

        return validNotifications.collect { ProcessingStep step, String notification ->
            return "${validNotifications.size() > 1 ? "# ${step.displayName.capitalize()}:\n" : ""}${notification.trim()}"
        }.join("\n" * 3).trim()
    }

    /**
     * Build the SeqCenter comment.
     *
     * Properly combines the comments of different sources and applies them in the common format.
     *
     * The comment is SeqCenter specific. Should there be data from more than one SeqCenter connected
     * to the ticket it will return an empty comment string.
     */
    String buildSeqCenterComment(OtrsTicket otrsTicket) {
        String ticketComment = otrsTicket.seqCenterComment ?: ""

        List<SeqCenter> seqCenters = otrsTicket.findAllSeqTracks()*.seqCenter.unique()
        String generalComment = seqCenters.size() == 1 ? processingOptionService.findOptionAsString(
                OptionName.NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE,
                seqCenters.first().name
        ) : ""

        if (ticketComment || generalComment) {
            return """\
                |******************************
                |Note from sequencing center:
                |${combineSeqCenterComments(ticketComment, generalComment)}
                |******************************""".stripMargin()
        }
        return ""
    }

    /**
     * Combine the general SeqCenter note with the comment provided by the import ticket.
     *
     * The typical use case is to just join the two strings with a newline. However, if the general secondary note
     * is completely contained in the primary note, it is shortened to the primary note only.
     */
    String combineSeqCenterComments(String primary, String secondary) {
        Closure<String> equalize = { String input ->
            return input.replaceAll("[\r\n ]+", ' ').trim()
        }
        if (equalize(primary).contains(equalize(secondary))) {
            return primary
        }
        return "${primary}\n${secondary}".trim()
    }

    String getFaq() {
        if (ProcessingOptionService.findOption(OptionName.NOTIFICATION_TEMPLATE_FAQ_LINK)) {
            return messageSourceService.createMessage("notification.template.base.faq", [
                    faqLink    : processingOptionService.findOptionAsString(OptionName.NOTIFICATION_TEMPLATE_FAQ_LINK),
                    contactMail: processingOptionService.findOptionAsString(OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL),
            ])
        }
        return ""
    }

    String installationNotification(ProcessingStatus statusInput) {
        assert statusInput

        ProcessingStatus status = new ProcessingStatus(statusInput.seqTrackProcessingStatuses.findAll {
            it.installationProcessingStatus == ALL_DONE
        })

        List<String> samples = []
        status.seqTrackProcessingStatuses.groupBy {
            getSampleName(it.seqTrack)
        }.sort().each { String sample, List<SeqTrackProcessingStatus> seqTracksOfSample ->
            seqTracksOfSample.groupBy {
                it.alignmentProcessingStatus != NOTHING_DONE_WONT_DO
            }.each { boolean willAlign, List<SeqTrackProcessingStatus> seqTracksOfAlign ->
                samples.add("${willAlign ? '[A]' : '[-]'} ${sample} (${seqTracksOfAlign*.seqTrack.sampleIdentifier.unique().sort().join(", ")})")
            }
        }

        Collection<SeqTrack> seqTracks = status.seqTrackProcessingStatuses*.seqTrack
        if (!seqTracks) {
            log.debug("No installation finished yet.")
            return ''
        }
        String runNames = seqTracks*.run*.name.unique().sort().join(", ")
        String directories = getSeqTypeDirectories(seqTracks)
        String otpLinks = createOtpLinks(seqTracks*.project, 'sampleOverview', 'index')

        String message = messageSourceService.createMessage('notification.template.installation.base', [
                runs   : runNames,
                paths  : directories,
                samples: samples.join('\n'),
                links  : otpLinks,
        ])

        if (status.alignmentProcessingStatus != NOTHING_DONE_WONT_DO) {
            if (!(seqTracks.any { it.seqType in SeqTypeService.cellRangerAlignableSeqTypes })) {
                message += '\n' + messageSourceService.createMessage('notification.template.installation.furtherProcessing')
            } else {
                message += '\n' + messageSourceService.createMessage('notification.template.installation.furtherProcessing.cellRanger', [
                        links: createOtpLinks(seqTracks*.project, "cellRangerConfiguration", "index"),
                ])
                if (ProcessingOptionService.findOption(OptionName.NOTIFICATION_TEMPLATE_FAQ_LINK)) {
                    message += messageSourceService.createMessage('notification.template.installation.furtherProcessing.cellRanger.faq', [
                            faq: processingOptionService.findOptionAsString(OptionName.NOTIFICATION_TEMPLATE_FAQ_LINK),
                    ])
                }
            }
            message += messageSourceService.createMessage('notification.template.installation.furtherProcessing.furtherNotification')
        }

        return message
    }

    String alignmentNotification(ProcessingStatus statusInput) {
        assert statusInput

        ProcessingStatus status = new ProcessingStatus(statusInput.seqTrackProcessingStatuses.findAll {
            it.alignmentProcessingStatus == ALL_DONE
        })

        Collection<SeqTrack> seqTracks = status.seqTrackProcessingStatuses*.seqTrack
        if (!seqTracks) {
            log.debug('No alignment finished yet.')
            return ''
        }
        String sampleNames = seqTracks.groupBy {
            getSampleName(it)
        }.sort().collect { String sample, List<SeqTrack> seqTracksOfSample ->
            "${sample} (${seqTracksOfSample*.sampleIdentifier.unique().sort().join(", ")})"
        }.join('\n')

        Collection<AbstractMergedBamFile> allGoodBamFiles = status.mergingWorkPackageProcessingStatuses*.completeProcessableBamFileInProjectFolder
        Map<AlignmentConfig, AlignmentInfo> alignmentInfoByConfig = allGoodBamFiles*.alignmentConfig.unique().collectEntries {
            [it, alignmentInfoService.getAlignmentInformationFromConfig(it)]
        }

        String links = createOtpLinks(allGoodBamFiles*.project, 'alignmentQualityOverview', 'index')

        String directories = getMergingDirectories(allGoodBamFiles)

        Map<Project, List<AbstractMergedBamFile>> bamFilesByProject = allGoodBamFiles.groupBy { it.project }
        boolean multipleProject = bamFilesByProject.size() > 1
        StringBuilder builder = new StringBuilder()

        bamFilesByProject.sort {
            it.key.name
        }.each { Project project, List<AbstractMergedBamFile> projectBamFiles ->
            if (multipleProject) {
                builder << "\n***********************\n"
                builder << project
            }
            projectBamFiles.groupBy {
                it.seqType
            }.sort {
                it.key.displayNameWithLibraryLayout
            }.each { SeqType seqType, List<AbstractMergedBamFile> seqTypeBamFiles ->
                Map<AlignmentConfig, List<AbstractMergedBamFile>> bamFilePerConfig = seqTypeBamFiles.groupBy {
                    it.alignmentConfig
                }
                boolean multipleConfigs = bamFilePerConfig.size() > 1 && project.alignmentDeciderBeanName == AlignmentDeciderBeanName.PAN_CAN_ALIGNMENT
                bamFilePerConfig.each { AlignmentConfig config, List<AbstractMergedBamFile> configBamFiles ->
                    AlignmentInfo alignmentInfo = alignmentInfoByConfig.get(config)
                    String individuals = multipleConfigs ? (config.individual ?: "default") : ""
                    builder << messageSourceService.createMessage("notification.template.alignment.processing", [
                            seqType           : seqType.displayNameWithLibraryLayout,
                            individuals       : individuals,
                            referenceGenome   : configBamFiles*.referenceGenome.unique().join(', '),
                            alignmentProgram  : alignmentInfo.alignmentProgram,
                            alignmentParameter: alignmentInfo.alignmentParameter,
                    ])
                    Map<String, Object> codeAndParams = alignmentInfo.getAlignmentSpecificMessageAttributes()
                    builder << messageSourceService.createMessage(codeAndParams.code as String, codeAndParams.params as Map)
                }
            }
        }

        String message = messageSourceService.createMessage("notification.template.alignment.base", [
                samples         : sampleNames,
                links           : links,
                cellRangerNote  : cellRangerAlignmentNotificationHelper(allGoodBamFiles.findAll { it instanceof SingleCellBamFile } as Set<SingleCellBamFile>),
                processingValues: builder.toString().trim(),
                paths           : directories,
        ])

        Map<Boolean, List<SamplePairProcessingStatus>> samplePairs = status.samplePairProcessingStatuses.groupBy {
            it.variantCallingProcessingStatus != NOTHING_DONE_WONT_DO
        }
        if (samplePairs[true]) {
            String variantCallingPipelines = samplePairs[true]*.variantCallingWorkflowNames().flatten().unique().sort().join(', ')
            message += '\n' + messageSourceService.createMessage("notification.template.alignment.furtherProcessing", [
                    samplePairsWillProcess : getSamplePairRepresentation(samplePairs[true]*.samplePair),
                    variantCallingPipelines: variantCallingPipelines,
            ])
        }
        if (samplePairs[false]) {
            message += '\n' + messageSourceService.createMessage("notification.template.alignment.noFurtherProcessing", [
                    emailSenderSalutation : processingOptionService.findOptionAsString(OptionName.EMAIL_SENDER_SALUTATION),
                    samplePairsWontProcess: getSamplePairRepresentation(samplePairs[false]*.samplePair.findAll {
                        !it.processingDisabled
                    }),
            ])
        }

        alignmentInfoByConfig.keySet()*.pipeline*.name.sort().unique().each {
            switch (it) {
                case Pipeline.Name.PANCAN_ALIGNMENT:
                    message += '\n' + messageSourceService.createMessage("notification.template.references.alignment.pancan")
                    break
                case Pipeline.Name.RODDY_RNA_ALIGNMENT: // no documentation/code available
                case Pipeline.Name.EXTERNALLY_PROCESSED: // alignment was done externally
                    break
                case Pipeline.Name.CELL_RANGER:
                    message += '\n' + messageSourceService.createMessage("notification.template.references.alignment.cellRanger")
                    break
                default:
                    log.error("Alignment pipeline ${it} is unknown for notification")
                    break
            }
        }

        return message
    }

    private String cellRangerAlignmentNotificationHelper(Set<SingleCellBamFile> bams) {
        if (!bams) {
            return ""
        }
        String finalRunSelectionLink = createOtpLinks(bams*.project, "cellRanger", "finalRunSelection")

        List<String> message = []
        message << messageSourceService.createMessage("notification.template.annotation.cellRanger.selfservice", [finalRunSelectionLink: finalRunSelectionLink])
        if (bams*.mergingWorkPackage.any { it.status == CellRangerMergingWorkPackage.Status.FINAL }) {
            message << messageSourceService.createMessage("notification.template.annotation.cellRanger.selfservice.alreadyFinal", [
                    serviceMail: processingOptionService.findOptionAsString(OptionName.EMAIL_RECIPIENT_NOTIFICATION),
            ])
        }
        return message.join('\n\n')
    }

    String snvNotification(ProcessingStatus status) {
        return variantCallingNotification(status, SNV, 'notification.template.references.snv')
    }

    String indelNotification(ProcessingStatus status) {
        return variantCallingNotification(status, INDEL, 'notification.template.references.indel')
    }

    String sophiaNotification(ProcessingStatus status) {
        return variantCallingNotification(status, SOPHIA, 'notification.template.references.sophia')
    }

    String aceseqNotification(ProcessingStatus status) {
        return variantCallingNotification(status, ACESEQ, 'notification.template.references.aceseq')
    }

    String runYapsaNotification(ProcessingStatus status) {
        return variantCallingNotification(status, RUN_YAPSA, 'notification.template.references.runyapsa')
    }

    String variantCallingNotification(ProcessingStatus status, ProcessingStep notificationStep, String additionalInfo = null) {
        assert status

        List<SeqType> seqTypes = SeqTypeService."${notificationStep}PipelineSeqTypes"
        Map<WorkflowProcessingStatus, List<SamplePairProcessingStatus>> map =
                status.samplePairProcessingStatuses.findAll {
                    it.samplePair.seqType in seqTypes
                }.groupBy { it."${notificationStep}ProcessingStatus" }

        List<SamplePair> samplePairsFinished = map[ALL_DONE]*.samplePair
        if (!samplePairsFinished) {
            log.debug("No ${notificationStep.displayName} finished yet.")
            return ''
        }
        String directories = variantCallingDirectories(samplePairsFinished, notificationStep)
        String message = messageSourceService.createMessage("notification.template.step.processed", [
                displayName        : notificationStep.displayName,
                samplePairsFinished: getSamplePairRepresentation(samplePairsFinished),
        ])
        if (notificationStep.controllerName && notificationStep.actionName) {
            message += messageSourceService.createMessage("notification.template.step.processed.results.links", [
                    displayName: notificationStep.displayName,
                    otpLinks   : createOtpLinks(samplePairsFinished*.project, notificationStep.controllerName, notificationStep.actionName),
            ])
        }
        message += messageSourceService.createMessage("notification.template.step.processed.results.directories", [
                directories: directories,
        ])

        List<SamplePair> samplePairsNotProcessed = map[NOTHING_DONE_WONT_DO]*.samplePair.findAll {
            it."${notificationStep}ProcessingStatus" != SamplePair.ProcessingStatus.DISABLED
        }
        if (samplePairsNotProcessed) {
            message += '\n' + messageSourceService.createMessage("notification.template.step.notProcessed", [
                    notificationSubject    : notificationStep.notificationSubject,
                    samplePairsNotProcessed: getSamplePairRepresentation(samplePairsNotProcessed),
                    emailSenderSalutation  : processingOptionService.findOptionAsString(OptionName.EMAIL_SENDER_SALUTATION),
            ])
        }

        if (additionalInfo) {
            message += '\n' + messageSourceService.createMessage(additionalInfo)
        }

        return message
    }

    String references(String referencesKey) {
        assert referencesKey
        return messageSourceService.createMessage(referencesKey)
    }

    String createOtpLinks(List<Project> projects, String controller, String action, Map params = [:]) {
        assert projects
        assert controller
        assert action

        return projects*.name.unique().sort().collect {
            linkGenerator.link([
                    controller: controller,
                    action    : action,
                    absolute  : true,
                    params    : [
                            (ProjectSelectionService.PROJECT_SELECTION_PARAMETER): it,
                    ] + params
            ])
        }.join('\n')
    }

    String getSampleName(SeqTrack seqTrack) {
        assert seqTrack
        return "${seqTrack.individual.displayName} ${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayNameWithLibraryLayout}"
    }

    @SuppressWarnings('JavaIoPackageAccess')
    String getSeqTypeDirectories(List<SeqTrack> seqTracks) {
        assert seqTracks

        return DataFile.findAllBySeqTrackInList(seqTracks).collect { DataFile file ->
            String basePath = file.project.projectSequencingDirectory
            String seqTypeDir = lsdfFilesService.seqTypeDirectory(file)
            new File("${basePath}/${seqTypeDir}/")
        }.unique().sort()*.path.join('\n')
    }

    @SuppressWarnings('GStringExpressionWithinString')
    String getMergingDirectories(List<AbstractBamFile> bamFiles) {
        assert bamFiles
        String pid = '${PID}'
        String sampleType = '${SAMPLE_TYPE}'

        return bamFiles.collect {
            String projectDir = it.project.projectDirectory
            String seqTypeDir = it.seqType.dirName
            String layout = it.seqType.libraryLayoutDirName
            String antiBodyTarget = it.seqType.hasAntibodyTarget ? '-${ANTI_BODY_TARGET}' : ''
            "${projectDir}/sequencing/" +
                    "${seqTypeDir}/" +
                    "view-by-pid/${pid}/" +
                    "${sampleType}${antiBodyTarget}/" +
                    "${layout}/merged-alignment"
        }.unique().sort().join('\n')
    }

    String variantCallingDirectories(List<SamplePair> samplePairsFinished, ProcessingStep notificationStep) {
        assert samplePairsFinished
        assert notificationStep
        return (samplePairsFinished*."${notificationStep}SamplePairPath"*.absoluteDataManagementPath as List<File>)
                .unique().sort()*.path.join('\n')
    }

    String getSamplePairRepresentation(List<SamplePair> samplePairs) {
        return samplePairs.collect { SamplePair samplePair ->
            "${samplePair.individual.displayName} ${samplePair.sampleType1.displayName} ${samplePair.sampleType2.displayName} " +
                    "${samplePair.seqType.displayNameWithLibraryLayout}"
        }.sort().unique().join('\n')
    }
}
