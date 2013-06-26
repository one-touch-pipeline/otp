<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title>OTP - project overview</title>
<r:require module="core" />
<r:require module="jqueryDatatables" />
<r:require module="graph" />
</head>
<body>
    <div class="body">
        <div class="overviewMenu">
            <div class="projectLabel">project: </div>
            <div class="projectSelect">
                <g:select id="project_select" name='project_select'
                    from='${projects}' value='${project}'></g:select>
            </div>
        </div>
         <h3 class="statisticTitle">
            <g:message code="projectOverview.pageTitle" />
        </h3>
        <p class="statisticTitle">
            <b>Number of Individuals: </b> <span id="patient-count"></span>
        </p>
        <div class="table">
            <div style="width: 20px; height: 20px;"></div>
            <h3 class="statisticTableTitle">
                <g:message code="projectOverview.table.sampletype.title" />
            </h3>
            <div class="dataTableContainer sampleTypeNameCountBySample"
                style="margin-right: 1%;">
                <otp:dataTable
                    codes="${[
                        'projectOverview.table.sampleTypeName',
                        'projectOverview.table.sampleCount',
                    ]}"
                    id="sampleTypeNameCountBySample" />
            </div>
            <div style="width: 20px; height: 50px;"></div>
            <h3 class="statisticTableTitle" >
                <g:message code="projectOverview.title.centersOverviewTable" />
            </h3>
           <div class="dataTableContainer centerNameRunId"
                style="margin-right: 1%;">
                <otp:dataTable
                    codes="${[
                        'overview.statistic.center.name',
                        'overview.statistic.center.totalCount',
                        'overview.statistic.center.newCount',
                    ] }"
                    id="centerNameRunId" />
            </div>
            <div style="width: 20px; height: 50px;"></div>
            <h3 class="statisticTableTitle">
                <g:message code="projectOverview.table.statistic.title" /></h3>
            <div class="dataTableContainer projectOverviewTable"
                style="margin-right: 1%;">
                <otp:dataTable
                    codes="${[
                        'projectOverview.index.PID',
                        'projectOverview.index.sampleType',
                        'projectOverview.index.sequenceTypeName',
                        'projectOverview.index.sequenceTypeLibraryLayout',
                        'projectOverview.index.centerName',
                        'projectOverview.index.platformId',
                        'projectOverview.index.laneCount',
                        'projectOverview.index.gigaBase'
                    ] }"
                    id="projectOverviewTable" />
            </div>
            <div style="width: 20px; height: 50px;"></div>
            <h3 class="statisticTableTitle">
                <g:message code="projectOverview.table.seqtype.title" />
            </h3>
            <div
                class="dataTableContainer patientsAndSamplesGBCountPerProject"
                style="margin-right: 1%;">
                <otp:dataTable
                    codes="${[
                        'projectOverview.seqtype.seqName',
                        'projectOverview.seqtype.labreryLayout',
                        'projectOverview.seqtype.individualCount',
                        'projectOverview.seqtype.sampleCount',
                        'projectOverview.seqtype.gb'
                    ]}"
                    id="patientsAndSamplesGBCountPerProject" />
            </div>
        </div>
        <div class="homeGraph">
            <div style="margin-top: 80px"></div>
            <div style="float: left;">
                <canvas id="sampleTypeCountBySeqType" width="660"
                    height="380">[No canvas support]</canvas>
            </div>
            <div style="float: right;">
                <canvas id="laneCountPerDateByProject" width="500"
                    height="440">[No canvas support]</canvas>
            </div>
            <div style="margin-top: 80px"></div>
            <div style="text-align: center;">
                <canvas id="sampleTypeCountByPatient" width="550">[No canvas support]</canvas>
            </div>
        </div>
    </div>

    <r:script>
        $(function() {
            $.otp.projectOverviewTable.register();
            $.otp.graph.project.init();
        });
    </r:script>
</body>
</html>