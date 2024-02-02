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
    <title>${g.message(code: "projectOverview.index.title")}</title>
    <asset:javascript src="common/sharedCharts.js"/>
    <asset:javascript src="pages/projectOverview/index/datatable.js"/>
    <asset:javascript src="pages/projectOverview/index/charts.js"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/projectSelection"/>

        <h1><g:message code="projectOverview.pageTitle" args="[selectedProject.name]"/></h1>
        <g:message code="projectOverview.numberOfPatient"/>: <span id="patient-count"></span>

        <h2 class="data-table-header"><g:message code="projectOverview.table.sampleType.title"/></h2>
        <div class="otpDataTables" data-csv-title="number_of_samples_by_sample_type">
            <otp:dataTable
                codes="${[
                    'projectOverview.table.sampleTypeName',
                    'projectOverview.table.sampleCount',
                ]}"
                id="sampleTypeNameCountBySample" />
        </div>

        <h2 class="data-table-header"><g:message code="projectOverview.title.centersOverviewTable"/></h2>
        <div class="otpDataTables" data-csv-title="sequencing_center">
            <otp:dataTable
                codes="${[
                    'overview.statistic.center.name',
                    'overview.statistic.center.totalCount',
                    'overview.statistic.center.newCount',
                ]}"
                id="centerNameRunId" />
        </div>

        <h2 class="data-table-header"><g:message code="projectOverview.table.statistic.title"/></h2>
        <div class="otpDataTables" data-csv-title="sequences_of_samples">
            <otp:dataTable
                codes="${[
                        'projectOverview.index.PID',
                        'projectOverview.index.sampleType',
                        'projectOverview.index.sequenceTypeName',
                        'projectOverview.index.sequencingReadType',
                        'projectOverview.index.singleCell',
                        'projectOverview.index.centerName',
                        'projectOverview.index.platformId',
                        'projectOverview.index.laneCount',
                        'projectOverview.index.gigaBase'
                ]}"
                id="projectOverviewTable" />
        </div>

        <h2 class="data-table-header"><g:message code="projectOverview.table.seqType.title"/></h2>
        <div class="otpDataTables" data-csv-title="sequencing_technologies">
            <otp:dataTable
                codes="${[
                        'projectOverview.seqType.seqName',
                        'projectOverview.seqType.libraryLayout',
                        'projectOverview.seqType.singleCell',
                        'projectOverview.seqType.individualCount',
                        'projectOverview.seqType.sampleCount',
                        'projectOverview.seqType.gb'
                ]}"
                id="patientsAndSamplesGBCountPerProject" />
        </div>
        <br>
        <div style="display: inline-flex;">
            <div style="width: 300px;">
                <canvas id="sampleCountPerSequenceTypePie">[No canvas support]</canvas>
            </div>
            <div style="width: 550px;">
                <canvas id="laneCountPerDate">[No canvas support]</canvas>
            </div>
        </div>
    </div>
</body>
</html>
