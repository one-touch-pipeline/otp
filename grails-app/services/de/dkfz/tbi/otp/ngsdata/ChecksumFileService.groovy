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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.ParsingException
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.utils.exceptions.FileIsEmptyException

import java.nio.file.*

import static org.springframework.util.Assert.notNull

@Transactional
class ChecksumFileService {

    LsdfFilesService lsdfFilesService
    FileSystemService fileSystemService

    String pathToMd5File(DataFile file) {
        String path = lsdfFilesService.getFileFinalPath(file)
        return "${path}.md5sum"
    }

    String md5FileName(DataFile file) {
        return "${file.fileName}.md5sum"
    }

    /**
     * example: BLOOD_SomePid_WHOLE_GENOME_PAIRED_merged.mdup.bam.md5sum
    */
    String md5FileName(String fileName) {
        return "${fileName}.md5sum"
    }

    boolean compareMd5(DataFile file) {
        String path = pathToMd5File(file)

        FileSystem fs = fileSystemService.filesystemForProcessingForRealm
        Path md5File = fs.getPath(path)

        FileService.ensureFileIsReadableAndNotEmpty(md5File)
        String md5sum
        List<String> lines = md5File.readLines()
        List<String> tokens = lines.get(0).tokenize()
        md5sum = tokens.get(0)
        return (md5sum.trim().toLowerCase(Locale.ENGLISH) == file.md5sum)
    }

    /**
     * @param file, the checksum file
     * @return the checksum of the first file, which is included in the checksum file
     */
    String firstMD5ChecksumFromFile(Path file) {
        notNull(file, "the input file for the method firstMD5ChecksumFromFile is null")
        if (!Files.isReadable(file)) {
            throw new FileNotReadableException("MD5 file \"${file}\" is not readable or does not exist")
        }
        if (Files.size(file) == 0) {
            throw new FileIsEmptyException("MD5 file \"${file}\" is empty")
        }
        String md5sum = file.readLines().get(0).tokenize().get(0)
        if (!(md5sum ==~ /^[0-9a-fA-F]{32}$/)) {
            throw new ParsingException("The format of the MD5sum of the MD5 file \"${file}\" is wrong: value=${md5sum}")
        }
        return md5sum
    }
}
