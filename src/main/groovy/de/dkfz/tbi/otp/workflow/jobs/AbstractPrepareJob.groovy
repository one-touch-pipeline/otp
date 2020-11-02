/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.infrastructure.CreateLinkOption
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

/**
 * Prepares the file system
 *  - creates a working directory
 *  - set permissions so that users can't access the work directory while the job is running
 *  - link input files, if the job requires a certain file structure
 *  - create configuration files, if required
 */
abstract class AbstractPrepareJob extends AbstractJob {

    @Autowired
    FileService fileService

    @Override
    final void execute(WorkflowStep workflowStep) {
        Path workDirectory = buildWorkDirectoryPath(workflowStep)
        if (workDirectory) {
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(
                    workDirectory,
                    workflowStep.workflowRun.project.realm,
                    workflowStep.workflowRun.project.unixGroup,
            )
            workflowStep.workflowRun.workDirectory = workDirectory
            workflowStep.workflowRun.save(flush: true)
        }
        generateMapForLinking(workflowStep).each { LinkEntry entry ->
            fileService.createLink(
                    entry.target,
                    entry.link,
                    workflowStep.workflowRun.project.realm,
                    CreateLinkOption.DELETE_EXISTING_FILE,
            )
        }
        doFurtherPreparation(workflowStep)
        workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    @Override
    final JobStage getJobStage() {
        return JobStage.PREPARE
    }

    abstract protected Path buildWorkDirectoryPath(WorkflowStep workflowStep)

    abstract protected Collection<LinkEntry> generateMapForLinking(WorkflowStep workflowStep)

    abstract protected void doFurtherPreparation(WorkflowStep workflowStep)
}
