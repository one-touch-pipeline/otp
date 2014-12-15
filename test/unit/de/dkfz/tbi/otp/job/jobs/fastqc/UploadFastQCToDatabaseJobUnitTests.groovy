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

    SeqTrack seqTrack
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

        final long seqTrackId = 526065890
        job.metaClass.getProcessParameterValue = { -> Long.toString(seqTrackId) }
        SeqTrack.metaClass.static.get = { id -> assert id == seqTrackId; return seqTrack }
    }

    @After
    void tearDown() {
        SeqTrack.metaClass = null
        job == null
    }

    void testExecuteFailureByFileDoesNotExist() {
        job.metaClass.getEndState = { -> null }
        job.metaClass.getFastqcProcessedFile = { DataFile dataFile2 -> throw new FileNotReadableException("pathToTheFile") }
        job.start()
        try {
            job.execute()
        } catch (FileNotReadableException e) {
            job.end()
            assertTrue(isJobFinished(job))
            assertTrue(!isJobStateSuccessful(job))
        }
    }

    void testExecuteSuccess() {
        int setReadyForAlignmentCalls = 0
        job.metaClass.getFastqcProcessedFile = { DataFile dataFile2 -> }
        job.seqTrackService.metaClass.setReadyForAlignment = { SeqTrack s -> assert s == seqTrack; setReadyForAlignmentCalls++ }
        job.start()
        job.execute()
        job.end()
        assertTrue(isJobFinished(job))
        assertTrue(isJobStateSuccessful(job))
        assert setReadyForAlignmentCalls == 1
    }

    private boolean isJobFinished(def job) {
        return job.getState() == AbstractJobImpl.State.FINISHED
    }

    private boolean isJobStateSuccessful(def job) {
        return job.endState == ExecutionState.SUCCESS
    }
}
