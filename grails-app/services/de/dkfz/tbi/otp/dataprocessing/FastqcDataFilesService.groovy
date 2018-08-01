package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

import java.nio.file.*
import java.util.zip.*

/**
 * This service is used by jobs running "fastqc" program.
 * Implements all conventions of organization of fastqc output files.
 * Creates and updates "FastqcDataFile" object.
 * Serves content of fastqc zipped output file.
 */

class FastqcDataFilesService {

    ConfigService configService
    DataProcessingFilesService dataProcessingFilesService
    LsdfFilesService lsdfFilesService
    FileSystemService fileSystemService

    final String FASTQC_FILE_SUFFIX = "_fastqc"
    final String FASTQC_ZIP_SUFFIX = ".zip"


    public String fastqcOutputDirectory(SeqTrack seqTrack) {
        def type = DataProcessingFilesService.OutputDirectories.FASTX_QC

        File baseString = lsdfFilesService.getFileViewByPidDirectory(seqTrack)
        return "${baseString}/${type.toString().toLowerCase()}"
    }

    public String fastqcOutputFile(DataFile dataFile) {
        SeqTrack seqTrack = dataFile.seqTrack
        if (!seqTrack) {
            throw new ProcessingException("DataFile not assigned to a SeqTrack")
        }
        String base = fastqcOutputDirectory(seqTrack)
        String fileName = fastqcFileName(dataFile)
        return "${base}/${fileName}"
    }

    public String fastqcFileName(DataFile dataFile) {
        return "${fastqcFileNameWithoutZipSuffix(dataFile)}${FASTQC_ZIP_SUFFIX}"
    }

    private String fastqcFileNameWithoutZipSuffix(DataFile dataFile) {
        String fileName = dataFile.fileName
        /*
         * The fastqc tool does not allow to specify the output file name, only the output directory.
         * To access the file we need code to create the same name for the output file as the fastqc tool.
         * How the name is created from the input file name is looked up from the fastqc tool. The rule is in:
         * uk.ac.babraham.FastQC.Analysis.OfflineRunner.analysisComplete
         */
        String body = fileName.replaceAll("stdin:","").replaceAll("\\.gz\$","")
                .replaceAll("\\.bz2\$","").replaceAll("\\.txt\$","")
                .replaceAll("\\.fastq\$", "").replaceAll("\\.fq\$", "")
                .replaceAll("\\.csfastq\$", "").replaceAll("\\.sam\$", "")
                .replaceAll("\\.bam\$", "");
        return "${body}${FASTQC_FILE_SUFFIX}"
    }

    public Realm fastqcRealm(SeqTrack seqTrack) {
        return seqTrack.project.realm
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

    public FastqcProcessedFile getAndUpdateFastqcProcessedFile(DataFile dataFile) {
        FastqcProcessedFile fastqc = CollectionUtils.exactlyOneElement(FastqcProcessedFile.findAllByDataFile(dataFile))
        updateFastqcProcessedFile(fastqc)
        return fastqc
    }

    public void setFastqcProcessedFileUploaded(FastqcProcessedFile fastqc) {
        fastqc.contentUploaded = true
        assert(fastqc.save(flush: true))
    }

    /**
     * Returns an inputStream from the contents of a fastqc zip file
     * @param dataFile The dataFile the zip file belongs to
     * @param withinZipPath Path to the resource within the zip file
     * @return An inputStream for the combination of zipPath and the withinZipPath parameters
     */
    public InputStream getInputStreamFromZipFile(DataFile dataFile, String withinZipPath) {
        String zipPath = fastqcOutputFile(dataFile)
        withinZipPath = "${fastqcFileNameWithoutZipSuffix(dataFile)}/${withinZipPath}"
        File input = new File(zipPath)
        if (!input.canRead()) {
            throw new FileNotReadableException(input.path)
        }
        ZipFile zipFile = new ZipFile(input)
        ZipEntry zipEntry = zipFile.getEntry(withinZipPath)
        if (!zipEntry) {
            throw new FileNotReadableException(withinZipPath)
        }
        return zipFile.getInputStream(zipEntry)
    }

    public Path pathToFastQcResultFromSeqCenter(DataFile dataFile) {
        String fastqcFileName = this.fastqcFileName(dataFile)
        File pathToSeqCenterFastQcFile = new File(lsdfFilesService.getFileInitialPath(dataFile)).parentFile
        return fileSystemService.filesystemForFastqImport.getPath(pathToSeqCenterFastQcFile.path, fastqcFileName)
    }

    public Path pathToFastQcResultMd5SumFromSeqCenter(DataFile dataFile) {
        return fileSystemService.filesystemForFastqImport.getPath("${pathToFastQcResultFromSeqCenter(dataFile)}.md5sum")
    }
}
