package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsqc.FastqcUploadService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 
 */
@Component("uploadFastQCToDatabaseJob")
//class UploadFastQCToDatabaseJob extends AbstractJobImpl {
class UploadFastQCToDatabaseJob extends AbstractEndStateAwareJobImpl {

    /** FastQC files service */
    @Autowired
    FastqcUploadService fastqcUploadService

    @Autowired
    FastqcDataFilesService fastqcDataFilesService

    @Autowired
    SeqTrackService seqTrackService

    /**
     * Check if all files are in the final location
     * @throws Exception
     */
    @Override
    public void execute() throws Exception {
        long seqTrackId =  Long.parseLong(getProcessParameterValue())
        SeqTrack seqTrack = SeqTrack.get(seqTrackId)
        List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
        for (DataFile file in files) {
            FastqcProcessedFile fastqc = getFastqcProcessedFile(file)
            fastqcUploadService.uploadFileContentsToDataBase(fastqc)
            setContentUploaded(fastqc)
        }
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assert(seqTrack.save(flush: true))
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
            String path = fastqcDataFilesService.fastqcFileName(fastqc.dataFile)
            throw new FileNotReadableException(path)
        }
    }

    private setContentUploaded(FastqcProcessedFile fastqc) {
        fastqc.contentUploaded = true
        assert(fastqc.save(flush: true))
    }
}
