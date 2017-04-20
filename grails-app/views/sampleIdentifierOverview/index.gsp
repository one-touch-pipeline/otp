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
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
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
