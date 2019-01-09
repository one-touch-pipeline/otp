package de.dkfz.tbi.otp.job.jobs.dataInstallation

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        DataFile,
        FileType,
        Individual,
        JobDefinition,
        JobExecutionPlan,
        Process,
        ProcessingStep,
        ProcessParameter,
        Project,
        ProjectCategory,
        Realm,
        Run,
        RunSegment,
        Sample,
        SampleType,
        SeqCenter,
        SeqPlatformGroup,
        SeqPlatform,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SoftwareTool,
        SoftwareToolIdentifier,
])
class CreateViewByPidJobSpec extends Specification {

    final long PROCESSING_STEP_ID = 1234567

    CreateViewByPidJob createViewByPidJob
    ProcessingStep step

    def setup() {
        step = DomainFactory.createProcessingStep(id: PROCESSING_STEP_ID)
        createViewByPidJob = new CreateViewByPidJob()
        createViewByPidJob.processingStep = step

        createViewByPidJob.lsdfFilesService = new LsdfFilesService()
        createViewByPidJob.configService = new TestConfigService()
    }


    void "test execute when everything is fine"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile([:],
                [
                        fileExists    : true,
                        dateFileSystem: new Date(),
                        fileSize      : DomainFactory.counter++,
                ]
        )

        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)

        File source = new File(createViewByPidJob.lsdfFilesService.getFileFinalPath(dataFile))
        File target = new File(createViewByPidJob.lsdfFilesService.getFileViewByPidPath(dataFile))

        DomainFactory.createProcessParameter([
                process: step.process, value: seqTrack.id, className: SeqTrack.class.name])

        createViewByPidJob.linkFileUtils = Mock(LinkFileUtils) {
            1 * createAndValidateLinks(_, _) >> { Map<File, File> sourceLinkMap, Realm realm ->
                assert sourceLinkMap.size() == 1
                assert sourceLinkMap.containsKey(source)
                assert sourceLinkMap.containsValue(target)
            }
        }

        when:
        createViewByPidJob.execute()

        then:
        dataFile.fileLinked
        dataFile.dateLastChecked
        seqTrack.dataInstallationState == SeqTrack.DataProcessingState.FINISHED
        seqTrack.fastqcState == SeqTrack.DataProcessingState.NOT_STARTED
    }

}
