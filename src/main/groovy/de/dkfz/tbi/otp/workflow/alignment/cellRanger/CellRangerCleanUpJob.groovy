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
package de.dkfz.tbi.otp.workflow.alignment.cellRanger

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.CellRangerWorkFileService
import de.dkfz.tbi.otp.workflow.jobs.AbstractCleanUpJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

@Component
@Slf4j
class CellRangerCleanUpJob extends AbstractCleanUpJob implements CellRangerShared {

    @Autowired
    CellRangerWorkFileService cellRangerWorkFileService

    @Autowired
    CellRangerLinkFileService cellRangerLinkFileService

    @Override
    List<Path> getAdditionalPathsToDelete(WorkflowStep workflowStep) {
        SingleCellBamFile singleCellBamFile = getBamFile(workflowStep)

        // Remove all directories in the output directory except the result directory
        Path outputDirectory = cellRangerLinkFileService.getOutputDirectory(singleCellBamFile)
        Path resultDirectory = cellRangerLinkFileService.getResultDirectory(singleCellBamFile)

        List<Path> pathsToDelete = Files.list(outputDirectory).withCloseable { stream ->
            stream.collect(Collectors.toList())
        }
        pathsToDelete.remove(resultDirectory)

        pathsToDelete.addAll(deleteOldLinkFolders(singleCellBamFile))
        return pathsToDelete
    }

    /**
     * Also remove all other SingleCellBamFile directories in the same merging work package
     */
    @CompileDynamic
    private List<Path> deleteOldLinkFolders(SingleCellBamFile singleCellBamFile) {
        List<SingleCellBamFile> singleCellBamFiles = SingleCellBamFile.findAllByWorkPackageAndIdNotEqual(
                singleCellBamFile.mergingWorkPackage, singleCellBamFile.id)
        return singleCellBamFiles.collect {
            cellRangerLinkFileService.getDirectoryPath(it)
        }.findAll()
    }

    @Override
    @CompileDynamic
    List<WorkFolder> getWorkFoldersToClear(WorkflowStep workflowStep) {
        SingleCellBamFile singleCellBamFile = getBamFile(workflowStep)

        List<SingleCellBamFile> singleCellBamFiles = SingleCellBamFile.findAllByWorkPackageAndIdNotEqual(singleCellBamFile.mergingWorkPackage,
                singleCellBamFile.id)
        return singleCellBamFiles.collect { cellRangerWorkFileService.getWorkFolder(it) }.findAll()
    }
}

