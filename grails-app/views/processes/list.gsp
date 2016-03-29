%<@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processes.list.title"/></title>
    <asset:javascript src="modules/workflows"/>
</head>
<body>
    <div class="body">
        <otp:autoRefresh/>
        <div id="workflowOverview">
            <div class="otpDataTables">
                <otp:dataTable codes="${[
                    'workflow.list.table.headers.workflow',
                    'workflow.list.table.headers.count',
                    'workflow.list.table.headers.countFailed',
                    'workflow.list.table.headers.countRunning',
                    'workflow.list.table.headers.lastSuccess',
                    'workflow.list.table.headers.lastFailure',
                 ]}" id="workflowOverviewTable"/>
            </div>
        </div>
        <asset:script type="text/javascript">
            $(document).ready(function() {
                $.otp.workflows.registerJobExecutionPlan('#workflowOverviewTable');
            });
        </asset:script>
    </div>
</body>
</html>
