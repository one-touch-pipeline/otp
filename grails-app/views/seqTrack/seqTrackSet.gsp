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

<%@ page import="de.dkfz.tbi.otp.project.Project; de.dkfz.tbi.util.UnitHelper" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="seqTrack.seqTrackSet.title" args="[individual.pid]"/></title>
</head>

<body>
<div class="body">
    <g:set var="archived" value="${(individual.project.state == Project.State.ARCHIVED) ? 'archivedClickable' : ''}"/>
    <g:set var="deleted" value="${(individual.project.state == Project.State.DELETED) ? 'deletedClickable' : ''}"/>

    <h1><g:message code="seqTrack.seqTrackSet.header"/></h1>
    <g:render template="/templates/bootstrap/noChange" model="[project: individual.project]"/>


    <h2><g:message code="seqTrack.seqTrackSet.selectionCriteria.header"/></h2>
    <table class="seq-track-set-tables">
        <tbody>
        <tr>
            <td><g:message code="seqTrack.seqTrackSet.selectionCriteria.individual"/></td>
            <td><g:link controller="individual" action="show" id="${individual.id}">${individual.pid}</g:link></td>
        </tr>
        <tr>
            <td><g:message code="seqTrack.seqTrackSet.selectionCriteria.sampleType"/></td>
            <td>${sampleType}</td>
        </tr>
        <tr>
            <td><g:message code="seqTrack.seqTrackSet.selectionCriteria.seqType"/></td>
            <td>${seqType}</td>
        </tr>
        </tbody>
    </table>

    <h2><g:message code="seqTrack.seqTrackSet.setInfo.header"/></h2>
    <table class="seq-track-set-tables">
        <tbody>
        <tr>
            <td><g:message code="seqTrack.seqTrackSet.setInfo.seqPlatform"/></td>
            <td>${seqTrackSet.seqPlatforms.join(", ")}</td>
        </tr>
        <tr>
            <td><g:message code="seqTrack.seqTrackSet.setInfo.seqCenter"/></td>
            <td>${seqTrackSet.seqCenters.join(", ")}</td>
        </tr>

        <tr>
            <td><g:message code="seqTrack.seqTrackSet.setInfo.totalNumberOfLanes"/></td>
            <td>${seqTrackSet.numberOfLanes}</td>
        </tr>
        <tr>
            <td><g:message code="seqTrack.seqTrackSet.setInfo.totalNumberOfBases"/></td>
            <td>${UnitHelper.asNucleobases(seqTrackSet.numberOfBases, true)} (${UnitHelper.asNucleobases(seqTrackSet.numberOfBases)})</td>
        </tr>
        </tbody>
    </table>

    <h2><g:message code="seqTrack.seqTrackSet.lanesPerRun.header"/></h2>
    <otp:annotation type="info" style="white-space: pre"><g:message code="seqTrack.seqTrackSet.lanesPerRun.info"/></otp:annotation>
    <g:each var="entry" in="${lanesPerRun}">
        <g:set var="run" value="${entry.key}"/>
        <div class="color-left-border run slim">
            <div class="run-information">
                <div class="grid-element ${archived} ${deleted}"
                     title="Its project ${selectedProject} is ${individual.project.state.name().toLowerCase()}">
                    <strong>
                        <sec:access expression="hasRole('ROLE_OPERATOR')">
                            <g:link controller="run" action="show" id="${run.id}">${run.name}</g:link>
                        </sec:access>
                        <sec:noAccess expression="hasRole('ROLE_OPERATOR')">
                            ${run.name}
                        </sec:noAccess>
                    </strong>
                </div>

                <div class="grid-element"><strong><g:message code="seqTrack.seqTrackSet.lanesPerRun.seqCenter"/></strong> ${run.seqCenter}</div>

                <div class="grid-element"><strong><g:message code="seqTrack.seqTrackSet.lanesPerRun.seqPlatform"/></strong> ${run.seqPlatform}</div>
            </div>

            <div class="lane-and-datafile-grid-wrapper color-left-border non">
                <div class="grid-element cHeader identifier"><g:message code="seqTrack.seqTrackSet.lanesPerRun.lanesAndFile"/></div>

                <div class="grid-element cHeader wellIdentifier"><g:message code="seqTrack.seqTrackSet.lanesPerRun.singleCellWellLabel"/></div>

                <div class="grid-element cHeader bases" title="${g.message(code: "seqTrack.seqTrackSet.lanesPerRun.numberOfBases.tooltip")}">
                    <g:message code="seqTrack.seqTrackSet.lanesPerRun.numberOfBases"/>
                </div>

                <div class="grid-element cHeader fileSize"><g:message code="seqTrack.seqTrackSet.lanesPerRun.fileSize"/></div>

                <div class="grid-element cHeader reads" title="${g.message(code: "seqTrack.seqTrackSet.lanesPerRun.numberOfReadsAndSeqLength.tooltip")}">
                    <g:message code="seqTrack.seqTrackSet.lanesPerRun.numberOfReadsAndSeqLength"/>
                </div>
            </div>
            <g:each var="seqTrack" in="${lanesPerRun[run]}">
                <g:set var="rawSequenceFiles" value="${seqTrack.sequenceFiles.sort { it.readName }}"/>
                <g:set var="totalFileSize" value="${seqTrack.totalFileSize()}"/>
                <g:set var="totalNReads" value="${seqTrack.NReads}"/>
                <div class="lane-and-datafile-grid-wrapper color-left-border lane slim">
                    <div class="grid-element lane identifier">
                        <strong><g:message code="seqTrack.seqTrackSet.lanesPerRun.laneId"/>:</strong>
                        ${seqTrack.laneId}
                    </div>

                    <div class="grid-element lane wellIdentifier">
                        ${seqTrack.singleCellWellLabel}
                    </div>

                    <div class="grid-element lane bases" title="${UnitHelper.asNucleobases(seqTrack.nBasePairs)}">
                        <strong>${UnitHelper.asNucleobases(seqTrack.nBasePairs, true)}</strong>
                    </div>

                    <div class="grid-element lane fileSize" title="${UnitHelper.asBytes(totalFileSize)}">
                        <strong>${UnitHelper.asBytes(totalFileSize, true)}</strong>
                    </div>

                    <div class="grid-element lane reads" title="${UnitHelper.asReads(totalNReads)}">
                        <strong>${UnitHelper.asReads(totalNReads, true)}</strong>
                    </div>

                    <g:if test="${seqTrack.swapped}">
                        <div class="grid-element lane commentSwapped"
                             title="${g.message(code: "seqTrack.seqTrackSet.lanesPerRun.swapped.tooltip")}">&nbsp;</div>
                    </g:if>

                    <g:each var="rawSequenceFile" status="i" in="${rawSequenceFiles}">
                        <g:set var="row" value="${i + 3}"/>
                        <g:set var="withdrawn" value="${rawSequenceFile.fileWithdrawn ? 'withdrawn' : ''}"/>
                        <g:set var="withdrawnComment" value="Withdrawn Comment: ${rawSequenceFile.fileWithdrawn ? rawSequenceFile.withdrawnComment : ''}"/>
                        <g:set var="withdrawnDate" value="Withdrawn Date: ${rawSequenceFile.fileWithdrawn ? rawSequenceFile.withdrawnDate : ''}"/>
                        <g:set var="exists" value="${rawSequenceFile.fileExists ? '' : 'nonexistent'}"/>
                        <g:set var="nBasePairs" value="${rawSequenceFile.NBasePairsOrNull}"/>
                        <g:set var="fileType" value="${rawSequenceFile.indexFile ? "index" : "fastq"}"/>
                        <div class="grid-element identifier dataFile color-left-border ${fileType} ${withdrawn} ${exists} ${archived} ${deleted} trim-text-with-ellipsis"
                             style="grid-row: ${row};"
                             title="${rawSequenceFile.fileName}">
                            <g:link controller="rawSequenceFile" action="showDetails" id="${rawSequenceFile.id}">${rawSequenceFile.fileName}</g:link>
                            <g:if test="${rawSequenceFile.fileWithdrawn}">
                                <img src="/assets/warning.png" title="${withdrawnComment}&#013;${withdrawnDate}">
                            </g:if>
                            <g:if test="${archived}">
                                <span title="${selectedProject} is archived">&#128451;</span>
                            </g:if>
                        </div>

                        <div class="grid-element dataFile bases" style="grid-row: ${row}" title="${UnitHelper.asNucleobases(nBasePairs)}">
                            ${UnitHelper.asNucleobases(nBasePairs, true)}
                        </div>

                        <div class="grid-element dataFile fileSize" style="grid-row: ${row}" title="${UnitHelper.asBytes(rawSequenceFile.fileSize)}">
                            ${UnitHelper.asBytes(rawSequenceFile.fileSize, true)}
                        </div>

                        <div class="grid-element dataFile reads" style="grid-row: ${row}" title="${UnitHelper.asReads(rawSequenceFile.nReads)}">
                            ${UnitHelper.asReads(rawSequenceFile.nReads, true)} (${rawSequenceFile.sequenceLength ?: "N/A"})
                        </div>
                    </g:each>
                </div>
            </g:each>
        </div>
        <br>
    </g:each>
</div>
</body>
</html>
