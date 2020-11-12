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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="projectUser.title" args="[selectedProject?.name]"/></title>
    <asset:javascript src="common/UserAutoComplete.js"/>
    <asset:javascript src="pages/projectUser/index/functions.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>
<body>
<div class="body">
    <g:set var="sharesUnixGroup" value="${projectsOfUnixGroup.size() > 1}"/>
    <g:set var="projectsWithSharedUnixGroupListing" value="${projectsOfUnixGroup.sort { it.name }.collect { "\n  - "+it }.join("")}"/>
    <g:set var="confirmationText" value="${sharesUnixGroup ? g.message(code: "projectUser.sharedUnixGroupConfirmation", args: [projectsWithSharedUnixGroupListing]) : ''}"/>

    <g:render template="/templates/messages"/>

    <div class="project-selection-header-container">
        <div class="grid-element">
            <g:render template="/templates/projectSelection"/>
        </div>
        <div class="grid-element">
            <g:if test="${sharesUnixGroup}">
                <otp:annotation type="info">
                    <span style="white-space: pre-wrap"><g:message code="projectUser.annotation.projectWithSharedUnixGroup" args="[projectsWithSharedUnixGroupListing]"/></span>
                </otp:annotation>
            </g:if>
        </div>
    </div>
    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
        <h2><g:message code="projectUser.addMember.action" args="[selectedProject?.name]"/></h2>
        <otp:annotation type="info"><g:message code="projectUser.annotation.delayedFileAccessChanges"/></otp:annotation>
        <g:form class="add-user-grid-wrapper" controller="projectUser" action="addUserToProject">
            <sec:access expression="hasRole('ROLE_OPERATOR')">
                <label class="select internal">
                    <g:radio name="addViaLdap" value="true" checked="true"/>
                    <g:message code="projectUser.addMember.ldapUser"/>
                </label>
                <label class="select external">
                    <g:radio name="addViaLdap" value="false"/>
                    <g:message code="projectUser.addMember.nonLdapUser"/>
                </label>
            </sec:access>
            <div class="form internal">
                <table>
                    <tr>
                        <td><g:message code="projectUser.addMember.username"/></td>
                        <td class="user-auto-complete"><input name="searchString" type="text" class="inputField ldapUser autocompleted" autocomplete="off" placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}"></td>
                    </tr>
                    <tr>
                        <td><g:message code="projectUser.addMember.role"/></td>
                        <td>
                            <div class="loader"></div>
                            <div class="loaded-content" hidden>
                                <select multiple name="projectRoleNames" class="inputField ldapUser use-select-2">
                                    <g:each in="${availableRoles}" var="role">
                                        <option value="${role.name}">${role.name}</option>
                                    </g:each>
                                </select>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="projectUser.addMember.accessToFiles"/></td>
                        <td><g:checkBox name="accessToFiles" class="inputField ldapUser" value="true" checked="false"/></td>
                    </tr>
                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'DELEGATE_USER_MANAGEMENT')">
                        <tr>
                            <td><g:message code="projectUser.addMember.manageUsers"/></td>
                            <td><g:checkBox name="manageUsers" class="inputField ldapUser" value="true" checked="false"/></td>
                        </tr>
                    </sec:access>
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
                <div class="form external">
                    <table>
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
                                <div class="loader"></div>
                                <div class="loaded-content" hidden>
                                    <select multiple name="projectRoleNames" class="inputField nonLdapUser use-select-2">
                                        <g:each in="${availableRoles}" var="role">
                                            <option value="${role.name}">${role.name}</option>
                                        </g:each>
                                    </select>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2"><otp:annotation type="info" variant="inline"><g:message code="projectUser.addMember.externalUserRestrictions"/></otp:annotation></td>
                        </tr>
                    </table>
                </div>
            </sec:access>
            <div class="submit-container">
                <div>
                    <input id="add-button" type="submit" value="${g.message(code: 'projectUser.addMember.action', args: [selectedProject?.name])}"
                           data-text="${confirmationText}"/>
                </div>
                <div>
                    <otp:annotation type="info" variant="inline"><g:message code="projectUser.annotation.legalNotice"/></otp:annotation>
                </div>
            </div>
        </g:form>
    </sec:access>

    <sec:access expression="hasRole('ROLE_OPERATOR')">
        <g:if test="${usersWithoutUserProjectRole || unknownUsersWithFileAccess}">
            <h2><g:message code="projectUser.additionalUsers.header" args="[selectedProject.unixGroup]"/></h2>
            <h3><g:message code="projectUser.additionalUsers.notConnected"/></h3>
            ${usersWithoutUserProjectRole.join(", ") ?: 'None'}

            <h3><g:message code="projectUser.additionalUsers.unregisteredUsers"/></h3>
            ${unknownUsersWithFileAccess.join(", ") ?: 'None'}
        </g:if>
    </sec:access>

    <div class="otpDataTables projectUserTable fixed-table-header">
        <h2><g:message code="projectUser.activeUsers" args="[selectedProject.displayName]"/></h2>
        <table>
            <g:render template="userListingTableHeaderRow" model="[mode: 'enabled', project: selectedProject]"/>
            <g:if test="${!enabledProjectUsers}">
                <g:render template="noUsersTableRow"/>
            </g:if>
            <g:each in="${enabledProjectUsers}" var="userEntry">
                <tr>
                    <td><g:render template="securedLinkedThumbnail" model="[user: userEntry.user, base64Data: userEntry.thumbnailPhoto]"/></td>
                    <td class="${userEntry.deactivated ? "userDisabled" : ""}">
                        <g:if test="${userEntry.inLdap}">
                            ${userEntry.realName}
                        </g:if>
                        <g:else>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateName', params: ["user.id": userEntry.user.id])}"
                                    value="${userEntry.realName}"/>
                        </g:else>
                    </td>
                    <td><g:render template="securedLinkedUsername" model="[user: userEntry.user]"/></td>
                    <td>${userEntry.department}</td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectUser', action: 'updateEmail', params: ["user.id": userEntry.user.id])}"
                                value="${userEntry.user.email}"/>
                    </td>
                    <td>
                        <div class="loader"></div>
                        <div class="loaded-content" hidden>
                            <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                <g:each in="${userEntry.projectRoleNames}" var="projectRoleName">
                                    <otp:editorSwitch
                                            template="remove"
                                            link="${g.createLink(controller: "projectUser", action: "deleteProjectRole", params: ['userProjectRole.id': userEntry.userProjectRole.id, 'currentRole': projectRoleName] )}"
                                            value="${projectRoleName}"
                                            confirmation="${confirmationText}"
                                            name="${projectRoleName}"/>
                                </g:each>
                                <div class="submit-container">
                                    <select multiple name="newRoles" class="use-select-2" style="width: 200px">
                                        <g:each in="${userEntry.availableRoles}" var="role">
                                            <option value="${role}">${role}</option>
                                        </g:each>
                                    </select>
                                    <input type="hidden" name="targetAddRole" value="${g.createLink(controller: "projectUser", action: "addRoleToUserProjectRole", params: ['userProjectRole.id': userEntry.userProjectRole.id, 'currentRole': null] )}"/>
                                    <button class="addRole js-add" data-confirmation="${confirmationText}"><g:message code="projectUser.addRoleToUserProjectRole"/></button>
                                </div>
                            </sec:access>
                        </div>
                        <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                            <g:each in="${userEntry.projectRoleNames}" var="projectRoleName">
                                    ${projectRoleName} <br />
                            </g:each>
                        </sec:noAccess>
                    </td>
                    <g:if test="${userEntry.inLdap}">
                        <td>
                            <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                <otp:editorSwitch
                                        template="toggle"
                                        link="${g.createLink(controller: "projectUser", action: "setAccessToOtp", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                        value="${userEntry.otpAccess.toBoolean()}"
                                        confirmation="${confirmationText}"/>
                            </sec:access>
                            <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                <span class="icon-${userEntry.otpAccess}"></span>
                            </sec:noAccess>
                        </td>
                        <td>
                            <div class="filesAccessHandler-${userEntry.fileAccess}">
                                <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                    <otp:editorSwitch
                                            template="toggle"
                                            tooltip="${g.message(code: "${userEntry.fileAccess.toolTipKey}")}"
                                            link="${g.createLink(controller: "projectUser", action: "setAccessToFiles", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                            value="${userEntry.fileAccess.toBoolean()}"
                                            confirmation="${confirmationText}"
                                            pageReload="${userEntry.fileAccess.inconsistency.toString()}"
                                    />
                                </sec:access>
                                <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                    <span></span>
                                </sec:noAccess>
                            </div>
                        </td>
                        <td>
                            <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'DELEGATE_USER_MANAGEMENT')">
                                <otp:editorSwitch
                                        template="toggle"
                                        link="${g.createLink(controller: "projectUser", action: "setManageUsers", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                        value="${userEntry.manageUsers.toBoolean()}"
                                        confirmation="${confirmationText}"/>
                            </sec:access>
                            <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'DELEGATE_USER_MANAGEMENT')">
                                <span class="icon-${userEntry.manageUsers}"></span>
                            </sec:noAccess>
                        </td>
                        <sec:access expression="hasRole('ROLE_OPERATOR')">
                            <td>
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="toggle"
                                        link="${g.createLink(controller: "projectUser", action: "setManageUsersAndDelegate", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                        value="${userEntry.manageUsersAndDelegate.toBoolean()}"
                                        confirmation="${confirmationText}"/>
                            </td>
                        </sec:access>
                    </g:if>
                    <g:else>
                        <td><span class="icon-${userEntry.otpAccess}"></span></td>
                        <td><span class="icon-${userEntry.fileAccess}" title="${g.message(code: "${userEntry.fileAccess.toolTipKey}")}"></span></td>
                        <td><span class="icon-${userEntry.manageUsers}"></span></td>
                        <sec:access expression="hasRole('ROLE_OPERATOR')">
                            <td><span class="icon-${userEntry.manageUsersAndDelegate}"></span></td>
                        </sec:access>
                    </g:else>
                    <td>
                        <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS') or ${userEntry.user.username == currentUser.username}">
                            <otp:editorSwitch
                                    template="toggle"
                                    link="${g.createLink(controller: "projectUser", action: "setReceivesNotifications", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                    value="${userEntry.receivesNotifications.toBoolean()}"
                                    confirmation="${confirmationText}"/>
                        </sec:access>
                        <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS') or ${userEntry.user.username == currentUser.username}">
                            <span class="icon-${userEntry.receivesNotifications}"></span>
                        </sec:noAccess>
                    </td>
                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                        <td>
                            <div class="submit-container">
                                <input type="hidden" name="changeProjectAccessButton" value="${g.createLink(controller: "projectUser", action: "setEnabled", params: ["userProjectRole.id": userEntry.userProjectRole.id, "value": false] )}"/>
                                <button class="changeProjectAccess js-add" data-confirmation="${confirmationText}"><g:message code="projectUser.table.deactivateUser"/></button>
                            </div>
                        </td>
                    </sec:access>
                </tr>
            </g:each>
        </table>
    </div>
    <sec:access expression="hasRole('ROLE_OPERATOR')">
        <button id="listEmails" title="${g.message(code: 'projectUser.table.tooltip.copyEmail')}"
                data-project="${selectedProject?.name}" data-emails="${emails}">
            <g:message code="projectUser.table.copyEmail" />
        </button>
    </sec:access>
    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
        <div class="otpDataTables projectUserTable">
            <h2><g:message code="projectUser.formerUsers"/></h2>
            <table class="fixed-table-header">
                <g:render template="userListingTableHeaderRow" model="[mode: 'disabled', project: selectedProject]"/>
                <g:if test="${!disabledProjectUsers}">
                    <g:render template="noUsersTableRow"/>
                </g:if>
                <g:each in="${disabledProjectUsers}" var="userEntry">
                    <tr>
                        <td><g:render template="securedLinkedThumbnail" model="[user: userEntry.user, base64Data: userEntry.thumbnailPhoto]"/></td>
                        <td class="${userEntry.deactivated ? "userDisabled" : ""}">
                            <g:if test="${userEntry.inLdap}">
                                ${userEntry.realName}
                            </g:if>
                            <g:else>
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        link="${g.createLink(controller: 'projectUser', action: 'updateName', params: ["user.id": userEntry.user.id])}"
                                        value="${userEntry.realName}"/>
                            </g:else>
                        </td>
                        <td><g:render template="securedLinkedUsername" model="[user: userEntry.user]"/></td>
                        <td>${userEntry.department}</td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectUser', action: 'updateEmail', params: ["user.id": userEntry.user.id])}"
                                    value="${userEntry.user.email}"/>
                        </td>
                        <td>
                            <g:each var="projectRoles" in="${userEntry.projectRoleNames}">
                                ${projectRoles} <br />
                            </g:each>
                        </td>
                        <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                            <td>
                                <div class="submit-container">
                                    <input type="hidden" name="changeProjectAccessButton" value="${g.createLink(controller: "projectUser", action: "setEnabled", params: ["userProjectRole.id": userEntry.userProjectRole.id, "value": true] )}"/>
                                    <button class="changeProjectAccess js-add" data-confirmation="${confirmationText}"><g:message code="projectUser.table.reactivateUser"/></button>
                                </div>
                            </td>
                        </sec:access>
                    </tr>
                </g:each>
            </table>
        </div>
    </sec:access>
</div>
</body>
</html>
