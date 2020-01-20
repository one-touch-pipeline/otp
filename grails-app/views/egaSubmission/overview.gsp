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

<%@ page import="de.dkfz.tbi.otp.administration.DocumentController; de.dkfz.tbi.otp.administration.Document" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.title"/></title>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
    <div class="body">
        <g:if test="${projects}">
            <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
        </g:if>
        <g:link style="float: right" action="helpPage" fragment="overview" target="_blank">
            <g:img file="info.png"/>
        </g:link>
        <div class="buttons">
            <g:link action="newSubmission" params="[id: project.id]"><g:message code="egaSubmission.overview.newSubmission"/></g:link>
        </div>
        <h3><g:message code="egaSubmission.overview.header"/></h3>
        <table>
            <tr>
                <th><g:message code="egaSubmission.overview.submissionID"/></th>
                <th><g:message code="egaSubmission.submissionName"/></th>
                <th><g:message code="egaSubmission.overview.submissionState"/></th>
                <th></th>
            </tr>
            <g:each in="${submissions}">
                <tr>
                    <td>${it.id}</td>
                    <td>${it.studyName}</td>
                    <td><otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            template="dropDown"
                            link="${g.createLink(controller: 'egaSubmission', action: "updateSubmissionState", params: ['submission.id': it.id])}"
                            values="${submissionStates}"
                            value="${it.state}"/></td>
                    <td>
                        <g:if test="${it.state == de.dkfz.tbi.otp.egaSubmission.EgaSubmission.State.SELECTION}">
                            <g:link action="editSubmission" params="['id': it.id]"><g:message code="egaSubmission.overview.continue"/></g:link>
                        </g:if><g:else>
                            <g:link action="sampleMetadata" params="['id': it.id]"><g:message code="egaSubmission.overview.sampleMetadata"/></g:link>
                            <br>
                            <g:link action="experimentalMetadata" params="['id': it.id]"><g:message code="egaSubmission.overview.experimentalMetadata"/></g:link>
                        </g:else>
                    </td>
                </tr>
            </g:each>
        </table>
    </div>
</body>
</html>
