package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsqc.FastqcUploadService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 */
@Component("uploadFastQCToDatabaseJob")
class UploadFastQCToDatabaseJob extends AbstractEndStateAwareJobImpl {

    @Autowired
    FastqcUploadService fastqcUploadService

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    SeqTrackService seqTrackService

    @Override
    public void execute() throws Exception {
        long seqTrackId =  Long.parseLong(getProcessParameterValue())
        SeqTrack seqTrack = SeqTrack.get(seqTrackId)
        /*
         * Running too many fastqc uploads can hang OTP so they should be limited. The job system does not provide a
         * way to do it so the synchronization is done here. Although it would be OK to run several jobs in parallel
         * it is simpler to limit the count to one. Because the jobs run in the order of minutes and the chance of
         * multiple fastqc cluster jobs finishing at the same time is low this is not a problem.
         *
         * There are two cases when there are many jobs are done: After an (unclean) OTP restart and after restarting
         * failed jobs. In these cases jobs are collected over longer periods of time. Due to the short runtime these
         * situations should quickly resolve themself even with only a single job running.
         *
         * The synchronization is done on the service as a monitoring object. This requires the service to be a
         * singleton which is the default in Spring. The job is a prototype (own instance) and cannot be used as
         * monitoring object because of that.
         */
        synchronized (seqTrackService) {
            DataFile.withTransaction { //Ensure that all updates are executed together
                List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
                for (DataFile file in files) {
                    FastqcProcessedFile fastqc = getFastqcProcessedFile(file)
                    fastqcUploadService.uploadFileContentsToDataBase(fastqc)
                    fastqcDataFilesService.setFastqcProcessedFileUploaded(fastqc)
                }
                seqTrackService.setFastqcFinished(seqTrack)
                seqTrackService.setReadyForAlignment(seqTrack)
            }
        }
        succeed()
    }

    private FastqcProcessedFile getFastqcProcessedFile(DataFile dataFile) {
        FastqcProcessedFile fastqc = FastqcProcessedFile.findByDataFile(dataFile)
        fastqcDataFilesService.updateFastqcProcessedFile(fastqc)
        assertFileExists(fastqc)
        return fastqc
    }

    private assertFileExists(FastqcProcessedFile fastqc) {
        if (!fastqc.fileExists) {
            String path = fastqcDataFilesService.fastqcOutputFile(fastqc.dataFile)
            throw new FileNotReadableException(path)
        }
    }
}
