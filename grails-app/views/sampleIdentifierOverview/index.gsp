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
    <g:if test="${projects}">
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
    <asset:script>
        $(function() {
            $.otp.sampleIdentifierOverviewTable.register();
        });
    </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
