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

<%@ page import="de.dkfz.tbi.otp.project.Project" contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="otp.menu.runYapsa.results" /></title>
    <asset:javascript src="pages/analysis/results/datatable.js"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>
        <g:render template="/templates/projectSelection"/>
        <h1><g:message code="otp.menu.runYapsa.results"/></h1>

        <g:render template="/templates/bootstrap/noPlot" model="[project: selectedProject]"/>

        <div class="table">
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                        'projectOverview.index.PID',
                        'analysis.sampleTypes',
                        'analysis.seqType',
                        'analysis.libPrepKits',
                        'analysis.programVersion',
                        'analysis.processingDate',
                        'analysis.progress',
                        'analysis.configFile',
                    ]}"
                    id="resultsTable" />
            </div>
        </div>
    <asset:script>
        $(function() {
            $.otp.resultsTable.registerRunYapsa();
        });
    </asset:script>
    </div>
</body>
</html>
