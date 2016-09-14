<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title>${g.message(code: "sampleIdentifierOverview.index.title")}</title>
    <asset:javascript src="pages/sampleIdentifierOverview/index/datatable.js"/>
</head>
<body>
    <div class="body">
        <div style="height: 20px;"></div>
        <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="home.projectfilter"/> :</span>
            <g:select class="criteria" id="project_select" name='project_select'
                      from='${projects}' value='${project}'></g:select>
        </form>
        <div style="width: 20px; height: 60px;"></div>
            <div class="otpDataTables">
                <otp:dataTable codes="${[
                        'sampleIdentifierOverview.index.pid',
                        'sampleIdentifierOverview.index.sampleType',
                        'sampleIdentifierOverview.index.sampleIdentifier',
                ]}" id="sampleIdentifierOverviewTable" />
            </div>
    </div>
<asset:script>
    $(function() {
        $.otp.sampleIdentifierOverviewTable.register();
    });
</asset:script>
</body>
</html>
