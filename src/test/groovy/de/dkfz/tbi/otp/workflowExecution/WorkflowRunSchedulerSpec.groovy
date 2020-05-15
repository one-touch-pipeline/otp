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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

class WorkflowRunSchedulerSpec extends Specification implements ServiceUnitTest<WorkflowRunScheduler>, DataTest, WorkflowSystemDomainFactory {

    //isEnabled is the mocked method name and can therefore not written as property
    @SuppressWarnings('UnnecessaryGetter')
    void "scheduleWorkflowRun, if system runs and nextWaitingWorkflow return a workflowRun, then call createJob with it"() {
        given:
        WorkflowRunScheduler scheduler = createWorkflowRunScheduler()
        WorkflowRun workflowRun = new WorkflowRun()

        when:
        scheduler.scheduleWorkflowRun()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true
        1 * scheduler.workflowRunService.countOfRunningWorkflows() >> 5
        1 * scheduler.workflowRunService.nextWaitingWorkflow(_) >> workflowRun
        1 * scheduler.jobService.createNextJob(workflowRun)
    }

    //isEnabled is the mocked method name and can therefore not written as property
    @SuppressWarnings('UnnecessaryGetter')
    void "scheduleWorkflowRun, if system runs and nextWaitingWorkflow return null, then do not call createJob "() {
        given:
        WorkflowRunScheduler scheduler = createWorkflowRunScheduler()

        when:
        scheduler.scheduleWorkflowRun()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true
        1 * scheduler.workflowRunService.countOfRunningWorkflows() >> 5
        1 * scheduler.workflowRunService.nextWaitingWorkflow(_) >> null
    }

    //isEnabled is the mocked method name and can therefore not written as property
    @SuppressWarnings('UnnecessaryGetter')
    void "scheduleWorkflowRun, if system does not run, then do not call createJob "() {
        given:
        WorkflowRunScheduler scheduler = createWorkflowRunScheduler()

        when:
        scheduler.scheduleWorkflowRun()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> false
        0 * scheduler.workflowRunService.countOfRunningWorkflows()
        0 * scheduler.workflowRunService.nextWaitingWorkflow(_) >> null
    }

    private WorkflowRunScheduler createWorkflowRunScheduler() {
        return new WorkflowRunScheduler([
                workflowSystemService: Mock(WorkflowSystemService),
                workflowRunService   : Mock(WorkflowRunService),
                jobService           : Mock(JobService) {
                    0 * createNextJob(_)
                },
        ])
    }
}
