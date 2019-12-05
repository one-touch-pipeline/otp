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

<%@ page import="de.dkfz.tbi.otp.ngsdata.ProjectOverviewService; de.dkfz.tbi.otp.ngsdata.MetaDataColumn" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="datafile.showDetails.title"/></title>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
    <div class="body">
        <div id="dataFileCommentBox" class="commentBoxContainer">
            <div id="commentLabel">Comment:</div>
            <sec:ifNotGranted roles="ROLE_OPERATOR">
                <textarea id="commentBox" readonly>${comment?.comment}</textarea>
            </sec:ifNotGranted>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <textarea id="commentBox">${comment?.comment}</textarea>
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
                <td class="myValue">
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <b><g:link controller="run" action="show" params="[id:  dataFile.run.name]">${dataFile.run.name}</g:link></b>
                    </sec:ifAllGranted>
                    <sec:ifNotGranted roles="ROLE_OPERATOR">
                        ${dataFile.run.name}
                    </sec:ifNotGranted>
                </td>
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
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.fastqcReport"/></td>
                <td class="myValue"><span class="wordBreak">
                    <g:if test="${fastqcAvailable == true}">
                        <g:link controller="fastqcResults" action="show" id="${dataFile.id}">
                            <g:message code="datafile.showDetails.fastqcReport"/>
                        </g:link>
                    </g:if>
                </span></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.checkSum"/></td>
                <td class="myValue">${dataFile.md5sum}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.fullPath"/></td>
                <td class="myValue"><span class="wordBreak">${values.get(0)}</span></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.viewByPidFullPath"/></td>
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
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <tr>
                    <td class="myKey"><g:message code="datafile.showDetails.import"/></td>
                    <td class="myValue"><g:link controller="metadataImport" action="details" id="${dataFile.fastqImportInstanceId}">Import</g:link></td>
                </tr>
            </sec:ifAllGranted>
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
                <td class="myValue"><g:link controller="projectOverview" action="index" params="[project: dataFile.project]">${dataFile.project}</g:link></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.metaDataStatus.isFileUsed"/></td>
                <td class="${dataFile.used}">${dataFile.used}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.metaDataStatus.isWithdrawn"/></td>
                <td class="${dataFile.fileWithdrawn}">${dataFile.fileWithdrawn}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.metaDataStatus.withdrawnDate"/></td>
                <td class="myValue">${dataFile.withdrawnDate?.format("yyyy-MM-dd HH:mm") ?: "-"}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="datafile.showDetails.metaDataStatus.withdrawnComment"/></td>
                <td class="myValue">${dataFile.withdrawnComment ?: "-"}</td>
            </tr>
        </table>
        <H1><g:message code="datafile.showDetails.metaDataEntries"/></H1>
        <p>${g.message(code: "datafile.showDetails.metaDataEntries.explanation")}</p>
        <table>
        <g:each var="metaDataEntry" in="${entries}">
            <tr>
                <td class="myKey">${metaDataEntry.key.name}</td>
                <td class="myValue">
                    <g:if test="${metaDataEntry.key.name == MetaDataColumn.SAMPLE_ID.name() && ProjectOverviewService.PROJECT_TO_HIDE_SAMPLE_IDENTIFIER.contains(dataFile.project.name)}">
                        <g:message code="datafile.showDetails.hiddenSampleIdentifier"/>
                    </g:if>
                    <g:else>
                        ${metaDataEntry.value}
                    </g:else>
                </td>
            </tr>
        </g:each>
    </table>
    </div>
</body>
<asset:script>
    $(function() {
        $.otp.initCommentBox(${dataFile.id},"#dataFileCommentBox");
    });
</asset:script>
</html>
