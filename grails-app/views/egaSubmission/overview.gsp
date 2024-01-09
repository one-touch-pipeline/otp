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

<%@ page import="de.dkfz.tbi.otp.egaSubmission.EgaSubmission; de.dkfz.tbi.otp.project.Project" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="egaSubmission.title"/></title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="pages/egaSubmission/datatable.js"/>
</head>

<body>
<div class="body">
    <g:set var="archived" value="${selectedProject.state == Project.State.ARCHIVED ? 'archived' : ''}"/>
    <g:set var="deleted" value="${selectedProject.state == Project.State.DELETED ? 'deleted' : ''}"/>

    <g:render template="/templates/projectSelection"/>
    <g:render template="/templates/bootstrap/noChange" model="[project: selectedProject]"/>

    <g:link style="float: right" action="helpPage" fragment="overview" target="_blank">
        <g:img file="info.png"/>
    </g:link>
    <div class="buttons ${archived} ${deleted}">
        <g:link action="newSubmission"><g:message code="egaSubmission.overview.newSubmission"/></g:link>
    </div>

    <h1><g:message code="egaSubmission.overview.header"/></h1>

    <div class="otpDataTables">
        <table id="dataTable">
            <thead>
            <tr>
                <th><g:message code="egaSubmission.overview.submissionID"/></th>
                <th><g:message code="egaSubmission.submissionName"/></th>
                <th><g:message code="egaSubmission.overview.submissionState"/></th>
                <th></th>
            </tr>
            </thead>
            <tbody>
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
                        <g:if test="${it.state == EgaSubmission.State.SELECTION}">
                            <g:link class="${archived}${deleted}" action="editSubmission" params="['id': it.id]">
                                <g:message code="egaSubmission.overview.continue"/>
                            </g:link>
                        </g:if><g:else>
                        <g:link action="studyMetadata" params="['id': it.id]"><g:message code="egaSubmission.overview.studyMetadata"/></g:link>
                        <br>
                        <g:link action="sampleMetadata" params="['id': it.id]"><g:message code="egaSubmission.overview.sampleMetadata"/></g:link>
                        <br>
                        <g:link action="experimentalMetadata" params="['id': it.id]"><g:message code="egaSubmission.overview.experimentalMetadata"/></g:link>
                    </g:else>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </div>
</div>
<asset:script type="text/javascript">
    $(function() {
        $.otp.egaTable.makeLandingTable();
    });
</asset:script>
</body>
</html>
