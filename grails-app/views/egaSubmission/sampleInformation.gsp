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

<%@ page import="de.dkfz.tbi.otp.egaSubmission.EgaMapKey" contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.sampleInformation.title"/></title>
    <asset:javascript src="pages/egaSubmission/datatable.js"/>
</head>
<body>
<div class="body">
    <g:link style="float: right" action="helpPage" fragment="sampleInformation" target="_blank">
        <g:img file="info.png"/>
    </g:link>
    <g:render template="/templates/messages"/>
    <div>
        <h1><g:message code="egaSubmission.sampleInformation.title"/></h1>
        <otp:annotation type="info"><g:message code="egaSubmission.sampleInformation.aliasArePublic"/></otp:annotation>
        <p>
            <g:uploadForm action="sampleInformationUploadForm">
                <div class="dialog">
                    <input type="file" name="file" id="file"/>
                    <g:hiddenField name="submission.id" value="${submissionId}"/>
                    <g:submitButton name="upload" value="${message(code: 'egaSubmission.uploadCsv')}"/>
                </div>
            </g:uploadForm>
        </p>
        <div class="otpDataTables">
        <g:form action="sampleInformationForms">
            <g:hiddenField name="submission.id" value="${submissionId}"/>
            <table id="dataTable">
                <thead>
                    <tr>
                        <th><g:message code="egaSubmission.individual"/></th>
                        <th><g:message code="egaSubmission.seqType"/></th>
                        <th><g:message code="egaSubmission.sampleType"/></th>
                        <th><g:message code="egaSubmission.sampleAlias"/></th>
                    </tr>
                </thead>
                <tbody>
                    <g:each status="i" in="${sampleSubmissionObjects}" var="it">
                        <g:set var="individual" value="${it.sample.individual.displayName}" />
                        <g:set var="seqType" value="${it.seqType.toString()}" />
                        <g:set var="sampleType" value="${it.sample.sampleType.displayName}" />
                        <g:set var="key" value="${new EgaMapKey(it)}" />
                        <tr>
                            <g:hiddenField name="sampleObjectId[${i}]" value="${it.id}"/>
                            <td>${individual}</td>
                            <td>${seqType}</td>
                            <td>${sampleType}</td>
                            <td><g:textField name="egaSampleAlias[${i}]" size="50" value="${egaSampleAliases.get(key)}"/></td>
                            <g:hiddenField name="fileType[${i}]" value="${message(code: 'egaSubmission.fastq')}"/>
                        </tr>
                    </g:each>
                </tbody>
            </table>
            <p>
                <g:submitButton name="back" value="${message(code: 'egaSubmission.sampleInformation.back')}"/>&nbsp;
                <g:submitButton name="csv" value="${message (code: 'egaSubmission.downloadCsv')}"/>&nbsp;
                <g:submitButton name="next" value="${message(code: 'egaSubmission.sampleInformation.next')}"/>
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
