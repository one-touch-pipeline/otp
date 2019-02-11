<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.sampleMetadata.title"/></title>
    <asset:javascript src="pages/egaSubmission/datatable.js"/>
</head>
<body>
<div class="body">
    <h3><g:message code="egaSubmission.sampleMetadata.title"/></h3>
    <div class="otpDataTables">
        <table id="dataTable">
            <thead>
            <tr>
                <th><g:message code="egaSubmission.experimentalMetadata.studyName"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.designName"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.instrumentModel"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.LibraryName"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.libraryLayout"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.librarySource"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.librarySelection"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.libraryStrategy"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.libraryConstructionProtocol"/></th>
            </tr>
            </thead>
            <tbody>
            <g:each in="${metadata}">
                <tr>
                    <td>${submission.studyName}</td>
                    <td><g:message code="egaSubmission.unknown"/></td>
                    <td><g:message code="egaSubmission.unknown"/></td>
                    <td><g:message code="egaSubmission.unknown"/></td>
                    <td>${it.libraryLayout}</td>
                    <td><g:message code="egaSubmission.unknown"/></td>
                    <td><g:message code="egaSubmission.unknown"/></td>
                    <td>${it.seqType}</td>
                    <td>${it.libraryPreparationKit}</td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </div>
</div>
<asset:script>
    $(function() {
        $.otp.egaTable.makeDownloadableDataTable();
    });
</asset:script>
</body>
</html>
