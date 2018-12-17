package de.dkfz.tbi.otp.job.jobs.dataInstallation

import grails.test.mixin.Mock
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateFileHelper

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
class CopyFilesJobSpec extends Specification {

    final long PROCESSING_STEP_ID = 1234567

    TestConfigService configService
    CopyFilesJob copyFilesJob
    ProcessingStep step

    @Rule
    TemporaryFolder temporaryFolder

    def setup() {
        step = DomainFactory.createProcessingStep(id: PROCESSING_STEP_ID)
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder("root").path])
        copyFilesJob = new CopyFilesJob()
        copyFilesJob.processingStep = step
        copyFilesJob.configService = configService
        copyFilesJob.lsdfFilesService = new LsdfFilesService()
        copyFilesJob.checksumFileService = new ChecksumFileService()
        copyFilesJob.checksumFileService.lsdfFilesService = copyFilesJob.lsdfFilesService
        copyFilesJob.fileService = new FileService()
        copyFilesJob.fileSystemService = new TestFileSystemService()
    }

    def cleanup() {
        configService.clean()
    }

    void "test checkInitialSequenceFiles where no data files are connected, should fail"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()

        when:
        copyFilesJob.checkInitialSequenceFiles(seqTrack)

        then:
        ProcessingException e = thrown()
        e.message.contains("No files in processing for seqTrack")
    }


    void "test checkInitialSequenceFiles where no files exist on the filesystem, should fail"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile()

        when:
        copyFilesJob.checkInitialSequenceFiles(seqTrack)

        then:
        ProcessingException e = thrown()
        e.message.contains("files are missing")
    }


    void "test checkInitialSequenceFiles where files exist on the filesystem, succeeds"() {
        given:
        SeqTrack seqTrack = createSeqTrack()

        when:
        copyFilesJob.checkInitialSequenceFiles(seqTrack)

        then:
        noExceptionThrown()
    }


    void "test maybe submit when file has to be copied and not exists already"() {
        given:
        createSeqTrack()
        copyFilesJob.clusterJobSchedulerService = Mock(ClusterJobSchedulerService) {
            1 * executeJob(_, _) >> { Realm realm, String command ->
                assert command ==~ """
#for debug kerberos problem
klist \\|\\| true


mkdir -p -m 2750 .*
cd .*
if \\[ -e ".*" \\]; then
    echo "File .* already exists."
    rm .*
fi
cp .* .*
md5sum .* > .*
chmod 440 .* .*
"""
            }
        }

        expect:
        NextAction.WAIT_FOR_CLUSTER_JOBS == copyFilesJob.maybeSubmit()
    }


    void "test maybe submit when file has to be linked and not exists already"() {
        given:
        SeqTrack seqTrack = createSeqTrack(true)
        copyFilesJob.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommand(_, _) >> { Realm realm, String command ->
                assert command ==~ """
#for debug kerberos problem
klist \\|\\| true


mkdir -p -m 2750 .*
cd .*
if \\[ -e ".*" \\]; then
    echo "File .* already exists."
    rm .*
fi
ln -s .* .*


"""
                DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)
                CreateFileHelper.createFile(new File(copyFilesJob.lsdfFilesService.getFileFinalPath(dataFile)))
            }
        }

        expect:
        NextAction.SUCCEED == copyFilesJob.maybeSubmit()
    }


    void "test maybe submit when file has to be linked and exists already"() {
        given:
        SeqTrack seqTrack = createSeqTrack(true)
        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)
        CreateFileHelper.createFile(new File(copyFilesJob.lsdfFilesService.getFileFinalPath(dataFile)))
        copyFilesJob.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommand(_, _) >> { Realm realm, String command ->
                assert command ==~ """
#for debug kerberos problem
klist \\|\\| true


mkdir -p -m 2750 .*
cd .*
if \\[ -e ".*" \\]; then
    echo "File .* already exists."
    rm .*
fi
ln -s .* .*


"""
            }
        }

        expect:
        NextAction.SUCCEED == copyFilesJob.maybeSubmit()
    }


    void "test validate when everything is valid"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)
        CreateFileHelper.createFile(new File(copyFilesJob.lsdfFilesService.getFileFinalPath(dataFile)))
        CreateFileHelper.createFile(new File(copyFilesJob.checksumFileService.pathToMd5File(dataFile)), "${dataFile.md5sum}  dataFileName")

        when:
        copyFilesJob.validate()

        then:
        noExceptionThrown()
    }

    void "test validate when md5Sum is not equal"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)
        CreateFileHelper.createFile(new File(copyFilesJob.lsdfFilesService.getFileFinalPath(dataFile)))
        CreateFileHelper.createFile(new File(copyFilesJob.checksumFileService.pathToMd5File(dataFile)), "${DomainFactory.DEFAULT_MD5_SUM}  dataFileName")

        when:
        copyFilesJob.validate()

        then:
        ProcessingException processingException = thrown()
        processingException.message.contains("Copying or linking of targetFile")
        processingException.message.contains("assert checksumFileService.compareMd5(dataFile)")
    }

    void "test validate when copying did not work"() {
        given:
        SeqTrack seqTrack = createSeqTrack()
        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)
        CreateFileHelper.createFile(new File(copyFilesJob.checksumFileService.pathToMd5File(dataFile)), "${dataFile.md5sum}  dataFileName")

        when:
        copyFilesJob.validate()

        then:
        ProcessingException processingException = thrown()
        processingException.message.contains("Copying or linking of targetFile")
        processingException.message.contains("not found")
    }


    private SeqTrack createSeqTrack(boolean hasToBeLinked = false) {
        SeqTrack seqTrack = DomainFactory.createSeqTrackWithOneDataFile(
                [linkedExternally: hasToBeLinked],
                [initialDirectory: temporaryFolder.newFolder().path]
        )

        DataFile dataFile = CollectionUtils.exactlyOneElement(seqTrack.dataFiles)
        CreateFileHelper.createFile(new File("${dataFile.initialDirectory}/${dataFile.fileName}"))

        DomainFactory.createProcessParameter([
                process: step.process, value: seqTrack.id, className: SeqTrack.class.name])


        return seqTrack
    }
}
