package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import static org.springframework.util.Assert.notNull

class ProcessedSaiFileService {

    DataProcessingFilesService dataProcessingFilesService
    ProcessedAlignmentFileService processedAlignmentFileService

    public String getFilePath(ProcessedSaiFile saiFile) {
        String dir = getDirectory(saiFile)
        String filename = getFileName(saiFile)
        return "${dir}/${filename}"
    }

    /**
     * Retrieves the path to a log file used by bwa aln
     * (Although is not Philosophy of OTP to keep track of log files,
     * it is required by bwa since it produces not empty output files
     * even when it fails, and so we need to analyse the log file contents too)
     *
     * @param saiFile processed sai file object
     * @return Path to the outputted error file produced by bwa aln
     */
    public String bwaAlnErrorLogFilePath(ProcessedSaiFile saiFile) {
        return "${getFilePath(saiFile)}_bwaAlnErrorLog.txt"
    }

    public String getDirectory(ProcessedSaiFile saiFile) {
        return processedAlignmentFileService.getDirectory(saiFile.alignmentPass)
    }

    public String getFileName(ProcessedSaiFile saiFile) {
        SeqTrack seqTrack = saiFile.alignmentPass.seqTrack
        String sampleType = seqTrack.sample.sampleType.dirName
        String runName = seqTrack.run.name
        String filename = saiFile.dataFile.fileName
        filename = filename.substring(0, filename.lastIndexOf("."))
        return "${sampleType}_${runName}_${filename}.sai"
    }

    /**
     * Checks consistency for {@link #deleteProcessingFiles(ProcessedSaiFile)}.
     *
     * If there are inconsistencies, details are logged to the thread log (see {@link LogThreadLocal}).
     *
     * @return true if there is no serious inconsistency.
     */
    public boolean checkConsistencyForProcessingFilesDeletion(final ProcessedSaiFile saiFile) {
        notNull saiFile
        return dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(saiFile, new File(getFilePath(saiFile)))
    }

    /**
     * Deletes the *.sai file and the *.sai_bwaAlnErrorLog.txt file from the "processing" directory on
     * the file system. Sets {@link ProcessedSaiFile#fileExists} to <code>false</code> and
     * {@link ProcessedSaiFile#deletionDate} to the current time.
     *
     * @return The number of bytes that have been freed on the file system.
     */
    public long deleteProcessingFiles(final ProcessedSaiFile saiFile) {
        notNull saiFile
        return dataProcessingFilesService.deleteProcessingFiles(
                saiFile,
                new File(getFilePath(saiFile)),
                new File(bwaAlnErrorLogFilePath(saiFile)),
        )
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
