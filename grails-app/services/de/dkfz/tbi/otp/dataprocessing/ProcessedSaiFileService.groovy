package de.dkfz.tbi.otp.dataprocessing

import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

import de.dkfz.tbi.otp.ngsdata.*

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
        String sampleType = seqTrack.sample.sampleType.name.toLowerCase()
        String runName = seqTrack.run.name
        String filename = saiFile.dataFile.fileName
        filename = filename.substring(0, filename.lastIndexOf("."))
        return "${sampleType}_${runName}_${filename}.sai"
    }

    public ProcessedSaiFile createSaiFile(AlignmentPass alignmentPass, DataFile dataFile) {
        ProcessedSaiFile psf = new ProcessedSaiFile(
                alignmentPass: alignmentPass,
                dataFile: dataFile
            )
        assertSave(psf)
        return psf
    }

    /**
     * @return <code>null</code> if successful, otherwise an error message. (It is still possible
     * that this method throws an exception.)
     */
    public String updateSaiFileInfoFromDisk(ProcessedSaiFile saiFile) {
        File file = new File(getFilePath(saiFile))
        if (!file.canRead()) {
            return "${file} is not readable."
        }
        saiFile.fileExists = true
        saiFile.fileSize = file.length()
        saiFile.dateFromFileSystem = new Date(file.lastModified())
        assertSave(saiFile)
        final def fileSize = saiFile.fileSize
        return fileSize ? null : "File size of ${file} is ${fileSize}."
    }


    /**
     * Deletes the *.sai file and the *.sai_bwaAlnErrorLog.txt file from the "processing" directory on
     * the file system. Sets {@link ProcessedSaiFile#fileExists} to <code>false</code> and
     * {@link ProcessedSaiFile#deletionDate} to the current time.
     *
     * @return The number of bytes that have been freed on the file system.
     */
    // ProcessedBamFileService has a similar method.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long deleteProcessingFiles(final ProcessedSaiFile saiFile) {
        final Project project = saiFile.project
        final File fsSaiFile = new File(getFilePath(saiFile))
        if (dataProcessingFilesService.checkConsistencyForDeletion(saiFile, fsSaiFile)) {
            long freedBytes = 0L
            freedBytes += dataProcessingFilesService.deleteProcessingFile(project, bwaAlnErrorLogFilePath(saiFile))
            freedBytes += dataProcessingFilesService.deleteProcessingFile(project, fsSaiFile)
            saiFile.fileExists = false
            saiFile.deletionDate = new Date()
            assertSave saiFile
            return freedBytes
        } else {
            return 0L
        }
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
