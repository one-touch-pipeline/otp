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
import groovy.transform.TupleConstructor
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStateChangeService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class AbstractCleanUpJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
        ]
    }

    @TupleConstructor
    class TestAbstractCleanUpJob extends AbstractCleanUpJob {
        List<Path> files
        List<Path> dirs

        @Override
        List<Path> getFilesToDelete(WorkflowStep workflowStep) {
            return files
        }

        @Override
        List<Path> getDirectoriesToDelete(WorkflowStep workflowStep) {
            return dirs
        }
    }

    void "test execute"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        Path file = Paths.get("file")
        Path file2 = Paths.get("file2")
        Path dir = Paths.get("dir")
        AbstractCleanUpJob job = new TestAbstractCleanUpJob([file, file2], [dir])
        job.fileService = Mock(FileService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)

        when:
        job.execute(workflowStep)

        then:
        1 * job.fileService.deleteDirectoryRecursively(file)
        1 * job.fileService.deleteDirectoryRecursively(file2)
        1 * job.fileService.deleteDirectoryRecursively(dir)
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        0 * _
    }
}
