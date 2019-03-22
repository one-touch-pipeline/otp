%{--
  - Copyright 2011-2019 The OTP authors
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

<%@ page import="de.dkfz.tbi.otp.job.jobs.RestartableStartJob" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processes.process.title.listOfProcessingSteps" args="${ [id] }"/> <g:message code="processes.process.title.workflow" args="${ [name] }"/></title>
    <asset:javascript src="modules/workflows"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>

        <div id="processInfoBox">
            <h1><g:message code="processes.process.title.listOfProcessingSteps" args="${ [id] }"/>  <g:link action="plan" id="${planId}"><g:message code="processes.process.title.workflow" args="${ [name] }"/></g:link><br>
            <g:if test="${process.restarted}">
                <g:message code="processes.process.title.restartedProcessFrom"/>  <g:link action="process" id="${process.restarted.id}"><g:message code="processes.process.title.restartedProcessLink" args="${ [process.restarted.id] }"/></g:link>
            </g:if>
            <g:if test="${restartedProcess}">
                <g:message code="processes.process.title.restartedProcess"/>  <g:link action="process" id="${restartedProcess.id}"><g:message code="processes.process.title.restartedProcessLink" args="${ [restartedProcess.id] }"/></g:link>
            </g:if>
            </h1>
            <g:if test="${hasError && !restartedProcess}">
                <table>
                    <tr>
                        <td>
                            <g:form name="operatorIsAwareOfFailureForm" controller="processes" action="updateOperatorIsAwareOfFailure">
                                <g:message code="processes.process.operatorIsAwareOfFailure"/>
                                <g:checkBox id="operatorIsAwareOfFailureCheckBox" name="operatorIsAwareOfFailure" checked="${operatorIsAwareOfFailure}" value="true" onChange="submit();"/>
                                <g:hiddenField name="process.id" value="${id}"/>
                            </g:form>
                        </td>
                        <td>
                            <g:if test="${showRestartButton}">
                                <sec:ifAllGranted roles="ROLE_ADMIN">
                                    <g:form action="restartWithProcess" method="POST">
                                        <g:hiddenField name="id" value="${process.id}"/>
                                        <g:submitButton id="restartProcessButton" name="${g.message(code: "processes.process.restartProcess")}"/>
                                    </g:form>
                                </sec:ifAllGranted>
                            </g:if>
                        </td>
                    </tr>
                </table>
            </g:if>
            <g:if test="${parameter}">
                <p>
                    <g:message code="processes.process.operatesOn"/>
                    ${parameter.text}
                </p>
            </g:if>
            <div>
                <div id="process-visualization" style="display: none"></div>
                <button id="show-visualization"><g:message code="processes.process.showProcessVisualization"/></button>
                <button id="hide-visualization" style="display: none"><g:message code="processes.process.hideProcessVisualization"/></button>
            </div>
        </div>
        <div id="processCommentBox" class="commentBoxContainer">
            <div id="commentLabel">Comment:</div>
            <sec:ifNotGranted roles="ROLE_OPERATOR">
                <textarea id="commentBox" readonly>${comment?.comment}</textarea>
            </sec:ifNotGranted>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <textarea id="commentBox">${comment?.comment}</textarea>
                <div id="commentButtonArea">
                    <button id="saveComment" disabled>&nbsp;&nbsp;&nbsp;<g:message code="commentBox.save" /></button>
                    <button id="cancelComment" disabled><g:message code="commentBox.cancel" /></button>
                </div>
            </sec:ifAllGranted>
            <div id="commentDateLabel">${comment?.modificationDate?.format('EEE, d MMM yyyy HH:mm')}</div>
            <div id="commentAuthorLabel">${comment?.author}</div>
        </div>
        <div id="processOverview">
            <div class="otpDataTables">
                <otp:dataTable codes="${[
                    'otp.blank',
                    'otp.blank',
                    'workflow.process.table.headers.processingStep',
                    'workflow.process.table.headers.job',
                    'workflow.process.table.headers.creationDate',
                    'workflow.process.table.headers.lastUpdate',
                    'workflow.process.table.headers.duration',
                    'workflow.process.table.headers.status',
                    'otp.blank'
                ]}" id="processOverviewTable"/>
            </div>
        </div>
        <asset:script type="text/javascript">
            $(document).ready(function() {
                $.otp.workflows.registerProcessingStep("#processOverviewTable", ${id});
                $.otp.initCommentBox(${id}, "#processCommentBox");
            });
        </asset:script>
    </div>
</body>
</html>
