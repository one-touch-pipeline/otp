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
<%@ page import="de.dkfz.tbi.util.TimeFormats" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="metadataImport.details.title"/></title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <div class="basic-flex-box">
        <div class="item basic-right-padding">
            <h2><g:message code="metadataImport.details.otrsTicket"/></h2>
            <table>
                <tr>
                    <td><g:message code="metadataImport.details.otrsTicketNumber"/></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="urlValue"
                                link="${g.createLink(controller: 'metadataImport', action: 'assignOtrsTicketToFastqImportInstance', id: fastqImportInstance.id)}"
                                url="${fastqImportInstance.otrsTicket?.url ?: "#"}"
                                value="${fastqImportInstance.otrsTicket?.ticketNumber ?: g.message(code: "metadataImport.details.ticketless")}"/>
                    </td>
                </tr>
                <g:if test="${fastqImportInstance.otrsTicket}">
                    <tr>
                        <td><g:message code="metadataImport.otrs.automaticNotificationFlag"/>:</td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="dropDown"
                                    link="${g.createLink(controller: "metadataImport", action: "updateAutomaticNotificationFlag", params: ["otrsTicket.id": fastqImportInstance.otrsTicket.id])}"
                                    values="${["true", "false"]}"
                                    value="${fastqImportInstance.otrsTicket.automaticNotification}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="metadataImport.otrs.finalNotificationFlag"/>:</td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="dropDown"
                                    link="${g.createLink(controller: "metadataImport", action: "updateFinalNotificationFlag", params: ["otrsTicket.id": fastqImportInstance.otrsTicket.id])}"
                                    values="${["true", "false"]}"
                                    value="${fastqImportInstance.otrsTicket.finalNotificationSent}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="metadataImport.details.otrsTicket.seqCenter.comment"/></td>
                        <td class="comment-td">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="textArea"
                                    link="${g.createLink(controller: "metadataImport", action: "updateSeqCenterComment", params: ["otrsTicket.id": fastqImportInstance.otrsTicket.id])}"
                                    value="${fastqImportInstance.otrsTicket.seqCenterComment}"/>
                        </td>
                    </tr>
                </g:if>
            </table>
        </div>
        <g:if test="${fastqImportInstance.otrsTicket}">
            <div class="item basic-right-padding">
                <h2><g:message code="notification.notificationSelection.notification"/></h2>
                <g:form controller="notification" action="notificationPreview">
                    <g:render template="/notification/notificationSelection" model="[
                            otrsTicket         : fastqImportInstance.otrsTicket,
                            fastqImportInstance: fastqImportInstance,
                    ]"/>
                    <br><br>
                    <g:submitButton name="notificationPreview" value="Prepare notification report"/>
                </g:form>
            </div>
            <div class="item basic-right-padding">
                <g:render template="/notification/notificationAnnotation"/>
            </div>
        </g:if>
    </div>

    <h2><g:message code="metadataImport.details.metadataFiles"/></h2>
    <ul>
        <g:each in="${metaDataDetails.metaDataFileWrapper}" var="file">
            <li>
                ${file.fullPath}<br>
                <g:message code="metadataImport.details.dateCreated"/>: ${file.dateCreated}<br>
                <g:message code="metadataImport.details.md5"/>: ${file.metaDataFile.md5sum ?: "-"}
            </li>
        </g:each>
    </ul>

    <h2><g:message code="metadataImport.details.dataFiles"/></h2>
    <g:each in="${metaDataDetails.runs}" var="run">
        <h3>
            <g:message code="metadataImport.details.run"/>
            <g:link controller="run" action="show" id="${run.run.id}">${run.run.name}</g:link>,
            ${[
                    run.run.seqCenter.name,
                    run.run.seqPlatform.fullName,
                    TimeFormats.DATE.getFormattedDate(run.run.dateExecuted),
            ].findAll().join(', ')}
        </h3>
        <ul>
            <g:each in="${run.seqTracks}" var="seqTrack">
                <li>
                    <g:link controller="seqTrack" action="show" id="${seqTrack.seqTrack.id}"><g:message code="metadataImport.details.lane"/> ${seqTrack.seqTrack.laneId}</g:link>,
                    ${[
                            seqTrack.seqTrack.ilseId ? "${g.message(code: "metadataImport.details.ilse")} ${seqTrack.seqTrack.ilseId}" : null,
                            seqTrack.seqTrack.project.name,
                    ].findAll().join(', ')},
                    <g:link controller="individual" action="show" id="${seqTrack.seqTrack.individual.id}" params="[individual: seqTrack.seqTrack.individual]">${seqTrack.seqTrack.individual.displayName}</g:link>,
                    ${[
                            seqTrack.seqTrack.sampleType.name,
                            "${seqTrack.seqTrack.seqType.name} ${seqTrack.seqTrack.seqType.libraryLayout}",
                            seqTrack.seqTrack.antibodyTarget?.name ?: null,
                            seqTrack.seqTrack.antibody ?: null,
                            seqTrack.seqTrack.libraryPreparationKit?.name,
                            seqTrack.seqTrack.normalizedLibraryName ? "${g.message(code: "metadataImport.details.normalizedLibraryName")} ${seqTrack.seqTrack.normalizedLibraryName}" : null,
                            seqTrack.seqTrack.pipelineVersion?.displayName,
                    ].findAll().join(', ')}
                    <ul>
                        <g:each in="${seqTrack.dataFiles}" var="dataFile">
                            <li>
                                <g:message code="metadataImport.details.mateNumber" />
                                <g:if test="${dataFile.indexFile}">
                                    <g:message code="metadataImport.details.indexNumber" />
                                </g:if>
                                ${dataFile.mateNumber}:
                                <g:link controller="dataFile" action="showDetails" id="${dataFile.id}">
                                    ${dataFile.fileName}
                                </g:link>
                            </li>
                        </g:each>
                        <g:each in="${seqTrack.seqTrack.logMessages}" var="msg">
                            <li>${de.dkfz.tbi.util.TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(msg.dateCreated)}: ${msg.message}</li>
                        </g:each>
                    </ul>
                </li>
                <br>
            </g:each>
        </ul>
    </g:each>

    <h3><g:message code="metadataImport.details.notAssigned"/></h3>
    <g:if test="${metaDataDetails.dataFilesNotAssignedToSeqTrack}">
        <ul>
            <g:each in="${metaDataDetails.dataFilesNotAssignedToSeqTrack}" var="dataFile">
                <li><g:link controller="dataFile" action="showDetails" id="${dataFile.id}">${dataFile.fileName}</g:link></li>
            </g:each>
        </ul>
    </g:if><g:else>
    <g:message code="metadataImport.details.none"/>
</g:else>
</div>
</body>

</html>
