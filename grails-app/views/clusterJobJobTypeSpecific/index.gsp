<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="jobstats.jobTypeSpecific.title"/></title>
    <asset:javascript src="modules/graph"/>
    <asset:javascript src="modules/jqueryUI"/>
    <asset:stylesheet src="modules/jqueryUI"/>
    <asset:javascript src="pages/clusterJobJobTypeSpecific/index/clusterJobJobTypeSpecific.js"/>
</head>
<body>
    <div class="body">
        <h1><g:message code="jobstats.jobTypeSpecific.title"/></h1><br><br>
        <div class="optionsContainer">
            <p>jobclass: <g:select name="jobClassSelect" id="jobClassSelect" from="${jobClasses}"/></p>
            <p>seqType: <select name="seqTypeSelect" id="seqTypeSelect"></select></p>
            <p>from: <input type="text" class="datePicker" id="dpFrom" value="${latestDate}"></p>
            <p>to: <input type="text" class="datePicker" id="dpTo" value="${latestDate}"></p>
        </div>
        <div class="optionsContainer">
            <p>Referencesize in Gigabases:
                <input list="basesList" id="basesInput" value="3.2348">
                <datalist id="basesList">
                    <option value="3.2348" selected="selected">WHOLE GENOME</option>
                    <option value="0.0350">WHOLE EXOME</option>
                </datalist></p>
            <p>coverage:
                <input list="coverageList" id="coverageInput" value="30">
                <datalist id="coverageList">
                    <option value="15">15x</option>
                    <option value="30" selected="selected">30x</option>
                    <option value="60">60x</option>
                    <option value="90">90x</option>
                    <option value="100">100x</option>
                </datalist>
            </p>
        </div>
        <div id="jobTypeSpecificAverageValues">
            <table>
                <col class="columnProperty">
                <col class="columnValue">
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
                <div class="graphDescription"><g:message code="jobstats.jobTypeSpecific.graphs.walltimes"/></div>
                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.graphs.toolTip.walltimes"/></span>
                </div>
                <br>
                <canvas id="jobTypeSpecificGraphWalltimes" class="scatterChart" width=1000px height=500px></canvas>
            </div>
            <div class="scatterGraphContainer">
                <div class="graphDescription"><g:message code="jobstats.jobTypeSpecific.graphs.memoryUsage"/></div>
                <div class="toolTipContainer">
                    <span class="toolTip"><g:message code="jobstats.jobTypeSpecific.graphs.toolTip.memoryUsage"/></span>
                </div>
                <br>
                <canvas id="jobTypeSpecificGraphMemories" class="scatterChart" width=1000px height=500px></canvas>
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
