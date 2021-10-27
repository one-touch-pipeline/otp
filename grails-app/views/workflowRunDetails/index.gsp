%{--
  - Copyright 2011-2020 The OTP authors
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
            <li class="breadcrumb-item"><g:link controller="workflowRunList" action="index" params="['workflow.id': cmd.workflow?.id, state: cmd.states?.join(','), name: cmd.name]">${g.message(code: "workflow.navigation.list")}</g:link></li>
            <li class="breadcrumb-item active" aria-current="page">${g.message(code: "workflow.navigation.details")} (${workflowRun.id})</li>
        </ol>
    </nav>

    <nav class="navbar">
        <div class="navbar-brand">
            <div id="statusDot" title="${workflowRun.state}" data-status="${workflowRun.state}" class="d-inline-block"></div>
            <span class="d-inline-flex align-top pt-1 ml-2">
                ${g.message(code: "workflowRun.details.title")} (${workflowRun.id}) ${g.message(code: "workflowRun.details.of")} ${workflowRun.workflow.name}
            </span>
        </div>
        <div class="btn-group">
            <g:if test="${previous}">
                <g:link class="btn btn-primary" action="index" id="${previous.id}" params="['workflow.id': cmd.workflow?.id, state: cmd.states?.join(','), name: cmd.name]">
                    <i title="${g.message(code: "workflowRun.details.previous")}" class='bi-caret-left'></i>
                </g:link>
            </g:if>
            <g:else>
                <button class="btn btn-primary" disabled><i title="${g.message(code: "workflowRun.details.previous")}" class='bi-caret-left'></i></button>
            </g:else>
            <g:if test="${next}">
                <g:link class="btn btn-primary" action="index" id="${next.id}" params="['workflow.id': cmd.workflow?.id, state: cmd.states?.join(','), name: cmd.name]">
                    <i title="${g.message(code: "workflowRun.details.next")}" class='bi-caret-right'></i>
                </g:link>
            </g:if>
            <g:else>
                <button class="btn btn-primary" disabled><i title="${g.message(code: "workflowRun.details.next")}" class='bi-caret-right'></i></button>
            </g:else>
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
                    <button class="btn btn-sm btn-primary" ${workflowRun.state != WorkflowRun.State.FAILED ? "disabled" : ""}
                            formaction="${g.createLink(action: "setFailedFinal")}" title="${g.message(code: "workflowRun.details.setFailed")}">
                        <i class="bi-file-earmark-x"></i> ${g.message(code: "workflowRun.details.setFailed")}
                    </button>
                    <button class="btn btn-sm btn-primary" ${workflowRun.state != WorkflowRun.State.FAILED ? "disabled" : ""}
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
                <p>${g.message(code: "workflowRun.details.restartedFromMultiple", args: [workflowRun.restartCount])} <g:link id="${workflowRun.restartedFrom.id}">
                    ${workflowRun.restartedFrom.id}</g:link>${g.message(code: "workflowRun.details.restartedFromMultipleOriginally")}
                    <g:link id="${workflowRun.originalRestartedFrom.id}">${workflowRun.originalRestartedFrom.id}</g:link>.</p>
            </g:elseif>

            <g:if test="${restartedAs}">
                <p>${g.message(code: "workflowRun.details.restartedAs")} <g:link id="${restartedAs.id}">${restartedAs.id}</g:link>.<p>
            </g:if>

            <p>${workflowRun.state.description}</p>

            <g:if test="${workflowRun.omittedMessage}">
                <p>${g.message(code: "workflowRun.details.omitted", args: [workflowRun.omittedMessage?.category, workflowRun.omittedMessage?.message])}</p>
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

    <h2>${g.message(code: "workflowRun.details.steps")}</h2>

    <g:if test="${workflowRun.state == WorkflowRun.State.RUNNING_WES}">
        <div class="alert alert-primary" role="alert">
            Workflow is running in WES.
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

    <br>

    <h2>${g.message(code: "workflowRun.details.input")}</h2>
    <g:each in="${workflowRun.inputArtefacts}" var="artefact" status="index">
        <div class="alert alert-secondary">
            <b>${artefact.key}:</b><br>
            <g:link controller="workflowArtefact" action="index" id="${artefact.value.id}">
                ${raw(artefact.value.displayName.replace("\n", "<br>"))}</g:link>
        </div>
    </g:each>
    <g:if test="${!workflowRun.inputArtefacts}">
        ${g.message(code: "workflowRun.details.no.input")}
    </g:if>

    <h2>${g.message(code: "workflowRun.details.output")}</h2>
    <g:each in="${workflowRun.outputArtefacts}" var="artefact" status="index">
        <div class="alert alert-secondary">
            <b>${artefact.key}:</b><br>
            <g:link controller="workflowArtefact" action="index" id="${artefact.value.id}">
                ${raw(artefact.value.displayName.replace("\n", "<br>"))}</g:link>
        </div>
    </g:each>
    <g:if test="${!workflowRun.outputArtefacts}">
        ${g.message(code: "workflowRun.details.no.output")}
    </g:if>
    <h2>${g.message(code: "workflowRun.details.config")}</h2>
    <g:if test="${workflowRun.combinedConfig != "{}"}">
        <textarea id="configurationHolder" class="configurationHolder" disabled>${workflowRun.combinedConfig}</textarea>
    </g:if>
    <g:else>
        ${g.message(code: "workflowRun.details.no.config")}
    </g:else>
</div>
</body>
</html>
