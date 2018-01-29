<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="otp.menu.indel.results" /></title>
    <asset:javascript src="pages/analysis/results/datatable.js"/>
</head>
<body>
    <div class="body">
    <g:if test="${projects}">
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
        <br><br><br>
        <div class="table">
            <div class="otpDataTables">
                <table id="resultsTable">
                    <thead>
                    <tr>
                        <th colspan="4"></th>
                        <th colspan="5"><g:message code="analysis.indel.indels"/></th>
                        <th colspan="6"><g:message code="analysis.indel.swapChecker"/></th>
                        <th colspan="2"><g:message code="analysis.indel.tinda"/></th>
                        <th colspan="4"></th>
                    </tr>
                    <tr>
                        <th><g:message code="projectOverview.index.PID"/></th>
                        <th><g:message code="analysis.sampleTypes"/></th>
                        <th><g:message code="analysis.seqType"/></th>
                        <th><g:message code="analysis.libPrepKits"/></th>
                        <th><g:message code="analysis.indel.indels.numIndels"/></th>
                        <th><g:message code="analysis.indel.indels.numIns"/></th>
                        <th><g:message code="analysis.indel.indels.numDels"/></th>
                        <th><g:message code="analysis.indel.indels.numSize1_3"/></th>
                        <th><g:message code="analysis.indel.indels.numSize4_10"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInTumor"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInControl"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInTumorCommonInGnomad"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInControlCommonInGnomad"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInTumorPass"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInControlPass"/></th>
                        <th><g:message code="analysis.indel.tinda.tindaSomaticAfterRescue"/></th>
                        <th><g:message code="analysis.indel.tinda.tindaSomaticAfterRescueMedianAlleleFreqInControl"/></th>
                        <th><g:message code="analysis.pluginVersion"/></th>
                        <th><g:message code="analysis.processingDate"/></th>
                        <th><g:message code="analysis.progress"/></th>
                        <th><g:message code="analysis.plots"/></th>
                    </tr>
                    </thead>
                    <tbody></tbody>
                    <tfoot>
                    </tfoot>
                </table>
            </div>
        </div>
    <asset:script>
        $(function() {
            $.otp.resultsTable.registerIndel();
        });
    </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
