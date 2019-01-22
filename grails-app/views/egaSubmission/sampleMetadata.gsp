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
                    <th><g:message code="egaSubmission.SubmissionProject"/></th>
                    <th><g:message code="egaSubmission.individual"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.egaPid"/></th>
                    <th><g:message code="egaSubmission.sampleType"/></th>
                    <th><g:message code="egaSubmission.alias"/></th>
                    <th><g:message code="egaSubmission.seqType"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.gender"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.phenotype"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.case"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.organism"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.title1"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.description"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.bioSampleId"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.cellLine"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.organismPart"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.geographicalRegion"/></th>
                </tr>
            </thead>
            <tbody>
                <g:each in="${submission.samplesToSubmit}">
                    <tr>
                        <td>${submission.project.name}</td>
                        <td>${it.sample.individual.displayName}</td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td>${it.sample.displayName}</td>
                        <td>${it.egaAliasName}</td>
                        <td>${it.seqType.toString()}</td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td>${it.sample.sampleTypeCategory ?: g.message(code: "egaSubmission.unknown")}</td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                    </tr>
                </g:each>
            </tbody>
        </table>
    </div>
    <g:form action="sampleMetadataForm">
        <g:hiddenField name="submission.id" value="${submission.id}"/>
        <g:submitButton name="download" value="Download"/>
    </g:form>
</div>
<asset:script>
    $(function() {
        $.otp.egaTable.makeDataTable();
    });
</asset:script>
</body>
</html>
