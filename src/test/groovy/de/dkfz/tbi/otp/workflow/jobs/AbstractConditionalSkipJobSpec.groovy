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
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflow.shared.JobFailedException
import de.dkfz.tbi.otp.workflow.shared.SkipWorkflowStepException
import de.dkfz.tbi.otp.workflowExecution.*

class AbstractConditionalSkipJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    AbstractConditionalSkipJob job
    WorkflowStep workflowStep

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
                ProcessingPriority,
                Project,
                WorkflowRun,
        ]
    }

    void setup() {
        job = Spy(AbstractConditionalSkipJob)
        workflowStep = createWorkflowStep()
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
    }

    void "execute, should change the state to skipped, when SkipWorkflowStepException exception is thrown"() {
        given:
        WorkflowStepSkipMessage skipMessage = new WorkflowStepSkipMessage([
                message: 'test',
                category: WorkflowStepSkipMessage.Category.PREREQUISITE_WORKFLOW_RUN_NOT_SUCCESSFUL,
        ])

        when:
        job.execute(workflowStep)

        then:
        1 * job.checkRequirements(workflowStep) >> { throw new SkipWorkflowStepException(skipMessage) }
        1 * job.workflowStateChangeService.changeStateToSkipped(workflowStep, skipMessage)
    }

    void "execute, should throw exception when other exception than SkipWorkflowStepException is thrown"() {
        when:
        job.execute(workflowStep)

        then:
        thrown(OtpRuntimeException)

        and:
        1 * job.checkRequirements(workflowStep) >> { throw new JobFailedException('Job failed') }
        0 * job.workflowStateChangeService.changeStateToSkipped(_, _)
        0 * _
    }

    void "execute, should run through without changing state when no exception is thrown"() {
        when:
        job.execute(workflowStep)

        then:
        1 * job.checkRequirements(workflowStep) >> {
        }
        0 * _
    }
}
