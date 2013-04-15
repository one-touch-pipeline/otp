<%@page import="com.sun.xml.internal.org.jvnet.mimepull.DataFile"%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="datafile.showDetails.title"/></title>
    <r:require module="editorSwitch"/>
    <r:require module="changeLog"/>
</head>
<body>
    <div class="body_grow">
        <h1><g:message code="datafile.showDetails.title"/></h1>
        <table>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.runName"/></td>
                <td class="myValue"><b>${dataFile.run.name}</b></td>
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
                <td class="myValue">${dataFile.project}</td>
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
                <td class="myValue"}">
                    <otp:editorSwitch roles="ROLE_ADMIN" link="${g.createLink(controller: 'dataFile', action: 'updateMetaData', id: metaDataEntry.id)}" value="${metaDataEntry.value}"/>
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
<r:script>
    $(function() {
        $.otp.growBodyInit(240);
    });
</r:script>
</html>
