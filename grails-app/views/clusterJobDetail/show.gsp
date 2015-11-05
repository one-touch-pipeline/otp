<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="jobstats.detail.title"/></title>
    <asset:javascript src="modules/multiprogressbar"/>
    <asset:stylesheet src="modules/multiprogressbar"/>
    <asset:javascript src="pages/clusterJobDetail/show/clusterJobDetail.js"/>
</head>
<!--
This page is just available for cluster jobs with filled properties
Cluster Jobs without filled properties have no link to this page in the datatable
on the general cluster job page
-->
<body>
    <div class="body">
        <h1><g:message code="jobstats.jobSpecific.detail.title"/></h1>
        <br>
        <div class="detailContainer" id="general">
            <div class="detailContainerTitle"><g:message code="jobstats.jobSpecific.detail.table.general"/></div>
            <div class="toolTipContainer">
                <span class="toolTip"><g:message code="jobstats.jobSpecific.detail.table.toolTip.general"/></span>
            </div>
            <br>
            <table>
                <colgroup>
                    <col class="columnColors">
                    <col class="columnSpace">
                    <col class="columnProperty">
                    <col class="columnValue">
                </colgroup>
                <tr>
                    <!-- these empty td-elements are used to define the colored element and the space on the left of each table -->
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.id"/></td>
                    <td>${job.clusterJobId}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.cluster"/></td>
                    <td>${job.cluster}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.jobClass"/></td>
                    <td>${job.jobClass}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.seqType"/></td>
                    <td>
                        <g:if test="${job.seqType != null}">
                            ${job.seqType.toString()}
                        </g:if>
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.project"/></td>
                    <td>
                        <g:if test="${individual != null}">
                            <g:link controller="projectOverview" action="index" params="[projectName: individual.project]">${individual.project}</g:link>
                        </g:if>
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.individual"/></td>
                    <td>
                        <g:if test="${individual != null}">
                            <g:link controller="individual" action="show" params="[mockPid: individual.mockPid]">${individual.mockPid}</g:link>
                        </g:if>
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.name"/></td>
                    <td>${job.clusterJobName.replace("_", "_<br>")}</td>
                </tr>
            </table>
        </div>
        <div class="detailContainer" id="status">
            <div class="detailContainerTitle"><g:message code="jobstats.jobSpecific.detail.table.state"/></div>
            <div class="toolTipContainer">
                <span class="toolTip"><g:message code="jobstats.jobSpecific.detail.table.toolTip.state"/></span>
            </div>
            <br>
            <table>
                <col class="columnColors">
                <col class="columnSpace">
                <col class="columnProperty">
                <col class="columnValue">
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.status"/></td>
                    <td>${job.exitStatus}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.code"/></td>
                    <td>${job.exitCode}</td>
                </tr>
            </table>
        </div>
        <div class="detailContainer" id="time">
            <div class="detailContainerTitle"><g:message code="jobstats.jobSpecific.detail.table.time"/></div>
            <div class="toolTipContainer">
                <span class="toolTip"><g:message code="jobstats.jobSpecific.detail.table.toolTip.time"/></span>
            </div>
            <br>
            <g:set var="formatDateString" value="${"yyyy-MM-dd HH:mm:ss"}"/>
            <table>
                <col class="columnColors">
                <col class="columnSpace">
                <col class="columnProperty">
                <col class="columnValue">
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.queued"/></td>
                    <td>${job.queued.toString(formatDateString)}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.started"/></td>
                    <td>${job.started?.toString(formatDateString)}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.ended"/></td>
                    <td>${job.ended?.toString(formatDateString)}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.requestedWalltime"/></td>
                    <td>${job.getRequestedWalltimeAsISO()}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.elapsedWalltime"/></td>
                    <td>${job.getElapsedWalltimeAsISO()}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.walltimeDifference"/></td>
                    <td>${job.getWalltimeDiffAsISO()}</td>
                </tr>
            </table>
        </div>

        <div id="jobTypeSpecificProgressContainer">
            <div class="graphDescription">delay</div>
            <div class="toolTipContainer">
               <span class="toolTip"><g:message code="jobstats.detail.graphs.toolTip.progressBar.QueueProcess"/></span>
            </div>
            <div class="progressLegend">
                <div class="progressLegendItem" id="progressLegendItemQueue"></div><div class="progressLegendText">Queue</div>
                <div class="progressLegendItem" id="progressLegendItemProcess"></div><div class="progressLegendText">Process</div>
            </div>
            <div id="jobTypeSpecificGraphProgress" class="multiProgress"></div>
        </div>

        <div class="detailContainer" id="cpu">
            <div class="detailContainerTitle"><g:message code="jobstats.jobSpecific.detail.table.cpu"/></div>
            <div class="toolTipContainer">
                <span class="toolTip"><g:message code="jobstats.jobSpecific.detail.table.toolTip.cpu"/></span>
            </div>
            <br>
            <table>
                <col class="columnColors">
                <col class="columnSpace">
                <col class="columnProperty">
                <col class="columnValue">
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.requestedCores"/></td>
                    <td>${job.requestedCores}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.cpuTime"/></td>
                    <td>${job.getCpuTimeAsISO()}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.cpuAvgUtilised"/></td>
                    <td>${job.cpuAvgUtilised.round(2)}</td>
                </tr>
            </table>
        </div>
        <div class="detailContainer" id="memory">
            <div class="detailContainerTitle"><g:message code="jobstats.jobSpecific.detail.table.memory"/></div>
            <div class="toolTipContainer">
                <span class="toolTip"><g:message code="jobstats.jobSpecific.detail.table.toolTip.memory"/></span>
            </div>
            <br>
            <table>
                <col class="columnColors">
                <col class="columnSpace">
                <col class="columnProperty">
                <col class="columnValue">
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.requestedMemory"/></td>
                    <td>${job.requestedMemory}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.usedMemory"/></td>
                    <td>${job.usedMemory}</td>
                </tr>
                <tr>
                    <td></td>
                    <td></td>
                    <td><g:message code="jobstats.jobSpecific.detail.table.attribute.memoryEfficiency"/></td>
                    <td>${job.memoryEfficiency.round(2)}</td>
                </tr>
            </table>
        </div>
    </div>
</body>
<asset:script>
    $(function() {
        $.otp.clusterJobDetailProgress.register(${job.id});
    });
</asset:script>
</html>
