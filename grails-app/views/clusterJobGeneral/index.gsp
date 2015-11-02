<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="jobstats.general.title"/></title>
    <asset:javascript src="modules/graph"/>
    <asset:javascript src="modules/multiprogressbar"/>
    <asset:stylesheet src="modules/multiprogressbar"/>
    <asset:javascript src="pages/clusterJobGeneral/index/clusterJobGeneral.js"/>
</head>
<body>
    <div class="body">
        <h1><g:message code="jobstats.general.title"></g:message></h1><br><br>
        <div id="optionsContainer">
            <p>from: <input type="text" class="datePicker" id="dpFrom" value="${latestDate}"></p>
            <p>to: <input type="text" class="datePicker" id="dpTo" value="${latestDate}"></p>
        </div><br><br><br>
        <div id="clusterJobGeneralTableContainer">
            <div class="otpDataTables">
                 <otp:dataTable
                     codes="${[
                         'jobstats.general.table.id',
                         'jobstats.general.table.name',
                         'jobstats.general.table.status',
                         'jobstats.general.table.queued',
                         'jobstats.general.table.started',
                         'jobstats.general.table.ended'
                     ]}"
                     id="clusterJobGeneralTable" />
             </div>
        </div>
        <br>
        <div class="progressBarContainer" id="progressBarContainer_generalGraphQueuedStartedEndedProgress">
            <div class="graphDescription">delay</div>
            <div class="toolTipContainer">
               <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.progressBar.QueueProcess"/></span>
            </div>
            <div class="progressLegend">
                <div class="progressLegendItem" id="progressLegendItemQueue"></div><div class="progressLegendText">Queue</div>
                <div class="progressLegendItem" id="progressLegendItemProcess"></div><div class="progressLegendText">Process</div>
            </div>
            <div id="generalGraphQueuedStartedEndedProgress" class="multiProgress"></div>
        </div>
        <br>
        <div class="pieGraphContainer">
            <div class="graphDescription">exit codes</div>
            <div class="toolTipContainer">
               <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.exitCodes"/></span>
            </div>
            <canvas id="generalGraphExitCode" class="pieChart" width=450px height=225px></canvas>
        </div>
        <div class="pieGraphContainer" id="pieGraphContainerSecond">
            <div class="graphDescription">exit statuses</div>
            <div class="toolTipContainer">
               <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.exitStatuses"/></span>
            </div>
            <canvas id="generalGraphExitStatus" class="pieChart" width=450px height=225px></canvas>
        </div>
        <br>
        <div class="lineGraphContainer">
           <div class="graphDescription">queued/started/ended (in jobs)</div>
           <div class="toolTipContainer">
               <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.queuedStartedEnded"/></span>
           </div>
           <br>
           <canvas id="generalGraphStates" class="lineChart" width=1000px height=300px></canvas>
        </div>
        <br>
        <div class="lineGraphContainer">
           <div class="graphDescription">failed jobs (in jobs)</div>
           <div class="toolTipContainer">
               <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.failed"/></span>
           </div>
           <br>
           <canvas id="generalGraphFailed" class="lineChart" width=1000px height=300px></canvas>
        </div>
        <br>
        <div class="lineGraphContainer">
           <div class="graphDescription">average core usage (in cores)</div>
           <div class="toolTipContainer">
               <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.coreUsage"/></span>
           </div>
           <br>
           <canvas id="generalGraphCores" class="lineChart" width=1000px height=300px></canvas>
        </div>
        <br>
        <div class="lineGraphContainer">
           <div class="graphDescription">average memory usage (in GiB)</div>
           <div class="toolTipContainer">
               <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.memoryUsage"/></span>
           </div>
           <br>
           <canvas id="generalGraphMemory" class="lineChart" width=1000px height=300px></canvas>
        </div>
    </div>
    <asset:script>
        $(function() {
            $.otp.clusterJobGeneral.register();
        });
    </asset:script>
</body>
</html>
