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

<%@ page import="de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.selectBamFiles.title"/></title>
    <asset:javascript src="pages/egaSubmission/datatable.js"/>
</head>
<body>
<div class="body">
    <g:link style="float: right" action="helpPage" fragment="selectBam" target="_blank">
        <g:img file="info.png"/>
    </g:link>
    <g:render template="/templates/messages"/>
    <div>
        <h3><g:message code="egaSubmission.selectBamFiles.title"/></h3>
        <p>
            <g:uploadForm action="bamFilesListFileUploadForm">
                <div class="dialog">
                    <input type="file" name="file" id="file"/>
                    <g:hiddenField name="submission.id" value="${submission.id}"/>
                    <g:submitButton name="upload" value="Upload BAM meta file" disabled="${!hasFiles || bamFilesHasFileAliases}"/>
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
                        <th><g:message code="egaSubmission.individual"/></th>
                        <th><g:message code="egaSubmission.seqType"/></th>
                        <th><g:message code="egaSubmission.sampleType"/></th>
                        <th><g:message code="egaSubmission.alias"/></th>
                        <th><g:message code="egaSubmission.selectFiles.filenameAlias"/></th>
                        <th><g:message code="egaSubmission.selectFiles.filename"/></th>
                        <th><g:message code="egaSubmission.selectFiles.intern"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each status="i" in="${bamFileList}" var="it">
                        <g:set var="internally" value="${!(it[0] instanceof ExternallyProcessedMergedBamFile)}"/>
                        <tr>
                            <g:if test="${!hasFiles}">
                                %{--TODO this is prepared for multible bam files. At the moment only intern processed bam files should be selectable--}%
                                <td><g:checkBox name="selectBox[${i}]" disabled="${/*TODO !internally*/true}" checked="${internally}"/></td>
                            </g:if>
                            <td>${it[0].individual.displayName}</td>
                            <td>${it[0].seqType.toString()}</td>
                            <td>${it[0].sampleType.displayName}</td>
                            <td>${it[1]}<g:hiddenField name="egaSampleAlias[${i}]" value="${it[1]}"/></td>
                            <g:if test="${bamFilesHasFileAliases}">
                                <td>${bamFileSubmissionObject.find {
                                    bamFileSubmissionObject -> bamFileSubmissionObject.bamFile == it[0]
                                }.egaAliasName}</td>
                            </g:if><g:else>
                                <td><g:textField name="egaFileAlias[${i}]" size="50" value="${egaFileAliases?.getAt(it[0].bamFileName + it[1])}" disabled="${!hasFiles}"/></td>
                            </g:else>
                            <td>${it[0].bamFileName}<g:hiddenField name="fileId[${i}]" value="${it[0].id}"/></td>
                            <td>${internally? "Yes" : "No"}</td>
                        </tr>
                    </g:each>
                    </tbody>
                </table>
                <p>
                    <g:submitButton name="saveSelection" value="Confirm with file selection" disabled="${hasFiles}"/>
                    <g:submitButton name="download" value="Download" disabled="${!hasFiles || bamFilesHasFileAliases}"/>
                    <g:submitButton name="saveAliases" value="Confirm with aliases" disabled="${!hasFiles || bamFilesHasFileAliases}"/>
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
