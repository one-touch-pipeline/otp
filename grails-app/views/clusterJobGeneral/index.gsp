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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="jobstats.general.title"/></title>
    <asset:javascript src="modules/rGraph.js"/>
    <asset:javascript src="modules/multiprogressbar.js"/>
    <asset:stylesheet src="modules/multiprogressbar.css"/>
    <asset:javascript src="pages/clusterJobGeneral/index/clusterJobGeneral.js"/>
</head>
<body>
    <div class="body">
        <h1><g:message code="jobstats.general.title"></g:message></h1>
        <div class="rounded-page-header-box">
            <span>from: <input type="text" class="datePicker" id="dpFrom" value="${latestDate}"></span>
            <span>to: <input type="text" class="datePicker" id="dpTo" value="${latestDate}"></span>
        </div>
        <div id="clusterJobGeneralTableContainer">
            <div class="otpDataTables">
                 <otp:dataTable codes="${tableHeader}" id="clusterJobGeneralTable" />
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
