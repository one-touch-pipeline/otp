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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processes.plan.title" args="${ [state, name] }" /></title>
    <asset:javascript src="modules/workflows"/>
</head>
<body>
    <div class="body">
        <otp:autoRefresh/>
        <div id="plan-dsl-dialog" title="DSL for Workflow ${name}" style="display: none">
            <textarea readonly="readonly" cols="200" rows="100"></textarea>
        </div>
        <h1><g:message code="processes.plan.title" args="${ [state, name] }" /></h1>
        <div>
            <g:img dir="assets/status" file="${enabled ? 'green.png' : 'grey.png'}" style="vertical-align: middle"/>
            <g:if test="${enabled}">
                <g:message code="processes.plan.workflowIsEnabled"/>
            </g:if>
            <g:else>
                <g:message code="processes.plan.workflowIsDisabled"/>
            </g:else>
            <% boolean buttonRendered = false; %>
            <sec:permitted className="de.dkfz.tbi.otp.job.plan.JobExecutionPlan" id="${id}" permission="write">
                <% buttonRendered = true; %>
                <button id="enable-workflow-button" style="${enabled ? 'display: none;' : ''}"><g:message code="processes.plan.enableWorkflow"/></button>
                <button id="disable-workflow-button" style="${enabled ? '' : 'display: none;'}"><g:message code="processes.plan.disableWorkflow"/></button>
            </sec:permitted>
            <g:if test="${!buttonRendered}">
                <sec:ifAllGranted roles="ROLE_ADMIN">
                    <button id="enable-workflow-button" style="${enabled ? 'display: none;' : ''}"><g:message code="processes.plan.enableWorkflow"/></button>
                    <button id="disable-workflow-button" style="${enabled ? '' : 'display: none;'}"><g:message code="processes.plan.disableWorkflow"/></button>
                </sec:ifAllGranted>
            </g:if>
        </div>
        <div>
            <div id="plan-visualization" style="display: none"></div>
            <button id="show-visualization"><g:message code="processes.plan.showPlanVisualization"/></button>
            <button id="hide-visualization" style="display: none"><g:message code="processes.plan.hidePlanVisualization"/></button>
            <button id="generate-dsl"><g:message code="processes.plan.generatePlanMarkup"/></button>
        </div>
        <div id="workflowOverview">
            <div class="otpDataTables">
                <otp:dataTable codes="${[
                    'otp.blank',
                    'otp.blank',
                    'workflow.plan.table.headers.operatesOn',
                    'workflow.plan.table.headers.creationDate',
                    'workflow.plan.table.headers.lastUpdate',
                    'workflow.plan.table.headers.processingStep',
                    'workflow.plan.table.headers.status',
                    'otp.blank',
                    'otp.blank'
                ]}" id="workflowOverviewTable"/>
            </div>
        </div>
        <asset:script type="text/javascript">
            $(document).ready(function() {
                $.otp.workflows.registerProcesses("#workflowOverviewTable", ${id}, "${state}");
            });
        </asset:script>
    </div>
</body>
</html>
