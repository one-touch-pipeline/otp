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
    <title><g:message code="egaSubmission.studyMetadata.title"/></title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>

<body>
<div class="body">
    <h1><g:message code="egaSubmission.studyMetadata.header"/></h1>

    <div>
        <table class="key-value-table key-input">
            <tr>
                <td><g:message code="egaSubmission.SubmissionProject"/></td>
                <td>${submission.project.name}</td>
            </tr>
            <tr>
                <td><g:message code="egaSubmission.newSubmission.egaBox"/></td>
                <td>${submission.egaBox}</td>
            </tr>
            <tr>
                <td><g:message code="egaSubmission.submissionName"/></td>
                <td>${submission.submissionName}</td>
            </tr>
            <tr>
                <td><g:message code="egaSubmission.newSubmission.studyName"/></td>
                <td>${submission.studyName}</td>
            </tr>
            <tr>
                <td><g:message code="egaSubmission.newSubmission.studyType"/></td>
                <td>${submission.studyType}</td>
            </tr>
            <tr>
                <td><g:message code="egaSubmission.newSubmission.studyAbstract"/></td>
                <td>${submission.studyAbstract}</td>
            </tr>
            <tr>
                <td><g:message code="egaSubmission.newSubmission.pubMedId"/></td>
                <td>
                    <otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            link="${g.createLink(controller: "egaSubmission", action: 'updatePubMedId', params: ['submission.id': submission.id,])}"
                            value="${submission.pubMedId}"/>
                </td>
            </tr>
        </table>
    </div>
</div>
</body>
</html>
