<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="otp.menu.snv.results" /></title>
    <asset:javascript src="pages/snv/results/datatable.js"/>
</head>
<body>
    <div class="body">
        <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="home.projectfilter"/> :</span>
            <g:select class="criteria" id="projectName" name='projectName'
                from='${projects}' value='${project}' onChange='submit();' />
        </form>
        <br><br><br>
        <div class="table">
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                        'projectOverview.index.PID',
                        'snv.sampleTypes',
                        'snv.seqType',
                        'snv.processingDate',
                        'snv.progress',
                        'snv.plots',
                    ] }"
                    id="snvResultsTable" />
            </div>
        </div>
    </div>
    <asset:script>
        $(function() {
            $.otp.snvResultsTable.register();
        });
    </asset:script>
</body>
</html>
