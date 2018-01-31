package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkFileUtils
import grails.test.mixin.*
import grails.test.mixin.integration.*
import org.joda.time.*
import org.junit.*


@Ignore
@TestMixin(IntegrationTestMixin)
class DataInstallationWorkflowTests extends WorkflowTestCase {

    LsdfFilesService lsdfFilesService


    // files to be processed by the tests
    String fastqR1Filepath
    String fastqR2Filepath

    String fastqR1Filename = "example_fileR1.fastq.gz"
    String fastqR2Filename = "example_fileR2.fastq.gz"

    private String md5sum(String filepath) {
        String cmdMd5sum = "md5sum ${filepath}"
        String output = executionService.executeCommand(realm, cmdMd5sum)
        String md5sum = output.split().first()
        return md5sum
    }

    @Before
    void setUp() {
        fastqR1Filepath = "${testDataDir}/35-3B_NoIndex_L007_R1_complete_filtered.fastq.gz"
        fastqR2Filepath = "${testDataDir}/35-3B_NoIndex_L007_R2_complete_filtered.fastq.gz"

        File softLinkFastqR1Filepath = new File("${ftpDir}/${fastqR1Filename}")
        File softLinkFastqR2Filepath = new File("${ftpDir}/${fastqR2Filename}")

        createDirectoriesString([ftpDir])
        linkFileUtils.createAndValidateLinks([(new File(fastqR1Filepath)): softLinkFastqR1Filepath, (new File(fastqR2Filepath)): softLinkFastqR2Filepath], realm)
    }


    DataFile createDataFile(SeqTrack seqTrack, Integer mateNumber, String fastqFilename, String fastqFilepath) {
        return DomainFactory.createDataFile([
            project: seqTrack.project,
            fileName: fastqFilename,
            md5sum: md5sum(fastqFilepath),
            seqTrack: seqTrack,
            vbpFileName: fastqFilename,
            fileExists: false,
            fileLinked: false,
            fileSize: 0,
            mateNumber: mateNumber,
            initialDirectory: ftpDir,
            run: seqTrack.run,
        ])
    }


    @Test
    void testDataInstallation_FilesHaveToBeCopied() {
        SeqTrack seqTrack = createWholeGenomeSetup()

        execute()

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    @Test
    void testDataInstallation_FilesHaveToBeLinked() {
        SeqTrack seqTrack = createWholeGenomeSetup()
        seqTrack.linkedExternally = true
        assert seqTrack.save(flush: true)

        execute()

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    @Test
    void testChipSeqInstallation() {
        ChipSeqSeqTrack seqTrack = DomainFactory.createChipSeqSeqTrack()
        createDataFiles(seqTrack)
        seqTrack.project.realm = realm
        assert seqTrack.project.save(flush: true)
        execute()

        checkThatWorkflowWasSuccessful(seqTrack)
    }

    @Test
    void testDataInstallationWithFastTrack() {
        SeqTrack seqTrack = createWholeGenomeSetup()
        seqTrack.project.processingPriority = ProcessingPriority.FAST_TRACK_PRIORITY
        assert seqTrack.project.save(flush: true)

        execute()

        checkThatWorkflowWasSuccessful(seqTrack)
    }


    private void checkThatWorkflowWasSuccessful(SeqTrack seqTrack) {
        seqTrack.refresh()
        assert seqTrack.dataInstallationState == SeqTrack.DataProcessingState.FINISHED
        assert SeqTrack.DataProcessingState.NOT_STARTED == seqTrack.fastqcState

        seqTrack.dataFiles.collect {
            it.refresh()
            assert it.fileExists
            assert it.fileLinked
            assert it.fileSize > 0
            [
                lsdfFilesService.getFileFinalPath(it),
                lsdfFilesService.getFileViewByPidPath(it)
            ]
        }.flatten().each {
            assert new File(it).exists()
        }
    }

    private void createDataFiles(SeqTrack seqTrack) {
        createDataFile(seqTrack, 1, fastqR1Filename, fastqR1Filepath)
        createDataFile(seqTrack, 2, fastqR2Filename, fastqR2Filepath)
    }


    protected SeqTrack createWholeGenomeSetup() {
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        SeqTrack seqTrack = DomainFactory.createSeqTrack([seqType: seqType])
        createDataFiles(seqTrack)
        seqTrack.project.realm = realm
        assert seqTrack.project.save(flush: true)
        return seqTrack
    }


    @Override
    List<String> getWorkflowScripts() {
        return ["scripts/workflows/DataInstallationWorkflow.groovy"]
    }

    @Override
    Duration getTimeout() {
        Duration.standardMinutes(30)
    }
}
