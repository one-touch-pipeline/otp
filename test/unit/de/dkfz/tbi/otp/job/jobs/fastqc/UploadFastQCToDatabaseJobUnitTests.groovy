package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsqc.FastqcUploadService
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.apache.commons.logging.Log
import org.junit.*
import static org.junit.Assert.*

@Mock([FastqcDataFilesService,
        SeqTrackService,
        ConfigService,
        DataProcessingFilesService,
        FastqcProcessedFile,
        DataFile,
        SeqTrack,
        FastqcUploadService
])
class UploadFastQCToDatabaseJobUnitTests {

    UploadFastQCToDatabaseJob job

    @Before
    void setUp() {
        def dataFile = [fileName: "sampleDataFile"] as DataFile

        def seqTrackService = [
                fileTypeService : new FileTypeService(),
                projectService: new ProjectService(),
                lsdfFilesService: new LsdfFilesService(),
                setFastqcFinished: { p -> },
                getSequenceFilesForSeqTrack: { p -> return [dataFile] }
        ] as SeqTrackService

        def fastqcDataFilesService = [
            getFastqcProcessedFile: { p -> },
            setFastqcProcessedFileUploaded: { p -> }
        ] as FastqcDataFilesService

        def fastqcUploadService = [
            uploadFileContentsToDataBase: { p -> }
        ] as FastqcUploadService

        job = new UploadFastQCToDatabaseJob(
                fastqcDataFilesService: fastqcDataFilesService,
                seqTrackService: seqTrackService,
                fastqcUploadService: fastqcUploadService
            )
        job.log = this.log

        UploadFastQCToDatabaseJob.metaClass.getProcessParameterValue = { -> "1" }
    }

    @After
    void tearDown() {
        job == null
    }

    void testExecuteFailureByFileDoesNotExist() {
        UploadFastQCToDatabaseJob.metaClass.getEndState = { -> null }
        UploadFastQCToDatabaseJob.metaClass.getFastqcProcessedFile = { DataFile dataFile2 -> throw new FileNotReadableException("pathToTheFile") }
        job.start()
        try {
            job.execute()
        } catch (FileNotReadableException e) {
            job.end()
            assertTrue(isJobFinished(job))
            assertTrue(!isJobStateSucessfull(job))
        }
    }

    void testExecuteSucess() {
        UploadFastQCToDatabaseJob.metaClass.getFastqcProcessedFile = { DataFile dataFile2 -> }
        job.start()
        job.execute()
        job.end()
        assertTrue(isJobFinished(job))
        assertTrue(isJobStateSucessfull(job))
    }

    private boolean isJobFinished(def job) {
        return job.getState() == AbstractJobImpl.State.FINISHED
    }

    private boolean isJobStateSucessfull(def job) {
        return job.endState == ExecutionState.SUCCESS
    }
}
