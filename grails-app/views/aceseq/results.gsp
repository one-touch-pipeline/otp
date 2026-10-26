%{--
  - Copyright 2011-2026 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

<%@ page import="de.dkfz.tbi.otp.project.Project" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="otp.menu.cnv.results"/></title>
    <asset:javascript src="pages/analysis/results/datatable.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <g:render template="/templates/projectSelection"/>
    <h1><g:message code="aceseq.results.title"/></h1>

    <g:render template="/templates/bootstrap/noPlot" model="[project: selectedProject]"/>

    <g:set var="tableHeaders" value="${[
            'projectOverview.index.PID',
            'analysis.sampleTypes',
            'aceseq.tcc',
            'aceseq.ploidy',
            'aceseq.ploidyFactor',
            'aceseq.goodnessOfFit',
            'aceseq.gender',
            'aceseq.solutionPossible',
            'analysis.plots',
            'analysis.programVersion',
            'analysis.processingDate',
            'analysis.progress',
            'analysis.configFile'
    ]}" />

    <g:set var="showWorkflowRun" value="${false}" />
    <sec:ifAllGranted roles="ROLE_OPERATOR">
        <g:set var="showWorkflowRun" value="${true}" />
        <% tableHeaders.add('analysis.workflowRun') %>
    </sec:ifAllGranted>

    <div class="table">
        <div class="otpDataTables">
            <otp:dataTable
                    codes="${tableHeaders}"
                    id="resultsTable"/>
        </div>
    </div>
    <asset:script>
        $(function() {
            if (${showWorkflowRun}) {
                $('#resultsTable thead th').last().addClass('no_csv_export');
            }

            $.otp.resultsTable.registerAceseq(${showWorkflowRun});
        });
    </asset:script>
</div>
</body>
</html>
