/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflow.jobs

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

/**
 * Deletes files, directories that should not be kept
 */
abstract class AbstractCleanUpJob extends AbstractJob {

    @Autowired
    FileService fileService

    @Autowired
    FilestoreService filestoreService

    @Override
    final void execute(WorkflowStep workflowStep) {
        List<WorkFolder> workFolderToClear = getWorkFoldersToClear(workflowStep)
        List<Path> workFolderDirsToDelete = workFolderToClear.collect { filestoreService.getWorkFolderPath(it) }
        (getAdditionalPathsToDelete(workflowStep) + workFolderDirsToDelete).unique().each {
            fileService.deleteDirectoryRecursively(it)
        }
        resetWorkFoldersSize(workFolderToClear)
        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    private void resetWorkFoldersSize(List<WorkFolder> workFolders) {
        workFolders.each { workFolder ->
            workFolder.size = 0
            workFolder.save(flush: true)
        }
    }

    abstract List<Path> getAdditionalPathsToDelete(WorkflowStep workflowStep)

    abstract List<WorkFolder> getWorkFoldersToClear(WorkflowStep workflowStep)

    @Override
    final JobStage getJobStage() {
        return JobStage.CLEANUP
    }
}
