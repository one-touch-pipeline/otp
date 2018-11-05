<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="egaSubmission.newSubmission.title"/></title>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>
        <g:uploadForm controller="egaSubmission" action="newSubmissionForm">
            <table>
                <tr>
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionProject"/></td>
                    <td><g:hiddenField name="project" value="${selectedProject.id}"/>${selectedProject.name}</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionEgaBox"/></td>
                    <td><g:textField name="egaBox" size="130" value="${cmd?.egaBox?: ""}"/></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionName"/></td>
                    <td><g:textField name="submissionName" size="130" value="${cmd?.submissionName?: ""}"/></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionStudyName"/></td>
                    <td><g:textField name="studyName" size="130" value="${cmd?.studyName?: ""}"/></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionStudyType"/></td>
                    <td><g:select class="criteria" id="studyType" name='studyType' from='${studyTypes}' value="${cmd?.studyType?: defaultStudyType}"/></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="egaSubmission.newSubmissionStudyAbstract"/></td>
                    <td><g:textArea name="studyAbstract" value="${cmd?.studyAbstract}"/></td>
                </tr>
                <tr>
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