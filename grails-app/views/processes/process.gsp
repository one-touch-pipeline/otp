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

<%@ page import="de.dkfz.tbi.otp.job.jobs.RestartableStartJob" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processes.process.title.listOfProcessingSteps" args="${[id]}"/> <g:message code="processes.process.title.workflow" args="${[name]}"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/processes/common.js"/>
    <asset:javascript src="pages/processes/process/process.js"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>

        <div class="two-column-grid-container">
            <div class="grid-element">
                <h1>
                    <g:message code="processes.process.title.listOfProcessingSteps" args="${[id]}"/>
                    <g:link action="plan" id="${planId}"><g:message code="processes.process.title.workflow" args="${[name]}"/></g:link>
                    <g:if test="${process.restarted}">
                        <g:message code="processes.process.title.restartedProcessFrom"/> <g:link action="process" id="${process.restarted.id}"><g:message code="processes.process.title.restartedProcessLink" args="${[process.restarted.id]}"/></g:link>
                    </g:if>
                    <g:if test="${restartedProcess}">
                        <g:message code="processes.process.title.restartedProcess"/> <g:link action="process" id="${restartedProcess.id}"><g:message code="processes.process.title.restartedProcessLink" args="${[restartedProcess.id]}"/></g:link>
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
            </div>
            <div class="grid-element comment-box">
                <g:render template="/templates/commentBox" model="[
                        commentable     : process,
                        targetController: 'processes',
                        targetAction    : 'saveProcessComment',
                ]"/>
            </div>
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
            });
        </asset:script>
    </div>
</body>
</html>
