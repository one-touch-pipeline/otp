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
package de.dkfz.tbi.otp.dataprocessing.singleCell

import grails.gorm.transactions.Transactional
import groovy.transform.Synchronized

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile

import java.nio.file.*

@Transactional
class SingleCellMappingFileService {

    FileSystemService fileSystemService

    FileService fileService

    SingleCellService singleCellService

    /**
     * Adds the mapping entry for the given RawSequenceFile to the mapping file in a safe manner.
     *
     * It creates the mapping file if it does not already exist and only adds the entry
     * if it was not already added.
     *
     * @param rawSequenceFile to add entry for
     */
    @Synchronized
    void addMappingFileEntryIfMissing(RawSequenceFile rawSequenceFile) {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        Path mappingFile = fileService.changeFileSystem(singleCellService.singleCellMappingFile(rawSequenceFile), fileSystem)
        String value = singleCellService.mappingEntry(rawSequenceFile)

        if (!Files.exists(mappingFile)) {
            fileService.createFileWithContent(mappingFile, "", FileService.OWNER_READ_WRITE_GROUP_READ_FILE_PERMISSION)
        }

        if (!mappingFile.text.contains(value)) {
            if (!Files.isWritable(mappingFile)) {
                fileService.setPermission(mappingFile, FileService.OWNER_READ_WRITE_GROUP_READ_FILE_PERMISSION)
            }
            mappingFile << value << '\n'
        }
        fileService.setPermission(mappingFile, FileService.DEFAULT_FILE_PERMISSION)
    }
}
