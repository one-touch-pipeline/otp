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
                            <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                <g:form action="updateProjectRole" params="['userProjectRole.id': userEntry.userProjectRole.id]">
                                    <g:select onchange="submit()"
                                              name="newRole"
                                              from="${availableRoles*.name}"
                                              value="${userEntry.projectRole.name}"/>
                                </g:form>
                            </sec:access>
                            <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                ${userEntry.projectRole.name}
                            </sec:noAccess>
                        </td>
                        <td><img src="${assetPath(src: userEntry.otpAccess.iconName)}" title="${userEntry.otpAccess.description}"></td>
                        <td><img src="${assetPath(src: userEntry.fileAccess.iconName)}" title="${userEntry.fileAccess.description}"></td>
                        <td>
                            <g:if test="${userEntry.manageUsers}">
                                <img src="${assetPath(src: 'ok.png')}" title="${g.message(code: 'projectUser.table.tooltip.true')}">
                            </g:if> <g:else>
                                <img src="${assetPath(src: 'error.png')}" title="${g.message(code: 'projectUser.table.tooltip.false')}">
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
                                <g:if test="${!userEntry.projectRole?.manageUsersAndDelegate}">
                                    <g:form action="updateManageUsers" params='["userProjectRole.id": userEntry.userProjectRole.id, "manageUsers": userEntry.userProjectRole.manageUsers]'>
                                        <g:submitButton name="${userEntry.manageUsers ? "disallow" : "allow"}"/>
                                    </g:form>
                                </g:if>
                            </td>
                        </sec:access>
                        <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                            <td>
                                <g:form action="switchEnabledStatus" params='["userProjectRole.id": userEntry.userProjectRole.id, "enabled": userEntry.userProjectRole.enabled]'>
                                    <g:submitButton name="${g.message(code: 'projectUser.table.deactivateUser')}"/>
                                </g:form>
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
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                        <td>
                            <g:form action="switchEnabledStatus" params='["userProjectRole.id": userEntry.userProjectRole.id, "enabled": userEntry.userProjectRole.enabled]'>
                                <g:submitButton name="${g.message(code: 'projectUser.table.activateUser')}"/>
                            </g:form>
                        </td>
                    </sec:access>
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
                <g:message code="projectUser.addMember.title"/>
            </h3>
            <g:form class="addUserForm" controller="projectUser" action="addUserToProject" params='["project": project.id]'>
                <div class="form left">
                    <table>
                        <sec:access expression="hasRole('ROLE_OPERATOR')">
                            <tr>
                                <td colspan="2">
                                    <label class="methodSelection">
                                        <g:radio name="addViaLdap" value="${true}" checked="true"/>
                                        <g:message code="projectUser.addMember.ldapUser"/>
                                    </label>
                                </td>
                            </tr>
                        </sec:access>
                        <tr>
                            <td><g:message code="projectUser.addMember.user"/></td>
                            <td><input id="search" name="searchString" class="inputField ldapUser autocompleted" autocomplete="off" placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}"></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectUser.addMember.role"/></td>
                            <td>
                                <select name="projectRoleName" class="inputField ldapUser">
                                    <option disabled selected hidden><g:message code="projectUser.addMember.roleSelection"/></option>
                                    <g:each in="${availableRoles}" var="role">
                                        <option value="${role.name}">${role.name}</option>
                                    </g:each>
                                </select>
                            </td>
                        </tr>
                    </table>
                </div>
                <sec:access expression="hasRole('ROLE_OPERATOR')">
                    <div class="form right">
                        <em>Not yet implemented</em> <!-- TODO: remove when implementing OTP-2743 -->
                        <table>
                            <tr>
                                <td colspan="2">
                                    <label class="methodSelection">
                                        <g:radio name="addViaLdap" value="${false}"/>
                                        <g:message code="projectUser.addMember.nonLdapUser"/>
                                    </label>
                                </td>
                            </tr>
                            <tr>
                                <td><g:message code="projectUser.addMember.name"/></td>
                                <td><input name="realName" type="text" class="inputField nonLdapUser" placeholder="${g.message(code: 'projectUser.addMember.realNameDescription')}"></td>
                            </tr>
                            <tr>
                                <td><g:message code="projectUser.addMember.email"/></td>
                                <td><input name="email" type="text" class="inputField nonLdapUser"></td>
                            </tr>
                        </table>
                    </div>
                </sec:access>
                <input type="hidden" name="projectName" value="${project}">
                <g:submitButton class="addButton" name="${g.message(code: 'projectUser.addMember.add')}"/>
            </g:form>

            <h3>
                <g:message code="projectUser.roleExplanation.title"/>
            </h3>
            <table class="projectRoleLegend">
                <tr>
                    <th><g:message code="projectUser.roleExplanation.role"/></th>
                    <g:each in="${["otpAccess", "fileAccess", "userManager"]}" var="property">
                        <th><g:message code="projectUser.roleExplanation.${property}"/></th>
                    </g:each>
                </tr>
                <g:each in="${availableRoles}" var="role">
                    <tr>
                        <td>${role.name}</td>
                        <g:each in="${[role.accessToOtp, role.accessToFiles, role.manageUsersAndDelegate]}" var="property">
                            <td><img src="${assetPath(src: property ? 'ok.png' : 'error.png')}"></td>
                        </g:each>
                    </tr>
                </g:each>
            </table>
        </sec:access>
        <asset:script type="text/javascript">
            $(function() {
                $.otp.projectUser.autocompletion();
                $.otp.projectUser.formSelect();
            });
        </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
