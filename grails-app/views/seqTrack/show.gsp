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
    <div class="tableBlock">
    <input type="hidden" name="seqTrackId" value="${seqTrack.id}"/>
    <h2><g:message code="seqTrack.show.general"/></h2>
    <table>
        <otp:seqTrackMainPart seqTrack="${seqTrack}"/>
       <tr>
            <td class="myKey"><g:message code="seqTrack.show.details.hasOriginalBam"/></td>
            <td class="myValue ${seqTrack.hasOriginalBam}"></td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="seqTrack.show.details.nBasePairs"/></td>
            <td class="myValue">${seqTrack.nBasePairs}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="seqTrack.show.details.nReads"/></td>
            <td class="myValue">
                <g:if test="${seqTrack.getNReads()}">
                    ${seqTrack.getNReads()}
                </g:if>
            </td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="seqTrack.show.details.insertSize"/></td>
            <td class="myValue">${seqTrack.insertSize}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="seqTrack.show.details.qualityEncoding"/></td>
            <td class="myValue">${seqTrack.qualityEncoding}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="seqTrack.show.details.alignmentState"/></td>
            <td class="myValue">${seqTrack.alignmentState}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="seqTrack.show.details.fastqcState"/></td>
            <td class="myValue">${seqTrack.fastqcState}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="seqTrack.show.seqPlatform"/></td>
            <td class="myValue">${seqTrack.seqPlatform.name}</td>
       </tr>
       <tr>
            <td class="myKey"><g:message code="seqTrack.show.pipelineVersion"/></td>
            <td class="myValue">${seqTrack.pipelineVersion.getDisplayName()}</td>
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
                <td class="myValue ${jobExecutionPlan.obsoleted}"></td>
                <td class="myValue ${jobExecutionPlan.enabled}"></td>
            </tr>
        </g:each>
        </tbody>
    </table>
    </g:if>
    </div>
  </div>
</body>
</html>
