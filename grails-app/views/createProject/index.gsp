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
                <td class="myKey"><g:message code="createProject.analysisDirectory"/></td>
                <td><g:textField name="analysisDirectory" size="130" value="${cmd.analysisDirectory}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.processingPriority"/></td>
                <td><g:select class="criteria" id="priority" name='processingPriority' from='${processingPriorities}' value="${cmd.priority}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.metadata.name"/></td>
                <td><g:textField name="nameInMetadataFiles" size="130" value="${cmd.nameInMetadataFiles}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.unixGroup"/></td>
                <td><g:textField name="unixGroup" size="130" value="${cmd.unixGroup}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.mailingListName"/></td>
                <td><g:textField name="mailingListName" size="130" value="${cmd.mailingListName}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.costCenter"/></td>
                <td><g:textField name="costCenter" size="130" value="${cmd.costCenter}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.projectGroup"/></td>
                <td><g:select class="criteria" id="group" name='projectGroup' from='${projectGroups}' value="${cmd.projectGroup}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.tumorEntity"/></td>
                <td><g:select class="criteria" name='tumorName' from='${tumorEntities}' value="${cmd.tumorEntity}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.category"/></td>
                <td>
                    <g:each in="${projectCategories}" var="category">
                        <g:checkBox name="projectCategories" value="${category}" id="category-${category}" checked="false" /><label for="category-${category}">${category}</label>
                    </g:each>
                </td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.copyFiles"/></td>
                <td><g:checkBox name="copyFiles" checked="${cmd == null || cmd.copyFiles}" /></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.fingerPrinting"/></td>
                <td><g:checkBox name="fingerPrinting" checked="${cmd == null ? true : cmd.fingerPrinting}" /></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.description"/></td>
                <td><g:textArea name="description" value="${cmd.description}"/></td>
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
