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
        <g:render template="/templates/messages"/>

        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />

        <div class="otpDataTables projectUserTable">
            <h3>
                <g:message code="projectUser.activeUsers" args="[project.name]"/>
            </h3>
            <table>
                <tr>
                    <th></th>
                    <th><g:message code="projectUser.table.name"/></th>
                    <th><g:message code="projectUser.table.username"/></th>
                    <th><g:message code="projectUser.table.department"/></th>
                    <th><g:message code="projectUser.table.mail"/></th>
                    <th><g:message code="projectUser.table.role"/></th>
                    <th><g:message code="projectUser.table.otpAccess"/></th>
                    <th><g:message code="projectUser.table.fileAccess"/></th>
                    <th><g:message code="projectUser.table.manageUsers"/></th>
                    <th><g:message code="projectUser.table.asperaAccount"/></th>
                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'DELEGATE_USER_MANAGEMENT')">
                        <th><g:message code="projectUser.table.manageUsers"/></th>
                    </sec:access>
                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                        <th><g:message code="projectUser.table.projectAccess"/></th>
                    </sec:access>
                </tr>
                <g:each in="${enabledProjectUsers}" var="userEntry">
                    <tr>
                        <td>
                            <img class="ldapThumbnail" src="data:image/png;base64,${userEntry.thumbnailPhoto}" alt=""/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateName', params: ["user.id": userEntry.user.id])}"
                                    value="${userEntry.user.realName}"/>
                            <g:if test="${userEntry.deactivated}">
                                <g:message code="projectUser.accessPerson.deactivated"/>
                            </g:if>
                        </td>
                        <td>${userEntry.user.username}</td>
                        <td>${userEntry.department}</td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateEmail', params: ["user.id": userEntry.user.id])}"
                                    value="${userEntry.user.email}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="dropDown"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateProjectRole', params: ["userProjectRole.id": userEntry.userProjectRole.id])}"
                                    value="${userEntry.projectRole?.name ?: ''}"
                                    values="${availableRoles}"/>
                        </td>
                        <td><img src="${assetPath(src: userEntry.otpAccess.iconName)}" title="${userEntry.otpAccess.description}"></td>
                        <td><img src="${assetPath(src: userEntry.fileAccess.iconName)}" title="${userEntry.fileAccess.description}"></td>
                        <td>
                            <g:if test="${userEntry.manageUsers}">
                                <img src="${assetPath(src: 'ok.png')}" title="is allowed">
                            </g:if> <g:else>
                                <img src="${assetPath(src: 'error.png')}" title="is not allowed">
                            </g:else>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateAspera', params: ["user.id": userEntry.user.id])}"
                                    value="${userEntry.user.asperaAccount}"/>
                        </td>
                        <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'DELEGATE_USER_MANAGEMENT')">
                            <td>
                                <g:if test="${userEntry.userProjectRole.projectRole != null && !userEntry.projectRole?.manageUsersAndDelegate}">
                                    <g:form action="updateManageUsers" params='["userProjectRole.id": userEntry.userProjectRole.id, "manageUsers": userEntry.userProjectRole.manageUsers]'>
                                        <g:submitButton name="${userEntry.manageUsers ? "disallow" : "allow"}"/>
                                    </g:form>
                                </g:if>
                            </td>
                        </sec:access>
                        <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                            <td>
                                <input type="button" class="deleteButton" value="deny" data-id="${userEntry.userProjectRole.id}"/>
                            </td>
                        </sec:access>
                    </tr>
                </g:each>
            </table>
        </div>

        <div class="otpDataTables projectUserTable">
            <h3>
                <g:message code="projectUser.formerUsers"/>
            </h3>
            <table>
                <tr>
                    <th></th>
                    <th><g:message code="projectUser.table.name"/></th>
                    <th><g:message code="projectUser.table.username"/></th>
                    <th><g:message code="projectUser.table.department"/></th>
                    <th><g:message code="projectUser.table.mail"/></th>
                    <th><g:message code="projectUser.table.role"/></th>
                    <th><g:message code="projectUser.table.otpAccess"/></th>
                    <th><g:message code="projectUser.table.fileAccess"/></th>
                    <th><g:message code="projectUser.table.manageUsers"/></th>
                    <th><g:message code="projectUser.table.asperaAccount"/></th>
                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                        <th><g:message code="projectUser.table.projectAccess"/></th>
                    </sec:access>
                </tr>
                <g:each in="${disabledProjectUsers}" var="userEntry">
                <tr>
                    <td>${userEntry.user.username}</td>
                </tr>
                </g:each>
            </table>
        </div>

        <sec:access expression="hasRole('ROLE_OPERATOR')">
        <h3>
            <g:message code="projectUser.notConnected"/>
        </h3>
        ${usersWithoutUserProjectRole.join(", ") ?: 'None'}

        <h3>
            <g:message code="projectUser.unknownUsers"/>
        </h3>
        ${unknownUsersWithFileAccess.join(", ") ?: 'None'}
        </sec:access>

        <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
            <h3>
                <g:message code="projectUser.addMember"/>
            </h3>
        </sec:access>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
