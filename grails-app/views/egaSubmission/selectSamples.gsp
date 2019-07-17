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

<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.selectSamples.title"/></title>
    <asset:javascript src="pages/egaSubmission/selectSamples/datatable.js"/>
</head>
<body>
    <div class="body">
        <g:link style="float: right" action="helpPage" fragment="selectSamples" target="_blank">
            <g:img file="info.png"/>
        </g:link>
        <h3><g:message code="egaSubmission.selectSamples.title"/></h3>
        <div class="searchCriteriaTableSequences">
            <table id="searchCriteriaTable" style="display: inline-block">
                <tr>
                    <td id="projectName">${project.name}</td>
                    <td>
                        <span class="blue_label">Filter by <g:message code="egaSubmission.seqType"/>:</span>
                    </td>
                    <td>
                        <table id="searchCriteriaTableSeqType">
                            <tr>
                                <td class="attribute">
                                    <g:select class="criteria"
                                              name="criteria"
                                              from="${seqTypes}"
                                              option="${seqTypes}"
                                              noSelection="${["none": message(code:"otp.filter.seqType")]}"/>
                                </td>
                                <td class="value">
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </div>
        <g:form controller="egaSubmission" action="selectSamplesForm">
            <div class="otpDataTables">
                <otp:dataTable
                    codes="${[
                            '',
                            'egaSubmission.individual',
                            'egaSubmission.seqType',
                            'egaSubmission.sampleType',
                    ]}"
                    id="selectSamplesTable" />
            </div>
            <g:hiddenField name="submission.id" value="${submissionId}" />
            <g:submitButton name="next" value="${message(code: 'egaSubmission.selectSamples.next')}"/>
        </g:form>
        <asset:script>
            $(function() {
                $.otp.selectSamplesTable.selectSamples("${samplesWithSeqType}");
            });
        </asset:script>
    </div>
</body>
</html>
