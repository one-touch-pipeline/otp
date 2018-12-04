package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.*
import de.dkfz.tbi.otp.utils.*
import org.apache.commons.logging.impl.*
import org.junit.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*

class FastqcJobTest {

    @Autowired
    ApplicationContext applicationContext

    FastqcJob fastqcJob

    SeqTrack seqTrack
    DataFile dataFile
    RunSegment runSegment

    File testDirectory

    @Before
    void setUp() {
        testDirectory = TestCase.createEmptyTestDirectory()

        seqTrack = DomainFactory.createSeqTrack()
        runSegment = DomainFactory.createRunSegment()
        dataFile = DomainFactory.createDataFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run, runSegment: runSegment, initialDirectory: "${testDirectory.path}/${seqTrack.run.name}"])

        fastqcJob = applicationContext.getBean('fastqcJob')
        fastqcJob.processingStep = DomainFactory.createAndSaveProcessingStep(FastqcJob.toString(), seqTrack)
        fastqcJob.log = new NoOpLog()

        ProcessingOptionService processingOptionService = new ProcessingOptionService()
        processingOptionService.createOrUpdate(ProcessingOption.OptionName.COMMAND_FASTQC, "fastqc-0.10.1")

        WaitingFileUtils.metaClass.static.waitUntilExists = { File file -> true }
        LocalShellHelper.metaClass.static.executeAndAssertExitCodeAndErrorOutAndReturnStdout = { String cmd ->
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

        TestCase.removeMetaClass(ClusterJobSchedulerService, fastqcJob.clusterJobSchedulerService)
        TestCase.removeMetaClass(RemoteShellHelper, fastqcJob.remoteShellHelper)
        TestCase.removeMetaClass(LsdfFilesService, fastqcJob.lsdfFilesService)
        TestCase.removeMetaClass(FastqcUploadService, fastqcJob.fastqcUploadService)
        TestCase.removeMetaClass(FastqcJob, fastqcJob)
        WaitingFileUtils.metaClass = null
        LocalShellHelper.metaClass = null

        seqTrack = null
        dataFile = null
    }


    @Test
    void testMaybeSubmit_FastQcResultsNotAvailable_executesFastQcCommand() {
        fastqcJob.clusterJobSchedulerService.metaClass.executeJob = { Realm inputRealm, String inputCommand ->
            assert inputCommand.contains("fastqc-0.10.1")
            assert !inputCommand.contains("bzip2 --decompress --keep")
            assert !inputCommand.contains("rm -f")
            return 'pbsJobId'
        }

        fastqcJob.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm inputRealm, String command ->
            assert command.contains("umask 027; mkdir -p -m 2750")
            assert !command.contains("cp ")
            return new LocalShellHelper.ProcessOutput('','',0)
        }

        fastqcJob.maybeSubmit()
    }

    @Test
    void testMaybeSubmit_FastQcResultsNotAvailableAndDatafileIsBzip_executesFastQcCommandForBzip() {
        dataFile.vbpFileName = dataFile.fileName = 'file.bz2'
        fastqcJob.clusterJobSchedulerService.metaClass.executeJob = { Realm inputRealm, String inputCommand ->
            assert inputCommand.contains("fastqc-0.10.1")
            assert inputCommand.contains("bzip2 --decompress --keep")
            assert inputCommand.contains("rm -f")
            return 'pbsJobId'
        }

        fastqcJob.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm inputRealm, String command ->
            assert command.contains("umask 027; mkdir -p -m 2750")
            assert !command.contains("cp ")
            return new LocalShellHelper.ProcessOutput('','',0)
        }

        fastqcJob.maybeSubmit()
    }

    @Test
    void testMaybeSubmit_FastQcResultsAvailable_executesCopyCommand() {
        long nReads = 100
        String sequenceLength = "90"

        File fastqcFile = CreateFileHelper.createFile(
                new File(fastqcJob.fastqcDataFilesService.pathToFastQcResultFromSeqCenter(dataFile).toString())
        )

        dataFile.nReads = nReads
        dataFile.sequenceLength = sequenceLength
        assert dataFile.save(flush: true)

        fastqcJob.clusterJobSchedulerService.metaClass.executeJob = { Realm inputRealm, String inputCommand ->
            assert false : "this method should not be reached"
        }

        fastqcJob.remoteShellHelper.metaClass.executeCommandReturnProcessOutput = { Realm inputRealm, String command ->
            assert command.contains("umask 027; mkdir -p -m 2750") || command.contains("cp ")
            return new LocalShellHelper.ProcessOutput('','',0)
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
    void testValidate_FastqcAreCreatedByClusterJob_shallBeUploadToDB() {
        long nReads = 100
        String sequenceLength = "90"

        FastqcProcessedFile fastqcProcessedFile = DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        dataFile.nReads = nReads
        dataFile.sequenceLength = sequenceLength
        assert dataFile.save(flush: true)

        assert fastqcProcessedFile.contentUploaded == false

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()

        assert fastqcProcessedFile.contentUploaded == true
        assert seqTrack.fastqcState == SeqTrack.DataProcessingState.FINISHED
    }

    @Test
    void testValidate_totalSequencesIsSameForAllDataFiles_ShouldPassValidation() {
        long nReads = 100
        String sequenceLength = "90"

        dataFile.nReads = nReads
        dataFile.sequenceLength = sequenceLength
        dataFile.save(flush: true, failOnError: true)
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        DataFile dataFile2 = DomainFactory.createDataFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run, runSegment: runSegment, nReads: nReads, sequenceLength: sequenceLength, fileType: dataFile.fileType])
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile2])

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()
    }

    @Test
    void testValidate_totalSequencesIsDifferentForAllDataFiles_ShouldFail() {
        long nReads1 = 100
        long nReads2 = 105
        String sequenceLength = "90"

        dataFile.nReads = nReads1
        dataFile.sequenceLength = sequenceLength
        dataFile.save(flush: true, failOnError: true)
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        DataFile dataFile2 = DomainFactory.createDataFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run, runSegment: runSegment, nReads: nReads2, sequenceLength: sequenceLength])
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile2])

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }

        TestCase.shouldFail(AssertionError) {
            fastqcJob.validate()
        }
    }

    @Test
    void testValidate_sequenceLengthIsIntegerValueAndIsSameForAllDataFiles_ShouldPassValidation() {
        String sequenceLength = "100"
        long nReads = 100

        dataFile.sequenceLength = sequenceLength
        dataFile.nReads = nReads
        dataFile.save(flush: true, failOnError: true)
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        DataFile dataFile2 = DomainFactory.createDataFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run, runSegment: runSegment, sequenceLength: sequenceLength, nReads: nReads, fileType: dataFile.fileType])
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile2])

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()
    }

    @Test
    void testValidate_sequenceLengthIsRangeValueAndIsDifferentForAllDataFiles_ShouldPassValidation() {
        String sequenceLength1 = "0-50"
        String sequenceLength2 = "50-100"
        long nReads = 100

        dataFile.sequenceLength = sequenceLength1
        dataFile.nReads = nReads
        dataFile.save(flush: true, failOnError: true)
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        DataFile dataFile2 = DomainFactory.createDataFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run, runSegment: runSegment, sequenceLength: sequenceLength2, nReads: nReads, fileType: dataFile.fileType])
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile2])

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()
    }

    @Test
    void testValidate_sequenceLengthIsDifferentForAllDataFiles_ShouldPassValidation() {
        String sequenceLength1 = "50"
        String sequenceLength2 = "100"
        long nReads = 100

        dataFile.sequenceLength = sequenceLength1
        dataFile.nReads = nReads
        dataFile.save(flush: true, failOnError: true)
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile])

        DataFile dataFile2 = DomainFactory.createDataFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run, runSegment: runSegment, sequenceLength: sequenceLength2, nReads: nReads, fileType: dataFile.fileType])
        DomainFactory.createFastqcProcessedFile([dataFile: dataFile2])

        fastqcJob.fastqcUploadService.metaClass.uploadFastQCFileContentsToDataBase = { FastqcProcessedFile fastqc -> }
        fastqcJob.validate()
    }
}
