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
    <title><g:message code="egaSubmission.newSubmission.title"/></title>
</head>
<body>
    <div class="body">
        <g:link style="float: right" action="helpPage" fragment="new" target="_blank">
            <g:img file="info.png"/>
        </g:link>
        <g:render template="/templates/messages"/>
        <g:uploadForm controller="egaSubmission" action="newSubmissionForm">
            <table>
                <tr>
                    <td class="myKey"><g:message code="egaSubmission.SubmissionProject"/></td>
                    <td><g:hiddenField name="project" value="${selectedProject.id}"/>${selectedProject.name}</td>
                </tr>
                <tr title="<g:message code="egaSubmission.newSubmissionEgaBox.tooltip"/>">
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionEgaBox"/></td>
                    <td><g:textField name="egaBox" size="130" value="${cmd?.egaBox?: ""}"/></td>
                </tr>
                <tr title="<g:message code="egaSubmission.newSubmissionName.tooltip"/>">
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionName"/></td>
                    <td><g:textField name="submissionName" size="130" value="${cmd?.submissionName?: ""}"/></td>
                </tr>
                <tr title="<g:message code="egaSubmission.newSubmissionStudyName.tooltip"/>">
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionStudyName"/></td>
                    <td><g:textField name="studyName" size="130" value="${cmd?.studyName?: ""}"/></td>
                </tr>
                <tr title="<g:message code="egaSubmission.newSubmissionStudyType.tooltip"/>">
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionStudyType"/></td>
                    <td><g:select class="criteria" id="studyType" name='studyType' from='${studyTypes}' value="${cmd?.studyType?: defaultStudyType}"/></td>
                </tr>
                <tr title="<g:message code="egaSubmission.newSubmissionStudyAbstract.tooltip"/>">
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionStudyAbstract"/></td>
                    <td><g:textArea name="studyAbstract" value="${cmd?.studyAbstract}"/></td>
                </tr>
                <tr title="<g:message code="egaSubmission.newSubmissionPubMedId.tooltip"/>">
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionPubMedId"/></td>
                    <td><g:textField name="pubMedId" size="130" value="${cmd?.pubMedId}"/></td>
                </tr>
                <tr>
                    <td></td>
                    <td><g:submitButton name="submit" value="Submit"/></td>
                </tr>
            </table>
        </g:uploadForm>
    </div>
</body>
</html>
