/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * This service is used by jobs running "fastqc" program.
 * Implements all conventions of organization of fastqc output files.
 * Creates and updates "FastqcDataFile" object.
 * Serves content of fastqc zipped output file.
 */
@Transactional
class FastqcDataFilesService {

    LsdfFilesService lsdfFilesService
    FileSystemService fileSystemService

    static final String FASTQC_FILE_SUFFIX = "_fastqc"
    static final String FASTQC_ZIP_SUFFIX = ".zip"


    String fastqcOutputDirectory(SeqTrack seqTrack) {
        def type = DataProcessingFilesService.OutputDirectories.FASTX_QC

        File baseString = lsdfFilesService.getFileViewByPidDirectory(seqTrack)
        return "${baseString}/${type.toString().toLowerCase()}"
    }

    String fastqcOutputFile(DataFile dataFile) {
        SeqTrack seqTrack = dataFile.seqTrack
        if (!seqTrack) {
            throw new ProcessingException("DataFile not assigned to a SeqTrack")
        }
        String base = fastqcOutputDirectory(seqTrack)
        String fileName = fastqcFileName(dataFile)
        return "${base}/${fileName}"
    }

    String fastqcFileName(DataFile dataFile) {
        return "${fastqcFileNameWithoutZipSuffix(dataFile)}${FASTQC_ZIP_SUFFIX}"
    }

    private String fastqcFileNameWithoutZipSuffix(DataFile dataFile) {
        return fastqcFileNameWithoutZipSuffix(inputFileNameAdaption(dataFile.fileName))
    }

    /**
     * Remove suffix for compressed files
     */
    String inputFileNameAdaption(String fileName) {
        Integer suffixLength = CompressionFormat.getUsedFormat(fileName)?.suffix?.length()
        if (suffixLength) {
            return fileName[0..<-suffixLength]
        } else {
            return fileName
        }
    }

    private String fastqcFileNameWithoutZipSuffix(String fileName) {
        /*
         * The fastqc tool does not allow to specify the output file name, only the output directory.
         * To access the file we need code to create the same name for the output file as the fastqc tool.
         * How the name is created from the input file name is looked up from the fastqc tool. The rule is in:
         * uk.ac.babraham.FastQC.Analysis.OfflineRunner.analysisComplete
         */
        String body = fileName.replaceAll("stdin:", "").replaceAll("\\.gz\$", "")
                .replaceAll("\\.bz2\$", "").replaceAll("\\.txt\$", "")
                .replaceAll("\\.fastq\$", "").replaceAll("\\.fq\$", "")
                .replaceAll("\\.csfastq\$", "").replaceAll("\\.sam\$", "")
                .replaceAll("\\.bam\$", "")
        return "${body}${FASTQC_FILE_SUFFIX}"
    }

    /**
     * Support for compression formats not supported by FastQC natively.
     */
    @TupleConstructor
    static enum CompressionFormat {
        // gzip compressed files are supported by FastQC, so they're not listed here
        /** The FastQC tool should work for bz2 files, we get often problems with this file type. Therefore we extract the files ourselves. */
        BZIP2(".bz2", "bzip2 --decompress"),
        TAR_BZIP2(".tar.bz2", "bzip2 --decompress | tar --extract --to-stdout"),
        TAR_GZIP(".tar.gz", "gzip --decompress | tar --extract --to-stdout"),

        final String suffix
        final String decompressionCommand

        static CompressionFormat getUsedFormat(String fileName) {
            // sort by length descending, so eg. ".tar.bz2" is found before ".bz2"
            return values().sort { -it.name().length() }.find { fileName.endsWith(it.suffix) }
        }
    }

    Realm fastqcRealm(SeqTrack seqTrack) {
        return seqTrack.project.realm
    }

    void createFastqcProcessedFile(DataFile dataFile) {
        assert (new FastqcProcessedFile(dataFile: dataFile).save(flush: true))
    }

    void updateFastqcProcessedFiles(List<FastqcProcessedFile> fastqcList) {
        fastqcList.each {
            updateFastqcProcessedFile(it)
        }
    }

    void updateFastqcProcessedFile(FastqcProcessedFile fastqc) {
        String path = fastqcOutputFile(fastqc.dataFile)
        File fastqcFile = new File(path)
        if (fastqcFile.canRead()) {
            fastqc.fileExists = true
            fastqc.fileSize = fastqcFile.length()
            fastqc.dateFromFileSystem = new Date(fastqcFile.lastModified())
        }
        assert (fastqc.save(flush: true))
    }

    FastqcProcessedFile getAndUpdateFastqcProcessedFile(DataFile dataFile, WorkflowArtefact workflowArtefact) {
        FastqcProcessedFile fastqc = CollectionUtils.exactlyOneElement(FastqcProcessedFile.findAllByDataFile(dataFile))
        fastqc.workflowArtefact = workflowArtefact
        updateFastqcProcessedFile(fastqc)
        return fastqc
    }

    FastqcProcessedFile getAndUpdateFastqcProcessedFile(DataFile dataFile) {
        FastqcProcessedFile fastqc = CollectionUtils.exactlyOneElement(FastqcProcessedFile.findAllByDataFile(dataFile))
        updateFastqcProcessedFile(fastqc)
        return fastqc
    }

    void setFastqcProcessedFileUploaded(FastqcProcessedFile fastqc) {
        fastqc.contentUploaded = true
        assert (fastqc.save(flush: true))
    }

    /**
     * Returns an inputStream from the contents of a fastqc zip file
     * @param dataFile The dataFile the zip file belongs to
     * @param withinZipPath Path to the resource within the zip file
     * @return An inputStream for the combination of zipPath and the withinZipPath parameters
     */
    InputStream getInputStreamFromZipFile(DataFile dataFile, String withinZipPath) {
        String zipPath = fastqcOutputFile(dataFile)
        String zipEntryPath = "${fastqcFileNameWithoutZipSuffix(dataFile)}/${withinZipPath}"
        File input = new File(zipPath)
        if (!input.canRead()) {
            throw new FileNotReadableException(input.path)
        }
        ZipFile zipFile = new ZipFile(input)
        ZipEntry zipEntry = zipFile.getEntry(zipEntryPath)
        if (!zipEntry) {
            throw new FileNotReadableException(zipEntryPath)
        }
        return zipFile.getInputStream(zipEntry)
    }

    /**
     * @Deprecated old workflow system
     */
    @Deprecated
    Path pathToFastQcResultFromSeqCenter(DataFile dataFile) {
        return pathToFastQcResultFromSeqCenter(fileSystemService.filesystemForFastqImport, dataFile)
    }

    Path pathToFastQcResultFromSeqCenter(FileSystem fileSystem, DataFile dataFile) {
        String fastqcFileName = fastqcFileName(dataFile)
        File pathToSeqCenterFastQcFile = new File(lsdfFilesService.getFileInitialPath(dataFile)).parentFile
        return fileSystem.getPath(pathToSeqCenterFastQcFile.path, fastqcFileName)
    }

    /**
     * @Deprecated old workflow system
     */
    @Deprecated
    Path pathToFastQcResultMd5SumFromSeqCenter(DataFile dataFile) {
        return pathToFastQcResultMd5SumFromSeqCenter(fileSystemService.filesystemForFastqImport, dataFile)
    }

    Path pathToFastQcResultMd5SumFromSeqCenter(FileSystem fileSystem, DataFile dataFile) {
        return fileSystem.getPath("${pathToFastQcResultFromSeqCenter(fileSystem, dataFile)}.md5sum")
    }
}
