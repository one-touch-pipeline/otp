package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import groovy.text.*
import org.codehaus.groovy.grails.web.mapping.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.ngsdata.ProjectOverviewService.*
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.*
import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class CreateNotificationTextService {

    static final String BASE_NOTIFICATION_TEMPLATE = 'BaseNotificationTemplate'
    static final String INSTALLATION_NOTIFICATION_TEMPLATE = 'InstallationNotificationTemplate'
    static final String INSTALLATION_FURTHER_PROCESSING_TEMPLATE = 'InstallationFurtherProcessingTemplate'
    static final String ALIGNMENT_NOTIFICATION_TEMPLATE = 'AlignmentNotificationTemplate'
    static final String ALIGNMENT_FURTHER_PROCESSING_TEMPLATE = 'AlignmentFurtherProcessingTemplate'
    static final String ALIGNMENT_NO_FURTHER_PROCESSING_TEMPLATE = 'AlignmentNoFurtherProcessingTemplate'
    static final String ALIGNMENT_PROCESSING_INFORMATION_TEMPLATE = 'AlignmentProcessingInformationTemplate'
    static final String SNV_NOTIFICATION_TEMPLATE = 'SnvNotificationTemplate'
    static final String SNV_NOT_PROCESSED_TEMPLATE = 'SnvNotProcessedTemplate'


    @Autowired
    LinkGenerator linkGenerator

    ConfigService configService

    LsdfFilesService lsdfFilesService

    MergedAlignmentDataFileService mergedAlignmentDataFileService

    ProjectOverviewService projectOverviewService

    ProcessingOptionService processingOptionService


    String notification(OtrsTicket otrsTicket, ProcessingStatus status, OtrsTicket.ProcessingStep processingStep) {
        assert otrsTicket
        assert status
        assert processingStep

        String stepInformation = "${processingStep}Notification"(status).trim()
        String seqCenterComment = otrsTicket.seqCenterComment ? "\n\n${otrsTicket.seqCenterComment}" : ''

        return createMessage(BASE_NOTIFICATION_TEMPLATE, [
                stepInformation : stepInformation,
                seqCenterComment: seqCenterComment,
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
        String runNames = seqTracks*.run*.name.unique().sort().join(", ")
        String directories = getSeqTypeDirectories(seqTracks)
        String otpLinks = createOtpLinks(seqTracks*.project, 'projectOverview', 'laneOverview')

        String message = createMessage(INSTALLATION_NOTIFICATION_TEMPLATE, [
                runs    : runNames,
                paths   : directories,
                samples : samples.join('\n'),
                links   : otpLinks,
        ])

        if (status.alignmentProcessingStatus != NOTHING_DONE_WONT_DO) {
            message += '\n' + createMessage(INSTALLATION_FURTHER_PROCESSING_TEMPLATE, [:]).toString()
        }

        return message
    }


    String alignmentNotification(ProcessingStatus status) {
        assert status

        status = new ProcessingStatus(status.seqTrackProcessingStatuses.findAll {
            it.alignmentProcessingStatus == ALL_DONE })

        Collection<SeqTrack> seqTracks = status.seqTrackProcessingStatuses*.seqTrack
        String sampleNames = seqTracks.groupBy {
            getSampleName(it)
        }.sort().collect { String sample, List<SeqTrack> seqTracksOfSample ->
            "${sample}${getSampleIdentifiers(seqTracksOfSample)}"
        }.join('\n')
        String directories = getMergingDirectories(seqTracks)

        Collection<AbstractMergedBamFile> allGoodBamFiles =
                status.mergingWorkPackageProcessingStatuses*.completeProcessableBamFileInProjectFolder
        Map<AlignmentConfig, AlignmentInfo> alignmentInfoByConfig =
                allGoodBamFiles*.alignmentConfig.unique().collectEntries {
                    [it, projectOverviewService.getAlignmentInformationFromConfig(it)]
                }

        String links = createOtpLinks(allGoodBamFiles*.project, 'alignmentQualityOverview', 'index')

        Map<Project, List<AbstractMergedBamFile>> bamFilesByProject = allGoodBamFiles.groupBy { it.project }
        boolean multipleProject = bamFilesByProject.size() > 1
        StringBuilder builder = new StringBuilder()

        bamFilesByProject.sort { it.key.name }.each { Project project, List<AbstractMergedBamFile> projectBamFiles ->
            if (multipleProject) {
                builder << "\n***********************\n"
                builder << project
            }
            projectBamFiles.groupBy { it.seqType }.sort { it.key.displayName }.
                    each { SeqType seqType, List<AbstractMergedBamFile> seqTypeBamFiles ->
                seqTypeBamFiles.groupBy { it.alignmentConfig }.
                        each { AlignmentConfig config, List<AbstractMergedBamFile> configBamFiles ->
                    AlignmentInfo alignmentInfo = alignmentInfoByConfig.get(config)
                    builder << createMessage(ALIGNMENT_PROCESSING_INFORMATION_TEMPLATE, [
                            seqType           : seqType.displayName,
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

        String message = createMessage(ALIGNMENT_NOTIFICATION_TEMPLATE, [
                samples         : sampleNames,
                links           : links,
                processingValues: builder.toString().trim(),
                paths           : directories,
        ])

        Map<Boolean, List<SamplePairProcessingStatus>> samplePairs = status.samplePairProcessingStatuses.groupBy {
            it.snvProcessingStatus != NOTHING_DONE_WONT_DO }
        if (samplePairs[true]) {
            message += '\n' + createMessage(ALIGNMENT_FURTHER_PROCESSING_TEMPLATE, [
                    samplePairsWillProcess: getSamplePairRepresentation(samplePairs[true]*.samplePair),
            ])
        }
        if (samplePairs[false]) {
            message += '\n' + createMessage(ALIGNMENT_NO_FURTHER_PROCESSING_TEMPLATE, [
                    samplePairsWontProcess: getSamplePairRepresentation(samplePairs[false]*.samplePair.findAll {
                        it.processingStatus != SamplePair.ProcessingStatus.DISABLED }),
            ])
        }

        return message
    }


    String snvNotification(ProcessingStatus status) {
        assert status

        Map<WorkflowProcessingStatus, List<SamplePairProcessingStatus>> map =
                status.samplePairProcessingStatuses.groupBy { it.snvProcessingStatus }

        List<SamplePair> samplePairsFinished = map[ALL_DONE]*.samplePair
        String directories = snvDirectory(samplePairsFinished)
        String otpLinks = createOtpLinks(samplePairsFinished*.project, 'snv', 'results', 'projectName')
        String message = createMessage(SNV_NOTIFICATION_TEMPLATE, [
                samplePairsFinished    : getSamplePairRepresentation(samplePairsFinished),
                directories            : directories,
                otpLinks               : otpLinks,
        ])

        List<SamplePair> samplePairsNotProcessed = map[NOTHING_DONE_WONT_DO]*.samplePair.findAll {
            it.processingStatus != SamplePair.ProcessingStatus.DISABLED }
        if (samplePairsNotProcessed) {
            message += '\n' + createMessage(SNV_NOT_PROCESSED_TEMPLATE, [
                    samplePairsNotProcessed: getSamplePairRepresentation(samplePairsNotProcessed),
            ])
        }

        return message
    }


    String createMessage(String templateName, Map properties) {
        assert templateName

        String template = ProcessingOptionService.findOptionAssure(templateName, null, null)
        return new SimpleTemplateEngine().createTemplate(template).make(properties).toString()
    }


    String createOtpLinks(List<Project> projects, String controller, String action, String parameterProjectName = 'project') {
        assert projects
        assert controller
        assert action

        return projects*.name.unique().sort().collect {
            linkGenerator.link([
                    controller: controller,
                    action    : action,
                    absolute  : true,
                    params    : [
                            (parameterProjectName): it
                    ]
            ])
        }.join('\n')
    }


    String getSampleName(SeqTrack seqTrack) {
        assert seqTrack

        return "${seqTrack.individual.displayName} ${seqTrack.sampleType.displayName} ${seqTrack.seqType.displayName}"
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

    String getMergingDirectories(List<SeqTrack> seqTracks) {
        assert seqTracks

        return seqTracks.collect {
            LsdfFilesService.normalizePathForCustomers(new OtpPath(it.project, mergedAlignmentDataFileService.buildRelativePath(it.seqType, it.sample)).absoluteDataManagementPath)
        }.unique().sort()*.path.join('\n')
    }

    String snvDirectory(List<SamplePair> samplePairsFinished) {
        return samplePairsFinished*.getSamplePairPath()*.absoluteDataManagementPath.collect {
            LsdfFilesService.normalizePathForCustomers(it)
        }.unique().sort()*.path.join('\n')
    }

    String getSamplePairRepresentation(List<SamplePair> samplePairs) {
        return samplePairs.collect { SamplePair samplePair ->
            "${samplePair.individual.displayName} ${samplePair.sampleType1.displayName} ${samplePair.sampleType2.displayName} ${samplePair.seqType.displayName}"
        }.sort().unique().join('\n')
    }
}
