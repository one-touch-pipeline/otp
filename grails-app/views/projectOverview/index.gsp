%{--
  - Copyright 2011-2019 The OTP authors
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
    <asset:javascript src="modules/graph"/>
    <asset:javascript src="pages/projectOverview/index/datatable.js"/>
</head>
<body>
    <div class="body">
    <g:if test="${projects}">
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
        <h1 class="statisticTitle">
            <g:message code="projectOverview.pageTitle" args="[project.name]"/>
        </h1>
        <div class="statisticTitle">
            <g:message code="projectOverview.numberOfPatient"/>: <span id="patient-count"></span>
        </div>
        <div class="table">
            <div style="width: 20px; height: 20px;"></div>
            <h2 class="statisticTableTitle">
                <g:message code="projectOverview.table.sampleType.title" />
            </h2>
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                        'projectOverview.table.sampleTypeName',
                        'projectOverview.table.sampleCount',
                    ]}"
                    id="sampleTypeNameCountBySample" />
            </div>
            <div style="width: 20px; height: 20px;"></div>
            <h2 class="statisticTableTitle" >
                <g:message code="projectOverview.title.centersOverviewTable" />
            </h2>
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                        'overview.statistic.center.name',
                        'overview.statistic.center.totalCount',
                        'overview.statistic.center.newCount',
                    ] }"
                    id="centerNameRunId" />
            </div>
            <div style="width: 20px; height: 30px;"></div>
            <h2 class="statisticTableTitle">
                <g:message code="projectOverview.table.statistic.title" /></h2>
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                        'projectOverview.index.PID',
                        'projectOverview.index.sampleType',
                        'projectOverview.index.sequenceTypeName',
                        'projectOverview.index.sequenceTypeLibraryLayout',
                        'projectOverview.index.singleCell',
                        'projectOverview.index.centerName',
                        'projectOverview.index.platformId',
                        'projectOverview.index.laneCount',
                        'projectOverview.index.gigaBase'
                    ] }"
                    id="projectOverviewTable" />
            </div>
            <div style="width: 20px; height: 40px;"></div>
            <h2 class="statisticTableTitle">
                <g:message code="projectOverview.table.seqType.title" />
            </h2>
            <div class="otpDataTables">
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
            <div style="width: 20px; height: 20px;"></div>
        </div>
        <div class="homeGraph">
            <div style="margin-top: 60px"></div>
            <div style="float: left; border: 25px solid #E1F1FF;">
                <canvas id="sampleTypeCountBySeqType" width="530"
                    height="400">[No canvas support]</canvas>
            </div>
            <div style="float: right; border: 25px solid #E1F1FF;">
                <canvas id="laneCountPerDateByProject" width="530"
                    height="400">[No canvas support]</canvas>
            </div>
            <div style="margin-top: 80px"></div>
            <div style="text-align: center;">
                <canvas id="sampleTypeCountByPatient" width="550">[No canvas support]</canvas>
         </div>
        </div>
    <asset:script type="text/javascript">
        $(function() {
            $.otp.projectOverviewTable.register();
            $.otp.graph.project.init();
        });
    </asset:script>
    </g:if>
    <g:else>
        <g:render template="/templates/noProject"/>
    </g:else>
    </div>
</body>
</html>
