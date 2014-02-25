<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="projectStatistic.title" /></title>
<r:require module="core" />
<r:require module="jqueryDatatables" />
</head>

<body>
    <div class="body">
        <div class="dataTableContainer overviewTableMMMLMappingId"
            style="margin-right: 1%;">
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