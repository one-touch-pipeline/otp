<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>List of ${failed ? 'failed' : ''} Processes for Workflow ${name}</title>
<r:require module="workflows"/>
</head>
<body>
  <div class="body">
    <otp:autoRefresh/>
    <div id="plan-dsl-dialog" title="DSL for Workflow ${name}" style="display: none">
        <textarea readonly="readonly" cols="200" rows="100"></textarea>
    </div>
    <h1>List of ${failed ? 'failed' : ''} Processes for Workflow ${name}</h1>
    <div>
        <g:img dir="images/status" file="${enabled ? 'green.png' : 'grey.png'}" style="vertical-align: middle"/>
        This Workflow is currently <strong>${enabled ? 'enabled' : 'disabled'}</strong>!
        <% boolean buttonRendered = false; %>
        <sec:permitted className="de.dkfz.tbi.otp.job.plan.JobExecutionPlan" id="${id}" permission="write">
            <% buttonRendered = true; %>
            <button id="enable-workflow-button" style="${enabled ? 'display: none;' : ''}">Enable Workflow</button>
            <button id="disable-workflow-button" style="${enabled ? '' : 'display: none;'}">Disable Workflow</button>
        </sec:permitted>
        <g:if test="${!buttonRendered}">
            <sec:ifAllGranted roles="ROLE_ADMIN">
                <button id="enable-workflow-button" style="${enabled ? 'display: none;' : ''}">Enable Workflow</button>
                <button id="disable-workflow-button" style="${enabled ? '' : 'display: none;'}">Disable Workflow</button>
            </sec:ifAllGranted>
        </g:if>
    </div>
    <div>
        <div id="plan-visualization" style="display: none"></div>
        <button id="show-visualization">Show Plan Visualization</button>
        <button id="hide-visualization" style="display: none">Hide Plan Visualization</button>
        <button id="generate-dsl">Generate Plan Markup</button>
    </div>
    <div id="workflowOverview">
        <otp:dataTable codes="${[
            'otp.blank',
            'otp.blank',
            'workflow.plan.table.headers.operatesOn',
            'workflow.plan.table.headers.creationDate',
            'workflow.plan.table.headers.lastUpdate',
            'workflow.plan.table.headers.processingStep',
            'workflow.plan.table.headers.status',
            'otp.blank'
            ]}" id="workflowOverviewTable"/>
    </div>
    <g:javascript>
       $(document).ready(function() {
            $.otp.workflows.registerProcesses("#workflowOverviewTable", ${id}, ${failed ? 'true' : 'false'});
        });
    </g:javascript>
  </div>
</body>
</html>