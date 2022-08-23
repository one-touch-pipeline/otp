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
    <title><g:message code="seqTrack.show.title"/></title>
</head>

<body>
<div class="body">
    <h1><g:message code="seqTrack.show.title"/></h1>
    <g:if test="${seqTrack.project.archived}">
        <otp:annotation type="warning">
            <g:message code="configurePipeline.info.projectArchived.noChange" args="[seqTrack.project.name]"/>
        </otp:annotation>
    </g:if>

    <div class="tableBlock">
        <input type="hidden" name="seqTrackId" value="${seqTrack.id}"/>

        <h2><g:message code="seqTrack.show.general"/></h2>
        <table class="key-value-table key-input">
            <tr>
                <td><g:message code="seqTrack.show.details.laneId"/></td>
                <td>${seqTrack.laneId}</td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.run"/></td>
                <td>
                    <sec:access expression="hasRole('ROLE_OPERATOR')">
                        <g:link controller="run" action="show" id="${seqTrack.run.id}">${seqTrack.run.name}</g:link>
                    </sec:access>
                    <sec:noAccess expression="hasRole('ROLE_OPERATOR')">
                        ${seqTrack.run.name}
                    </sec:noAccess>
                </td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.sampleType"/></td>
                <td>${seqTrack.sample.individual.mockPid} ${seqTrack.sample.sampleType.name}</td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.seqType"/></td>
                <td>${seqTrack.seqType.name}</td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.details.hasOriginalBam"/></td>
                <td><span class="icon-${seqTrack.hasOriginalBam}"></span></td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.details.nBasePairs"/></td>
                <td>${seqTrack.nBasePairs}</td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.details.nReads"/></td>
                <td>
                    <g:if test="${seqTrack.NReads}">
                        ${seqTrack.NReads}
                    </g:if>
                </td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.details.insertSize"/></td>
                <td>${seqTrack.insertSize}</td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.details.qualityEncoding"/></td>
                <td>${seqTrack.qualityEncoding}</td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.details.alignmentState"/></td>
                <td>${seqTrack.alignmentState}</td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.details.fastqcState"/></td>
                <td>${seqTrack.fastqcState}</td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.seqPlatform"/></td>
                <td>${seqTrack.seqPlatform.name}</td>
            </tr>
            <tr>
                <td><g:message code="seqTrack.show.pipelineVersion"/></td>
                <td>${seqTrack.pipelineVersion.displayName}</td>
            </tr>
        </table>
        <g:if test="${jobExecutionPlans}">
            <h2><g:message code="seqTrack.show.jobExecutionPlan"/></h2>
            <table>
                <thead>
                <tr>
                    <td><g:message code="seqTrack.show.jobExecutionPlan.name"/></td>
                    <td><g:message code="seqTrack.show.jobExecutionPlan.planVersion"/></td>
                    <td><g:message code="seqTrack.show.jobExecutionPlan.obsoleted"/></td>
                    <td><g:message code="seqTrack.show.jobExecutionPlan.enabled"/></td>
                </tr>
                </thead>
                <tbody>
                <g:each var="jobExecutionPlan" in="${jobExecutionPlans}">
                    <tr>
                        <td>${jobExecutionPlan.name}</td>
                        <td>${jobExecutionPlan.planVersion}</td>
                        <td><span class="icon-${jobExecutionPlan.obsoleted}"></span></td>
                        <td><span class="icon-${jobExecutionPlan.enabled}"></span></td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </g:if>
    </div>
</div>
</body>
</html>
