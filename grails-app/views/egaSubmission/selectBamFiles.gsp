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

<%@ page import="de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="egaSubmission.selectFiles.bamTitle"/></title>
    <asset:javascript src="pages/egaSubmission/datatable.js"/>
</head>

<body>
<div class="body">
    <g:link style="float: right" action="helpPage" fragment="selectBam" target="_blank">
        <g:img file="info.png"/>
    </g:link>
    <g:render template="/templates/messages"/>
    <div>
    <h1><g:message code="egaSubmission.selectFiles.bamTitle"/></h1>
    <p>
        <g:uploadForm action="bamFilesListFileUploadForm">
            <div class="dialog">
                <input type="file" name="file" id="file"/>
                <g:hiddenField name="submission.id" value="${submission.id}"/>
                <g:submitButton name="upload" value="${message(code: 'egaSubmission.uploadCsv')}" disabled="${!hasFiles || bamFilesHasFileAliases}"/>
            </div>
        </g:uploadForm>
    </p>
        <div class="otpDataTables">
            <g:form action="selectFilesBamFilesForm">
                <g:hiddenField name="submission.id" value="${submission.id}"/>
                <table id="dataTable">
                    <thead>
                    <tr>
                        <g:if test="${!hasFiles}">
                            <th></th>
                        </g:if>
                        <th></th>
                        <th><g:message code="egaSubmission.individual"/></th>
                        <th><g:message code="egaSubmission.seqType"/></th>
                        <th><g:message code="egaSubmission.sampleType"/></th>
                        <th><g:message code="egaSubmission.sampleAlias"/></th>
                        <th><g:message code="egaSubmission.selectFiles.filenameAlias"/></th>
                        <th><g:message code="egaSubmission.selectFiles.filename"/></th>
                        <th><g:message code="egaSubmission.selectFiles.intern"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each status="i" in="${bamFileList}" var="it">
                        <tr class="${it.bamFile.withdrawn ? "withdrawn " : ""}">
                            <g:if test="${!hasFiles}">
                                <td><g:checkBox name="selectBox[${i}]" disabled="${!(it.selectionEditable)}" checked="${it.defaultSelectionState}"/></td>
                            </g:if>
                            <td>
                                <g:if test="${it.bamFile.withdrawn}">
                                    <span title="${g.message(code: "egaSubmission.withdrawn.tooltip")}">
                                        <img src="${assetPath(src: 'warning.png')}"/> ${g.message(code: "egaSubmission.withdrawn")}
                                    </span>
                                </g:if>
                            </td>
                            <td>${it.bamFile.individual.displayName}</td>
                            <td>${it.bamFile.seqType.toString()}</td>
                            <td>${it.bamFile.sampleType.displayName}</td>
                            <td>${it.sampleSubmissionObject.egaAliasName}<g:hiddenField name="egaSampleAlias[${i}]"
                                                                                        value="${it.sampleSubmissionObject.egaAliasName}"/></td>
                            <g:if test="${bamFilesHasFileAliases}">
                                <td>${bamFileSubmissionObject.find {
                                    bamFileSubmissionObject -> bamFileSubmissionObject.bamFile == it.bamFile
                                }.egaAliasName}</td>
                            </g:if><g:else>
                            <td><g:textField name="egaFileAlias[${i}]" size="50"
                                             value="${egaFileAliases?.getAt(it.bamFile.bamFileName + it.sampleSubmissionObject.egaAliasName)}"
                                             disabled="${!hasFiles}"/></td>
                        </g:else>
                            <td>${it.bamFile.bamFileName}<g:hiddenField name="fileId[${i}]" value="${it.bamFile.id}"/></td>
                            <td>${it.producedByOtp ? "Yes" : "No"}</td>
                        </tr>
                    </g:each>
                    </tbody>
                </table>

                <p>
                    <g:submitButton name="saveSelection" value="${message(code: 'egaSubmission.selectFiles.saveSelection')}" disabled="${hasFiles}"/>
                    >>
                    <g:submitButton name="saveAliases" value="${message(code: 'egaSubmission.selectFiles.saveAliases')}"
                                    disabled="${!hasFiles || bamFilesHasFileAliases}"/>
                    <g:submitButton name="download" value="${message(code: 'egaSubmission.downloadCsv')}" disabled="${!hasFiles || bamFilesHasFileAliases}"/>
                </p>
            </g:form>
        </div>
    </div>
    <asset:script>
        $(function() {
            $.otp.egaTable.makeDataTable();
        });
    </asset:script>
</div>
</body>
</html>
