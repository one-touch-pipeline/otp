package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class ProcessedSaiFileService {

    def processedAlignmentFileService

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

    public boolean updateSaiFile(ProcessedSaiFile saiFile) {
        File file = new File(getFilePath(saiFile))
        if (!file.canRead()) {
            return false
        }
        saiFile.fileExists = true
        saiFile.fileSize = file.length()
        saiFile.dateFromFileSystem = new Date(file.lastModified())
        assertSave(saiFile)
        return saiFile.fileSize
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
