package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import static org.springframework.util.Assert.notNull

class ProcessedBamFileService {

    DataProcessingFilesService dataProcessingFilesService
    ProcessedAlignmentFileService processedAlignmentFileService
    ConfigService configService

    String getFilePath(ProcessedBamFile bamFile) {
        String dir = getDirectory(bamFile)
        String filename = getFileName(bamFile)
        return "${dir}/${filename}"
    }

    /**
     * Retrieves the path to a log file used by bwa sampe
     * (Although is not Philosophy of OTP to keep track of log files,
     * it is required by bwa since it produces not empty output files
     * even when it fails, and so we need to analyse the log file contents too)
     *
     * @param saiFile processed bam file object
     * @return Path to the outputted error file produced by bwa sampe
     */
    String bwaSampeErrorLogFilePath(ProcessedBamFile bamFile) {
        return "${getFilePath(bamFile)}_bwaSampeErrorLog.txt"
    }

    String baiFilePath(ProcessedBamFile bamFile) {
        return "${getFilePath(bamFile)}.bai"
    }

    String getDirectory(ProcessedBamFile bamFile) {
        return processedAlignmentFileService.getDirectory(bamFile.alignmentPass)
    }

    String getFileName(ProcessedBamFile bamFile) {
        String body = getFileNameNoSuffix(bamFile)
        return "${body}.bam"
    }

    String getFileNameNoSuffix(ProcessedBamFile bamFile) {
        SeqTrack seqTrack = bamFile.alignmentPass.seqTrack
        String sampleType = seqTrack.sample.sampleType.dirName
        String runName = seqTrack.run.name
        String lane = seqTrack.laneId
        String layout = seqTrack.seqType.libraryLayout
        String suffix = ""
        switch (bamFile.type) {
            case AbstractBamFile.BamType.SORTED:
                suffix = ".sorted"
                break
            case AbstractBamFile.BamType.RMDUP:
                suffix = ".sorted.rmdup"
                break
            case AbstractBamFile.BamType.MDUP:
                suffix = ".sorted.mdup"
                break
        }
        return "${sampleType}_${runName}_s_${lane}_${layout}${suffix}"
    }

    /**
     * Checks consistency for {@link #deleteProcessingFiles(ProcessedBamFile)}.
     *
     * If there are inconsistencies, details are logged to the thread log (see {@link LogThreadLocal}).
     *
     * @return true if there is no serious inconsistency.
     */
    boolean checkConsistencyForProcessingFilesDeletion(final ProcessedBamFile bamFile) {
        notNull bamFile
        return dataProcessingFilesService.checkConsistencyWithDatabaseForDeletion(bamFile, new File(getFilePath(bamFile)))
    }

    /**
     * Deletes the *.bam file, the *.bam.bai file and the *.bam_bwaSampeErrorLog.txt file from the
     * "processing" directory on the file system. Sets {@link ProcessedBamFile#fileExists} to
     * <code>false</code> and {@link ProcessedBamFile#deletionDate} to the current time.
     *
     * @return The number of bytes that have been freed on the file system.
     */
    long deleteProcessingFiles(final ProcessedBamFile bamFile) {
        notNull bamFile
        return dataProcessingFilesService.deleteProcessingFiles(
                bamFile,
                new File(getFilePath(bamFile)),
                new File(baiFilePath(bamFile)),
                new File(bwaSampeErrorLogFilePath(bamFile)),
        )
    }
}
