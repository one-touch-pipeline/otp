<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="seqTrack.show.title"/></title>
</head>
<body>
  <div class="body">

    <ul>
        <g:if test="${next}">
            <li class="button"><g:link action="show" id="${next.id}"><g:message code="seqTrack.show.nextSeqTrack"/></g:link></li>
        </g:if>
        <g:if test="${previous}">
            <li class="button"><g:link action="show" id="${previous.id}"><g:message code="seqTrack.show.previousSeqTrack"/></g:link></li>
        </g:if>
    </ul>

    <h1><g:message code="seqTrack.show.title"/></h1>
    <div class="tableBlock">
    <input type="hidden" name="seqTrackId" value="${seqTrack.id}"/>
    <h2><g:message code="seqTrack.show.general"/></h2>
    <table>
        <otp:seqTrackMainPart seqTrack="${seqTrack}"/>
       <tr>
            <td class="myKey"><g:message code="seqTrack.show.details.hasOriginalBam"/></td>
            <g:if test="${seqTrack.hasOriginalBam}">
                <td class="myValue true">
            </g:if>
            <g:else>
                <td class="myValue false">
            </g:else>
            </td>
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
            <td class="myValue">${seqTrack.pipelineVersion.programName}</td>
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
                <td><g:message code="seqTrack.show.jobExecutionPlan.finishedSuccessful"/></td>
            </tr>
        </thead>
        <tbody>
        <g:each var="jobExecutionPlan" in="${jobExecutionPlans}">
            <tr>
                <td>${jobExecutionPlan.name}</td>
                <td>${jobExecutionPlan.planVersion}</td>
                <g:if test="${jobExecutionPlan.obsoleted}">
                    <td class="myValue true">
                </g:if>
                <g:else>
                    <td class="myValue false">
                </g:else>
                </td>
                <g:if test="${jobExecutionPlan.enabled}">
                    <td class="myValue true">
                </g:if>
                <g:else>
                    <td class="myValue false">
                </g:else>
                </td>
                <td>${jobExecutionPlan.finishedSuccessful}</td>
            </tr>
        </g:each>
        </tbody>
    </table>
    </g:if>
    </div>
  </div>
</body>
</html>
