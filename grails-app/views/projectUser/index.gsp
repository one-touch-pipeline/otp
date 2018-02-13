<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="projectUser.title" args="[project?.name]"/></title>
    <asset:javascript src="pages/projectUser/index/functions.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
    <div class="body">
    <g:if test="${projects}">
        <g:if test="${hasErrors == true}">
            <div class="errors"> <li>${message}</li></div>
        </g:if>
        <g:elseif test="${message}">
            <div class="message">${message}</div>
        </g:elseif>

        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />

        <div class="otpDataTables">
            <h3>
                <g:message code="projectOverview.contactPerson.headline" />
            </h3>
            <table>
                <tr>
                    <th><g:message code="projectOverview.contactPerson.name"/></th>
                    <th><g:message code="projectOverview.contactPerson.email"/></th>
                    <th><g:message code="projectOverview.contactPerson.aspera"/></th>
                    <th><g:message code="projectOverview.contactPerson.role"/></th>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <th></th>
                    </sec:ifAllGranted>
                </tr>
                <g:each in="${projectContactPersons}" var="projectContactPerson">
                    <tr>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateName', params: ["user.id": projectContactPerson.user.id])}"
                                    value="${projectContactPerson.user.realName}"
                                    values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateEmail', params: ["user.id": projectContactPerson.user.id])}"
                                    value="${projectContactPerson.user.email}"
                                    values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateAspera', params: ["user.id": projectContactPerson.user.id])}"
                                    value="${projectContactPerson.user.asperaAccount}"
                                    values="${projectContactPersons}"/>
                        </td>
                        <td>
                            ${projectContactPerson.contactPersonRole?.name ?: ''}
                        </td>
                    </tr>
                </g:each>
            </table>
        </div>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <br>
            <div>
                <h3><g:message code="projectOverview.accessPerson.headline" /></h3>
                <a id="toggleLink" href="javascript:void(0)" onclick="$.otp.projectUser.toggle('controlElement', 'toggleLink')">Show list</a>
                <ul id="controlElement" style="display: none">
                    <g:each in="${accessPersons}">
                        <li>
                            ${it}
                        </li>
                    </g:each>
                </ul>
            </div>
        </sec:ifAllGranted>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
