%{--
  - Copyright 2011-2024 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%
<%@ page import="de.dkfz.tbi.otp.workflowExecution.WorkflowRun" %>
<html>
<head>
    <title>${g.message(code: "workflowRun.details.title")} (${workflowRun.id})</title>
    <asset:javascript src="pages/workflowRunList/common.js"/>
    <asset:javascript src="pages/workflowRunDetails/index.js"/>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:stylesheet src="pages/workflowRunDetails/style.less"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><g:link controller="workflowRunList" action="index"
                                                params="['workflow.id': cmd.workflow?.id, state: cmd.states?.join(','), name: cmd.name]">${g.message(code: "workflow.navigation.list")}</g:link></li>
            <li class="breadcrumb-item active" aria-current="page">${g.message(code: "workflow.navigation.details")} (${workflowRun.id})</li>
        </ol>
    </nav>
    <nav class="navbar">
        <div class="navbar-brand">
            <div id="statusDot" title="${workflowRun.state}" data-status="${workflowRun.state}" class="d-inline-block"></div>
            <h3 class="d-inline-flex align-top pt-1 ml-2">
                ${g.message(code: "workflowRun.details.title")} (${workflowRun.id}) ${g.message(code: "workflowRun.details.of")} ${workflowRun.workflow.name}
            </h3>
        </div>
    </nav>

    <div class="dropdown-divider"></div>

    <div class="row">
        <div class="col">
            ${raw(workflowRun.displayName.replace("\n", "<br>"))}
            <p></p>
            <g:form method="POST">
                <input type="hidden" name="step" value="${workflowRun.workflowSteps ? workflowRun.workflowSteps.last().id : null}">
                <input type="hidden" name="redirect" value="${uriWithParams}"/>

                <div class="btn-group">
                    %{-- Button to set the workflow run to failed final, only enabled if the workflow run is in the failed or failed waiting state --}%
                    <button class="btn btn-sm btn-primary failed-final-btn"
                        ${(workflowRun.state != WorkflowRun.State.FAILED && workflowRun.state != WorkflowRun.State.FAILED_WAITING) ? "disabled" : ""}
                            formaction="${g.createLink(action: "setFailedFinal")}" title="${g.message(code: "workflowRun.details.setFailedFinal")}">
                        <i class="bi-file-earmark-x"></i> ${g.message(code: "workflowRun.details.setFailedFinal")}
                    </button>

                    %{-- Button to set the workflow run to toggle between the failed and failed waiting state, only visible and enabled if the workflow run is in the failed state --}%
                    <button class="btn btn-sm btn-primary failed-waiting-btn"
                        ${(workflowRun.state != WorkflowRun.State.FAILED && workflowRun.state != WorkflowRun.State.FAILED_WAITING) ? "hidden disabled" : ""}
                            formaction="${g.createLink(action: "toggleFailedWaiting")}" title="${g.message(code: "workflowRun.details.toggleFailedWaiting")}">
                        <i class="bi-file-earmark-minus"></i>
                        ${workflowRun.state == WorkflowRun.State.FAILED_WAITING ?
                                g.message(code: "workflowRun.details.removeFailedWaiting")
                                : g.message(code: "workflowRun.details.setFailedWaiting")}
                    </button>

                    %{-- Button to restart the run, only enabled if the workflow run is in the failed or failed waiting state --}%
                    <button class="btn btn-sm btn-primary restart-run-btn"
                        ${(workflowRun.state != WorkflowRun.State.FAILED && workflowRun.state != WorkflowRun.State.FAILED_WAITING) ? "disabled" : ""}
                            formaction="${g.createLink(action: "restartRun")}" title="${g.message(code: "workflowRun.details.restartRun")}">
                        <i class="bi-reply-all"></i> ${g.message(code: "workflowRun.details.restartRun")}
                    </button>
                </div>
            </g:form>

            <g:if test="${workflowRun.restartCount == 1}">
                <p>${g.message(code: "workflowRun.details.restartedFrom")} <g:link id="${workflowRun.restartedFrom.id}">
                    ${workflowRun.restartedFrom.id}</g:link>.</p>
            </g:if>
            <g:elseif test="${workflowRun.restartCount > 1}">
                <p>${g.message(code: "workflowRun.details.restartedFromMultiple", args: [workflowRun.restartCount])} <g:link
                        id="${workflowRun.restartedFrom.id}">
                    ${workflowRun.restartedFrom.id}</g:link>${g.message(code: "workflowRun.details.restartedFromMultipleOriginally")}
                <g:link id="${workflowRun.originalRestartedFrom.id}">${workflowRun.originalRestartedFrom.id}</g:link>.</p>
            </g:elseif>

            <g:if test="${restartedAs}">
                <p>${g.message(code: "workflowRun.details.restartedAs")} <g:link id="${restartedAs.id}">${restartedAs.id}</g:link>.<p>
            </g:if>

            <p>${workflowRun.state.description}</p>

            <g:if test="${workflowRun.skipMessage}">
                <p>${g.message(code: "workflowRun.details.skipped", args: [workflowRun.skipMessage?.category, workflowRun.skipMessage?.message])}</p>
            </g:if>

        </div>

        <div class="col">
            <div class="float-right">
                <g:render template="/templates/commentBox" model="[
                        commentable     : workflowRun,
                        targetController: 'workflowRunDetails',
                        targetAction    : 'saveComment',
                        cols            : 40,
                ]"/>
            </div>
        </div>
    </div>

    <div class="accordion pt-3" id="workflowRunDetailsAccordion">
        <div class="accordion-item">
            <h4 class="accordion-header" id="stepsHeader">
                <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#stepsBody" aria-expanded="true"
                        aria-controls="stepsBody">
                    <h4>${g.message(code: "workflowRun.details.steps")}</h4>
                </button>
            </h4>

            <div id="stepsBody" class="accordion-collapse collapse show" aria-labelledby="stepsHeader">
                <div class="accordion-body">

                    <g:if test="${workflowRun.state == WorkflowRun.State.RUNNING_WES}">
                        <div class="alert alert-primary" role="alert">
                            Workflow is running in WorkflowExecutionSystem.
                        </div>
                    </g:if>

                    <table id="steps" class="table table-sm table-striped table-bordered w-100"
                           data-wf-run-state="${workflowRun.state}"
                           data-id="${workflowRun.id}"
                           data-wf-id="${cmd.workflow?.id}"
                           data-state="${cmd.states?.join(',')}"
                           data-name="${cmd.name}"
                           data-wf-run-id="${workflowRun.id}">
                        <thead>
                        <tr class="table-secondary">
                            <th></th>
                            <th></th>
                            <th>${g.message(code: "workflowRun.details.step")}</th>
                            <th>${g.message(code: "workflowRun.details.dateCreated")}</th>
                            <th>${g.message(code: "workflowRun.details.lastUpdated")}</th>
                            <th>${g.message(code: "workflowRun.details.duration")}</th>
                            <th>${g.message(code: "workflowRun.details.id")}</th>
                            <th></th>
                        </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
        </div>

        <div class="accordion-item">
            <h4 class="accordion-header" id="inputHeader">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#inputBody" aria-expanded="false"
                        aria-controls="inputBody">
                    <h4>${g.message(code: "workflowRun.details.input")} (${workflowRun.inputArtefacts.size()})</h4>
                </button>
            </h4>

            <div id="inputBody" class="accordion-collapse collapse" aria-labelledby="inputHeader">
                <div class="accordion-body alert alert-secondary">
                    <ul>
                        <g:each in="${workflowRun.inputArtefacts}" var="artefact" status="index">
                            <li>
                                <b>${artefact.key} (workflow artefact ID ${artefact.value.id}, object ID ${artefact.value.artefact.map { it.id.toString() }.orElse('N/A')}):</b><br>
                                <g:link controller="workflowArtefact" action="index" id="${artefact.value.id}">
                                    ${raw(artefact.value.displayName.replace("\n", "<br>"))}</g:link>
                            </li>
                        </g:each>
                    </ul>
                    <g:if test="${!workflowRun.inputArtefacts}">
                        ${g.message(code: "workflowRun.details.no.input")}
                    </g:if>
                </div>
            </div>
        </div>

        <div class="accordion-item">
            <h4 class="accordion-header" id="outputHeader">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#outputBody" aria-expanded="false"
                        aria-controls="outputBody">
                    <h4>${g.message(code: "workflowRun.details.output")} (${workflowRun.outputArtefacts.size()})</h4>
                </button>
            </h4>

            <div id="outputBody" class="accordion-collapse collapse" aria-labelledby="outputHeader">
                <div class="accordion-body alert alert-secondary">
                    <ul>
                        <g:each in="${workflowRun.outputArtefacts}" var="artefact" status="index">
                            <li>
                                <b>${artefact.key} (workflow artefact ID ${artefact.value.id}, object ID ${artefact.value.artefact.map { it.id.toString() }.orElse('N/A')}):</b><br>
                                <g:link controller="workflowArtefact" action="index" id="${artefact.value.id}">
                                    ${raw(artefact.value.displayName.replace("\n", "<br>"))}</g:link>
                            </li>
                        </g:each>
                    </ul>
                    <g:if test="${!workflowRun.outputArtefacts}">
                        ${g.message(code: "workflowRun.details.no.output")}
                    </g:if>
                </div>
            </div>
        </div>

        <div class="accordion-item">
            <h4 class="accordion-header" id="configurationHeader">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#configurationBody" aria-expanded="false"
                        aria-controls="configurationBody">
                    <h4>${g.message(code: "workflowRun.details.config")}</h4>
                </button>
            </h4>

            <div id="configurationBody" class="accordion-collapse collapse" aria-labelledby="configurationHeader">
                <div class="accordion-body">
                    <g:if test="${workflowRun.combinedConfig != "{}"}">
                        <span id="configurationHolder" class="configurationHolder" readonly>${combinedConfig}</span>
                    </g:if>
                    <g:else>
                        ${g.message(code: "workflowRun.details.no.config")}
                    </g:else>
                </div>
            </div>
        </div>

    </div>

</div>
</body>
</html>
