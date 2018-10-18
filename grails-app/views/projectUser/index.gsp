<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="de.dkfz.tbi.otp.ngsdata.PermissionStatus" %>
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

        <div class="otpDataTables projectUserTable enabled">
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
                    <th title="${g.message(code: 'projectUser.table.otpAccess.tooltip')}"><g:message code="projectUser.table.otpAccess"/></th>
                    <th title="${g.message(code: 'projectUser.table.fileAccess.tooltip')}"><g:message code="projectUser.table.fileAccess"/></th>
                    <th title="${g.message(code: 'projectUser.table.manageUsers.tooltip')}"><g:message code="projectUser.table.manageUsers"/></th>
                    <sec:access expression="hasRole('ROLE_OPERATOR')">
                        <th title="${g.message(code: 'projectUser.table.manageUsersAndDelegate.tooltip')}"><g:message code="projectUser.table.manageUsersAndDelegate"/></th>
                    </sec:access>
                    <th title="${g.message(code: 'projectUser.table.receivesNotifications.tooltip')}"><g:message code="projectUser.table.receivesNotifications"/></th>
                    <th><g:message code="projectUser.table.asperaAccount"/></th>
                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                        <th><g:message code="projectUser.table.projectAccess"/></th>
                    </sec:access>
                </tr>
                <g:if test="${!enabledProjectUsers}">
                    <tr>
                        <td></td>
                        <td colspan="3"><g:message code="projectUser.table.noUsers"/></td>
                    </tr>
                </g:if>
                <g:each in="${enabledProjectUsers}" var="userEntry">
                    <tr>
                        <td>
                            <img class="ldapThumbnail" src="data:image/png;base64,${userEntry.thumbnailPhoto}" alt=""/>
                        </td>
                        <td>
                            <g:if test="${userEntry.inLdap}">
                                ${userEntry.realName}
                            </g:if>
                            <g:else>
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        link="${g.createLink(controller: 'projectUser', action: 'updateName', params: ["user.id": userEntry.user.id])}"
                                        value="${userEntry.realName}"/>
                            </g:else>
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
                                <otp:editorSwitch
                                        template="dropDown"
                                        link="${g.createLink(controller: "projectUser", action: "updateProjectRole", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                        values="${availableRoles*.name}"
                                        value="${userEntry.projectRoleName}"/>
                            </sec:access>
                            <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                ${userEntry.projectRoleName}
                            </sec:noAccess>
                        </td>
                        <g:if test="${userEntry.inLdap}">
                            <td class="small">
                                <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                    <otp:editorSwitch
                                            template="toggle"
                                            link="${g.createLink(controller: "projectUser", action: "toggleAccessToOtp", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                            value="${userEntry.otpAccess.toBoolean()}"/>
                                </sec:access>
                                <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                    <span class="icon-${userEntry.otpAccess}"></span>
                                </sec:noAccess>
                            </td>
                            <td class="small">
                                <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                    <otp:editorSwitch
                                            template="toggle"
                                            link="${g.createLink(controller: "projectUser", action: "toggleAccessToFiles", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                            value="${userEntry.fileAccess.toBoolean()}"/>
                                </sec:access>
                                <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                    <span class="icon-${userEntry.fileAccess}"></span>
                                </sec:noAccess>
                            </td>
                            <td class="small">
                                <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'DELEGATE_USER_MANAGEMENT')">
                                    <otp:editorSwitch
                                            template="toggle"
                                            link="${g.createLink(controller: "projectUser", action: "toggleManageUsers", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                            value="${userEntry.manageUsers.toBoolean()}"/>
                                </sec:access>
                                <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'DELEGATE_USER_MANAGEMENT')">
                                    <span class="icon-${userEntry.manageUsers}"></span>
                                </sec:noAccess>
                            </td>
                            <sec:access expression="hasRole('ROLE_OPERATOR')">
                                <td class="large">
                                    <otp:editorSwitch
                                            roles="ROLE_OPERATOR"
                                            template="toggle"
                                            link="${g.createLink(controller: "projectUser", action: "toggleManageUsersAndDelegate", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                            value="${userEntry.manageUsersAndDelegate.toBoolean()}"/>
                                </td>
                            </sec:access>
                        </g:if>
                        <g:else>
                            <td class="small"><span class="icon-${userEntry.otpAccess}"></span></td>
                            <td class="small"><span class="icon-${userEntry.fileAccess}"></span></td>
                            <td class="small"><span class="icon-${userEntry.manageUsers}"></span></td>
                            <sec:access expression="hasRole('ROLE_OPERATOR')">
                                <td class="large"><span class="icon-${userEntry.manageUsersAndDelegate}"></span></td>
                            </sec:access>
                        </g:else>
                        <td class="large">
                            <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                <otp:editorSwitch
                                        template="toggle"
                                        link="${g.createLink(controller: "projectUser", action: "toggleReceivesNotifications", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                        value="${userEntry.receivesNotifications.toBoolean()}"/>
                            </sec:access>
                            <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                <span class="icon-${userEntry.receivesNotifications}"></span>
                            </sec:noAccess>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateAspera', params: ["user.id": userEntry.user.id])}"
                                    value="${userEntry.user.asperaAccount}"/>
                        </td>
                        <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                            <td>
                                <g:form action="switchEnabledStatus" params='["userProjectRole.id": userEntry.userProjectRole.id, "enabled": userEntry.userProjectRole.enabled]'>
                                    <g:submitButton
                                            title="${g.message(code: 'projectUser.table.tooltip.activateSwitchButton', args: ["Deactivate"])}"
                                            name="${g.message(code: 'projectUser.table.deactivateUser')}"/>
                                </g:form>
                            </td>
                        </sec:access>
                    </tr>
                </g:each>
            </table>
        </div>

        <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
        <div class="otpDataTables projectUserTable disabled">
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
                    <th><g:message code="projectUser.table.asperaAccount"/></th>
                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                        <th><g:message code="projectUser.table.projectAccess"/></th>
                    </sec:access>
                </tr>
                <g:if test="${!disabledProjectUsers}">
                    <tr>
                        <td></td>
                        <td colspan="3"><g:message code="projectUser.table.noUsers"/></td>
                    </tr>
                </g:if>
                <g:each in="${disabledProjectUsers}" var="userEntry">
                <tr>
                    <td>
                        <img class="ldapThumbnail" src="data:image/png;base64,${userEntry.thumbnailPhoto}" alt=""/>
                    </td>
                    <td>
                        <g:if test="${userEntry.inLdap}">
                            ${userEntry.realName}
                        </g:if>
                        <g:else>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateName', params: ["user.id": userEntry.user.id])}"
                                    value="${userEntry.realName}"/>
                        </g:else>
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
                    <td>${userEntry.projectRoleName}</td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectUser', action: 'updateAspera', params: ["user.id": userEntry.user.id])}"
                                value="${userEntry.user.asperaAccount}"/>
                    </td>
                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                        <td>
                            <g:form action="switchEnabledStatus" params='["userProjectRole.id": userEntry.userProjectRole.id, "enabled": userEntry.userProjectRole.enabled]'>
                                <g:submitButton
                                        title="${g.message(code: 'projectUser.table.tooltip.activateSwitchButton', args: ["Activate"])}"
                                        name="${g.message(code: 'projectUser.table.reactivateUser')}"/>
                            </g:form>
                        </td>
                    </sec:access>
                </tr>
                </g:each>
            </table>
        </div>
        </sec:access>

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
            <g:message code="projectUser.annotation.delayedFileAccessChanges"/>
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
                            <td><input id="search" name="searchString" type="text" class="inputField ldapUser autocompleted" autocomplete="off" placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}"></td>
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
                        <tr>
                            <td><g:message code="projectUser.addMember.accessToFiles"/></td>
                            <td><g:checkBox name="accessToFiles" class="inputField ldapUser" value="true" checked="false"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectUser.addMember.manageUsers"/></td>
                            <td><g:checkBox name="manageUsers" class="inputField ldapUser" value="true" checked="false"/></td>
                        </tr>
                        <sec:access expression="hasRole('ROLE_OPERATOR')">
                            <tr>
                                <td><g:message code="projectUser.addMember.manageUsersAndDelegate"/></td>
                                <td><g:checkBox name="manageUsersAndDelegate" class="inputField ldapUser" value="true" checked="false"/></td>
                            </tr>
                        </sec:access>
                        <tr>
                            <td><g:message code="projectUser.addMember.receivesNotification"/></td>
                            <td><g:checkBox name="receivesNotifications" class="inputField ldapUser" value="true" checked="true"/></td>
                        </tr>
                    </table>
                </div>
                <sec:access expression="hasRole('ROLE_OPERATOR')">
                    <div class="form right">
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
                            <tr>
                                <td><g:message code="projectUser.addMember.role"/></td>
                                <td>
                                    <select name="projectRoleName" class="inputField nonLdapUser">
                                        <option disabled selected hidden><g:message code="projectUser.addMember.roleSelection"/></option>
                                        <g:each in="${availableRoles}" var="role">
                                            <option value="${role.name}">${role.name}</option>
                                        </g:each>
                                    </select>
                                </td>
                            </tr>
                            <tr>
                                <td colspan="2"><g:message code="projectUser.addMember.externalUserRestrictions"/></td>
                            </tr>
                        </table>
                    </div>
                </sec:access>
                <input type="hidden" name="projectName" value="${project}">
                <g:submitButton class="addButton" name="${g.message(code: 'projectUser.addMember.add')}"/>
            </g:form>
            <br>
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
