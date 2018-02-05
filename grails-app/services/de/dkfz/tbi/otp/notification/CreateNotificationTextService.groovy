package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep
import groovy.text.*
import org.codehaus.groovy.grails.web.mapping.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.ngsdata.ProjectOverviewService.*
import static de.dkfz.tbi.otp.tracking.OtrsTicket.ProcessingStep.*
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.*
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class CreateNotificationTextService {

    @Autowired
    LinkGenerator linkGenerator

    ConfigService configService

    LsdfFilesService lsdfFilesService

    ProjectOverviewService projectOverviewService

    ProcessingOptionService processingOptionService


    String notification(OtrsTicket otrsTicket, ProcessingStatus status, ProcessingStep processingStep, Project project) {
        assert otrsTicket
        assert status
        assert processingStep
        assert project

        String stepInformation = "${processingStep}Notification"(status).trim()

        String otrsTicketSeqCenterComment = otrsTicket.seqCenterComment ?: ""
        List<SeqCenter> seqCenters = otrsTicket.findAllSeqTracks()*.seqCenter.unique()
        String generalSeqCenterComment = seqCenters.size() == 1 ? ProcessingOptionService.findOptionSafe(
                OptionName.NOTIFICATION_TEMPLATE_SEQ_CENTER_NOTE,
                seqCenters.first().name,
                null
        ) ?: "" : ""
        String seqCenterComment = ""

        if (otrsTicketSeqCenterComment || generalSeqCenterComment) {
            String prefix = "\n\n******************************\nNote from sequencing center:\n"
            String suffix = "\n******************************"
            if (otrsTicketSeqCenterComment.replaceAll("[\r\n ]+", ' ').trim().contains(generalSeqCenterComment.replaceAll("[\r\n ]+", ' ').trim())) {
                seqCenterComment = otrsTicketSeqCenterComment
            } else {
                seqCenterComment = "${otrsTicketSeqCenterComment}${otrsTicketSeqCenterComment ? "\n" : ""}${generalSeqCenterComment}"
            }
            seqCenterComment = "${prefix}${seqCenterComment}${suffix}"
        }

        String addition = ProcessingOptionService.findOptionSafe(
                OptionName.NOTIFICATION_TEMPLATE_ADDITION,
                processingStep.notificationSubject,
                null
        ) ?: ""

        //TODO talk to research group which steps should be included
        String phabricatorAlias = ""
        if (project.phabricatorAlias && processingStep == INSTALLATION) {
            phabricatorAlias = "\n!project #\$${project.phabricatorAlias}"
        }

        return createMessage(OptionName.NOTIFICATION_TEMPLATE_BASE, [
                stepInformation : stepInformation,
                seqCenterComment: seqCenterComment,
                addition        : addition,
                phabricatorAlias: phabricatorAlias,
        ])
    }


    String installationNotification(ProcessingStatus status) {
        assert status

        status = new ProcessingStatus(status.seqTrackProcessingStatuses.findAll {
            it.installationProcessingStatus == ALL_DONE })

        List<String> samples = []
        status.seqTrackProcessingStatuses.groupBy {
            getSampleName(it.seqTrack)
        }.sort().each { String sample, List<SeqTrackProcessingStatus> seqTracksOfSample ->
            seqTracksOfSample.groupBy {
                it.alignmentProcessingStatus != NOTHING_DONE_WONT_DO
            }.each { boolean willAlign, List<SeqTrackProcessingStatus> seqTracksOfAlign ->
                samples.add("${willAlign ? '[A]' : '[-]'} ${sample}${getSampleIdentifiers(seqTracksOfAlign*.seqTrack)}")
            }
        }

        Collection<SeqTrack> seqTracks = status.seqTrackProcessingStatuses*.seqTrack
        if (!seqTracks) {
            throw new RuntimeException("No installation finished yet.")
        }
        String runNames = seqTracks*.run*.name.unique().sort().join(", ")
        String directories = getSeqTypeDirectories(seqTracks)
        String otpLinks = createOtpLinks(seqTracks*.project, 'projectOverview', 'laneOverview')

        String message = createMessage(OptionName.NOTIFICATION_TEMPLATE_INSTALLATION, [
                runs    : runNames,
                paths   : directories,
                samples : samples.join('\n'),
                links   : otpLinks,
        ])

        if (status.alignmentProcessingStatus != NOTHING_DONE_WONT_DO) {
            message += '\n' + createMessage(OptionName.NOTIFICATION_TEMPLATE_INSTALLATION_FURTHER_PROCESSING, [:]).toString()
        }

        return message
    }


    String alignmentNotification(ProcessingStatus status) {
        assert status

        status = new ProcessingStatus(status.seqTrackProcessingStatuses.findAll {
            it.alignmentProcessingStatus == ALL_DONE })

        Collection<SeqTrack> seqTracks = status.seqTrackProcessingStatuses*.seqTrack
        if (!seqTracks) {
            throw new RuntimeException("No alignment finished yet.")
        }
        String sampleNames = seqTracks.groupBy {
            getSampleName(it)
        }.sort().collect { String sample, List<SeqTrack> seqTracksOfSample ->
            "${sample}${getSampleIdentifiers(seqTracksOfSample)}"
        }.join('\n')

        Collection<AbstractMergedBamFile> allGoodBamFiles =
                status.mergingWorkPackageProcessingStatuses*.completeProcessableBamFileInProjectFolder
        Map<AlignmentConfig, AlignmentInfo> alignmentInfoByConfig =
                allGoodBamFiles*.alignmentConfig.unique().collectEntries {
                    [it, projectOverviewService.getAlignmentInformationFromConfig(it)]
                }

        String links = createOtpLinks(allGoodBamFiles*.project, 'alignmentQualityOverview', 'index')

        String directories = getMergingDirectories(allGoodBamFiles)

        Map<Project, List<AbstractMergedBamFile>> bamFilesByProject = allGoodBamFiles.groupBy { it.project }
        boolean multipleProject = bamFilesByProject.size() > 1
        StringBuilder builder = new StringBuilder()

        bamFilesByProject.sort { it.key.name }.each { Project project, List<AbstractMergedBamFile> projectBamFiles ->
            if (multipleProject) {
                builder << "\n***********************\n"
                builder << project
            }
            projectBamFiles.groupBy { it.seqType }.sort { it.key.displayNameWithLibraryLayout }.
                    each { SeqType seqType, List<AbstractMergedBamFile> seqTypeBamFiles ->
                Map<AlignmentConfig, List<AbstractMergedBamFile>> bamFilePerConfig = seqTypeBamFiles.groupBy { it.alignmentConfig }
                boolean multipleConfigs = bamFilePerConfig.size() > 1 && project.alignmentDeciderBeanName == "panCanAlignmentDecider"
                bamFilePerConfig.each { AlignmentConfig config, List<AbstractMergedBamFile> configBamFiles ->
                    AlignmentInfo alignmentInfo = alignmentInfoByConfig.get(config)
                    String individuals = multipleConfigs ? (config.individual ?: "default") : ""
                    builder << createMessage(OptionName.NOTIFICATION_TEMPLATE_ALIGNMENT_PROCESSING, [
                            seqType           : seqType.displayNameWithLibraryLayout,
                            individuals       : individuals,
                            referenceGenome   : configBamFiles*.referenceGenome.unique().join(', '),
                            alignmentProgram  : alignmentInfo.bwaCommand,
                            alignmentParameter: alignmentInfo.bwaOptions,
                            mergingProgram    : alignmentInfo.mergeCommand,
                            mergingParameter  : alignmentInfo.mergeOptions,
                            samtoolsProgram   : alignmentInfo.samToolsCommand,
                    ])
                }
            }
        }

        String message = createMessage(OptionName.NOTIFICATION_TEMPLATE_ALIGNMENT, [
                samples         : sampleNames,
                links           : links,
                processingValues: builder.toString().trim(),
                paths           : directories,
        ])

        Map<Boolean, List<SamplePairProcessingStatus>> samplePairs = status.samplePairProcessingStatuses.groupBy {
            it.variantCallingProcessingStatus != NOTHING_DONE_WONT_DO }
        if (samplePairs[true]) {
            String variantCallingPipelines = samplePairs[true]*.variantCallingWorkflowNames().flatten().unique().sort().join(', ')
            message += '\n' + createMessage(OptionName.NOTIFICATION_TEMPLATE_ALIGNMENT_FURTHER_PROCESSING, [
                    samplePairsWillProcess: getSamplePairRepresentation(samplePairs[true]*.samplePair),
                    variantCallingPipelines: variantCallingPipelines,
            ])
        }
        if (samplePairs[false]) {
            message += '\n' + createMessage(OptionName.NOTIFICATION_TEMPLATE_ALIGNMENT_NO_FURTHER_PROCESSING, [
                    samplePairsWontProcess: getSamplePairRepresentation(samplePairs[false]*.samplePair.findAll {
                        !it.processingDisabled }),
            ])
        }

        return message
    }


    String snvNotification(ProcessingStatus status) {
        return variantCallingNotification(status, SNV)
    }


    String indelNotification(ProcessingStatus status) {
        return variantCallingNotification(status, INDEL)
    }

    String sophiaNotification(ProcessingStatus status) {
        return variantCallingNotification(status, SOPHIA)
    }

    String aceseqNotification(ProcessingStatus status) {
        return variantCallingNotification(status, ACESEQ)
    }


    String variantCallingNotification(ProcessingStatus status, ProcessingStep notificationStep) {
        assert status

        Map<WorkflowProcessingStatus, List<SamplePairProcessingStatus>> map =
                status.samplePairProcessingStatuses.groupBy { it."${notificationStep}ProcessingStatus" }


        List<SamplePair> samplePairsFinished = map[ALL_DONE]*.samplePair
        if (!samplePairsFinished) {
            throw new RuntimeException("No ${notificationStep.displayName} finished yet.")
        }
        String directories = variantCallingDirectories(samplePairsFinished, notificationStep)
        String otpLinks = ''
        switch (notificationStep) {
            case SNV:
            otpLinks = createOtpLinks(samplePairsFinished*.project, 'snv', 'results')
                break
            case INDEL:
                otpLinks = createOtpLinks(samplePairsFinished*.project, 'indel', 'results')
                break
            case SOPHIA:
                otpLinks = createOtpLinks(samplePairsFinished*.project, 'sophia', 'results')
                break
            case ACESEQ:
                otpLinks = createOtpLinks(samplePairsFinished*.project, 'aceseq', 'results')
                break
            default:
                //no links
                break
        }
        String message = createMessage((OptionName) OptionName."NOTIFICATION_TEMPLATE_${notificationStep.name().toUpperCase()}_PROCESSED", [
                samplePairsFinished    : getSamplePairRepresentation(samplePairsFinished),
                directories            : directories,
                otpLinks               : otpLinks,
        ])

        List<SamplePair> samplePairsNotProcessed = map[NOTHING_DONE_WONT_DO]*.samplePair.findAll {
            it."${notificationStep}ProcessingStatus" != SamplePair.ProcessingStatus.DISABLED }
        if (samplePairsNotProcessed) {
            message += '\n' + createMessage((OptionName) OptionName."NOTIFICATION_TEMPLATE_${notificationStep.name().toUpperCase()}_NOT_PROCESSED", [
                    samplePairsNotProcessed: getSamplePairRepresentation(samplePairsNotProcessed),
            ])
        }

        return message
    }


    String createMessage(OptionName templateName, Map properties) {
        assert templateName

        String template = ProcessingOptionService.findOptionAssure(templateName, null, null)
        return new SimpleTemplateEngine().createTemplate(template).make(properties).toString()
    }


    String createOtpLinks(List<Project> projects, String controller, String action) {
        assert projects
        assert controller
        assert action

        return projects*.name.unique().sort().collect {
            linkGenerator.link([
                    controller: controller,
                    action    : action,
                    absolute  : true,
                    params    : [
                            'project': it
                    ]
            ])
        }.join('\n')
    }


    String getSampleName(SeqTrack seqTrack) {
        assert seqTrack

        return "${seqTrack.individual.displayName} ${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayNameWithLibraryLayout}"
    }

    String getSampleIdentifiers(Collection<SeqTrack> seqTracks) {
        assert seqTracks

        if (!PROJECT_TO_HIDE_SAMPLE_IDENTIFIER.contains(exactlyOneElement(seqTracks*.project.unique(), 'seqtracks must be of the same project').name)) {
            return ' (' + MetaDataEntry.createCriteria().list {
                projections {
                    dataFile {
                        'in'('seqTrack', seqTracks)
                    }
                    key {
                        eq('name', MetaDataColumn.SAMPLE_ID.name())
                    }
                    distinct('value')
                }
            }.sort().join(', ') + ')'
        } else {
            return ''
        }
    }

    String getSeqTypeDirectories(List<SeqTrack> seqTracks) {
        assert seqTracks

        return DataFile.findAllBySeqTrackInList(seqTracks).collect { DataFile file ->
            String basePath = configService.getProjectSequencePath(file.project)
            String seqTypeDir = lsdfFilesService.seqTypeDirectory(file)
            LsdfFilesService.normalizePathForCustomers("${basePath}/${seqTypeDir}/")
        }.unique().sort()*.path.join('\n')
    }

    String getMergingDirectories(List<AbstractBamFile> bamFiles) {
        assert bamFiles
        String pid = '${PID}'
        String sampleType = '${SAMPLE_TYPE}'

        return bamFiles.collect {
            String projectDir = it.project.projectDirectory
            String seqTypeDir = it.seqType.dirName
            String layout = it.seqType.libraryLayoutDirName
            String antiBodyTarget = it.seqType.isChipSeq() ? '-${ANTI_BODY_TARGET}' : ''
            LsdfFilesService.normalizePathForCustomers("${projectDir}/sequencing/${seqTypeDir}/view-by-pid/${pid}/${sampleType}${antiBodyTarget}/${layout}/merged-alignment").absolutePath
        }.unique().sort().join('\n')
    }

    String variantCallingDirectories(List<SamplePair> samplePairsFinished, ProcessingStep notificationStep) {
        return samplePairsFinished*."${notificationStep}SamplePairPath"*.absoluteDataManagementPath.collect {
            LsdfFilesService.normalizePathForCustomers((File) it)
        }.unique().sort()*.path.join('\n')
    }

    String getSamplePairRepresentation(List<SamplePair> samplePairs) {
        return samplePairs.collect { SamplePair samplePair ->
            "${samplePair.individual.displayName} ${samplePair.sampleType1.displayName} ${samplePair.sampleType2.displayName} ${samplePair.seqType.displayNameWithLibraryLayout}"
        }.sort().unique().join('\n')
    }
}
