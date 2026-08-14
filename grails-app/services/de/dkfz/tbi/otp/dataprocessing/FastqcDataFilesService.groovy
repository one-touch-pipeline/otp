/*
 * Copyright 2011-2026 The OTP authors
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

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.fastqc.FastqcWorkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * This service is used by jobs running "fastqc" program.
 * Implements all conventions of organization of fastqc output files.
 * Creates and updates "FastqcDataFile" object.
 * Serves content of fastqc zipped output file.
 */
@Transactional
class FastqcDataFilesService {

    FileService fileService

    FastqcWorkFileService fastqcWorkFileService

    void updateFastqcProcessedFiles(List<FastqcProcessedFile> fastqcList) {
        fastqcList?.each {
            updateFastqcProcessedFile(it)
        }
    }

    @CompileDynamic
    void updateFastqcProcessedFile(FastqcProcessedFile fastqc) {
        Path path = fastqcWorkFileService.fastqcOutputPath(fastqc)
        if (fileService.fileIsReadable(path)) {
            fastqc.fileExists = true
            fastqc.fileSize = Files.size(path)
            fastqc.dateFromFileSystem = new Date(Files.getLastModifiedTime(path).toMillis())
            fastqc.save(flush: true)
        }
    }

    /**
     * Returns an inputStream from the contents of a fastqc zip file
     *
     * The zip file is read sequentially via {@link ZipInputStream}, since neither {@link java.util.zip.ZipFile} nor the zip
     * {@link java.nio.file.FileSystem} can be used: both need random access to the file, which the remote (sftp) file system
     * does not provide.
     *
     * The returned stream is positioned at the requested entry and delivers exactly its content. It is owned by the caller,
     * who has to close it.
     *
     * @param withinZipPath Path to the resource within the zip file
     * @return An inputStream for the combination of zipPath and the withinZipPath parameters
     */
    InputStream getInputStreamFromZipFile(FastqcProcessedFile fastqcProcessedFile, String withinZipPath) {
        Path zipPath = fastqcWorkFileService.fastqcOutputPath(fastqcProcessedFile)

        if (!fileService.fileIsReadable(zipPath)) {
            throw new FileNotReadableException(zipPath.toString())
        }

        ZipInputStream zipStream = new ZipInputStream(Files.newInputStream(zipPath))
        boolean entryFound = false
        try {
            for (ZipEntry zipEntry = zipStream.nextEntry; zipEntry != null; zipEntry = zipStream.nextEntry) {
                if (zipEntry.name == withinZipPath || zipEntry.name.endsWith("/${withinZipPath}")) {
                    entryFound = true
                    return zipStream
                }
            }
            throw new CouldNotFindFastqcDataInZipFileException(zipPath, withinZipPath)
        } finally {
            if (!entryFound) {
                zipStream.close()
            }
        }
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
    @CompileDynamic
    InputStream getInputStreamFromZipFile(RawSequenceFile rawSequenceFile, String withinZipPath) {
        return getInputStreamFromZipFile(CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFile(rawSequenceFile)), withinZipPath)
    }
}
