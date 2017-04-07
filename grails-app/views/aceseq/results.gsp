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
        <form class="blue_label" id="projectsGroupbox" action="${g.createLink(controller: 'projectSelection', action: 'select')}">
            <span class="blue_label"><g:message code="home.projectfilter"/> :</span>
            <g:hiddenField name="displayName" value=""/>
            <g:hiddenField name="type" value="PROJECT"/>
            <g:hiddenField name="redirect" value="${request.forwardURI - request.contextPath}"/>
            <g:select class="criteria" id="project" name='id'
                from='${projects}' value='${project.id}' optionKey='id' optionValue='name' onChange='submit();' />
        </form>
        <br><br><br>
        <div class="table">
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                        'projectOverview.index.PID',
                        'analysis.sampleTypes',
                        'aceseq.normalContamination',
                        'aceseq.ploidy',
                        'aceseq.ploidyFactor',
                        'aceseq.goodnessOfFit',
                        'aceseq.gender',
                        'aceseq.solutionPossible',
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
            $.otp.resultsTable.registerAceseq();
        });
    </asset:script>
</body>
</html>
