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
import io.swagger.client.wes.model.State
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.*

class AbstractWesValidationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    private AbstractWesValidationJob job

    private WorkflowStep workflowStepSendingClusterJob

    private WorkflowStep workflowStepValidatingClusterJob

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
        ]
    }

    private void setupData() {
        job = Spy(AbstractWesValidationJob)
        job.logService = Mock(LogService)

        workflowStepSendingClusterJob = createWorkflowStep()
        workflowStepValidatingClusterJob = createWorkflowStep([
                previous: workflowStepSendingClusterJob
        ])

        job.workflowStepService = Mock(WorkflowStepService) {
            _ * getPreviousRunningWorkflowStep(workflowStepValidatingClusterJob) >> workflowStepSendingClusterJob
        }
    }

    private void setupDataWithClusterJob(State state = State.COMPLETE, int exitCode = 0) {
        setupData()
        createWesRun(workflowStep: workflowStepSendingClusterJob, wesRunLog: createWesRunLog(
                runLog: createWesLog(exitCode: exitCode),
                state: state,
        ))
    }

    void "ensureExternalJobsRunThrough, when workflowStep has no WES jobs, then run successfully"() {
        given:
        setupData()

        when:
        job.ensureExternalJobsRunThrough(workflowStepValidatingClusterJob)

        then:
        noExceptionThrown()
    }

    @Unroll
    void "ensureExternalJobsRunThrough, when WES job is in state #state, then fail"() {
        given:
        setupDataWithClusterJob(state)

        when:
        job.ensureExternalJobsRunThrough(workflowStepValidatingClusterJob)

        then:
        ValidationJobFailedException e = thrown()
        e.message.contains(state.toString())

        where:
        state << State.values() - State.COMPLETE
    }

    void "ensureExternalJobsRunThrough, when WES job has exit code #exitCode, then fail"() {
        given:
        setupDataWithClusterJob(State.COMPLETE, exitCode)

        when:
        job.ensureExternalJobsRunThrough(workflowStepValidatingClusterJob)

        then:
        ValidationJobFailedException e = thrown()
        e.message.contains(exitCode.toString())

        where:
        exitCode << [
                1,
                123,
        ]
    }

    void "ensureExternalJobsRunThrough, when the WES job run successfully, then success"() {
        given:
        setupDataWithClusterJob()

        when:
        job.ensureExternalJobsRunThrough(workflowStepValidatingClusterJob)

        then:
        noExceptionThrown()
    }
}
