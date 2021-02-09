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

import grails.async.Promises
import grails.testing.gorm.DataTest
import org.grails.async.factory.SynchronousPromiseFactory
import org.springframework.context.ApplicationContext
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.workflow.jobs.Job
import de.dkfz.tbi.otp.workflow.restartHandler.AutoRestartHandlerService
import de.dkfz.tbi.otp.workflow.restartHandler.ErrorNotificationService
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.otp.workflowExecution.log.WorkflowError

class JobSchedulerSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    JobScheduler jobScheduler

    @Override
    Class[] getDomainClassesToMock() {
        [
            WorkflowStep,
        ]
    }

    void "test scheduleJob, when no CREATED step exists, no job is executed"() {
        given:
        jobScheduler = Spy(JobScheduler)
        jobScheduler.workflowSystemService = [enabled: true] as WorkflowSystemService
        createWorkflowStep(state: WorkflowStep.State.RUNNING)

        when:
        jobScheduler.scheduleJob()

        then:
        0 * jobScheduler.executeAndCheckJob(_)
    }

    void "test scheduleJob, when workflow system is disabled, no job is executed"() {
        given:
        jobScheduler = Spy(JobScheduler)
        jobScheduler.workflowSystemService = [enabled: false] as WorkflowSystemService
        createWorkflowStep(state: WorkflowStep.State.CREATED)

        when:
        jobScheduler.scheduleJob()

        then:
        0 * jobScheduler.executeAndCheckJob(_)
    }

    void "test scheduleJob, older step is executed first"() {
        given:
        Promises.promiseFactory = new SynchronousPromiseFactory()
        jobScheduler = Spy(JobScheduler)
        jobScheduler.workflowSystemService = [enabled: true] as WorkflowSystemService
        jobScheduler.workflowStateChangeService = Mock(WorkflowStateChangeService)
        WorkflowStep workflowStep1 = createWorkflowStep(id: 1, state: WorkflowStep.State.CREATED)
        createWorkflowStep(id: 2, state: WorkflowStep.State.CREATED)

        when:
        jobScheduler.scheduleJob()

        then:
        1 * jobScheduler.workflowStateChangeService.changeStateToRunning(workflowStep1) >> { }
        1 * jobScheduler.executeAndCheckJob(workflowStep1) >> { }
    }

    void "test executeAndCheckJob, when job succeeds, create next job"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.CREATED)
        jobScheduler = new JobScheduler()
        jobScheduler.applicationContext = [
                getBean: { name, cl ->
                    [execute: { WorkflowStep st ->
                            st.state = WorkflowStep.State.SUCCESS
                            st.save(flush: true)
                            st.workflowRun.state = WorkflowRun.State.RUNNING
                            st.workflowRun.save(flush: true)
                    }] as Job }
        ] as ApplicationContext
        jobScheduler.jobService = Mock(JobService)

        when:
        jobScheduler.executeAndCheckJob(workflowStep)

        then:
        1 * jobScheduler.jobService.createNextJob(workflowStep.workflowRun) >> { }
    }

    void "test executeAndCheckJob, when job and run succeed, notify users"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.CREATED)
        jobScheduler = Spy(JobScheduler)
        jobScheduler.applicationContext = [
                getBean: { name, cl ->
                    [execute: { WorkflowStep st ->
                            st.state = WorkflowStep.State.SUCCESS
                            st.save(flush: true)
                            st.workflowRun.state = WorkflowRun.State.SUCCESS
                            st.workflowRun.save(flush: true)
                    }] as Job }
        ] as ApplicationContext

        when:
        jobScheduler.executeAndCheckJob(workflowStep)

        then:
        1 * jobScheduler.notifyUsers(workflowStep) >> { }
    }

    void "test executeAndCheckJob, when job fails, trigger RestartHandler"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.CREATED)
        jobScheduler = Spy(JobScheduler)
        jobScheduler.applicationContext = [
                getBean: { name, cl ->
                    [execute: { WorkflowStep st ->
                            st.workflowRun.state = WorkflowRun.State.FAILED
                            st.workflowRun.save(flush: true)
                            st.workflowError = new WorkflowError(message: "message", stacktrace: "stacktrace")
                            st.state = WorkflowStep.State.FAILED
                            st.save(flush: true)
                    }] as Job }
        ] as ApplicationContext
        jobScheduler.workflowStateChangeService = Mock(WorkflowStateChangeService)
        jobScheduler.autoRestartHandlerService = Mock(AutoRestartHandlerService)

        when:
        jobScheduler.executeAndCheckJob(workflowStep)

        then:
        1 * jobScheduler.autoRestartHandlerService.handleRestarts(workflowStep) >> { }
    }

    void "test executeAndCheckJob, when job fails incorrectly, trigger RestartHandler"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.CREATED)
        jobScheduler = Spy(JobScheduler)
        jobScheduler.applicationContext = [
                getBean: { name, cl ->
                    [execute: { WorkflowStep st ->
                            st.workflowError = new WorkflowError(message: "message", stacktrace: "stacktrace")
                            st.state = WorkflowStep.State.FAILED
                            st.save(flush: true)
                    }] as Job }
        ] as ApplicationContext
        jobScheduler.workflowStateChangeService = Mock(WorkflowStateChangeService)
        jobScheduler.autoRestartHandlerService = Mock(AutoRestartHandlerService)

        when:
        jobScheduler.executeAndCheckJob(workflowStep)

        then:
        1 * jobScheduler.workflowStateChangeService.changeStateToFailed(workflowStep, _ as JobSchedulerException) >> { }
        1 * jobScheduler.autoRestartHandlerService.handleRestarts(workflowStep) >> { }
    }

    void "test executeAndCheckJob, when job state does not change, trigger RestartHandler"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.CREATED)
        jobScheduler = Spy(JobScheduler)
        jobScheduler.applicationContext = [
                getBean: { name, cl ->
                    [execute: { WorkflowStep st ->
                            st.state = WorkflowStep.State.RUNNING
                            st.save(flush: true)
                    }] as Job }
        ] as ApplicationContext
        jobScheduler.workflowStateChangeService = Mock(WorkflowStateChangeService)
        jobScheduler.autoRestartHandlerService = Mock(AutoRestartHandlerService)

        when:
        jobScheduler.executeAndCheckJob(workflowStep)

        then:
        1 * jobScheduler.workflowStateChangeService.changeStateToFailed(workflowStep, _ as JobSchedulerException) >> { }
        1 * jobScheduler.autoRestartHandlerService.handleRestarts(workflowStep) >> { }
    }

    void "test executeAndCheckJob, when job throws exception, trigger RestartHandler"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.CREATED)
        jobScheduler = Spy(JobScheduler)
        jobScheduler.applicationContext = [
                getBean: { name, cl ->
                    [execute: { WorkflowStep st ->
                            throw new FileNotFoundException()
                    }] as Job }
        ] as ApplicationContext
        jobScheduler.workflowStateChangeService = Mock(WorkflowStateChangeService)
        jobScheduler.autoRestartHandlerService = Mock(AutoRestartHandlerService)

        when:
        jobScheduler.executeAndCheckJob(workflowStep)

        then:
        1 * jobScheduler.workflowStateChangeService.changeStateToFailed(workflowStep, _ as FileNotFoundException) >> { }
        1 * jobScheduler.autoRestartHandlerService.handleRestarts(workflowStep) >> { }
    }

    void "test executeAndCheckJob, when job throws exception and restartHandler also throw exception, send simple error message"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep(state: WorkflowStep.State.CREATED)
        jobScheduler = Spy(JobScheduler)
        jobScheduler.applicationContext = [
                getBean: { name, cl ->
                    [execute: { WorkflowStep st ->
                        throw new FileNotFoundException()
                    }] as Job }
        ] as ApplicationContext
        jobScheduler.workflowStateChangeService = Mock(WorkflowStateChangeService)
        jobScheduler.autoRestartHandlerService = Mock(AutoRestartHandlerService)
        jobScheduler.errorNotificationService = Mock(ErrorNotificationService)

        when:
        jobScheduler.executeAndCheckJob(workflowStep)

        then:
        1 * jobScheduler.workflowStateChangeService.changeStateToFailed(workflowStep, _ as FileNotFoundException) >> { }
        1 * jobScheduler.autoRestartHandlerService.handleRestarts(workflowStep) >> { throw new WorkflowException() }
        1 * jobScheduler.errorNotificationService.sendMaintainer(workflowStep, _, _) >> { }
    }
}
