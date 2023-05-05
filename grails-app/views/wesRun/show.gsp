%{--
  - Copyright 2011-2023 The OTP authors
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
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title><g:message code="wesRun.title"/></title>
</head>
<body>
    <div class="container-fluid otp-main-container">
        <g:if test="${nav.workflowRun?.id}">
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb">
                    <li class="breadcrumb-item"><g:link controller="workflowRunList" action="index" params="['workflow.id': nav.workflow?.id, state: nav.states?.join(','), name: nav.name]">${g.message(code: "workflow.navigation.list")}</g:link></li>
                    <li class="breadcrumb-item"><g:link controller="workflowRunDetails" action="index" params="['workflow.id': nav.workflow?.id, state: nav.states?.join(','), name: nav.name]" id="${nav.workflowRun?.id}">
                        ${g.message(code: "workflow.navigation.details")} (${nav.workflowRun?.id})</g:link>
                    </li>
                    <li class="breadcrumb-item active" aria-current="page">${g.message(code: "wesRun.title")}</li>
                </ol>
            </nav>
        </g:if>

        <h1>${g.message(code: "wesRun.title")} ${wesRun.wesIdentifier ?: ""}</h1>
        <g:if test="${wesRun}">
            <div class="detailContainer" id="general">
                <div class="detailContainerTitle"><g:message code="wesRun.general"/></div>
                <br>
                <table class="w-100">
                    <colgroup>
                        <col class="columnProperty">
                        <col class="columnValue">
                    </colgroup>
                    <tr>
                        <td class="pl-4"><g:message code="wesRun.identifier"/></td>
                        <td>${wesRun.wesIdentifier ?: NA}</td>
                    </tr>
                    <tr>
                        <td class="pl-4"><g:message code="wesRun.subPath"/></td>
                        <td>${wesRun.subPath ?: NA}</td>
                    </tr>
                    <tr>
                        <td class="pl-4"><g:message code="wesRun.runRequest"/></td>
                        <td>${wesRun.wesRunLog?.runRequest ?: NA}</td>
                    </tr>
                    <g:if test="${wesRun.wesRunLog?.runLog}">
                        <tr>
                            <td class="pl-4"><g:message code="wesLog.name"/></td>
                            <td>${wesRun.wesRunLog?.runLog?.name ?: NA}</td>
                        </tr>
                        <tr>
                            <td class="pl-4"><g:message code="wesLog.command"/></td>
                            <td>${wesRun.wesRunLog?.runLog?.cmd ?: NA}</td>
                        </tr>
                    </g:if>
                </table>
            </div>
            <div class="detailContainer" id="status">
                <div class="detailContainerTitle"><g:message code="wesRun.state"/></div>
                <br>
                <table class="w-100">
                    <col class="columnColors">
                    <col class="columnProperty">
                    <col class="columnValue">
                    <tr>
                        <td></td>
                        <td class="pl-3"><g:message code="wesRun.monitoringState"/></td>
                        <td>${wesRun.state ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td class="pl-3"><g:message code="wesRun.runState"/></td>
                        <td>${wesRun.wesRunLog?.state ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td class="pl-3"><g:message code="wesLog.exitCode"/></td>
                        <td>${wesRun.wesRunLog?.runLog?.exitCode ?: NA}</td>
                    </tr>
                </table>
            </div>
            <div class="detailContainer" id="time">
                <div class="detailContainerTitle"><g:message code="wesRun.time"/></div>
                <br>
                <table class="w-100">
                    <col class="columnColors">
                    <col class="columnProperty">
                    <col class="columnValue">
                    <tr>
                        <td></td>
                        <td class="pl-3"><g:message code="wesLog.startTime"/></td>
                        <td>${de.dkfz.tbi.util.TimeFormats.DATE_TIME.getFormattedLocalDateTime(wesRun.wesRunLog?.runLog?.startTime) ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td class="pl-3"><g:message code="wesLog.endTime"/></td>
                        <td>${de.dkfz.tbi.util.TimeFormats.DATE_TIME.getFormattedLocalDateTime(wesRun.wesRunLog?.runLog?.endTime) ?: NA}</td>
                    </tr>
                </table>
            </div>
            <div class="detailContainer">
                <div class="detailContainerTitle"><g:message code="wesRun.tasks"/></div>
                <br>
                <table class="w-100">
                    <col class="columnColors">
                    <col class="columnProperty">
                    <col class="columnValue">
                    <g:each in="${wesRun.wesRunLog.taskLogs}" var="log">
                        <tr>
                            <td></td>
                            <td class="pl-3"><g:message code="wesRun.task"/></td>
                            <td>
                                <g:link action="showTask" id="${log.id}" params="['workflow.id': nav.workflow?.id, 'workflowRun.id': nav.workflowRun?.id,
                                                                                  state: nav.states?.join(','), name: nav.name]">${log.name}</g:link>
                            </td>
                        </tr>
                    </g:each>
                    <g:if test="${!wesRun.wesRunLog.taskLogs}">
                        <tr>
                            <td></td>
                            <td class="pl-3">${NA}</td>
                            <td></td>
                        </tr>

                    </g:if>
                </table>
            </div>
            <br><br>

            <h2>${g.message(code: "wesLog.stdout")}</h2>
            <g:if test="${wesRun.wesRunLog?.runLog?.stdout}">
                <pre>${wesRun.wesRunLog.runLog.stdout}</pre>
            </g:if>
            <g:else>
                <i>${NA}</i>
            </g:else>

            <h2>${g.message(code: "wesLog.stderr")}</h2>
            <g:if test="${wesRun.wesRunLog?.runLog?.stderr}">
                <pre>${wesRun.wesRunLog.runLog.stderr}</pre>
            </g:if>
            <g:else>
                <i>${NA}</i>
            </g:else>
        </g:if>
        <g:else>
            ${NA}
        </g:else>
    </div>
</body>
</html>
