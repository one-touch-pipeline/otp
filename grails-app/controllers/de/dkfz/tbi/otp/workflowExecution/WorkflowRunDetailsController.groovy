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

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.TimeFormats

import javax.servlet.http.HttpServletResponse

import static de.dkfz.tbi.otp.infrastructure.ClusterJob.CheckStatus.FINISHED

@Secured("hasRole('ROLE_OPERATOR')")
class WorkflowRunDetailsController extends AbstractWorkflowRunController {

    ClusterJobService clusterJobService
    CommentService commentService
    WorkflowStepService workflowStepService

    static allowedMethods = [
            index      : "GET",
            data       : "GET",
            saveComment: "POST",
            showError  : "GET",
            showLogs   : "GET",
    ]

    def index(RunShowDetailsCommand cmd) {
        if (!cmd.id) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return []
        }
        WorkflowRun restartedAs = CollectionUtils.atMostOneElement(WorkflowRun.findAllByRestartedFrom(cmd.id))

        Closure criteria = getCriteria(cmd.workflow, cmd.states, cmd.name,)
        List<WorkflowRun> data = WorkflowRun.createCriteria().list {
            criteria.delegate = delegate
            criteria()
        } as List<WorkflowRun>
        data.sort { -it.id }
        int index = data.findIndexOf { cmd.id.id == it.id }
        WorkflowRun previous = (index <= 0) ? null : data[index - 1]
        WorkflowRun next = (index in [-1, data.size() - 1]) ? null : data[index + 1]

        return [
                workflowRun: cmd.id,
                restartedAs: restartedAs,
                previous   : previous,
                next       : next,
                cmd        : cmd,
        ]
    }

    def data(DataCommand cmd) {
        assert cmd.validate()

        List<WorkflowStep> workflowSteps = cmd.workflowRun.workflowSteps.reverse()

        List<LinkedHashMap<String, Object>> result = workflowSteps.collect { step ->
            boolean isPreviousOfFailedStep = !workflowSteps.findAll {
                workflowStepService.getPreviousRunningWorkflowStep(it)?.id == step.id && it.state == WorkflowStep.State.FAILED
            }.empty

            return [
                    state      : step.state,
                    id         : step.id,
                    name       : step.beanName,
                    dateCreated: TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(step.dateCreated),
                    lastUpdated: TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(step.lastUpdated),
                    duration   : getFormattedDuration(convertDateToLocalDateTime(step.dateCreated), convertDateToLocalDateTime(step.lastUpdated)),

                    error                 : step.workflowError,
                    clusterJobs           : (step.clusterJobs as List<ClusterJob>).sort { it.dateCreated }.collect { ClusterJob clusterJob ->
                        [
                                state   : "${clusterJob.checkStatus}${clusterJob.checkStatus == FINISHED ? "/${clusterJob.exitStatus}" : ""}",
                                id      : clusterJob.id,
                                name    : clusterJob.clusterJobName,
                                jobId   : clusterJob.clusterJobId,
                                hasLog  : clusterJobService.doesClusterJobLogExist(clusterJob),
                                node    : clusterJob.node ?: "-",
                                wallTime: clusterJob.elapsedWalltimeAsHhMmSs,
                                exitCode: clusterJob.exitCode ?: "-",
                        ]
                    },
                    wes                   : step.wesIdentifier,
                    hasLogs               : !step.logs.empty,
                    obsolete              : step.obsolete,
                    previousStepId        : workflowStepService.getPreviousRunningWorkflowStep(step)?.id,
                    isPreviousOfFailedStep: isPreviousOfFailedStep,
            ]
        }
        render cmd.getDataToRender(result) as JSON
    }

    def saveComment(CommentCommand cmd) {
        WorkflowRun workflowRun = WorkflowRun.get(cmd.id)
        commentService.saveComment(workflowRun, cmd.comment)
        Map dataToRender = [date: TimeFormats.WEEKDAY_DATE_TIME.getFormattedDate(workflowRun.comment.modificationDate), author: workflowRun.comment.author]
        render dataToRender as JSON
    }

    def showError(RunShowDetailsCommand navParams) {
        WorkflowStep step = WorkflowStep.get(params.id as long)
        if (!step?.workflowError) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return []
        }

        return [
                nav : navParams,
                step: step,
        ]
    }

    def showLogs(RunShowDetailsCommand navParams) {
        WorkflowStep step = WorkflowStep.get(params.id as long)
        if (!step) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return []
        }

        return [
                nav     : navParams,
                step    : step,
                messages: step.logs*.displayLog(),
        ]
    }
}

class RunShowDetailsCommand {
    WorkflowRun id
    Workflow workflow

    void setState(String state) {
        states = state.split(",").collect { WorkflowRun.State.valueOf(it) } ?: []
    }
    List<WorkflowRun.State> states
    String name
}

class DataCommand extends DataTablesCommand {
    WorkflowRun workflowRun
}
