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

<%@ page import="de.dkfz.tbi.otp.utils.CollectionUtils; de.dkfz.tbi.otp.ngsdata.SampleTypePerProject;" contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.sampleMetadata.title"/></title>
    <asset:javascript src="pages/egaSubmission/datatable.js"/>
</head>
<body>
<div class="body">
    <h1><g:message code="egaSubmission.sampleMetadata.header"/></h1>
    <div class="otpDataTables">
        <table id="dataTable">
            <thead>
                <tr>
                    <th><g:message code="egaSubmission.SubmissionProject"/></th>
                    <th><g:message code="egaSubmission.individual"/></th>
                    <th><g:message code="egaSubmission.uuid"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.egaPid"/></th>
                    <th><g:message code="egaSubmission.sampleType"/></th>
                    <th><g:message code="egaSubmission.sampleAlias"/></th>
                    <th><g:message code="egaSubmission.seqTypeDisplayName"/></th>
                    <th><g:message code="egaSubmission.sequencingReadType"/></th>
                    <th><g:message code="egaSubmission.singleCellDisplayName"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.gender"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.phenotype"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.case"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.organism"/></th>
                    <th><g:message code="egaSubmission.sampleMetadata.titleColumn"/></th>
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
                        <td>${it.sample.individual.uuid}</td>
                        <td><g:message code="egaSubmission.egaPid"/> ${it.sample.individual.displayName}</td>
                        <td>${it.sample.displayName}</td>
                        <td>${it.egaAliasName}</td>
                        <td>${it.seqType.displayName}</td>
                        <td>${it.seqType.libraryLayout}</td>
                        <td>${it.seqType.singleCellDisplayName}</td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td><g:message code="egaSubmission.unknown"/></td>
                        <td>${CollectionUtils.atMostOneElement(SampleTypePerProject.findAllByProjectAndSampleType(it.project,it.sample.sampleType))?.category?.toString()
                                ?: g.message(code: "egaSubmission.unknown")}</td>
                        <td>${it.project.speciesWithStrains ?: g.message(code: "egaSubmission.unknown")}</td>
                        <td>${it.egaAliasName}</td>
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
</div>
<asset:script>
    $(function() {
        $.otp.egaTable.makeDownloadableDataTable();
    });
</asset:script>
</body>
</html>
