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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Paths
import java.text.NumberFormat

class CalculateSizeJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
        ]
    }

    void "execute, if workFolder exist, then calculate size of work folder"() {
        given:
        String workDir = "someWorkDir"
        long size = 1000000
        WorkFolder workFolder = createWorkFolder()
        WorkflowRun workflowRun = createWorkflowRun([
                workFolder   : workFolder,
                workDirectory: workDir,
        ])
        WorkflowStep workflowStep = createWorkflowStep(
                workflowRun: workflowRun,
        )

        CalculateSizeJob job = new CalculateSizeJob()
        job.fileSystemService = new TestFileSystemService()
        job.fileService = Mock(FileService) {
            1 * calculateSizeRecursive(Paths.get(workDir)) >> size
            0 * _
        }
        job.logService = Mock(LogService) {
            1 * addSimpleLogEntry(workflowStep, "Start calculation size of uuid folder: ${workDir}")
            1 * addSimpleLogEntry(workflowStep, "Calculated size: ${NumberFormat.getIntegerInstance().format(size)}")
            0 * _
        }
        job.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            1 * changeStateToSuccess(workflowStep)
            0 * _
        }

        when:
        job.execute(workflowStep)

        then:
        workFolder.size == size
    }

    void "execute, if no workFolder exist, then do nothing"() {
        given:
        String workDir = "someWorkDir"
        WorkflowRun workflowRun = createWorkflowRun([
                workFolder   : null,
                workDirectory: workDir,
        ])
        WorkflowStep workflowStep = createWorkflowStep(
                workflowRun: workflowRun,
        )

        CalculateSizeJob job = new CalculateSizeJob()
        job.fileService = Mock(FileService) {
            0 * _
        }
        job.logService = Mock(LogService) {
            1 * addSimpleLogEntry(workflowStep, "Skip size calculation, since no uuid work folder")
            0 * _
        }
        job.workflowStateChangeService = Mock(WorkflowStateChangeService) {
            1 * changeStateToSuccess(workflowStep)
            0 * _
        }

        when:
        job.execute(workflowStep)

        then:
        noExceptionThrown()
    }
}
