<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="processes.list.title"/></title>
<r:require module="workflows"/>
</head>
<body>
  <div class="body">
    <h1><g:message code="processes.list.title"/></h1>
    <otp:autoRefresh/>
    <div id="workflowOverview">
        <otp:dataTable codes="${[
            'otp.blank',
            'otp.blank',
            'workflow.list.table.headers.workflow',
            'workflow.list.table.headers.count',
            'workflow.list.table.headers.countFailed',
            'workflow.list.table.headers.lastSuccess',
            'workflow.list.table.headers.lastFailure',
            'workflow.list.table.headers.duration'
            ]}" id="workflowOverviewTable"/>
    </div>
    <g:javascript>
       $(document).ready(function() {
            $.otp.workflows.registerJobExecutionPlan('#workflowOverviewTable');
        });
    </g:javascript>
  </div>
</body>
</html>