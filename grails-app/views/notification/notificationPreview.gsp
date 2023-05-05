%{--
  - Copyright 2011-2020 The OTP authors
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
    <title><g:message code="notification.notificationPreview.title"/></title>
    <asset:javascript src="taglib/Expandable.js"/>
    <asset:javascript src="pages/notification/notificationPreview/functions.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <h1><g:message code="notification.notificationPreview.header"/>: <a href="${otrsTicketLink}">${prefixedTicketNumber}</a></h1>

    <g:form class="basic-flex-box">
        <div class="item basic-right-padding">
            <h2><g:message code="notification.notificationPreview.import.header"/></h2>
            <g:each var="importInstance" in="${importInstances}">
                <g:link controller="metadataImport" action="details" id="${importInstance.id}">${importInstance.id}</g:link>, imported on ${importInstance.dateCreated}
                <ul>
                    <g:each var="entry" in="${importInstance.dataFiles.groupBy { it.seqTrack?.project ?: it.project}}">
                        <li><g:message code="notification.notificationPreview.import.import.projectWithDataFiles" args="[entry.key.name, entry.value.size()]"/></li>
                    </g:each>
                </ul>
            </g:each>

            <h2><a id="summary-anchor"></a><g:message code="notification.notificationPreview.import.summary.header"/></h2>
            <g:each var="preparedNotification" in="${preparedNotifications}">
                <g:link controller="projectConfig" action="index" params="[(projectParameter): preparedNotification.project.name]">${preparedNotification.project}</g:link>
                <g:render template="projectNotifications" model="[project: preparedNotification.project]"/>

                <ul>
                    <li><a href="#project-anchor-${preparedNotification.project.id}"><g:message code="notification.notificationPreview.import.summary.view"/></a></li>
                    <li><g:message code="notification.notificationPreview.import.summary.lanes" args="[preparedNotification.seqTracks.size()]"/></li>
                    <li><g:message code="notification.notificationPreview.import.summary.bams" args="[preparedNotification.bams.size()]"/></li>
                    <li><g:message code="notification.notificationPreview.import.summary.users" args="[preparedNotification.toBeNotifiedProjectUsers.size()]"/></li>
                </ul>
            </g:each>
        </div>
        <div class="item basic-right-padding">
            <h2><g:message code="notification.notificationSelection.notification"/></h2>
            <g:render template="notificationSelection" model="[
                    otrsTicket         : cmd.otrsTicket,
                    fastqImportInstance: cmd.fastqImportInstance,
                    cmd                : cmd,
            ]"/>
            <br><br>
            <g:actionSubmit controller="notification" action="notificationPreview" name="notificationPreview" value="Prepare notification report"/>
        </div>
        <div class="item basic-right-padding">
            <g:actionSubmit id="send-button" name="sendNotificationDigest"
                            action="sendNotificationDigest" value="Send notification"
                            disabled="${preparedNotifications.size() == 0}"
            /><br>
            <g:render template="notificationAnnotation"/>
        </div>
    </g:form>

    <hr><br>

    <g:each var="preparedNotification" in="${preparedNotifications}">
        <g:render template="preparedNotification" model="[preparedNotification: preparedNotification]"/>
    </g:each>
    <br><br>
</div>
</body>
</html>
