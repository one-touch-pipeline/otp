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
    <title><g:message code="jobstats.general.title"/></title>
    <asset:javascript src="pages/clusterJobGeneral/index/clusterJobGeneral.js"/>
</head>

<body>
<div class="body container-fluid">
    <div class="row">
        <div class="col-12">
            <h1><g:message code="jobstats.general.title"/></h1>

            <div class="rounded-page-header-box">
                <span><label for="dpFrom">from:</label> <input type="date" class="datePicker" max="${latestDate}" id="dpFrom" value="${beginDate}"
                                                               required="required"></span>
                <span><label for="dpTo">to:</label> <input type="date" class="datePicker" max="${latestDate}" id="dpTo" value="${latestDate}"
                                                           required="required"></span>
            </div>

            <div id="clusterJobGeneralTableContainer">
                <div>
                    <otp:dataTable codes="${tableHeader}" id="clusterJobGeneralTable" classes="table table-sm table-striped table-hover table-bordered"/>
                </div>
            </div>
        </div>
    </div>

    <div class="row pieChartRowContainer">
        <div class="col-4 d-flex justify-content-center">
            <div class="pieGraphContainer graphContainer">
                <div class="graphDescription">Delay</div>

                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.progressBar.QueueProcess"/></span>
                </div>

                <div class="pieChartBox">
                    <canvas id="delayPieChart"></canvas>
                </div>
            </div>
        </div>

        <div class="col-4 d-flex justify-content-center">
            <div class="pieGraphContainer graphContainer">
                <div class="graphDescription">exit codes</div>

                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.exitCodes"/></span>
                </div>

                <div class="pieChartBox">
                    <canvas id="generalGraphExitCode" class="pieChart" width=450px height=225px></canvas>
                </div>
            </div>
        </div>

        <div class="col-4 d-flex justify-content-center">
            <div class="pieGraphContainer graphContainer">
                <div class="graphDescription">exit statuses</div>

                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.exitStatuses"/></span>
                </div>
                <div class="pieChartBox">
                    <canvas id="generalGraphExitStatus" class="pieChart" width=450px height=225px></canvas>
                </div>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-12 d-flex justify-content-center">
            <div class="lineGraphContainer">
                <div class="graphDescription">queued/started/ended (in jobs)</div>

                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.queuedStartedEnded"/></span>
                </div>
                <br>
                <canvas id="generalGraphStates" class="lineChart" width=1000px height=300px></canvas>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-12 d-flex justify-content-center">
            <div class="lineGraphContainer">
                <div class="graphDescription">failed jobs (in jobs)</div>

                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.failed"/></span>
                </div>
                <br>
                <canvas id="generalGraphFailed" class="lineChart" width=1000px height=300px></canvas>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-12 d-flex justify-content-center">
            <div class="lineGraphContainer">
                <div class="graphDescription">average core usage (in cores)</div>

                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.coreUsage"/></span>
                </div>
                <br>
                <canvas id="generalGraphCores" class="lineChart" width=1000px height=300px></canvas>
            </div>
        </div>
    </div>

    <div class="row">
        <div class="col-12 d-flex justify-content-center">
            <div class="lineGraphContainer">
                <div class="graphDescription">average memory usage (in GiB)</div>

                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.general.graphs.toolTip.memoryUsage"/></span>
                </div>
                <br>
                <canvas id="generalGraphMemory" class="lineChart" width=1000px height=300px></canvas>
            </div>
        </div>
    </div>

</div>
</body>
</html>
