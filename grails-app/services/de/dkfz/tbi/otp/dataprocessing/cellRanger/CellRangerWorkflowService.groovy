/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService

import java.nio.file.*
import java.util.stream.Collectors
import java.util.stream.Stream

@Transactional
class CellRangerWorkflowService {

    CellRangerWorkFileService cellRangerWorkFileService
    FileService fileService
    FileSystemService fileSystemService

    void linkResultFiles(SingleCellBamFile singleCellBamFile) {
        Path workDirectory = cellRangerWorkFileService.getDirectoryPath(singleCellBamFile)
        Path resultDirectory = cellRangerWorkFileService.getResultDirectory(singleCellBamFile)
        String unixGroup = singleCellBamFile.project.unixGroup

        cellRangerWorkFileService.getFileMappingForLinks(singleCellBamFile).each { String linkName, String resultPathName ->
            Path link = workDirectory.resolve(linkName)
            Path target = resultDirectory.resolve(resultPathName)
            if (!Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
                fileService.createLink(link, target, unixGroup)
            }
        }
    }

    void cleanupOutputDirectory(SingleCellBamFile singleCellBamFile) {
        Path outputDirectory = cellRangerWorkFileService.getOutputDirectory(singleCellBamFile)
        Path resultDirectory = cellRangerWorkFileService.getResultDirectory(singleCellBamFile)

        Stream<Path> stream = null
        try {
            stream = Files.list(outputDirectory)
            List<Path> pathToDelete = stream.collect(Collectors.toList())
            assert pathToDelete.remove(resultDirectory)
            pathToDelete.each {
                fileService.deleteDirectoryRecursively(it)
            }
            assert Files.exists(resultDirectory)
        } finally {
            stream?.close()
        }
    }

    void deleteOutputDirectory(SingleCellBamFile singleCellBamFile) {
        Path workDirectory = cellRangerWorkFileService.getDirectoryPath(singleCellBamFile)
        fileService.deleteDirectoryRecursively(workDirectory)
    }

    void correctFilePermissions(SingleCellBamFile singleCellBamFile) {
        Path workDirectory = cellRangerWorkFileService.getDirectoryPath(singleCellBamFile)
        fileService.correctPathPermissionAndGroupRecursive(workDirectory, singleCellBamFile.project.unixGroup)
    }
}
