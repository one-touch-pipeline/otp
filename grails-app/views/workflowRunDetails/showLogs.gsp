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
    <title>${g.message(code: "workflowRun.details.log")}</title>
    <asset:javascript src="pages/workflowRunList/common.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><g:link controller="workflowRunList" action="index" params="['workflow.id': nav.workflow?.id, state: nav.states?.join(','), name: nav.name]">${g.message(code: "workflow.navigation.list")}</g:link></li>
            <li class="breadcrumb-item"><g:link controller="workflowRunDetails" action="index" params="['workflow.id': nav.workflow?.id, state: nav.states?.join(','), name: nav.name]" id="${step.workflowRun.id}">
                ${g.message(code: "workflow.navigation.details")} (${step.workflowRun.id})</g:link>
            </li>
            <li class="breadcrumb-item active" aria-current="page">${g.message(code: "workflow.navigation.logs")} (${step.id})</li>
        </ol>
    </nav>

    <nav class="navbar">
        <div class="navbar-brand">
            <div id="statusDot" title="${step.state}" data-status="${step.state}" class="d-inline-block"></div>
            <span class="d-inline-flex align-top pt-1 ml-2">${g.message(code: "workflowRun.details.log")} for ${step.beanName} (${step.id})</span>
        </div>
    </nav>

    <div class="dropdown-divider"></div>

    ${raw(step.workflowRun.displayName.replace("\n", "<br>"))}

    <g:if test="${logs.empty}">
        <div class="dropdown-divider"></div>
        <pre>${g.message(code: "workflowRun.details.log.none")}</pre>
    </g:if>
    <g:else>
        <table class="table table-sm table-bordered table-striped mt-3">
            <thead>
            <tr>
                <th scope="col">Type</th>
                <th scope="col">Log message</th>
                <th scope="col">Created</th>
                <th scope="col">Database id</th>
            </tr>
            </thead>
            <tbody>
            <g:each in="${logs}" var="log">
                <tr>
                    <td>
                        ${log.type}
                    </td>
                    <td>
                        <pre>${log.message}</pre>
                    </td>
                    <td>
                        ${log.dateCreated}
                    </td>
                    <td>
                        ${log.id}
                    </td>
               </tr>
            </g:each>
            </tbody>
        </table>
    </g:else>
</div>
</body>
</html>
