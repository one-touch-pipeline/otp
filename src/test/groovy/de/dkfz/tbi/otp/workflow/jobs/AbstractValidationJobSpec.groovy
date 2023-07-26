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
package de.dkfz.tbi.otp.workflow.jobs

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Paths

class AbstractValidationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
        ]
    }

    void "test execute, is successful"() {
        given:
        AbstractValidationJob job = Spy(AbstractValidationJob)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        job.execute(workflowStep)

        then:
        1 * job.ensureExternalJobsRunThrough(workflowStep) >> { }

        then:
        1 * job.getExpectedFiles(workflowStep) >> []
        1 * job.getExpectedDirectories(workflowStep) >> []
        1 * job.doFurtherValidation(workflowStep)

        then:
        1 * job.saveResult(workflowStep) >> null

        then:
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    void "test execute, fails in ensureExternalJobsRunThrough, no further check done"() {
        given:
        AbstractValidationJob job = Spy(AbstractValidationJob)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        WorkflowStep workflowStep = createWorkflowStep()

        when:
        job.execute(workflowStep)

        then:
        1 * job.ensureExternalJobsRunThrough(workflowStep) >> { throw new WorkflowException("pipeline check fail") }
        0 * job.getExpectedFiles(workflowStep)
        0 * job.getExpectedDirectories(workflowStep)
        0 * job.doFurtherValidation(workflowStep)
        0 * job.saveResult(workflowStep) >> null
        0 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)

        WorkflowException e = thrown(WorkflowException)
        e.message.contains("pipeline check fail")
    }

    void "test execute, ensureExternalJobsRunThrough fine, but file checks fail"() {
        given:
        AbstractValidationJob job = Spy(AbstractValidationJob)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        WorkflowStep workflowStep = createWorkflowStep()
        job.logService = Mock(LogService)

        when:
        job.execute(workflowStep)

        then:
        1 * job.ensureExternalJobsRunThrough(workflowStep) >> { }
        1 * job.getExpectedFiles(workflowStep) >> [Paths.get("file-not-found")]
        1 * job.getExpectedDirectories(workflowStep) >> [Paths.get("dir-not-found")]
        0 * job.doFurtherValidation(workflowStep)

        0 * job.saveResult(workflowStep) >> null
        0 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)

        WorkflowException e = thrown(WorkflowException)
        e.message.contains("file-not-found")
        e.message.contains("dir-not-found")
    }

    void "test execute, ensureExternalJobsRunThrough and file checks fine, but further validation fails"() {
        given:
        AbstractValidationJob job = Spy(AbstractValidationJob)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        WorkflowStep workflowStep = createWorkflowStep()
        job.logService = Mock(LogService)

        when:
        job.execute(workflowStep)

        then:
        1 * job.ensureExternalJobsRunThrough(workflowStep) >> { }
        1 * job.getExpectedFiles(workflowStep) >> []
        1 * job.getExpectedDirectories(workflowStep) >> []
        1 * job.doFurtherValidation(workflowStep) >> { throw new ValidationJobFailedException(["further-error1", "further-error2"].join("\n")) }

        0 * job.saveResult(workflowStep) >> null
        0 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)

        WorkflowException e = thrown(WorkflowException)
        e.message.contains("further-error1")
        e.message.contains("further-error2")
    }
}
