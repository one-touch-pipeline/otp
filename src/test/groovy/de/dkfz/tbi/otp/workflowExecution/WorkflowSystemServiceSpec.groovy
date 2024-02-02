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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.config.PropertiesValidationService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflow.shared.WorkflowTestException

class WorkflowSystemServiceSpec extends Specification implements ServiceUnitTest<WorkflowSystemService>, DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep
        ]
    }

    void "hasRunAfterStart, when system started, then it should return false"() {
        expect:
        !service.hasRunAfterStart()
    }

    void "hasRunAfterStart, when the job system started, it should return true"() {
        given:
        createEmptyMockedServices()
        service.startWorkflowSystem()

        expect:
        service.hasRunAfterStart()
    }

    void "hasRunAfterStart, when the job system is stopped after starting, it should return true"() {
        given:
        createEmptyMockedServices()
        service.startWorkflowSystem()
        service.stopWorkflowSystem()

        expect:
        service.hasRunAfterStart()
    }

    void "hasRunAfterStart, when the job system failed to start, it should return false"() {
        given:
        setupWithFailedStart()

        expect:
        !service.hasRunAfterStart()
    }

    void "isEnabled, when system started, then it should return false"() {
        expect:
        !service.hasRunAfterStart()
    }

    void "isEnabled, when the job system started, it should return true"() {
        given:
        createEmptyMockedServices()
        service.startWorkflowSystem()

        expect:
        service.enabled
    }

    void "isEnabled, when the job system is stopped, it should return false"() {
        given:
        createEmptyMockedServices()
        service.startWorkflowSystem()
        service.stopWorkflowSystem()

        expect:
        !service.enabled
    }

    void "isEnabled, when the job system failed to start, it should return false"() {
        given:
        setupWithFailedStart()

        expect:
        !service.enabled
    }

    void "when starting or stopping the WorkflowSystem, then the flags should show the correct state and restart only for the first start running workflowSteps"() {
        given:
        service.propertiesValidationService = Mock(PropertiesValidationService) {
            _ * validateProcessingOptions() >> []
        }
        service.workflowBeanNameService = Mock(WorkflowBeanNameService) {
            _ * findWorkflowBeanNamesNotSet() >> []
        }
        service.jobService = Mock(JobService)
        service.workflowStepService = Mock(WorkflowStepService)
        WorkflowStep workflowStep = createWorkflowStep()

        expect:
        !service.hasRunAfterStart()
        !service.enabled

        when: "starting the system the first time"
        service.startWorkflowSystem()

        then: "running jobs should be restarted."
        1 * service.workflowStepService.runningWorkflowSteps() >> [workflowStep]
        1 * service.jobService.createRestartedJobAfterSystemRestart(workflowStep)
        0 * service.jobService.createRestartedJobAfterSystemRestart(_)
        service.hasRunAfterStart()
        service.enabled

        when: "stopping the system for the second time"
        service.stopWorkflowSystem()

        then:
        service.hasRunAfterStart()
        !service.enabled

        when: "starting the system for the second time"
        service.startWorkflowSystem()

        then: "no jobs should be restarted anymore."
        0 * service.workflowStepService.runningWorkflowSteps()
        0 * service.jobService.createRestartedJobAfterSystemRestart(_)
        service.hasRunAfterStart()
        service.enabled
    }

    void "check if exception is thrown, when bean name is not set properly"() {
        given:
        service.propertiesValidationService = Mock(PropertiesValidationService) {
            _ * validateProcessingOptions() >> []
        }
        service.workflowBeanNameService = Mock(WorkflowBeanNameService) {
            _ * findWorkflowBeanNamesNotSet() >> [ 'Wfl' ]
        }

        when:
        service.startWorkflowSystem()

        then:
        thrown WorkflowException

        expect:
        !service.hasRunAfterStart()
        !service.enabled
    }

    private void createEmptyMockedServices() {
        service.propertiesValidationService = Mock(PropertiesValidationService) {
            _ * validateProcessingOptions() >> []
        }
        service.jobService = Mock(JobService) {
            _ * createRestartedJobAfterSystemRestart(_)
        }
        service.workflowStepService = Mock(WorkflowStepService) {
            _ * runningWorkflowSteps() >> []
        }
        service.workflowBeanNameService = Mock(WorkflowBeanNameService) {
            _ * findWorkflowBeanNamesNotSet() >> []
        }
    }

    private void setupWithFailedStart() {
        service.propertiesValidationService = Mock(PropertiesValidationService) {
            _ * validateProcessingOptions() >> []
        }
        final String FAILED = 'failed to start'
        service.jobService = Mock(JobService) {
            _ * createRestartedJobAfterSystemRestart(_)
        }
        service.workflowStepService = Mock(WorkflowStepService) {
            _ * runningWorkflowSteps() >> {
                throw new WorkflowTestException(FAILED)
            }
        }
        service.workflowBeanNameService = Mock(WorkflowBeanNameService) {
            _ * findWorkflowBeanNamesNotSet() >> []
        }

        try {
            service.startWorkflowSystem()
        } catch (WorkflowTestException e) {
            assert e.message == FAILED
        }
    }
}
