<%@ page import="de.dkfz.tbi.otp.ngsdata.MetaDataColumn" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="datafile.showDetails.title"/></title>
    <asset:javascript src="modules/editorSwitch"/>
    <asset:javascript src="modules/changeLog"/>
</head>
<body>
    <div class="body_grow">
        <div id="dataFileCommentBox" class="commentBoxContainer">
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
        <h1><g:message code="datafile.showDetails.title"/></h1>
        <table>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.runName"/></td>
                <td class="myValue"><b><g:link controller="run" action="show" params="[id:  dataFile.run.name]">${dataFile.run.name}</g:link></b></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.fileName"/></td>
                <td class="myValue"><span class="wordBreak">${dataFile.fileName}</span></td>
            </tr>
            <g:if test="${!dataFile.pathName.isAllWhitespace()}">
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.filePath"/></td>
                <td class="myValue"><span class="wordBreak">${dataFile.pathName}</span></td>
            </tr>
            </g:if>
                <tr>
                <td class="myKey"><g:message code="datafile.showDetails.viewByPidName"/></td>
                <td class="myValue"><span class="wordBreak">${dataFile.vbpFileName}</span></td>
            </tr>
            <g:if test="${!dataFile.vbpFilePath.isAllWhitespace()}">
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.viewByPidPath"/></td>
                <td class="myValue"><span class="wordBreak">${dataFile.vbpFilePath}</span></td>
            </tr>
            </g:if>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.checkSum"/></td>
                <td class="myValue">${dataFile.md5sum}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.fullPath"/></td>
                <td class="myValue"><span class="wordBreak">${values.get(0)}</span></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.viewByPidfullPath"/></td>
                <td class="myValue"><span class="wordBreak">${values.get(1)}</span></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.fileExists"/></td>
                <td class="${dataFile.fileExists}">${dataFile.fileExists}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.fileLinked"/></td>
                <td class="${dataFile.fileLinked}">${dataFile.fileLinked}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.fileSize"/></td>
                <td class="myValue">${dataFile.fileSizeString()}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.import"/></td>
                <td class="myValue"><g:link controller="metadataImport" action="details" id="${dataFile.runSegmentId}">Import</g:link></td>
            </tr>
        </table>
        <H1><g:message code="datafile.showDetails.dates"/></H1>
        <table>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.dates.runExecutionDate"/></td>
                <td class="myValue">${dataFile.dateExecuted?.format("yyyy-MM-dd")}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.dates.fileSystemDate"/></td>
                <td class="myValue">${dataFile.dateFileSystem?.format("yyyy-MM-dd HH:mm")}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.dates.databaseRegistrationDate"/></td>
                <td class="myValue">${dataFile.dateCreated.format("yyyy-MM-dd HH:mm")}</td>
            </tr>
        </table>
        <H1><g:message code="datafile.showDetails.metaDataStatus"/></H1>
        <table>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.metaDataStatus.project"/></td>
                <td class="myValue"><g:link controller="projectOverview" action="index" params="[projectName: dataFile.project]">${dataFile.project}</g:link></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.metaDataStatus.isFileUsed"/></td>
                <td class="${dataFile.used}">${dataFile.used}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.metaDataStatus.isMetaDataValid"/></td>
                <td class="${dataFile.metaDataValid}">${dataFile.metaDataValid}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.metaDataStatus.isWithdrawn"/></td>
                <td class="${dataFile.fileWithdrawn}">${dataFile.fileWithdrawn}</td>
            </tr>
        </table>
        <H1><g:message code="datafile.showDetails.metaDataEntries"/></H1>
        <table>
        <g:each var="metaDataEntry" in="${entries}">
            <tr>
                <td class="myKey">${metaDataEntry.key.name}</td>
                <td class="myValue">
                    <sec:ifAllGranted roles="ROLE_ADMIN">
                        <otp:editorSwitch roles="ROLE_ADMIN" link="${g.createLink(controller: 'dataFile', action: 'updateMetaData', id: metaDataEntry.id)}" value="${metaDataEntry.value}"/>
                    </sec:ifAllGranted>
                    <sec:ifNotGranted roles="ROLE_ADMIN">
                        <g:if test="${metaDataEntry.key.name == MetaDataColumn.SAMPLE_ID.name()}">
                            <g:message code="datafile.showDetails.hiddenSampleIdentifier"/>
                        </g:if>
                        <g:else>
                            ${metaDataEntry.value}
                        </g:else>
                    </sec:ifNotGranted>
                </td>
                <td class="${metaDataEntry.status}">${metaDataEntry.status}</td>
                <td>${metaDataEntry.source}</td>
                <td>
                    <g:if test="${changelogs[metaDataEntry]}">
                        <otp:showChangeLog controller="dataFile" action="metaDataChangelog" id="${metaDataEntry.id}"/>
                    </g:if>
                </td>
            </tr>
        </g:each>
    </table>
    </div>
</body>
<asset:script>
    $(function() {
        $.otp.growBodyInit(240);
        $.otp.initCommentBox(${dataFile.id},"#dataFileCommentBox");
    });
</asset:script>
</html>
