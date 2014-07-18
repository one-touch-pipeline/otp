<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="projectStatistic.title" /></title>
</head>

<body>
    <div class="body">
        <div class="otpDataTables">
            <otp:dataTable
                codes="${[
                    'projectOverview.index.dataBaseID',
                    'individual.insert.mockFullName',
                    'individual.insert.internIdentifier',
                ] }"
                id="overviewTableMMMLMappingId"/>
        </div>
    </div>
    <r:script>
        $(function() {
            $.otp.projectOverviewTable.registerMMMLMappingId();
        });
    </r:script>
</body>
</html>
