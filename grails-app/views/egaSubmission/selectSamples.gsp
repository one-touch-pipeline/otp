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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="egaSubmission.selectSamples.title"/></title>
    <asset:javascript src="common/DataTableFilter.js"/>
    <asset:javascript src="pages/egaSubmission/selectSamples/datatable.js"/>
    <asset:javascript src="pages/egaSubmission/selectSamples/index.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <g:link style="float: right" action="helpPage" fragment="selectSamples" target="_blank">
        <g:img file="info.png"/>
    </g:link>

    <h1><g:message code="egaSubmission.selectSamples.title"/></h1>

    <div class="rounded-page-header-box">
        <table id="searchCriteriaTable" style="display: inline-block">
            <tr>
                <td><g:message code="egaSubmission.SubmissionProject"/>:</td>
                <td>${project.displayName}</td>
            </tr>
            <tr class="dtf_row">
                <td>
                    <span><g:message code="egaSubmission.seqType"/>:</span>
                </td>
                <td>
                    <table id="searchCriteriaTableSeqType">
                        <tr>
                            <td class="value">
                                <g:select name="seqTypeSelection"
                                          class="use-select-2"
                                          from="${seqTypesNames}"
                                          option="${seqTypesNames}"
                                          noSelection="${["none": message(code: "egaSubmission.allSeqTypes")]}"/>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </div>

    <g:uploadForm action="sampleInformationUploadForm">
        <div class="dialog">
            <input type="file" name="file" id="file"/>
            <g:hiddenField name="submission.id" value="${submissionId}"/>
            <button type="button" onclick="uploadCsvOfSelectedSamples()">
                ${message(code: 'egaSubmission.uploadCsv')}
            </button>
        </div>
    </g:uploadForm>

    <g:form controller="egaSubmission" action="selectSamplesForm">
        <div class="otpDataTables" id="sampleTable" data-project="${project}">
            <g:set var="dataTableHeaders" value="${[
                    'individual',
                    'uuid',
                    'seqTypeDisplayName',
                    'sequencingReadType',
                    'singleCellDisplayName',
                    'sampleType',
            ]}"/>
            <otp:dataTable codes="${[''] + dataTableHeaders.collect { "egaSubmission.${it}" }}" id="selectSamplesTable"/>
        </div>

        <g:hiddenField name="submission.id" value="${submissionId}"/>
        <g:set var="nextButton" value="next"/>
        <button type="button" onclick="downloadCsvOfSelectedSamples()">
            ${message(code: 'egaSubmission.downloadCsv')}
        </button>
        <g:submitButton name="next" value="${message(code: 'egaSubmission.selectSamples.next')}"/>
    </g:form>
    <g:set var="seqTypeColumnIndex" value="3"/>

    <asset:script type="text/javascript">
        $(function() {
            let table = $.otp.selectSamplesTable.selectableSampleList([${raw("\"" + dataTableHeaders.join("\",\"") + "\"")}], "${samplesWithSeqType}");
                $.otp.selectSamplesTable.applySeqTypeFilter(table, "${seqTypeColumnIndex}");

                $('#${nextButton}').on("click", function() {
                  $.otp.selectSamplesTable.removeFilterOnColumn(table, "${seqTypeColumnIndex}");
                });
            });
    </asset:script>
</div>
</body>
</html>
