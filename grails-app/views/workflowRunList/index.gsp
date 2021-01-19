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
<html>
<head>
    <title>${g.message(code: "workflowRun.list.title")}</title>
    <asset:javascript src="pages/workflowRunList/common.js"/>
    <asset:javascript src="pages/workflowRunList/index.js"/>
</head>

<body>
<div class="body">
    <h1>${g.message(code: "workflowRun.list.title")}</h1>
    <g:render template="/templates/messages"/>

    <div class="container-fluid">
        <div class="row">
            <div class="col-sm">
                <table class="key-value-table key-input">
                    <tr>
                        <td><label for="workflow">${g.message(code: "workflowRun.list.workflow")}</label></td>
                        <td><g:select id="workflow" name="workflow.id" from="${workflows}" optionKey="id" value="${cmd?.workflow?.id}"
                                      noSelection="['': g.message(code: 'workflowRun.list.all')]" class="use-select-2" autocomplete="off"/></td>
                    </tr>
                    <tr>
                        <td><label for="state">${g.message(code: "workflowRun.list.state")}</label></td>
                        <td>
                            <select id="state" class="use-select-2" autocomplete="off">
                                <option value="">${g.message(code: "workflowRun.list.all")}</option>
                                <g:each in="${states}" var="s">
                                    <option value="${s.value.join(",")}" ${cmd?.state == s.value.join(",") ? "selected" : ""}>${s.key}</option>
                                    <g:each in="${s.value}" var="state">
                                        <option value="${state}" ${cmd?.state == state.name() ? "selected" : ""}>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${state}</option>
                                    </g:each>
                                </g:each>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td><label for="name">${g.message(code: "workflowRun.list.name")}</label></td>
                        <td><input type="text" id="name" value="${cmd?.name}" autocomplete="off"/></td>
                    </tr>
                </table>
            </div>

            <div class="col-sm-1">
            </div>

            <div class="col-sm-5">
                <table class="key-value-table key-input">
                    <tr>
                        <td>${g.message(code: "workflowRun.list.allRuns")}</td>
                        <td id="allRuns"></td>
                    </tr>
                    <tr>
                        <td>${g.message(code: "workflowRun.list.running")}</td>
                        <td id="runningRuns"></td>
                    </tr>
                    <tr>
                        <td>${g.message(code: "workflowRun.list.failed")}</td>
                        <td id="failedRuns"></td>
                    </tr>
                </table>
            </div>
        </div>

        <div class="row ">
            <div class="col-auto mr-auto"></div>
            <div class="col-auto">
                %{-- using form because attribute "id" doesn't work in g:form --}%
                <form method="POST" id="bulk">
                    <div class="btn-group">
                        <button class="btn btn-primary" formaction="${g.createLink(action: "setFailedFinal")}"
                                title="${g.message(code: "workflowRun.list.setFailed")}"><i class="bi-file-earmark-x"></i></button>
                        <button class="btn btn-primary" formaction="${g.createLink(action: "restartStep")}"
                                title="${g.message(code: "workflowRun.list.restartSteps")}"><asset:image src="restart-step.svg"
                                                                                                         style="height:1em; width:1em"/></button>
                        <button class="btn btn-primary" formaction="${g.createLink(action: "restartRun")}"
                                title="${g.message(code: "workflowRun.list.restartRuns")}"><asset:image src="restart-run.svg"
                                                                                                        style="height:1em; width:1em"/></button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <br>

    <table id="runs">
        <thead>
        <tr>
            <g:each in="${columns}" var="column" status="i">
                <g:if test="${column == de.dkfz.tbi.otp.workflowExecution.WorkflowRunListController.Column.CHECKBOX}">
                    <th><input type='checkbox' name='selectAll' id="selectAll"></th>
                </g:if>
                <g:elseif test="${column == de.dkfz.tbi.otp.workflowExecution.WorkflowRunListController.Column.STATUS}">
                    <th><i class="bi-circle-fill" title="${g.message(code: column.message)}"></i></th>
                </g:elseif>
                <g:elseif test="${column == de.dkfz.tbi.otp.workflowExecution.WorkflowRunListController.Column.COMMENT}">
                    <th><i class="bi-info-circle" title="${g.message(code: column.message)}"></i></th>
                </g:elseif>
                <g:else>
                    <th>${g.message(code: column.message)}</th>
                </g:else>
            </g:each>
        </tr>
        </thead>
        <tbody></tbody>
        <tfoot>
        </tfoot>
    </table>
</div>

</body>
</html>
