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
    <title>${g.message(code: "workflowRun.details.title")} ${workflowRun.displayName}</title>
    <asset:javascript src="pages/workflowRunList/common.js"/>
    <asset:javascript src="pages/workflowRunDetails/index.js"/>
    <asset:javascript src="common/CommentBox.js"/>
</head>

<body>
<div class="body">
    <div class="container-fluid">
    <div class="row">
        <div class="col-7">
            <h1><div id="runStatus" title="${workflowRun.state}" data-status="${workflowRun.state}" style="display: inline-block"></div> ${g.message(code: "workflowRun.details.title")} ${workflowRun.displayName}</h1>
            <g:form method="POST">
                <input type="hidden" name="workflowRun.id" value="${workflowRun.id}">
                <div class="btn-group">
                    <button class="btn btn-primary" ${workflowRun.state != WorkflowRun.State.FAILED ? "disabled" : ""}
                            formaction="${g.createLink(action: "setFailedFinal")}" title="${g.message(code: "workflowRun.details.setFailed")}">
                        <i class="bi-file-earmark-x"></i>${g.message(code: "workflowRun.details.setFailed")}
                    </button>
                    <button class="btn btn-primary" ${workflowRun.state != WorkflowRun.State.FAILED ? "disabled" : ""}
                            formaction="${g.createLink(action: "restartRun")}" title="${g.message(code: "workflowRun.details.restartRun")}">
                        <i class="bi-file-earmark-x"></i>${g.message(code: "workflowRun.details.restartRun")}
                    </button>
                </div>
            </g:form>

            <p>
            <div>
                <g:if test="${workflowRun.restartCount == 1}">
                    ${g.message(code: "workflowRun.details.restartedFrom")} <g:link id="${workflowRun.restartedFrom.id}">${workflowRun.restartedFrom.displayName}</g:link>
                </g:if>
                <g:elseif test="${workflowRun.restartCount > 1}">
                    ${g.message(code: "workflowRun.details.restartedFromMultiple", args: [workflowRun.restartCount])}
                    <g:link id="${workflowRun.restartedFrom.id}">${workflowRun.restartedFrom.displayName}</g:link>
                    ${g.message(code: "workflowRun.details.restartedFromMultipleOriginally")}
                    <g:link id="${workflowRun.originalRestartedFrom.id}">${workflowRun.originalRestartedFrom.displayName}</g:link>
                </g:elseif>
            </div>
            <div>
                <g:if test="${restartedAs}">
                    ${g.message(code: "workflowRun.details.restartedAs")} <g:link id="${restartedAs.id}">${restartedAs.displayName}</g:link>
                </g:if>
            </div>
            <div>
                <g:if test="${workflowRun.skippedMessage}">
                    ${g.message(code: "workflowRun.details.skipped", args: [workflowRun.skippedMessage?.category, workflowRun.skippedMessage?.message])}
                </g:if>
            </div>
            <div>
                <g:if test="${workflowRun.state == WorkflowRun.State.WAITING_ON_USER}">
                    ${g.message(code: "workflowRun.details.inputRequired")}
                </g:if>
            </div>
            </p>

        </div>

        <div class="col-1">
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
        </div>

        <div class="col-4">
            <g:render template="/templates/commentBox" model="[
                    commentable     : workflowRun,
                    targetController: 'workflowRunDetails',
                    targetAction    : 'saveComment',
                    cols            : 40,
            ]"/>
        </div>
    </div>
    </div>

    <br>
    <h2>${g.message(code: "workflowRun.details.steps")}</h2>
    <div class="container-fluid">
        <div class="row">
            <div class="col-auto mr-auto"></div>
            <div class="col-auto">

            </div>
        </div>
    </div>

    <table id="steps" data-id="${workflowRun.id}">
        <thead>
        <tr>
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
        <tbody>
    </table>

    <br>
    <h2>${g.message(code: "workflowRun.details.inputOutput")}</h2>
    <h3>${g.message(code: "workflowRun.details.input")}</h3>
    <ul>
        <g:each in="${workflowRun.inputArtefacts}" var="artefact">
            <li>${artefact.key}: <g:link controller="workflowArtefact" action="index" id="${artefact.value.id}">${artefact.value.displayName}</g:link></li>
        </g:each>
        <g:if test="${!workflowRun.inputArtefacts}">
            ${g.message(code: "workflowRun.details.none")}
        </g:if>
    </ul>
    <h3>${g.message(code: "workflowRun.details.output")}</h3>
    <ul>
        <g:each in="${workflowRun.outputArtefacts}" var="artefact">
            <li>${artefact.key}: <g:link controller="workflowArtefact" action="index" id="${artefact.value.id}">${artefact.value.displayName}</g:link></li>
        </g:each>
        <g:if test="${!workflowRun.outputArtefacts}">
            ${g.message(code: "workflowRun.details.none")}
        </g:if>
    </ul>
</div>

</body>
</html>
