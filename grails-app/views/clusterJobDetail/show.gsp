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
    <title><g:message code="jobstats.detail.title"/></title>
    <asset:javascript src="modules/multiprogressbar.js"/>
    <asset:stylesheet src="modules/multiprogressbar.css"/>
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
        <g:if test="${job}">
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
                        <td>${job.clusterJobId ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.jobClass"/></td>
                        <td>${job.jobClass ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.seqType"/></td>
                        <td>${job.seqType?.toString() ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.project"/></td>
                        <td>
                            <g:if test="${individual != null}">
                                <g:link controller="projectOverview" action="index" params="[(projectParameter): individual.project.name]">${individual.project}</g:link>
                            </g:if>
                        </td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.individual"/></td>
                        <td>
                            <g:if test="${individual != null}">
                                <g:link controller="individual" action="show" id="${individual.id}">${individual.mockPid}</g:link>
                            </g:if>
                        </td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.name"/></td>
                        <td>${job.clusterJobName ?: NA}</td>
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
                        <td>${job.exitStatus ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.code"/></td>
                        <td>${job.exitCode ?: NA}</td>
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
                        <td>${job.started?.toString(formatDateString) ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.ended"/></td>
                        <td>${job.ended?.toString(formatDateString) ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.requestedWallTime"/></td>
                        <td>${job.getRequestedWalltimeAsISO()}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.elapsedWallTime"/></td>
                        <td>${job.getElapsedWalltimeAsISO()}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.wallTimeDifference"/></td>
                        <td>${job.getWalltimeDiffAsISO()}</td>
                    </tr>
                </table>
            </div>

            <g:if test="${job.queued && job.started && job.ended}">
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
            </g:if>

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
                        <td>${job.requestedCores ?: NA}</td>
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
                        <td>${job.cpuAvgUtilised?.round(2) ?: NA}</td>
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
                        <td>${job.requestedMemory ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.usedMemory"/></td>
                        <td>${job.usedMemory ?: NA}</td>
                    </tr>
                    <tr>
                        <td></td>
                        <td></td>
                        <td><g:message code="jobstats.jobSpecific.detail.table.attribute.memoryEfficiency"/></td>
                        <td>${job.memoryEfficiency?.round(2) ?: NA}</td>
                    </tr>
                </table>
            </div>
            <asset:script type="text/javascript">
                $(function() {
                    $.otp.clusterJobDetailProgress.register(${job.id});
                });
            </asset:script>
        </g:if>
        <g:else>
            <g:message code="jobstats.jobSpecific.detail.unavailable"/>
        </g:else>
    </div>
</body>
</html>
