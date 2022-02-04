%{--
  - Copyright 2011-2020 The OTP authors
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
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title><g:message code="projectUser.title" args="[selectedProject?.name]"/></title>
    <asset:javascript src="common/UserAutoComplete.js"/>
    <asset:javascript src="pages/projectUser/index/functions.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <g:set var="sharesUnixGroup" value="${projectsOfUnixGroup.size() > 1}"/>
    <g:set var="projectsWithSharedUnixGroupListing" value="${projectsOfUnixGroup.sort { it.name }.collect { "\n  - " + it }.join("")}"/>
    <g:set var="projectsWithSharedUnixGroupListingHtml" value="${projectsOfUnixGroup.sort { it.name }.collect { "- " + it + "<br />" }.join("")}"/>
    <g:set var="confirmationText"
           value="${sharesUnixGroup ? g.message(code: "projectUser.sharedUnixGroupConfirmation", args: [projectsWithSharedUnixGroupListing]) : ''}"/>
    <g:set var="confirmationTextHtml" value="${sharesUnixGroup ? g.message(code: "projectUser.sharedUnixGroupConfirmation",
            args: ["<br /><br />" + projectsWithSharedUnixGroupListingHtml + "<br />" + g.message(code: "projectUser.sharedUnixGroupConfirmation.dialogAcceptQuestion")]) : ''}"/>

    <input type="hidden" name="confirmationTextHtml" value="${confirmationTextHtml}">

    <div class="project-selection-header-container">
        <div class="grid-element">
            <g:render template="/templates/bootstrap/projectSelection"/>
        </div>
        <div class="grid-element">
            <g:if test="${sharesUnixGroup}">
                <otp:annotation type="info">
                    <span style="white-space: pre-wrap"><g:message code="projectUser.annotation.projectWithSharedUnixGroup"
                                                                   args="[projectsWithSharedUnixGroupListing]"/></span>
                </otp:annotation>
            </g:if>
        </div>
    </div>
    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
        <h5><strong><g:message code="projectUser.addMember.action" args="[selectedProject?.name]"/></strong></h5>
        <h6><strong><g:message code="projectUser.addMember.unix" args="[selectedProject?.unixGroup]"/></strong></h6>
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
                <table class="table table-sm table-striped table-hover">
                    <tr>
                        <td><g:message code="projectUser.addMember.username"/></td>
                        <td class="user-auto-complete"><input name="searchString" type="text" class="inputField" autocomplete="off"
                                                              placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}"></td>
                    </tr>
                    <tr>
                        <td><g:message code="projectUser.addMember.role"/></td>
                        <td>
                            <div class="loader"></div>
                            <div class="loaded-content" style="display: none">
                                <g:select id="selectorProjectRoleNamesLdap"
                                          name="projectRoleNames"
                                          class="inputField ldapUser use-select-2"
                                          multiple="true"
                                          value="${selectedProjectRoleNames}"
                                          from="${availableRoles}"
                                          optionKey="name"
                                          optionValue="name"
                                          data-placeholder="${g.message(code: "projectUser.addMember.roleSelection")}"/>
                            </div>
                        </td>
                    </tr>

                    <tr>
                        <td><g:message code="projectUser.addMember.accessToOtp"/></td>
                        <td><g:checkBox name="accessToOtp" onclick ="disableNotification(this)" class="inputField ldapUser" value="true" checked ="true" /></td>
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
                    <table class="table table-sm table-striped table-hover">
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
                                <div class="loaded-content" style="display: none">
                                    <g:select id="selectorProjectRoleNamesNonLdap"
                                              name="projectRoleNames"
                                              class="inputField nonLdapUser use-select-2"
                                              multiple="true"
                                              value="${selectedProjectRoleNames}"
                                              from="${availableRoles}"
                                              optionKey="name"
                                              optionValue="name"
                                              data-placeholder="${g.message(code: "projectUser.addMember.roleSelection")}"/>
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
                <div style="padding-right: 10px;">
                    <input type="submit" class="btn btn-primary" value="${g.message(code: 'projectUser.addMember.action', args: [selectedProject?.name])}"/>
                </div>
                <div>
                    <otp:annotation type="info" variant="inline"><g:message code="projectUser.annotation.legalNotice"/></otp:annotation>
                </div>
            </div>
        </g:form>
    </sec:access>

    <sec:access expression="hasRole('ROLE_OPERATOR')">
        <g:if test="${usersWithoutUserProjectRole || unknownUsersWithFileAccess}">
            <h5><strong><g:message code="projectUser.additionalUsers.header" args="[selectedProject.unixGroup]"/></strong></h5>
            <h6><strong><g:message code="projectUser.additionalUsers.notConnected"/></strong></h6>
            ${usersWithoutUserProjectRole.join(", ") ?: 'None'}

            <h6><strong><g:message code="projectUser.additionalUsers.unregisteredUsers"/></strong></h6>
            ${unknownUsersWithFileAccess.join(", ") ?: 'None'}
        </g:if>
    </sec:access>

    <div class="otpDataTables projectUserTable fixed-table-header">
        <h5><strong><g:message code="projectUser.activeUsers" args="[selectedProject.displayName]"/></strong></h5>
        <table class="table table-sm table-striped table-hover">
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
                        <div class="loaded-content bootstrapped" style="display: none">
                            <sec:access
                                    expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                <g:each in="${userEntry.projectRoleNames}" var="projectRoleName">
                                    <otp:editorSwitch
                                            template="remove"
                                            link="${g.createLink(controller: "projectUser", action: "deleteProjectRole", params: ['userProjectRole.id': userEntry.userProjectRole.id, 'currentRole': projectRoleName])}"
                                            value="${projectRoleName}"
                                            confirmation="${confirmationText}"
                                            name="${projectRoleName}"/>
                                </g:each>
                                <div class="submit-container">
                                    <g:select id="${userEntry.user.id}-selectorNewRoles"
                                              name="newRoles"
                                              class="use-select-2"
                                              multiple="true"
                                              from="${userEntry.availableRoles}"
                                              style="width: 200px"
                                              data-placeholder="${g.message(code: "projectUser.addMember.roleSelection")}"
                                    />
                                    <input type="hidden" name="targetAddRole"
                                           value="${g.createLink(controller: "projectUser", action: "addRoleToUserProjectRole", params: ['userProjectRole.id': userEntry.userProjectRole.id, 'currentRole': null])}"/>
                                    <button class="btn btn-primary addRole js-add" data-confirmation="${confirmationText}"><g:message
                                            code="projectUser.addRoleToUserProjectRole"/></button>
                                </div>
                            </sec:access>
                        </div>
                        <sec:noAccess
                                expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                            <g:each in="${userEntry.projectRoleNames}" var="projectRoleName">
                                ${projectRoleName} <br/>
                            </g:each>
                        </sec:noAccess>
                    </td>
                    <td>
                        <g:if test="${userEntry.inLdap}">
                            <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                            <otp:editorSwitch
                                    template="toggle"
                                    link="${g.createLink(controller: "projectUser", action: "setAccessToOtp", params: ['userProjectRole.id': userEntry.userProjectRole.id])}"
                                    value="${userEntry.otpAccess.toBoolean()}"
                                    confirmation="${confirmationText}"/>
                            </sec:access>
                            <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                <span class="icon-${userEntry.otpAccess}"></span>
                            </sec:noAccess>
                        </g:if>
                        <g:else>
                            <span class="icon-${userEntry.otpAccess}"></span>
                        </g:else>
                    </td>
                    <g:if test="${userEntry.inLdap}">
                        <td class="bootstrapped">
                            <div class="filesAccessHandler-${userEntry.fileAccess}">
                                <sec:access
                                        expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                    <div class="modal-editor-switch">
                                        <div class="modal-editor-switch-editor" style="display: none">
                                            <input type="hidden" name="target"
                                                   value="${g.createLink(controller: "projectUser", action: "setAccessToFiles", params: ['userProjectRole.id': userEntry.userProjectRole.id])}"/>
                                            <input type="hidden" name="hasFileAccess" value="${userEntry.fileAccess.toBoolean()}">
                                            <input type="hidden" name="permissionState" value="${userEntry.fileAccess}">
                                            <span class="icon-${userEntry.fileAccess}"></span><br>
                                            <button class="btn btn-primary" onclick="onToggleAccessToFiles(this)"><g:message code="default.button.toggle.label"/></button><br>
                                            <button class="btn btn-primary" onclick="hideEditorAndShowLabel(this)"><g:message code="default.button.cancel.label"/></button>
                                        </div>

                                        <p class="modal-editor-switch-label" data-toggle="tooltip" data-placement="top"
                                           title="${message(code: userEntry.fileAccess.toolTipKey) ?: ''}">
                                            <span class="icon-${userEntry.fileAccess}"></span>
                                            <button class="btn btn-primary edit" onclick="onFileAccessEditClick(this)">&nbsp;</button>
                                        </p>
                                    </div>
                                </sec:access>
                                <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                    <span></span>
                                </sec:noAccess>
                            </div>
                        </td>
                        <td class="bootstrapped">
                            <sec:access
                                    expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'DELEGATE_USER_MANAGEMENT')">
                                <otp:editorSwitch
                                        template="toggle"
                                        link="${g.createLink(controller: "projectUser", action: "setManageUsers", params: ['userProjectRole.id': userEntry.userProjectRole.id])}"
                                        value="${userEntry.manageUsers.toBoolean()}"
                                        confirmation="${confirmationText}"/>
                            </sec:access>
                            <sec:noAccess
                                    expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'DELEGATE_USER_MANAGEMENT')">
                                <span class="icon-${userEntry.manageUsers}"></span>
                            </sec:noAccess>
                        </td>
                        <sec:access expression="hasRole('ROLE_OPERATOR')">
                            <td class="bootstrapped">
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="toggle"
                                        link="${g.createLink(controller: "projectUser", action: "setManageUsersAndDelegate", params: ['userProjectRole.id': userEntry.userProjectRole.id])}"
                                        value="${userEntry.manageUsersAndDelegate.toBoolean()}"
                                        confirmation="${confirmationText}"/>
                            </td>
                        </sec:access>
                    </g:if>
                    <g:else>
                        <td><span class="icon-${userEntry.fileAccess}" title="${g.message(code: "${userEntry.fileAccess.toolTipKey}")}"></span></td>
                        <td><span class="icon-${userEntry.manageUsers}"></span></td>
                        <sec:access expression="hasRole('ROLE_OPERATOR')">
                            <td><span class="icon-${userEntry.manageUsersAndDelegate}"></span></td>
                        </sec:access>
                    </g:else>
                    <td class="bootstrapped">
                        <sec:access
                                expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS') or ${userEntry.user.username == currentUser.username}">
                            <otp:editorSwitch
                                    template="toggle"
                                    link="${g.createLink(controller: "projectUser", action: "setReceivesNotifications", params: ['userProjectRole.id': userEntry.userProjectRole.id])}"
                                    value="${userEntry.receivesNotifications.toBoolean()}"
                                    confirmation="${confirmationText}"/>
                        </sec:access>
                        <sec:noAccess
                                expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS') or ${userEntry.user.username == currentUser.username}">
                            <span class="icon-${userEntry.receivesNotifications}"></span>
                        </sec:noAccess>
                    </td>
                    <sec:access
                            expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                        <td>
                            <div class="submit-container">
                                <input type="hidden" name="changeProjectAccessButton"
                                       value="${g.createLink(controller: "projectUser", action: "setEnabled", params: ["userProjectRole.id": userEntry.userProjectRole.id, "value": false])}"/>
                                <button class="btn btn-primary changeProjectAccess js-add" data-confirmation="${confirmationTextHtml}"><g:message
                                        code="projectUser.table.deactivateUser"/></button>
                            </div>
                        </td>
                    </sec:access>
                </tr>
            </g:each>
        </table>
        <p><span class="icon-asterisk"></span>${g.message(code: 'projectUser.table.fileAccess.legend')}</p>
    </div>
    <sec:access expression="hasRole('ROLE_OPERATOR')">
        <button class="btn btn-primary" id="listEmails" title="${g.message(code: 'projectUser.table.tooltip.copyEmail')}"
                data-project="${selectedProject?.name}" data-emails="${emails}">
            <g:message code="projectUser.table.copyEmail"/>
        </button>
    </sec:access>
    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
        <div class="otpDataTables projectUserTable">
            <h5><strong><g:message code="projectUser.formerUsers"/></strong></h5>
            <table class="table table-sm table-striped table-hover fixed-table-header">
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
                                ${projectRoles} <br/>
                            </g:each>
                        </td>
                        <sec:access
                                expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                            <td>
                                <div class="submit-container">
                                    <input type="hidden" name="changeProjectAccessButton"
                                           value="${g.createLink(controller: "projectUser", action: "setEnabled", params: ["userProjectRole.id": userEntry.userProjectRole.id, "value": true])}"/>
                                    <button class="btn btn-primary changeProjectAccess js-add" data-confirmation="${confirmationTextHtml}"><g:message
                                            code="projectUser.table.reactivateUser"/></button>
                                </div>
                            </td>
                        </sec:access>
                    </tr>
                </g:each>
            </table>
        </div>
    </sec:access>
</div>
<otp:otpModal modalId="confirmationModal" title="Warning" type="dialog" closeText="Cancel" confirmText="Confirm" closable="false">
</otp:otpModal>
</body>
</html>
