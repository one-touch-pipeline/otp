<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title>OTP - project overview</title>
<r:require module="graph" />
</head>
<body>
    <div class="body">
        <h3 class="statisticTitle">
            <g:message code="projectOverview.pageTitle" />
        </h3>
        <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="home.projectfilter"/> :</span>
            <g:select class="criteria" id="project_select" name='project_select'
                from='${projects}' value='${project}'></g:select>
        </form>
        <h4 class="statisticTitle">
            <g:message code="projectOverview.numberOfPatient"/>: <span id="patient-count"></span>
        </h4>
        <div class="table">
            <div style="width: 20px; height: 20px;"></div>
            <h3 class="statisticTableTitle">
                <g:message code="projectOverview.table.sampletype.title" />
            </h3>
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                        'projectOverview.table.sampleTypeName',
                        'projectOverview.table.sampleCount',
                    ]}"
                    id="sampleTypeNameCountBySample" />
            </div>
            <div style="width: 20px; height: 20px;"></div>
            <h3 class="statisticTableTitle" >
                <g:message code="projectOverview.title.centersOverviewTable" />
            </h3>
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
            <h3 class="statisticTableTitle">
                <g:message code="projectOverview.table.statistic.title" /></h3>
            <div class="otpDataTables">
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
            <div style="width: 20px; height: 40px;"></div>
            <h3 class="statisticTableTitle">
                <g:message code="projectOverview.table.seqtype.title" />
            </h3>
            <div class="otpDataTables">
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
            <div style="width: 20px; height: 20px;"></div>
            <h3 class="statisticTableTitle">
                <g:message code="projectOverview.table.ReferenceGenome.title" />
            </h3>
            <div class="otpDataTables">
	            <otp:dataTable
	                codes="${[
	                    'projectOverview.index.referenceGenome.sequenceTypeName',
	                    'projectOverview.index.referenceGenome.sampleTypeName',
	                    'projectOverview.index.referenceGenome',
	                ] }"
	                id="listReferenceGenome" />
            </div>
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
    </div>
    <r:script>
        $(function() {
            $.otp.projectOverviewTable.register();
            $.otp.graph.project.init();
        });
    </r:script>
</body>
</html>
