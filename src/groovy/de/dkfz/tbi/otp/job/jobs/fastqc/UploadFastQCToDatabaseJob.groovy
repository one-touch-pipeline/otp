package de.dkfz.tbi.otp.job.jobs.fastqc

import de.dkfz.tbi.otp.ngsdata.*
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
            fastqcUploadService.uploadFileContentsToDataBase(file)
        }
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assert(seqTrack.save(flush: true))
        succeed()
    }
}
