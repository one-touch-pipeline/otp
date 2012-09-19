<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>List of ${failed ? 'failed' : ''} Processes for Workflow ${name}</title>
<jqDT:resources type="js"/>
<g:javascript src="jquery.timeago.js"/>
<r:require module="graphDracula"/>
<r:require module="jqueryUI"/>
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
        <table id="workflowOverviewTable">
            <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th>&nbsp;</th>
                    <th>Operates on</th>
                    <th>Creation Date</th>
                    <th>Last Update</th>
                    <th>Current Processing Step</th>
                    <th>Status</th>
                    <th>&nbsp;</th>
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
    </div>
    <g:javascript>
       $(document).ready(function() {
            $.otp.createProcessListView("#workflowOverviewTable", ${id}, ${failed ? 'true' : 'false'});
        });
    </g:javascript>
  </div>
</body>
</html>