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

<%@ page import="de.dkfz.tbi.util.UnitHelper" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title>${individual.mockFullName}</title>
    <asset:javascript src="modules/editorSwitch.js"/>
    <asset:javascript src="modules/editSamples.js"/>
</head>
<body>
    <div class="body">
        <h1><g:message code="individual.show.header"/></h1>
        <br>
        <div class="individual-grid-wrapper">
            <div id="individualDetailTbl" class="grid-element individualDetails tableBlock">
                <input type="hidden" name="individualId" value="${individual.id}"/>
                <table>
                    <tr>
                        <td class="myKey"><g:message code="individual.show.details.pid"/></td>
                        <td class="myValue">${individual.pid}</td>
                    </tr>
                    <tr>
                        <td class="myKey"><g:message code="individual.show.details.mockPid"/></td>
                        <td class="myValue">
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'individual', action: 'updateField', id: individual.id, params: [key: 'mockPid'])}"
                                value="${individual.mockPid}"/>
                        </td>
                    </tr>
                    <tr>
                        <td class="myKey"><g:message code="individual.show.details.mockFullName"/></td>
                        <td class="myValue">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'individual', action: 'updateField', id: individual.id, params: [key: 'mockFullName'])}"
                                    value="${individual.mockFullName}"/>
                        </td>
                    </tr>
                    <sec:ifAllGranted roles="ROLE_ADMIN">
                        <tr>
                            <td class="myKey"><g:message code="individual.show.details.internIdentifier"/></td>
                            <td class="myValue">
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        link="${g.createLink(controller: 'individual', action: 'updateField', id: individual.id, params: [key: 'internIdentifier'])}"
                                        value="${individual.internIdentifier}"/>
                            </td>
                        </tr>
                    </sec:ifAllGranted>
                    <tr>
                        <td class="myKey"><g:message code="individual.show.details.type"/></td>
                        <td class="myValue typeDropDown">
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'individual', action: 'updateField', id: individual.id, params: [key: 'type'])}"
                                value="${individual.type}"
                                values="${typeDropDown}"/>
                        </td>
                    </tr>
                    <tr>
                        <td class="myKey"><g:message code="individual.show.details.project"/></td>
                        <td class="myValue">
                            <g:link controller="projectOverview"
                                    action="index"
                                    params="[project: individual.project]">${individual.project.displayName}</g:link>
                        </td>
                    </tr>
                </table>
            </div>

            <div class="grid-element commentBox">
                <div id="individualCommentBox" class="commentBoxContainer">
                    <div id="commentLabel">Comment:</div>
                    <sec:ifNotGranted roles="ROLE_OPERATOR">
                        <textarea id="commentBox" readonly>${comment?.comment}</textarea>
                    </sec:ifNotGranted>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <textarea id="commentBox">${comment?.comment}</textarea>
                        <div id="commentButtonArea">
                            <button id="saveComment" disabled>&nbsp;&nbsp;&nbsp;<g:message code="commentBox.save"/></button>
                            <button id="cancelComment" disabled><g:message code="commentBox.cancel" /></button>
                        </div>
                    </sec:ifAllGranted>
                    <div id="commentDateLabel">${comment?.modificationDate?.format('EEE, d MMM yyyy HH:mm')}</div>
                    <div id="commentAuthorLabel">${comment?.author}</div>
                </div>
            </div>
        </div>

        <h1 id="samples"><g:message code="individual.show.samples"/></h1>
        <div class="tableBlock">
            <table>
                <g:each var="sample" in="${individual.samples}">
                    <tr>
                        <td class="myKey">${sample.sampleType.name}</td>
                        <td class="myValue sample">
                            <sec:access expression="hasRole('ROLE_OPERATOR') or ${!projectBlacklisted}">
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="sampleIdentifier"
                                        link="${g.createLink(controller: 'individual', action: 'updateSamples', id: individual.id)}"
                                        value="${sample.sampleIdentifiers}"/>
                                <input type="hidden" name="sampleIdentifiersIds" value="${sample.sampleIdentifiers.id}"/>
                            </sec:access>
                        </td>
                    </tr>
                </g:each>
                <tr>
                    <td class="myKey"></td>
                    <td class="myValue sample">
                        <otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            template="newValue"
                            link="${g.createLink(controller: 'individual', action: 'newSampleType', id: individual.id)}"
                            values="${sampleTypeDropDown}"/>
                    </td>
                </tr>
            </table>
        </div>

        <h1><g:message code="individual.show.laneOverview.header"/></h1>
        <div class="tableBlock">
            <g:each var="seqType" in="${groupedSeqTrackSets.keySet().sort { it.name }}">
                <h2>${seqType}</h2>
                <table>
                    <thead>
                        <tr>
                            <th><%-- contains withdrawn data warning placeholder --%></th>
                            <th><%-- details link placeholder --%></th>
                            <th><g:message code="individual.show.laneOverview.sampleType"/></th>
                            <th title="${g.message(code: "individual.show.laneOverview.numberOfLanes.tooltip")}"><g:message code="individual.show.laneOverview.numberOfLanes"/></th>
                            <th><g:message code="individual.show.laneOverview.numberOfBases"/></th>
                            <th><g:message code="individual.show.laneOverview.numberOfFiles"/></th>
                            <th><g:message code="individual.show.laneOverview.totalSize"/></th>
                            <th><g:message code="individual.show.laneOverview.seqPlatform"/></th>
                        </tr>
                    </thead>
                    <tbody>
                        <g:each var="sampleType" in="${groupedSeqTrackSets[seqType].keySet().sort { it.name }}">
                            <g:set var="seqTrackSelection" value="[individual: individual.id, seqType: seqType.id, sampleType: sampleType.id]"/>
                            <g:set var="seqTrackSet" value="${groupedSeqTrackSets[seqType][sampleType]}"/>
                            <tr>
                                <td>
                                    <g:if test="${seqTrackSet.containsWithdrawnData}">
                                        <img src="${assetPath(src: 'warning.png')}" title="${g.message(code: "individual.show.laneOverview.withdrawnDataWarning.tooltip")}"/>
                                    </g:if>
                                </td>
                                <td><g:link controller="seqTrack" action="seqTrackSet" params="${seqTrackSelection}">Details</g:link></td>
                                <td><strong>${sampleType}</strong></td>

                                <td>${seqTrackSet.numberOfLanes}</td>
                                <td title="${seqTrackSet.numberOfBases ? UnitHelper.asNucleobases(seqTrackSet.numberOfBases) : "N/A"}">
                                    ${seqTrackSet.numberOfBases ? UnitHelper.asNucleobases(seqTrackSet.numberOfBases, true) : "N/A"}
                                </td>

                                <td>${seqTrackSet.dataFiles.size()}</td>
                                <td title="${UnitHelper.asBytes(seqTrackSet.totalFileSize)}">${UnitHelper.asBytes(seqTrackSet.totalFileSize, true)}</td>

                                <td>${seqTrackSet.seqPlatforms.join(", ")}</td>
                            </tr>
                        </g:each>
                    </tbody>
                </table>
            </g:each>
        </div>
    </div>
</body>
<asset:script type="text/javascript">
    $(function() {
        $.otp.initCommentBox(${individual.id}, "#individualCommentBox");
    });
</asset:script>
</html>
