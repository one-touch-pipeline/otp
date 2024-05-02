%{--
  - Copyright 2011-2024 The OTP authors
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
        <g:render template="/templates/messages"/>
        <g:render template="/templates/projectSelection"/>
        <h1><g:message code="otp.menu.indel.results"/></h1>

        <div class="table">
            <div class="otpDataTables">
                <table id="resultsTable">
                    <thead>
                    <tr class="indel-table-header">
                        <th colspan="4"></th>
                        <th colspan="6"><g:message code="analysis.indel.indels"/></th>
                        <th colspan="7"><g:message code="analysis.indel.swapChecker"/></th>
                        <th colspan="3"><g:message code="analysis.indel.tinda"/></th>
                        <th colspan="4"></th>
                    </tr>
                    <tr class="indel-table">
                        <th><g:message code="projectOverview.index.PID"/></th>
                        <th><g:message code="analysis.sampleTypes"/></th>
                        <th><g:message code="analysis.seqType"/></th>
                        <th><g:message code="analysis.libPrepKits"/></th>
                        <%-- Indels --%>
                        <th><g:message code="analysis.indel.indels.numIndels"/></th>
                        <th><g:message code="analysis.indel.indels.numIns"/></th>
                        <th><g:message code="analysis.indel.indels.numDels"/></th>
                        <th><g:message code="analysis.indel.indels.numSize1_3"/></th>
                        <th><g:message code="analysis.indel.indels.numSize4_10"/></th>
                        <th><g:message code="analysis.plots"/></th>
                        <%-- Swap Checker --%>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInTumor"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInControl"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInTumorCommonInGnomad"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInControlCommonInGnomad"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInTumorPass"/></th>
                        <th><g:message code="analysis.indel.swapChecker.somaticSmallVarsInControlPass"/></th>
                        <th><g:message code="analysis.indel.swapChecker.germlineSmallVarsInBothRare"/></th>
                        <%-- TINDA --%>
                        <th><g:message code="analysis.indel.tinda.tindaSomaticAfterRescue"/></th>
                        <th><g:message code="analysis.indel.tinda.tindaSomaticAfterRescueMedianAlleleFreqInControl"/></th>
                        <th><g:message code="analysis.indel.tinda.plot"/></th>
                        <%-- Remainings --%>
                        <th><g:message code="analysis.programVersion"/></th>
                        <th><g:message code="analysis.processingDate"/></th>
                        <th><g:message code="analysis.progress"/></th>
                        <th><g:message code="analysis.configFile"/></th>
                    </tr>
                    </thead>
                    <tbody class="indel-table"></tbody>
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
    </div>
</body>
</html>
