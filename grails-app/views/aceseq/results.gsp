<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="otp.menu.cnv.results" /></title>
    <asset:javascript src="pages/analysis/results/datatable.js"/>
</head>
<body>
    <div class="body">
    <g:if test="${projects}">
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
        <br><br><br>
        <div class="table">
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                            'projectOverview.index.PID',
                            'analysis.sampleTypes',
                            'aceseq.tcc',
                            'aceseq.ploidy',
                            'aceseq.ploidyFactor',
                            'aceseq.goodnessOfFit',
                            'aceseq.gender',
                            'aceseq.solutionPossible',
                            'analysis.pluginVersion',
                            'analysis.processingDate',
                            'analysis.progress',
                            'analysis.plots',
                    ] }"
                    id="resultsTable" />
            </div>
        </div>
    <asset:script>
        $(function() {
            $.otp.resultsTable.registerAceseq();
        });
    </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
