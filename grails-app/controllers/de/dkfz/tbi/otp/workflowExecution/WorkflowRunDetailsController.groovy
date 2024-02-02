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

import grails.converters.JSON
import org.grails.web.json.JSONObject
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.utils.DataTablesCommand
import de.dkfz.tbi.util.TimeFormats

import javax.servlet.http.HttpServletResponse

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class WorkflowRunDetailsController extends AbstractWorkflowRunController {

    CommentService commentService
    WorkflowLogService workflowLogService
    WorkflowRunService workflowRunService

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

        WorkflowRun restartedAs = workflowRunService.findAllByRestartedFrom(cmd.id)

        List<WorkflowRun> workflowRuns = workflowRunService.workflowRunList(cmd.workflow, cmd.states, cmd.name)

        int index = workflowRuns.findIndexOf { cmd.id == it.id }
        WorkflowRun previous = (index <= 0) ? null : workflowRuns[index - 1]
        WorkflowRun next = (index in [-1, workflowRuns.size() - 1]) ? null : workflowRuns[index + 1]

        String unFormattedJson = cmd.id.combinedConfig ?: '{}'
        JSONObject jsonObject = JSON.parse(unFormattedJson) as JSONObject
        String formattedJson = jsonObject.toString(4)

        return [
                workflowRun   : cmd.id,
                restartedAs   : restartedAs,
                previous      : previous,
                next          : next,
                cmd           : cmd,
                combinedConfig: formattedJson,
        ]
    }

    def data(DataCommand cmd) {
        assert cmd.validate()

        List<Map<String, Object>> data = workflowRunService.workflowRunDetails(cmd.workflowRun)

        render(cmd.getDataToRender(data) as JSON)
    }

    def saveComment(CommentCommand cmd) {
        WorkflowRun workflowRun = workflowRunService.getById(cmd.id)
        commentService.saveComment(workflowRun, cmd.comment)
        Map dataToRender = [date: TimeFormats.WEEKDAY_DATE_TIME.getFormattedDate(workflowRun.comment.modificationDate), author: workflowRun.comment.author]
        render(dataToRender as JSON)
    }

    def showError(RunShowDetailsCommand navParams, WorkflowStep step) {
        if (!step?.workflowError) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return []
        }

        return [
                nav : navParams,
                step: step,
        ]
    }

    def showLogs(RunShowDetailsCommand navParams, WorkflowStep step) {
        if (!step) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND)
            return []
        }

        return [
                nav : navParams,
                step: step,
                logs: workflowLogService.findAllByWorkflowStepInCorrectOrder(step).collect {
                    return [
                            type       : it.class.simpleName,
                            message    : it.displayLog(),
                            dateCreated: TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(it.dateCreated),
                            id         : it.id,
                    ]
                },
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
