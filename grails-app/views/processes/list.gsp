<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>List of Workflows</title>
<r:require module="jqueryDatatables"/>
<g:javascript src="jquery.timeago.js"/>
</head>
<body>
  <div class="body">
    <otp:autoRefresh/>
    <div id="workflowOverview">
        <table id="workflowOverviewTable">
            <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th>&nbsp;</th>
                    <th>Workflow</th>
                    <th>#</th>
                    <th># of Failed</th>
                    <th>Last Success</th>
                    <th>Last Failure</th>
                    <th>Duration</th>
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
    </div>
    <g:javascript>
       $(document).ready(function() {
            $.otp.createJobExecutionPlanListView('#workflowOverviewTable');
        });
    </g:javascript>
  </div>
</body>
</html>