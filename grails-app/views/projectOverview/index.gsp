<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title>OTP - project overview</title>
    <r:require module="core"/>
    <r:require module="jqueryDatatables"/>
</head>
<body>
    <div class="body">
        <div class="overviewMenu">
            <div class="projectLabel">project:</div>
            <div class="projectSelect"><g:select id="project_select" name='project_select' from='${projects}'></g:select></div>
        </div>
        <div class="table overviewContainer">
            <div style="width:20px;height:20px;"></div>

            <div class="dataTableContainer projectOverviewTable" style="margin-right:1%;">
                <otp:dataTable codes="${[
                    'projectOverview.index.PID',
                    'projectOverview.index.sampleType',
                    'projectOverview.index.sequenceTypeName',
                    'projectOverview.index.sequenceTypeLibraryLayout',
                    'projectOverview.index.centerName',
                    'projectOverview.index.platformId',
                    'projectOverview.index.gigaBase'
                ] }" id="projectOverviewTable"/>
            </div>
        </div>
    </div>
    <r:script>
        $(function() {
            $.otp.projectOverviewTable.register();
        });
    </r:script>
</body>
</html>