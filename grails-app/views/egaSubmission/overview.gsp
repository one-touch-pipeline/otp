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
        <g:link style="float: right" action="helpPage" target="_blank"><g:message code="egaSubmission.userDocumentation"/></g:link>
        <div class="buttons">
            <g:link action="newSubmission" params="[id: project.id]"><g:message code="egaSubmission.newSubmission"/></g:link>
        </div>
        <h3><g:message code="egaSubmission.submissionHeader"/></h3>
        <table>
            <tr>
                <th><g:message code="egaSubmission.submissionID"/></th>
                <th><g:message code="egaSubmission.submissionName"/></th>
                <th><g:message code="egaSubmission.submissionState"/></th>
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
                            <g:link action="editSubmission" params="['id': it.id]"><g:message code="egaSubmission.submissionEdit"/></g:link>
                        </g:if>
                    </td>
                </tr>
            </g:each>
        </table>
    </div>
</body>
</html>
