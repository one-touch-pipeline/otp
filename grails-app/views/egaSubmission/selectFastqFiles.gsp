<%@ page import="de.dkfz.tbi.otp.ngsdata.DataFile" contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.selectFastqFiles.title"/></title>
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
        <h3><g:message code="egaSubmission.selectFastqFiles.title"/></h3>
        <p>
            <g:uploadForm action="dataFilesListFileUploadForm">
                <div class="dialog">
                    <input type="file" name="file" id="file"/>
                    <g:hiddenField name="submission.id" value="${submission.id}"/>
                    <g:submitButton name="upload" value="Upload FASTQ meta file" disabled="${!hasDataFiles || dataFilesHasFileAliases}"/>
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
                        <th><g:message code="egaSubmission.individual"/></th>
                        <th><g:message code="egaSubmission.sampleType"/></th>
                        <th><g:message code="egaSubmission.seqType"/></th>
                        <th><g:message code="egaSubmission.alias"/></th>
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
                            <tr>
                                <g:if test="${!hasDataFiles}">
                                    <td><g:checkBox name="selectBox[${i}]" checked="true" value="${true}" data-group="group${it[0].run }${it[0].seqTrack.laneId}"/></td>
                                </g:if>
                                <td>${it[0].individual.displayName}</td>
                                <td>${it[0].sampleType.displayName}</td>
                                <td>${it[0].seqType.toString()}</td>
                                <td>${it[1]}<g:hiddenField name="egaSampleAlias[${i}]" value="${it[1]}"/></td>
                                <td>${it[0].run.seqCenter}</td>
                                <td>${it[0].run}<g:hiddenField name="runName[${i}]" value="${it[0].run.name}"/></td>
                                <td>${it[0].seqTrack.laneId}</td>
                                <td>${it[0].seqTrack.normalizedLibraryName}</td>
                                <td>${it[0].seqTrack.ilseId}</td>
                                <g:if test="${dataFilesHasFileAliases}">
                                    <td>${dataFileSubmissionObject.find {
                                        dataFileSubmissionObject -> dataFileSubmissionObject.dataFile == it[0]
                                    }.egaAliasName}</td>
                                </g:if><g:else>
                                    <td><g:textField name="egaFileAlias[${i}]" size="50" value="${egaFileAliases?.getAt(it[0].fileName + it[0].run)}" disabled="${!hasDataFiles}"/></td>
                                </g:else>
                                <td>${it[0].fileName}<g:hiddenField name="filename[${i}]" value="${it[0].fileName}"/></td>
                            </tr>
                        </g:each>
                    </tbody>
                </table>
                <p>
                    <g:submitButton name="saveSelection" value="Confirm with file selection" disabled="${hasDataFiles}"/>
                    <g:submitButton name="download" value="Download" disabled="${!hasDataFiles || dataFilesHasFileAliases}"/>
                    <g:submitButton name="saveAliases" value="Confirm with aliases" disabled="${!hasDataFiles || dataFilesHasFileAliases}"/>
                </p>
            </g:form>
        </div>
    </div>
    <asset:script>
        $(function() {
            $.otp.egaTable.makeDataTable();
            $.otp.selectFastqFiles.combineCheckBoxes();

        });
    </asset:script>
</div>
</body>
</html>
