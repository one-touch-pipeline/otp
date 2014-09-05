package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*
import java.util.zip.*

/**
 * This service is used by jobs running "fastqc" program.
 * Implements all conventions of organization of fastqc output files.
 * Creates and updates "FastqcDataFile" object.
 * Serves content of fastqc zipped output file.
 */

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
        /*
         * The fastqc tool does not allow to specify the output file name, only the output directory.
         * To access the file we need code to create the same name for the output file as the fastqc tool.
         * How the name is created from the input file name is looked up from the fastqc tool. The rule is in:
         * uk.ac.babraham.FastQC.Analysis.OfflineRunner.analysisComplete
         */
        String body = fileName.replaceAll(".gz\$", "").replaceAll(".bz2\$", "").replaceAll(".txt\$", "").
                        replaceAll(".fastq\$", "").replaceAll(".sam\$", "").replaceAll(".bam\$", "")
        return "${body}${fastqcFileSuffix}"
    }

    public Realm fastqcRealm(SeqTrack seqTrack) {
        Project project = seqTrack.project
        return configService.getRealmDataProcessing(project)
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
            fastqc.dateFromFileSystem = new Date(fastqcFile.lastModified())
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
       String zipBasePath = zipPath.substring(zipPath.lastIndexOf("/")+1, zipPath.lastIndexOf(".zip"))
       ZipEntry zipEntry = zipFile.getEntry("${zipBasePath}/${withinZipPath}")
       return zipFile.getInputStream(zipEntry)
   }
}
