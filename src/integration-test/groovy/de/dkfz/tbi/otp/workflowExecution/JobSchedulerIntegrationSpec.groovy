/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import spock.lang.Unroll

import de.dkfz.tbi.otp.AbstractIntegrationSpecWithoutRollbackAnnotation
import de.dkfz.tbi.otp.FileNotFoundException
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.workflow.restartHandler.AutoRestartHandlerService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException

class JobSchedulerIntegrationSpec extends AbstractIntegrationSpecWithoutRollbackAnnotation implements WorkflowSystemDomainFactory {

    final static String JOB_ERROR_MESSAGE = "file not found"
    final static String BEAN_NAME = "testBean"

    @Autowired
    WorkflowStateChangeService workflowStateChangeService

    JobScheduler jobScheduler

    @Unroll
    void "test executeAndCheckJob, test executeAndCheckJob and handleException, when the job fails, the workflow status should be set to FAILED and should not be rolled back on restartError: #restartError and/or notificationError: #notificationError"() {
        given:
        jobScheduler = new JobScheduler()
        jobScheduler.autoRestartHandlerService = Mock(AutoRestartHandlerService)
        jobScheduler.applicationContext = Mock(ApplicationContext)
        jobScheduler.logService = Mock(LogService)
        jobScheduler.workflowStateChangeService = workflowStateChangeService

        SessionUtils.withNewSession {
            createWorkflowStep(beanName: BEAN_NAME, state: WorkflowStep.State.CREATED)
        }

        when:
        WorkflowStep step = null
        SessionUtils.withNewSession {
            step = CollectionUtils.exactlyOneElement(WorkflowStep.findAllByBeanName(BEAN_NAME))
        }
        jobScheduler.executeAndCheckJob(step)

        then:
        _ * jobScheduler.applicationContext.getBean(_, _) >> { throw new FileNotFoundException(JOB_ERROR_MESSAGE) }
        _ * jobScheduler.autoRestartHandlerService.handleRestarts(_ as WorkflowStep) >> {
            if (restartError) {
                throw new WorkflowException("some workflow failure")
            }
        }

        and:
        SessionUtils.withNewSession {
            WorkflowStep workflowStep = CollectionUtils.exactlyOneElement(WorkflowStep.findAllByBeanName(BEAN_NAME))
            assert workflowStep.workflowRun.state == WorkflowRun.State.FAILED
            assert workflowStep.state == WorkflowStep.State.FAILED
            assert workflowStep?.workflowError?.message == JOB_ERROR_MESSAGE
            return workflowStep
        }

        where:
        restartError | notificationError
        true         | true
        true         | false
        false        | true
        false        | false
    }
}
