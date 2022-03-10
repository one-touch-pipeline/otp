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

<%@ page import="de.dkfz.tbi.otp.ngsdata.DataFile" contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.selectFiles.fastqTitle"/></title>
    <asset:javascript src="pages/egaSubmission/datatable.js"/>
    <asset:javascript src="pages/egaSubmission/selectFastqFiles/scripts.js"/>
</head>
<body>
<div class="body">
    <g:link style="float: right" action="helpPage" fragment="selectFastq" target="_blank">
        <g:img file="info.png"/>
    </g:link>
    <g:render template="/templates/messages"/>
    <div>
        <h1><g:message code="egaSubmission.selectFiles.fastqTitle"/></h1>
        <p>
            <g:uploadForm action="dataFilesListFileUploadForm">
                <div class="dialog">
                    <input type="file" name="file" id="file"/>
                    <g:hiddenField name="submission.id" value="${submission.id}"/>
                    <g:submitButton name="upload" value="${message(code: 'egaSubmission.uploadCsv')}" disabled="${!hasDataFiles || dataFilesHasFileAliases}"/>
                </div>
            </g:uploadForm>
        </p>
        <div class="otpDataTables">
            <g:form action="selectFilesDataFilesForm">
                <g:hiddenField name="submission.id" value="${submission.id}"/>
                <table id="dataTable">
                    <thead>
                    <tr>
                        <g:if test="${!hasDataFiles}">
                            <th></th>
                        </g:if>
                        <th></th>
                        <th><g:message code="egaSubmission.individual"/></th>
                        <th><g:message code="egaSubmission.seqTypeDisplayName"/></th>
                        <th><g:message code="egaSubmission.sequencingReadType"/></th>
                        <th><g:message code="egaSubmission.singleCellDisplayName"/></th>
                        <th><g:message code="egaSubmission.sampleType"/></th>
                        <th><g:message code="egaSubmission.sampleAlias"/></th>
                        <th><g:message code="egaSubmission.selectFiles.seqCenter"/></th>
                        <th><g:message code="egaSubmission.selectFiles.run"/></th>
                        <th><g:message code="egaSubmission.selectFiles.lane"/></th>
                        <th><g:message code="egaSubmission.selectFiles.library"/></th>
                        <th><g:message code="egaSubmission.selectFiles.ilse"/></th>
                        <th><g:message code="egaSubmission.selectFiles.filenameAlias"/></th>
                        <th><g:message code="egaSubmission.selectFiles.filename"/></th>
                    </tr>
                    </thead>
                    <tbody>
                        <g:each status="i" in="${dataFileList}" var="it">
                            <tr class="${it.dataFile.fileWithdrawn ? "withdrawn " : ""}">
                                <g:if test="${!hasDataFiles}">
                                    <td>
                                        <g:checkBox name="selectBox[${i}]" checked="true" value="${true}" data-group="group${it.dataFile.run}${it.dataFile.seqTrack.laneId}"/>
                                    </td>
                                </g:if>
                                <td>
                                    <g:hiddenField name="fastqFile[${i}]" value="${it.dataFile.id}"/>
                                    <g:hiddenField name="egaSample[${i}]" value="${it.sampleSubmissionObject.id}"/>
                                    <g:if test="${it.dataFile.fileWithdrawn}">
                                        <span title="${g.message(code: "egaSubmission.withdrawn.tooltip")}">
                                            <img src="${assetPath(src: 'warning.png')}"/> ${g.message(code: "egaSubmission.withdrawn")}
                                        </span>
                                    </g:if>
                                </td>
                                <td>${it.dataFile.individual.displayName}</td>
                                <td>${it.dataFile.seqType.displayName}</td>
                                <td>${it.dataFile.seqType.libraryLayout}</td>
                                <td>${it.dataFile.seqType.singleCellDisplayName}</td>
                                <td>${it.dataFile.sampleType.displayName}</td>
                                <td>${it.sampleSubmissionObject.egaAliasName}</td>
                                <td>${it.dataFile.run.seqCenter}</td>
                                <td>${it.dataFile.run}</td>
                                <td>${it.dataFile.seqTrack.laneId}</td>
                                <td>${it.dataFile.seqTrack.normalizedLibraryName}</td>
                                <td>${it.dataFile.seqTrack.ilseId}</td>
                                <g:if test="${dataFilesHasFileAliases}">
                                    <td>${dataFileSubmissionObject.find { dataFileSubmissionObject ->
                                        dataFileSubmissionObject.dataFile == it.dataFile
                                    }.egaAliasName}</td>
                                </g:if>
                                <g:else>
                                    <td><g:textField name="egaFileAlias[${i}]" size="50" value="${egaFileAliases?.getAt(it.dataFile.fileName + it.dataFile.run)}" disabled="${!hasDataFiles}"/></td>
                                </g:else>
                                <td>${it.dataFile.fileName}</td>
                            </tr>
                        </g:each>
                    </tbody>
                </table>
                <p>
                    <g:submitButton name="saveSelection" value="${message(code: 'egaSubmission.selectFiles.saveSelection')}" disabled="${hasDataFiles}"/>
                    >>
                    <g:submitButton name="saveAliases" value="${message(code: 'egaSubmission.selectFiles.saveAliases')}" disabled="${!hasDataFiles || dataFilesHasFileAliases}"/>
                    <g:submitButton name="download" value="${message(code: 'egaSubmission.downloadCsv')}" disabled="${!hasDataFiles || dataFilesHasFileAliases}"/>
                </p>
            </g:form>
        </div>
    </div>
    <asset:script type="text/javascript">
        $(function() {
            $.otp.egaTable.makeDataTable();
            $.otp.selectFastqFiles.combineCheckBoxes();
        });
    </asset:script>
</div>
</body>
</html>
