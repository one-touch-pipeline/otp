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

<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.experimentalMetadata.title"/></title>
    <asset:javascript src="pages/egaSubmission/datatable.js"/>
</head>
<body>
<div class="body">
    <h1><g:message code="egaSubmission.experimentalMetadata.header"/></h1>
    <div class="otpDataTables">
        <table id="dataTable">
            <thead>
            <tr>
                <th><g:message code="egaSubmission.experimentalMetadata.studyName"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.designName"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.instrumentModel"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.LibraryName"/></th>
                <th><g:message code="egaSubmission.experimentalMetadata.sequencingReadType"/></th>
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
                    <td>
                        ${it.libraryLayout.toString().toLowerCase()} ${it.displayName} on ${it.mappedEgaPlatformModel} ${it.libraryPreparationKit ? " using ${it.libraryPreparationKit}" : ''}</td>
                    <td>${it.mappedEgaPlatformModel}</td>
                    <td></td>
                    <td>${it.libraryLayout}</td>
                    <td>${it.mappedEgaLibrarySource}</td>
                    <td>${it.mappedEgaLibrarySelection}</td>
                    <td>${it.mappedEgaLibraryStrategy}</td>
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
