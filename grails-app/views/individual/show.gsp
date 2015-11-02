<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>${ind.mockFullName}</title>
    <asset:javascript src="modules/editorSwitch"/>
    <asset:javascript src="modules/editSamples"/>
    <asset:javascript src="modules/changeLog"/>
</head>
<body>
    <div class="body_grow">
        <div id="processInfoBox">
            <h1><g:message code="individual.show.title"/></h1>
            <div>
                <g:if test="${next}">
                    <div class="button" id="nextIndividualBtn"><g:link action="show" id="${next.id}"><g:message code="individual.show.nextIndividual"/></g:link></div>
                </g:if>
                <g:if test="${previous}">
                    <div class="button" id ="prevIndividualBtn"><g:link action="show" id="${previous.id}"><g:message code="individual.show.previousIndividual"/></g:link></div>
                </g:if>
            </div>
        </div>
        <div id="individualCommentBox" class="commentBoxContainer">
            <div id="commentLabel">Comment:</div>
            <sec:ifNotGranted roles="ROLE_OPERATOR">
                <textarea id="commentBox" readonly>${comment?.comment?.encodeAsHTML()}</textarea>
            </sec:ifNotGranted>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <textarea id="commentBox">${comment?.comment?.encodeAsHTML()}</textarea>
                <div id="commentButtonArea">
                    <button id="saveComment" disabled>&nbsp;&nbsp;&nbsp;<g:message code="commentBox.save" /></button>
                    <button id="cancelComment" disabled><g:message code="commentBox.cancel" /></button>
                </div>
            </sec:ifAllGranted>
            <div id="commentDateLabel">${comment?.modificationDate?.format('EEE, d MMM yyyy HH:mm')}</div>
            <div id="commentAuthorLabel">${comment?.author}</div>
        </div>
        <div class="tableBlock" id="individualDetailTbl">
            <input type="hidden" name="individualId" value="${ind.id}"/>
            <table>
                <tr>
                    <td class="myKey"><g:message code="individual.show.details.pid"/></td>
                    <td class="myValue">${ind.pid}</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="individual.show.details.mockPid"/></td>
                    <td class="myValue"><otp:editorSwitch roles="ROLE_OPERATOR" link="${g.createLink(controller: 'individual', action: 'updateField', id: ind.id, params: [key: 'mockPid'])}" value="${ind.mockPid}"/></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="individual.show.details.mockFullName"/></td>
                    <td class="myValue"><otp:editorSwitch roles="ROLE_OPERATOR" link="${g.createLink(controller: 'individual', action: 'updateField', id: ind.id, params: [key: 'mockFullName'])}" value="${ind.mockFullName}"/></td>
                </tr>
                <sec:ifAllGranted roles="ROLE_ADMIN">
                    <tr>
                        <td class="myKey"><g:message code="individual.show.details.internIdentifier"/></td>
                        <td class="myValue"><otp:editorSwitch roles="ROLE_OPERATOR" link="${g.createLink(controller: 'individual', action: 'updateField', id: ind.id, params: [key: 'internIdentifier'])}" value="${ind.internIdentifier}"/></td>
                    </tr>
                </sec:ifAllGranted>
                <tr>
                    <td class="myKey"><g:message code="individual.show.details.type"/></td>
                    <td class="myValue typeDropDown"><otp:editorSwitch roles="ROLE_OPERATOR" template="dropDown" link="${g.createLink(controller: 'individual', action: 'updateField', id: ind.id, params: [key: 'type'])}" value="${ind.type}"/></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="individual.show.details.project"/></td>
                    <td class="myValue"><g:link controller="projectOverview" action="index" params="[projectName: ind.project]">${ind.project}</g:link></td>
                </tr>
            </table>
        </div>
        <h1 id="samples" style="display:inline-block"><g:message code="individual.show.samples"/>
            <otp:editorSwitch roles="ROLE_OPERATOR" template="newValue" link="${g.createLink(controller: 'individual', action: 'newSampleType', id: ind.id)}" value="${ind.type}"/>
        </h1>
        <div class="tableBlock">
            <table>
                <g:each var="sample" in="${ind.samples}">
                    <tr>
                        <td class="myKey">${sample.sampleType.name}</td>
                        <td class="myValue sample">
                            <sec:ifAllGranted roles="ROLE_ADMIN">
                                <otp:editorSwitch roles="ROLE_OPERATOR" template="sampleIdentifier" link="${g.createLink(controller: 'individual', action: 'updateSamples', id: ind.id)}" value="${sample.sampleIdentifiers}"/>
                                <input type="hidden" name="sampleIdentifiersIds" value="${sample.sampleIdentifiers.id}"/>
                            </sec:ifAllGranted>
                        </td>
                    </tr>
                </g:each>
            </table>
        </div>
    <%--${mergedBams}--%>
        <g:if test="${ind.seqTypes}">
            <H1><g:message code="individual.show.sequencingScans"/></H1>
        </g:if>
        <g:form>
            <g:each var="type" in="${ind.seqTypes}">
                <div class="tableBlock">
                    <h2>${type}</h2>
                    <table>
                        <thead>
                            <tr>
                                <th></th>
                                <th><g:message code="individual.show.sequencingScans.type"/></th>
                                <th><g:message code="individual.show.sequencingScans.platform"/></th>
                                <th><g:message code="individual.show.sequencingScans.status"/></th>
                                <th><g:message code="individual.show.sequencingScans.center"/></th>
                                <th><g:message code="individual.show.sequencingScans.numberOfLanes"/></th>
                                <th><g:message code="individual.show.sequencingScans.numberOfBases"/></th>
                                <th><g:message code="individual.show.sequencingScans.insertSize"/></th>
                            </tr>
                        </thead>
                        <tbody>
                            <g:each var="scan" in="${ind.seqScans}">
                                <g:if test="${scan.seqType.id == type.id}">
                                    <g:if test="${scan.state != de.dkfz.tbi.otp.ngsdata.SeqScan.State.OBSOLETE}">
                                        <tr>
                                            <td><g:link controller="seqScan" action="show" id="${scan.id}"><g:message code="individual.show.sequencingScans.details"/></g:link></td>
                                            <td><strong>${scan.sample.sampleType.name}</strong></td>
                                            <td>${scan.seqPlatform}</td>
                                            <td>${scan.state}</td>
                                            <td>${scan.seqCenters.toLowerCase()}</td>
                                            <td>${scan.nLanes}</td>
                                            <td>${scan.basePairsString()}</td>
                                            <td>${scan.insertSize}</td>
                                        </tr>
                                    </g:if>
                                </g:if>
                            </g:each>
                        </tbody>
                    </table>
                </div>
            </g:each>
            <h1><g:link controller="samplePair" action="index" params="[mockPid: ind.mockPid]"><g:message code="individual.snv.detail"/></g:link></h1>
             <g:if test="${ind.mutations}">
                <h1><g:message code="individual.show.analysisResults"/></h1>
                <div class="tableBlock">
                    <g:each var="mutation" in="${ind.mutations}">
                        ${mutation.gene},
                    </g:each>
                </div>
            </g:if>
            <!-- Start IGV feature does not work. See OTP-1203.
            <h1><g:message code="individual.show.dataAccess"/></h1>
            <div class="buttons show_button">
                <g:actionSubmit class="button" value="${message(code:'individual.show.sequencingScans.startIgv')}" action="igvStart"/>
            </div>
            -->
        </g:form>
    </div>
</body>
<asset:script>
    $(function() {
        $.otp.growBodyInit(240);
        $.otp.initCommentBox(${ind.id}, "#individualCommentBox");
    });
</asset:script>
</html>
