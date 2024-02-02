%{--
  - Copyright 2011-2024 The OTP authors
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

<%@ page import="de.dkfz.tbi.otp.project.Project; de.dkfz.tbi.otp.ngsdata.MetaDataColumn" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="rawSequenceFiles.showDetails.title"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/bootstrap/noChange" model="[project: rawSequenceFile.project]"/>

        <div class="two-column-grid-container">
            <div class="grid-element">
                <h1><g:message code="rawSequenceFiles.showDetails.title"/></h1>
            </div>
            <div class="grid-element comment-box">
                <g:render template="/templates/commentBox" model="[
                        commentable     : rawSequenceFile,
                        targetController: 'rawSequenceFile',
                        targetAction    : 'saveDataFileComment',
                ]"/>
            </div>
        </div>
        <table>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.runName"/></td>
                <td class="myValue">
                    <strong>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <g:link controller="run" action="show" params="[id: rawSequenceFile.run.name]">${rawSequenceFile.run.name}</g:link>
                        </sec:ifAllGranted>
                        <sec:ifNotGranted roles="ROLE_OPERATOR">
                            ${rawSequenceFile.run.name}
                        </sec:ifNotGranted>
                    </strong>
                </td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.fileName"/></td>
                <td class="myValue"><span class="wordBreak">${rawSequenceFile.fileName}</span></td>
            </tr>
            <g:if test="${!rawSequenceFile.pathName.isAllWhitespace()}">
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.filePath"/></td>
                <td class="myValue"><span class="wordBreak">${rawSequenceFile.pathName}</span></td>
            </tr>
            </g:if>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.viewByPidName"/></td>
                <td class="myValue"><span class="wordBreak">${rawSequenceFile.vbpFileName}</span></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.fastqcReport"/></td>
                <td class="myValue"><span class="wordBreak">
                    <g:if test="${fastqcAvailable == true}">
                        <g:link controller="fastqcResults" action="show" id="${rawSequenceFile.id}">
                            <g:message code="rawSequenceFiles.showDetails.fastqcReport"/>
                        </g:link>
                    </g:if>
                </span></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.checkSum"/></td>
                <td class="myValue">${rawSequenceFile.fastqMd5sum}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.fullPath"/></td>
                <td class="myValue"><span class="wordBreak">${fullPath}</span></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.viewByPidFullPath"/></td>
                <td class="myValue"><span class="wordBreak">${vbpPath}</span></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.fileExists"/></td>
                <td class="${rawSequenceFile.fileExists}">${rawSequenceFile.fileExists}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.fileLinked"/></td>
                <td class="${rawSequenceFile.fileLinked}">${rawSequenceFile.fileLinked}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.fileSize"/></td>
                <td class="myValue">${fileSize}</td>
            </tr>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <tr>
                    <td class="myKey"><g:message code="rawSequenceFiles.showDetails.import"/></td>
                    <td class="myValue"><g:link controller="metadataImport" action="details" id="${rawSequenceFile.fastqImportInstanceId}">Import</g:link></td>
                </tr>
            </sec:ifAllGranted>
        </table>
        <h2><g:message code="rawSequenceFiles.showDetails.dates"/></h2>
        <table>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.dates.runExecutionDate"/></td>
                <td class="myValue">${dateExecuted}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.dates.fileSystemDate"/></td>
                <td class="myValue">${dateFileSystem}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.dates.databaseRegistrationDate"/></td>
                <td class="myValue">${dateCreated}</td>
            </tr>
        </table>
        <h2><g:message code="rawSequenceFiles.showDetails.metaDataStatus"/></h2>
        <table>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.metaDataStatus.project"/></td>
                <td class="myValue"><g:link controller="projectOverview" action="index" params="[(projectParameter): rawSequenceFile.project.name]">${rawSequenceFile.project}</g:link></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.metaDataStatus.isFileUsed"/></td>
                <td class="${rawSequenceFile.used}">${rawSequenceFile.used}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.metaDataStatus.isWithdrawn"/></td>
                <td class="${rawSequenceFile.fileWithdrawn}">${rawSequenceFile.fileWithdrawn}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.metaDataStatus.withdrawnDate"/></td>
                <td class="myValue">${withdrawnDate}</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="rawSequenceFiles.showDetails.metaDataStatus.withdrawnComment"/></td>
                <td class="myValue">${rawSequenceFile.withdrawnComment ?: "-"}</td>
            </tr>
        </table>
        <h2><g:message code="rawSequenceFiles.showDetails.metaDataEntries"/></h2>
        <otp:annotation type="warning">${g.message(code: "rawSequenceFiles.showDetails.metaDataEntries.explanation")}</otp:annotation>
        <table>
        <g:each var="metaDataEntry" in="${entries}">
            <tr>
                <td class="myKey">${metaDataEntry.key.name}</td>
                <td class="myValue">${metaDataEntry.value}</td>
            </tr>
        </g:each>
    </table>
    </div>
</body>
</html>
