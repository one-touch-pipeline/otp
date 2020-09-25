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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService

import java.nio.file.*
import java.util.stream.Collectors
import java.util.stream.Stream

@Transactional
class CellRangerWorkflowService {

    FileSystemService fileSystemService

    FileService fileService


    void linkResultFiles(SingleCellBamFile singleCellBamFile) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)
        Path workDirectory = fileSystem.getPath(singleCellBamFile.workDirectory.absolutePath)
        Path resultDirectory = fileSystem.getPath(singleCellBamFile.resultDirectory.absolutePath)

        singleCellBamFile.getFileMappingForLinks().each { String linkName, String resultPathName ->
            Path link = workDirectory.resolve(linkName)
            Path target = resultDirectory.resolve(resultPathName)
            if (!Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
                fileService.createLink(link, target, singleCellBamFile.realm)
            }
        }
    }

    void cleanupOutputDirectory(SingleCellBamFile singleCellBamFile) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)
        Path outputDirectory = fileSystem.getPath(singleCellBamFile.outputDirectory.absolutePath)
        Path resultDirectory = fileSystem.getPath(singleCellBamFile.resultDirectory.absolutePath)

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
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)
        Path workDirectory = fileSystem.getPath(singleCellBamFile.workDirectory.absolutePath)
        fileService.deleteDirectoryRecursively(workDirectory)
    }

    void correctFilePermissions(SingleCellBamFile singleCellBamFile) {
        FileSystem fileSystem = fileSystemService.getRemoteFileSystem(singleCellBamFile.realm)
        Path workDirectory = fileSystem.getPath(singleCellBamFile.workDirectory.absolutePath)
        fileService.correctPathPermissionAndGroupRecursive(workDirectory, singleCellBamFile.realm, singleCellBamFile.project.unixGroup)
    }
}
