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

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
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
@Slf4j
abstract class AbstractPrepareJob extends AbstractJob {

    @Autowired
    FileService fileService

    @Autowired
    ProcessingOptionService processingOptionService

    @Override
    final void execute(WorkflowStep workflowStep) {
        Path workDirectory = buildWorkDirectoryPath(workflowStep)
        if (workDirectory) {
            logService.addSimpleLogEntry(workflowStep, "Creating work directory ${workDirectory}")
            fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(
                    workDirectory,
                    workflowStep.workflowRun.project.realm,
                    workflowStep.workflowRun.project.unixGroup,
            )
            workflowStep.workflowRun.workDirectory = workDirectory
            workflowStep.workflowRun.save(flush: true)

            boolean protectWorkDirectory = shouldWorkDirectoryBeProtected()
            if (protectWorkDirectory) {
                fileService.setGroupViaBash(
                        workDirectory,
                        workflowStep.workflowRun.project.realm,
                        processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)
                )
            }
        }
        generateMapForLinking(workflowStep).each { LinkEntry entry ->
            logService.addSimpleLogEntry(workflowStep, "Creating link ${entry.link} to ${entry.target}")
            fileService.createLink(
                    entry.link,
                    entry.target,
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

    /**
     * Indicate, if the work directory should be created a way that it is not accessible for project members.
     *
     * Most of our workflows do protect it, but there are two old workflows not using it.
     * Since all new one should protect it, the default value 'true' is here directly returned.
     */
    protected boolean shouldWorkDirectoryBeProtected() {
        return true
    }

    abstract protected Path buildWorkDirectoryPath(WorkflowStep workflowStep)

    abstract protected Collection<LinkEntry> generateMapForLinking(WorkflowStep workflowStep)

    abstract protected void doFurtherPreparation(WorkflowStep workflowStep)
}
