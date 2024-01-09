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

import asset.pipeline.grails.LinkGenerator
import grails.gorm.transactions.Transactional
import groovy.transform.Synchronized
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.exceptions.FileAccessForProjectNotAllowedException
import de.dkfz.tbi.otp.workflow.restartHandler.BeanToRestartNotFoundInWorkflowRunException
import de.dkfz.tbi.otp.workflow.shared.WorkflowJobIsNotRestartableException

@Slf4j
@Transactional
class JobService {

    private static final List<WorkflowRun.State> WORKFLOW_RUN_STATES_ALLOW_NEXT_JOB = [
            WorkflowRun.State.PENDING,
            WorkflowRun.State.RUNNING_WES,
            WorkflowRun.State.RUNNING_OTP,
    ].asImmutable()

    @Autowired
    LinkGenerator linkGenerator

    LogService logService

    OtpWorkflowService otpWorkflowService

    WorkflowRunService workflowRunService

    WorkflowStateChangeService workflowStateChangeService

    void createNextJob(WorkflowRun workflowRun) {
        assert workflowRun
        assert workflowRun.workflow.beanName
        assert workflowRun.state in WORKFLOW_RUN_STATES_ALLOW_NEXT_JOB

        WorkflowStep lastWorkflowStep = workflowRun.workflowSteps ? workflowRun.workflowSteps.last() : null
        assert !lastWorkflowStep || lastWorkflowStep.state == WorkflowStep.State.SUCCESS:
                "Can not create next job, since last was not successfully, but ${lastWorkflowStep.state}"

        OtpWorkflow otpWorkflow = otpWorkflowService.lookupOtpWorkflowBean(workflowRun)
        List<String> jobBeanNames = otpWorkflow.jobBeanNames
        String beanName = lastWorkflowStep ?
                jobBeanNames[jobBeanNames.indexOf(lastWorkflowStep.beanName) + 1] :
                jobBeanNames.first()

        WorkflowStep newWorkflowStep = new WorkflowStep(
                workflowRun: workflowRun,
                beanName: beanName,
                state: WorkflowStep.State.CREATED,
                previous: lastWorkflowStep,
        ).save(flush: true)
        workflowRun.state = WorkflowRun.State.RUNNING_OTP
        workflowRun.save(flush: true)
        log.debug("create job: ${newWorkflowStep.displayInfo()}")
    }

    private void createRestartedJob(WorkflowStep stepToRestart) {
        assert stepToRestart
        WorkflowRun run = stepToRestart.workflowRun
        workflowRunService.lockAndRefreshWorkflowRunWithSteps(run)
        List<WorkflowStep> workflowSteps = run.workflowSteps

        if (run.project.state == Project.State.ARCHIVED || run.project.state == Project.State.DELETED) {
            throw new FileAccessForProjectNotAllowedException(
                    "${run.project} is ${run.project.state.name().toLowerCase()} and ${run} cannot be restarted"
            )
        }

        if (!run.jobCanBeRestarted) {
            String link = linkGenerator.link([
                    controller: 'workflowRunDetails',
                    action    : 'index',
                    id        : run.id,
                    params    : [:],
            ])

            throw new WorkflowJobIsNotRestartableException(
                    "Can not restart job ${run.id} of '${run.shortDisplayName}', since the job has failed at a timepoint when it was communicating with an " +
                            "external system. Therefore, the external system may have started already a process, so a restart of the job could cause " +
                            "multiple processes writing into the same files. To avoid the risk data corruption and inconsistency the job shouldn't be " +
                            "restarted. Please restart the workflow instead, if possible. See <a href = \"${link}\"> detail page</a>")
        }

        assert run.state == WorkflowRun.State.FAILED: "Can not restart job of ${run.displayName}, " +
                "since the workflow is not in the failed state"

        assert workflowSteps.last().state == WorkflowStep.State.FAILED: "Can not restart job of ${run.displayName}, " +
                "since the last job is not in the failed state"

        workflowSteps[workflowSteps.indexOf(stepToRestart)..(workflowSteps.size() - 1)].each { WorkflowStep step ->
            step.obsolete = true
            step.save(flush: true)
        }

        WorkflowStep restartedWorkflowStep = new WorkflowStep(
                beanName: stepToRestart.beanName,
                state: WorkflowStep.State.CREATED,
                previous: workflowSteps.last(),
                restartedFrom: stepToRestart,
                workflowRun: run,
        ).save(flush: true)
        run.state = WorkflowRun.State.RUNNING_OTP
        run.save(flush: true)
        log.debug("create restarted job: ${restartedWorkflowStep.displayInfo()}")
    }

    void createRestartedJobAfterJobFailures(List<WorkflowStep> stepsToRestart) {
        stepsToRestart.each {
            createRestartedJobAfterJobFailure(it)
        }
    }

    void createRestartedPreviousJobAfterJobFailures(List<WorkflowStep> stepsToRestart) {
        stepsToRestart.findAll { it.previous }.each {
            createRestartedJobAfterJobFailure(it.previous)
        }
    }

    @Synchronized
    void createRestartedJobAfterJobFailure(WorkflowStep stepToRestart) {
        assert stepToRestart
        stepToRestart.refresh()

        createRestartedJob(stepToRestart)
    }

    WorkflowStep searchForJobToRestart(WorkflowStep failedStep, String beanToRestart) {
        List<WorkflowStep> nonObsoleteReverseSteps = failedStep.workflowRun.workflowSteps.findAll {
            !it.obsolete
        }.reverse()
        WorkflowStep stepToRestart = nonObsoleteReverseSteps.find {
            it.beanName == beanToRestart
        }
        if (stepToRestart) {
            return stepToRestart
        }
        throw new BeanToRestartNotFoundInWorkflowRunException(
                "Could not find bean ${beanToRestart} in non obsolete running jobs, available are: ${nonObsoleteReverseSteps*.beanName.join(', ')}")
    }

    @Synchronized
    void createRestartedJobAfterSystemRestart(WorkflowStep workflowStep) {
        if (!workflowStep) {
            throw new WorkflowJobIsNotRestartableException("Cannot restart unknown workflow step.")
        }

        if (workflowStep.state != WorkflowStep.State.RUNNING || workflowStep.workflowRun.state != WorkflowRun.State.RUNNING_OTP) {
            throw new WorkflowJobIsNotRestartableException("Cannot restart workflow step which is not RUNNING.")
        }

        workflowStateChangeService.changeStateToFailedAfterRestart(workflowStep)
        logService.addSimpleLogEntry(workflowStep, "OTP restarted")
        createRestartedJob(workflowStep)
    }
}
