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
<%@ page import="grails.util.Pair; de.dkfz.tbi.otp.workflowExecution.WorkflowRun" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${g.message(code: "systemStatus.title")}</title>
    <asset:javascript src="modules/editorSwitch.js"/>
    <asset:javascript src="pages/systemStatus/index/index.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <h1>${g.message(code: "systemStatus.title")}</h1>

    <table>
        <tr>
            <td>${g.message(code: "systemStatus.workflowSystem")}</td>
            <td>
                <g:if test="${workflowSystem}">
                    <asset:image src="ok.png"/> ${g.message(code: "systemStatus.enabled")}
                    <g:form action="stopWorkflowSystem" useToken="true" style="display: inline">
                        <g:submitButton name="submit" value="${g.message(code: "systemStatus.workflowSystem.stop")}"/>
                    </g:form>
                </g:if>
                <g:else>
                    <asset:image src="error.png"/> ${g.message(code: "systemStatus.disabled")}
                    <g:form action="startWorkflowSystem" useToken="true" style="display: inline">
                        <g:submitButton name="submit" value="${g.message(code: "systemStatus.workflowSystem.start")}"/>
                    </g:form>
                </g:else>
            </td>
        </tr>
        <tr>
            <td>${g.message(code: "systemStatus.workflows")}</td>
            <td>
                ${g.message(code: "systemStatus.workflows.numberEnabled", args: [numberEnabledWorkflows, workflows.size()])}
                <button class="workflow">${g.message(code: "systemStatus.show")}</button>
                <button class="workflow" hidden>${g.message(code: "systemStatus.hide")}</button>
            </td>
        </tr>
        <tr>
            <td colspan="2">

                <table class="workflow" hidden style="width: 96%; margin: 1ch 2%;">
                    <g:each in="${workflows}" var="workflow">
                        <tr>
                            <td>${workflow.toString()}</td>
                            <td>
                                <g:if test="${workflow.enabled}">
                                    <asset:image src="ok.png"/> ${g.message(code: "systemStatus.enabled")}
                                    <g:form action="disableWorkflow" useToken="true" style="display: inline">
                                        <input type="hidden" name="workflow.id" value="${workflow.id}">
                                        <g:submitButton name="submit" value="${g.message(code: "systemStatus.workflows.disable")}"/>
                                    </g:form>
                                </g:if>
                                <g:else>
                                    <asset:image src="error.png"/> ${g.message(code: "systemStatus.disabled")}
                                    <g:form action="enableWorkflow" useToken="true" style="display: inline">
                                        <input type="hidden" name="workflow.id" value="${workflow.id}">
                                        <g:submitButton name="submit" value="${g.message(code: "systemStatus.workflows.enable")}"/>
                                    </g:form>
                                </g:else>
                            </td>
                            <td>
                                ${runs[new Pair(WorkflowRun.State.RUNNING, workflow)] ?: "0"} running
                                <g:link controller="workflowRunOverview" action="index" params="${["workflow.id": workflow.id]}">Show</g:link>
                            </td>
                            <td>
                                ${g.message(code: "systemStatus.workflows.priority")}:
                                <div style="display: inline-block">
                                    <otp:editorSwitch roles="ROLE_OPERATOR"
                                                      link="${g.createLink(controller: 'systemStatus', action: 'changePriority', params: ["workflow.id": workflow.id])}"
                                                      value="${workflow.priority}"/>
                                </div>
                            </td>
                        </tr>
                    </g:each>
                </table>

            </td>
        </tr>
    </table>
</div>
</body>
</html>
