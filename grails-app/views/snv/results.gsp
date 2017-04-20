<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="otp.menu.snv.results" /></title>
    <asset:javascript src="pages/analysis/results/datatable.js"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
        <br><br><br>
        <div class="table">
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                        'projectOverview.index.PID',
                        'analysis.sampleTypes',
                        'analysis.seqType',
                        'analysis.libPrepKits',
                        'analysis.processingDate',
                        'analysis.progress',
                        'analysis.plots',
                    ] }"
                    id="resultsTable" />
            </div>
        </div>
    </div>
    <asset:script>
        $(function() {
            $.otp.resultsTable.registerSnv();
        });
    </asset:script>
</body>
</html>
