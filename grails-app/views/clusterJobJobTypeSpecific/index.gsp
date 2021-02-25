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
    <title><g:message code="jobstats.jobTypeSpecific.title"/></title>
    <asset:javascript src="modules/rGraph.js"/>
    <asset:javascript src="pages/clusterJobJobTypeSpecific/index/clusterJobJobTypeSpecific.js"/>
</head>
<body>
    <div class="body">
        <h1><g:message code="jobstats.jobTypeSpecific.title"/></h1>

        <div class="rounded-page-header-box">
            <div>
                <span>Job Class: <g:select class="use-select-2" name="jobClassSelect" id="jobClassSelect" from="${jobClasses}"/></span>
                <span>Seq. Type: <select name="seqTypeSelect" id="seqTypeSelect"></select></span>
            </div>
            <br/>
            <div>
                <span>From: <input type="text" class="datePicker" id="dpFrom" value="${beginDate}"></span>
                <span>To: <input type="text" class="datePicker" id="dpTo" value="${latestDate}"></span>
            </div>
            <br/>

            <div>
                <span>Referencesize in Gigabases:
                    <select list="basesList" id="basesInput" value="3.2348">
                        <option value="3.2348" selected="selected">3.2348 (WGS)</option>
                        <option value="0.0350">0.0350 (WES)</option>
                    </select>
                </span>
                <span>Coverage:
                    <select list="coverageList" id="coverageInput" value="30">
                        <option value="15">15x</option>
                        <option value="30" selected="selected">30x</option>
                        <option value="60">60x</option>
                        <option value="90">90x</option>
                        <option value="100">100x</option>
                    </select>
                </span>
            </div>
        </div>
        <div id="jobTypeSpecificAverageValues">
            <table>
                <tr>
                    <td>
                        <div class="jobTypSpecificValue">Average Delay</div>
                        <div class="toolTipContainer  jobTypSpecificValueToolTip">
                            <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.averageTable.toolTip.averageDelay"/></span>
                        </div>
                    </td>
                    <td id="jobTypeSpecificAvgDelay">0</td>
                </tr>
                <tr>
                    <td>
                        <div class="jobTypSpecificValue"><g:message code="jobstats.jobTypeSpecific.averageTable.averageProcessingTime"/></div>
                        <div class="toolTipContainer  jobTypSpecificValueToolTip">
                            <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.averageTable.toolTip.averageProcessingTime"/></span>
                        </div>
                    </td>
                    <td id="jobTypeSpecificAvgProcessing">0</td>
                </tr>
                <tr>
                    <td>
                        <div class="jobTypSpecificValue"><g:message code="jobstats.jobTypeSpecific.averageTable.averageCpuUsedInCores"/></div>
                        <div class="toolTipContainer  jobTypSpecificValueToolTip">
                            <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.averageTable.toolTip.averageCpuUsed"/></span>
                        </div>
                    </td>
                    <td id="jobTypeSpecificAvgCPU"></td>
                </tr>
                <tr>
                    <td>
                        <div class="jobTypSpecificValue"><g:message code="jobstats.jobTypeSpecific.averageTable.averageMemoryUsedInKiB"/></div>
                        <div class="toolTipContainer jobTypSpecificValueToolTip">
                            <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.averageTable.toolTip.averageMemoryUsed"/></span>
                        </div>
                    </td>
                    <td id="jobTypeSpecificAvgMemory"></td>
                </tr>
                <tr>
                    <td>
                        <div class="jobTypSpecificValue"><g:message code="jobstats.jobTypeSpecific.averageTable.coverageStatistics"/></div>
                        <div class="toolTipContainer jobTypSpecificValueToolTip">
                            <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.averageTable.toolTip.coverageStatistics"/></span>
                        </div>
                    </td>
                    <td id="jobTypeSpecificMinCov"></td>
                </tr>
                <tr>
                    <td></td>
                    <td id="jobTypeSpecificMaxCov"></td>
                </tr>
                <tr>
                    <td></td>
                    <td id="jobTypeSpecificAvgCov"></td>
                </tr>
                <tr>
                    <td></td>
                    <td id="jobTypeSpecificMedianCov"></td>
                </tr>

            </table>
        </div>
        <div id="pieGraphicContainer">
            <div class="pieGraphContainer">
                <div class="graphDescription"><g:message code="jobstats.jobTypeSpecific.graphs.exitCodes"/></div>
                <div class="toolTipContainer">
                   <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.graphs.toolTip.exitCodes"/></span>
                </div>
                <canvas id="jobTypeSpecificGraphExitCode" class="pieChart" width=450px height=225px></canvas>
            </div>
            <div class="pieGraphContainer" id="pieGraphContainerSecond">
                <div class="graphDescription"><g:message code="jobstats.jobTypeSpecific.graphs.exitStatuses"/></div>
                <div class="toolTipContainer">
                   <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.graphs.toolTip.exitStatuses"/></span>
                </div>
                <canvas id="jobTypeSpecificGraphExitStatus" class="pieChart" width=450px height=225px></canvas>
            </div>
        </div>
        <div id="lineGraphicContainer">
            <div class="lineGraphContainer">
                <div class="graphDescription"><g:message code="jobstats.jobTypeSpecific.graphs.queuedStartedEnded"/></div>
                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.graphs.toolTip.queuedStartedEnded"/></span>
                </div>
                <br>
                <canvas id="jobTypeSpecificGraphStates" class="lineChart" width=1000px height=300px></canvas>
            </div>
        </div>
        <div id="scatterGraphicContainer">
            <div class="scatterGraphContainer">
                <div class="graphDescription"><g:message code="jobstats.jobTypeSpecific.graphs.wallTimes"/></div>
                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.graphs.toolTip.wallTimes"/></span>
                </div>
                <br>
                <canvas id="jobTypeSpecificGraphWalltimes" class="scatterChart" width=1000px height=500px></canvas>
            </div>
        </div>
        <br>
    </div>
    <asset:script>
        $(function() {
            $.otp.clusterJobJobTypeSpecific.register();
            $.otp.clusterJobJobTypeSpecific.updateSeqTypeSelect();
        });
    </asset:script>
</body>
</html>
