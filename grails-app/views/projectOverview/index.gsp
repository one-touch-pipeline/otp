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
        <h3 class="statisticTitle">
            <g:message code="projectOverview.pageTitle" />
        </h3>
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
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
    <asset:script>
        $(function() {
            $.otp.projectOverviewTable.register();
            $.otp.graph.project.init();
        });
    </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
