package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.FastqcUploadService
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.ProcessHelperService
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import org.apache.commons.logging.impl.NoOpLog
import org.junit.After
import org.junit.Before
import org.junit.Test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class FastqcJobTest {

    @Autowired
    ApplicationContext applicationContext

    FastqcJob fastqcJob

    SeqTrack seqTrack
    DataFile dataFile

    @Before
    void setUp() {
        File testDirectory = TestCase.createEmptyTestDirectory()

        seqTrack = DomainFactory.createSeqTrack()


        RunSegment runSegment = DomainFactory.createRunSegment(
                run: seqTrack.run,
                dataPath: testDirectory.path,
        )

        dataFile = DomainFactory.createDataFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run, runSegment: runSegment])

        DomainFactory.createRealmDataProcessing([name: seqTrack.project.realmName])
        DomainFactory.createRealmDataManagement([name: seqTrack.project.realmName])

        fastqcJob = applicationContext.getBean('fastqcJob',
                DomainFactory.createAndSaveProcessingStep(FastqcJob.toString(), seqTrack), [])
        fastqcJob.log = new NoOpLog()

        ProcessingOptionService processingOptionService = new ProcessingOptionService()
        processingOptionService.createOrUpdate("fastqcCommand", null, null, "fastqc-0.10.1", "command for fastqc")

        WaitingFileUtils.metaClass.static.waitUntilExists = { File file -> true }
        ProcessHelperService.metaClass.static.executeAndAssertExitCodeAndErrorOutAndReturnStdout = { String cmd ->
            return "OK"
        }

        fastqcJob.lsdfFilesService.metaClass.ensureFileIsReadableAndNotEmpty = { File file ->
            return true
        }

        fastqcJob.lsdfFilesService.metaClass.ensureDirIsReadableAndNotEmpty = { File file ->
            return true
        }


    }

    @After
    void tearDown() {

        TestCase.removeMetaClass(ExecutionHelperService, fastqcJob.pbsService)
        TestCase.removeMetaClass(ExecutionService, fastqcJob.executionService)
        TestCase.removeMetaClass(LsdfFilesService, fastqcJob.lsdfFilesService)
        TestCase.removeMetaClass(FastqcUploadService, fastqcJob.fastqcUploadService)
        TestCase.removeMetaClass(FastqcJob, fastqcJob)
        WaitingFileUtils.metaClass = null
        ProcessHelperService.metaClass = null

        seqTrack = null
        dataFile = null
    }


    @Test
    void testMaybeSubmit_FastQcResultsNotAvailable_executesFastQcCommand() {
        fastqcJob.pbsService.metaClass.executeJob = { Realm inputRealm, String inputCommand ->
            assert inputCommand.contains("fastqc-0.10.1")
            return 'pbsJobId'
        }

        fastqcJob.executionService.metaClass.executeCommand = { Realm inputRealm, String command ->
            assert command.contains("umask 027; mkdir -p -m 2750")
            assert !command.contains("cp ")
        }

        fastqcJob.maybeSubmit()
    }


    @Test
    void testMaybeSubmit_FastQcResultsAvailable_executesCopyCommand() {
        File fastqcFile = CreateFileHelper.createFile(fastqcJob.fastqcDataFilesService.pathToFastQcResultFromSeqCenter(dataFile))

        fastqcJob.pbsService.metaClass.executeJob = { Realm inputRealm, String inputCommand ->
            assert false : "this method should not be reached"
        }

        fastqcJob.executionService.metaClass.executeCommand = { Realm inputRealm, String command ->
            assert command.contains("umask 027; mkdir -p -m 2750") || command.contains("cp ")
        }

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }

        try {

            fastqcJob.maybeSubmit()
            assert seqTrack.fastqcState == SeqTrack.DataProcessingState.FINISHED

        } finally {
            fastqcFile.delete()
            TestCase.cleanTestDirectory()
        }

    }


    @Test
    void testValidate_DataNotFromGPCF_shallBeUploadToDB() {
        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        assert fastqcProcessedFile.contentUploaded == false

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()

        assert fastqcProcessedFile.contentUploaded == true
        assert seqTrack.fastqcState == SeqTrack.DataProcessingState.FINISHED
    }
}
