<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
<title><g:message code="otp.menu.createProject"/></title>
</head>
<body>
    <div class="body">
    <g:if test="${hasErrors}">
        <div class="errors"> <li>${message}</li></div>
    </g:if>
    <g:elseif test="${message}">
        <div class="message">${message}</div>
    </g:elseif>
    <g:else>
        <div class="empty"><br></div>
    </g:else>
    <g:form controller="createProject" action="index">
        <table>
            <tr>
                <td class="myKey"><g:message code="createProject.name"/></td>
                <td><g:textField name="name" size="130" value="${cmd.name}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.directory"/></td>
                <td><g:textField name="directory" size="130" value="${cmd.directory}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.metadata.name"/></td>
                <td><g:textField name="nameInMetadataFiles" size="130" value="${cmd.nameInMetadataFiles}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.group"/></td>
                <td><g:select class="criteria" id="group" name='group' from='${groups}' value="${cmd.group}"></g:select></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.copyFiles"/></td>
                <td><g:checkBox name="copyFiles" checked="${cmd == null || cmd.copyFiles}" /></td>
            </tr>
            <tr>
                <td></td>
                <td><g:submitButton name="submit" value="Submit"/></td>
            </tr>
        </table>
    </g:form>
    </div>
</body>
</html>
