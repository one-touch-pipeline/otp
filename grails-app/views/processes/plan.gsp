<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>List of ${failed ? 'failed' : ''} Processes for Workflow ${name}</title>
<link rel='stylesheet' href='http://www.datatables.net//release-datatables/media/css/demo_table.css' />
<jqDT:resources/>
<g:javascript library="jquery.dataTables" />
<g:javascript src="jquery.timeago.js"/>
</head>
<body>
  <div class="body">
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