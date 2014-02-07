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
        List<DataFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
        for (DataFile file in files) {
            FastqcProcessedFile fastqc = getFastqcProcessedFile(file)
            fastqcUploadService.uploadFileContentsToDataBase(fastqc)
            fastqcDataFilesService.setFastqcProcessedFileUploaded(fastqc)
        }
        seqTrackService.setFastqcFinished(seqTrack)
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
