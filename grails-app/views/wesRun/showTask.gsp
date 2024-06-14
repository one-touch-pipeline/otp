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

<%@ page import="de.dkfz.tbi.otp.utils.TimeFormats" %>
<html>
<head>
    <title>${g.message(code: "wesTask.title")}</title>
</head>

<body>
<div class="container-fluid otp-main-container">

    <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
            <li class="breadcrumb-item"><g:link controller="workflowRunList" action="index" params="['workflow.id': nav.workflow?.id, state: nav.states?.join(','), name: nav.name]">${g.message(code: "workflow.navigation.list")}</g:link></li>
            <li class="breadcrumb-item"><g:link controller="workflowRunDetails" action="index" params="['workflow.id': nav.workflow?.id, state: nav.states?.join(','), name: nav.name]" id="${nav.workflowRun?.id}">
                ${g.message(code: "workflow.navigation.details")} (${nav.workflowRun?.id})</g:link>
            </li>
            <li class="breadcrumb-item"><g:link controller="wesRun" action="show" id="${wesRun.id}" params="['workflow.id': nav.workflow?.id, 'workflowRun.id': nav.workflowRun?.id, state: nav.states?.join(','), name: nav.name]">${g.message(code: "wesRun.title")}</g:link></li>
            <li class="breadcrumb-item active" aria-current="page">${g.message(code: "wesTask.title")} (${wesLog.id})</li>
        </ol>
    </nav>

    <h1>${g.message(code: "wesTask.title")} ${wesLog.name}</h1>

    <div class="detailContainer">
        <table class="w-100">
            <colgroup>
                <col class="columnProperty">
                <col class="columnValue">
            </colgroup>
            <tr>
                <td>${g.message(code: "wesLog.command")}</td>
                <td><pre>${wesLog.cmd}</pre></td>
            </tr>
            <tr>
                <td>${g.message(code: "wesLog.exitCode")}</td>
                <td>${wesLog.exitCode}</td>
            </tr>
            <tr>
                <td>${g.message(code: "wesLog.startTime")}</td>
                <td>${TimeFormats.DATE_TIME.getFormattedLocalDateTime(wesLog.startTime)}</td>
            </tr>
            <tr>
                <td>${g.message(code: "wesLog.endTime")}</td>
                <td>${TimeFormats.DATE_TIME.getFormattedLocalDateTime(wesLog.endTime)}</td>
            </tr>
        </table>
    </div>
    <br><br>

    <h2>${g.message(code: "wesLog.stdout")}</h2>
    <g:if test="${wesLog.stdout}">
        <pre>${wesLog.stdout}</pre>
    </g:if>
    <g:else>
        <i>${NA}</i>
    </g:else>

    <h2>${g.message(code: "wesLog.stderr")}</h2>
    <g:if test="${wesLog.stderr}">
        <pre>${wesLog.stderr}</pre>
    </g:if>
    <g:else>
        <i>${NA}</i>
    </g:else>
</div>
</body>
</html>
