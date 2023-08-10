/*
 * Copyright 2011-2023 The OTP authors
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
import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * This service is used by jobs running "fastqc" program.
 * Implements all conventions of organization of fastqc output files.
 * Creates and updates "FastqcDataFile" object.
 * Serves content of fastqc zipped output file.
 */
@CompileDynamic
@Transactional
class FastqcDataFilesService {

    static final String FAST_QC_DIRECTORY_PART = DataProcessingFilesService.OutputDirectories.FASTX_QC.toString().toLowerCase()
    static final String FAST_QC_FILE_SUFFIX = "_fastqc"
    static final String FAST_QC_ZIP_SUFFIX = ".zip"
    static final String HTML_FILE_EXTENSION = ".html"
    static final String MD5SUM_FILE_EXTENSION = ".md5sum"

    LsdfFilesService lsdfFilesService
    FileService fileService
    FileSystemService fileSystemService
    FilestoreService filestoreService
    ConfigService configService

    Path fastqcOutputDirectory(FastqcProcessedFile fastqcProcessedFile, PathOption... options) {
        if (options.contains(PathOption.REAL_PATH) && fastqcProcessedFile.workflowArtefact.producedBy.workFolder) {
            return filestoreService.getWorkFolderPath(fastqcProcessedFile.workflowArtefact.producedBy)
        }
        Path baseString = lsdfFilesService.getRunDirectory(fastqcProcessedFile.sequenceFile)
        return baseString.resolve(FAST_QC_DIRECTORY_PART).resolve(fastqcProcessedFile.workDirectoryName)
    }

    Path fastqcOutputPath(FastqcProcessedFile fastqcProcessedFile, PathOption... options) {
        String fileName = fastqcFileName(fastqcProcessedFile)
        return fastqcOutputDirectory(fastqcProcessedFile, options).resolve(fileName)
    }

    Path fastqcHtmlPath(FastqcProcessedFile fastqcProcessedFile, PathOption... options) {
        String fileName = fastqcFileNameWithoutZipSuffix(fastqcProcessedFile).concat(HTML_FILE_EXTENSION)
        return fastqcOutputDirectory(fastqcProcessedFile, options).resolve(fileName)
    }

    Path fastqcOutputMd5sumPath(FastqcProcessedFile fastqcProcessedFile, PathOption... options) {
        String fileName = fastqcFileName(fastqcProcessedFile).concat(MD5SUM_FILE_EXTENSION)
        return fastqcOutputDirectory(fastqcProcessedFile, options).resolve(fileName)
    }

    protected String fastqcFileName(FastqcProcessedFile fastqcProcessedFile) {
        return "${fastqcFileNameWithoutZipSuffix(fastqcProcessedFile)}${FAST_QC_ZIP_SUFFIX}"
    }

    private String fastqcFileNameWithoutZipSuffix(FastqcProcessedFile fastqcProcessedFile) {
        return fastqcFileNameWithoutZipSuffix(inputFileNameAdaption(fastqcProcessedFile.sequenceFile.fileName))
    }

    /**
     * Remove suffix for compressed files
     */
    String inputFileNameAdaption(String fileName) {
        Integer suffixLength = CompressionFormat.getUsedFormat(fileName)?.suffix?.length()
        return suffixLength ? fileName[0..<-suffixLength] : fileName
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
        return "${body}${FAST_QC_FILE_SUFFIX}"
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

    void updateFastqcProcessedFiles(List<FastqcProcessedFile> fastqcList) {
        fastqcList?.each {
            updateFastqcProcessedFile(it)
        }
    }

    void updateFastqcProcessedFile(FastqcProcessedFile fastqc) {
        Path path = fastqcOutputPath(fastqc)
        if (fileService.fileIsReadable(path, configService.defaultRealm)) {
            fastqc.fileExists = true
            fastqc.fileSize = Files.size(path)
            fastqc.dateFromFileSystem = new Date(Files.getLastModifiedTime(path).toMillis())
            fastqc.save(flush: true)
        }
    }

    /**
     * Returns an inputStream from the contents of a fastqc zip file
     * @param withinZipPath Path to the resource within the zip file
     * @return An inputStream for the combination of zipPath and the withinZipPath parameters
     */
    InputStream getInputStreamFromZipFile(FastqcProcessedFile fastqcProcessedFile, String withinZipPath) {
        Path zipPath = fastqcOutputPath(fastqcProcessedFile)
        String zipEntryPath = "${fastqcFileNameWithoutZipSuffix(fastqcProcessedFile)}/${withinZipPath}"
        File input = new File(zipPath.toString())
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
     * Returns an inputStream from the contents of a fastqc zip file
     * @param rawSequenceFile The sequenceFile the zip file belongs to
     * @param withinZipPath Path to the resource within the zip file
     * @return An inputStream for the combination of zipPath and the withinZipPath parameters
     *
     * @Deprecated Please use {@link #fastqcFileNameWithoutZipSuffix(FastqcProcessedFile)}}
     */
    @Deprecated
    InputStream getInputStreamFromZipFile(RawSequenceFile rawSequenceFile, String withinZipPath) {
        return getInputStreamFromZipFile(CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile)), withinZipPath)
    }

    Path pathToFastQcResultFromSeqCenter(FastqcProcessedFile fastqcProcessedFile) {
        FileSystem fileSystem = fileSystemService.remoteFileSystemOnDefaultRealm
        String initialPath = fastqcProcessedFile.sequenceFile.initialDirectory
        String fastqcFileName = fastqcFileName(fastqcProcessedFile)
        return fileSystem.getPath(initialPath, fastqcFileName)
    }

    Path pathToFastQcResultMd5SumFromSeqCenter(FastqcProcessedFile fastqcProcessedFile) {
        Path path = pathToFastQcResultFromSeqCenter(fastqcProcessedFile)
        return path.resolveSibling(path.fileName.toString().concat(MD5SUM_FILE_EXTENSION))
    }
}
