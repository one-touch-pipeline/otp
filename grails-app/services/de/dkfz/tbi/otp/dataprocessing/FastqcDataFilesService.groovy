package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import java.util.zip.*

class FastqcDataFilesService {

    def configService
    def dataProcessingFilesService

    final String fastqcFileSuffix = "_fastqc.zip"

    public String fastqcOutputDirectory(SeqTrack seqTrack) {
        Individual individual = seqTrack.sample.individual
        def type = DataProcessingFilesService.OutputDirectories.FASTX_QC
        String baseString = dataProcessingFilesService.getOutputDirectory(individual, type)
        return "${baseString}/${seqTrack.run.name}"
    }

    public String fastqcOutputFile(DataFile dataFile) {
        SeqTrack seqTrack = dataFile.seqTrack
        if (!seqTrack) {
            ProcessingException("DataFile not assigned to a SeqTrack")
        }
        String base = fastqcOutputDirectory(seqTrack)
        String fileName = fastqcFileName(dataFile)
        return "${base}/${fileName}"
    }

    private String fastqcFileName(DataFile dataFile) {
        String fileName = dataFile.fileName
        String body = dataFile.fileName.substring(0, fileName.indexOf("."))
        return "${body}${fastqcFileSuffix}"
    }

    public Realm fastqcRealm(SeqTrack seqTrack) {
        Individual individual = seqTrack.sample.individual
        Realm realm = configService.getRealmDataProcessing(individual.project)
    }

    public void createFastqcProcessedFile(DataFile dataFile) {
        assert(new FastqcProcessedFile(dataFile: dataFile).save(flush: true))
    }

    public void updateFastqcProcessedFile(FastqcProcessedFile fastqc) {
        String path = fastqcOutputFile(fastqc.dataFile)
        File fastqcFile = new File(path)
        if (fastqcFile.canRead()) {
            fastqc.fileExists = true
            fastqc.fileSize = fastqcFile.length()
            fastqc.dateFileSystem = new Date(fastqcFile.lastModified())
        }
        assert(fastqc.save(flush: true))
    }

    public void setFastqcProcessedFileUploaded(FastqcProcessedFile fastqc) {
        fastqc.contentUploaded = true
        assert(fastqc.save(flush: true))
    }

    /**
    * Returns an inputStream from the contents of the zip file
    * @param zipPath Path to the zip file
    * @param withinZipPath Path to the resource within the zip file
    * @return An inputStream for the combination of zipPath and the withinZipPath parameters
    */
   public InputStream getInputStreamFromZip(String zipPath, String withinZipPath) {
       File input = new File(zipPath)
       if (!input.canRead()) {
           throw new FileNotReadableException(input.path)
       }
       ZipFile zipFile = new ZipFile(input)
       String zipBasePath = zipPath.substring(zipPath.lastIndexOf("/")+1,zipPath.lastIndexOf(".zip"))
       ZipEntry zipEntry = zipFile.getEntry("${zipBasePath}/${withinZipPath}")
       return zipFile.getInputStream(zipEntry)
   }
}
