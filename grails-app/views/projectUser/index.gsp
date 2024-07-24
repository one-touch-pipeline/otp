%{--
  - Copyright 2011-2024 The OTP authors
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
    <asset:javascript src="pages/projectUser/index/functions.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:stylesheet src="pages/projectUser/index.less"/>
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
            <div class="ldap-user">
                <sec:access
                        expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                    <g:set var="checkboxes" value="['otpAccess', 'fileAccess', 'receivesNotifications']"/>
                    <sec:access
                            expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'DELEGATE_USER_MANAGEMENT')">
                        <g:set var="checkboxes" value="${checkboxes + 'manageUsers'}"/>
                    </sec:access>
                    <sec:access expression="hasRole('ROLE_OPERATOR')">
                        <g:set var="checkboxes" value="${checkboxes + 'manageUsersAndDelegate'}"/>
                    </sec:access>
                    <g:render template="/templates/userFormItem"
                              model="[emptyForm: true, availableRoles: availableRoles, checkboxes: checkboxes]"/>
                </sec:access>
            </div>
            <sec:access expression="hasRole('ROLE_OPERATOR')">
                <div class="form external non-ldap-user">
                    <div class="card">
                        <div class="card-body pb-1">
                            <div class="mb-3 row">
                                <label class="col-sm-2 col-form-label" for="realName">${g.message(code: "projectUser.addMember.name")}</label>

                                <div class="col-sm-10">
                                    <input name="realName" type="text" class="input-field form-control" id="realName"
                                           placeholder="${g.message(code: 'projectUser.addMember.realNameDescription')}">
                                </div>
                            </div>

                            <div class="mb-3 row">
                                <label class="col-sm-2 col-form-label" for="email">${g.message(code: "projectUser.addMember.email")}</label>

                                <div class="col-sm-10">
                                    <input name="email" type="text" id="email" class="input-field form-control">
                                </div>
                            </div>

                            <div class="mb-3 row">
                                <label class="col-sm-2 col-form-label" for="projectRolesRealName">${g.message(code: "projectUser.addMember.role")}</label>

                                <div class="col-sm-10">
                                    <select class="form-control input-field use-select-2" name="projectRoles"
                                            id="projectRolesRealName"
                                            multiple="multiple">
                                        <g:each in="${availableRoles}" var="role">
                                            <option value="${role.id}">${role.name}</option>
                                        </g:each>
                                    </select>
                                </div>

                            </div>

                            <div class="mb-3 row">
                                <div class="col-sm-2"></div>

                                <div class="col-sm-10">
                                    <otp:annotation type="info" variant="inline"><g:message
                                            code="projectUser.addMember.externalUserRestrictions"/></otp:annotation>
                                </div>
                            </div>
                        </div>
                    </div>
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
        <g:if test="${enabledProjectUsers}">
        <table class="table table-sm table-striped table-hover" id="projectMemberTable">
            <g:render template="userListingTableHeaderRow" model="[mode: 'enabled', project: selectedProject, showProjectAccess: currentUser.id in enabledProjectUsers*.user*.id]"/>
            <tbody>
            <g:each in="${enabledProjectUsers}" var="userEntry">
                <tr>
                    <td><g:render template="securedLinkedThumbnail" model="[user: userEntry.user, base64Data: userEntry.thumbnailPhoto]"/></td>
                    <td class="csv_export ${userEntry.deactivated ? "userDisabled" : ""}">
                        <g:if test="${userEntry.inLdap}">
                            ${userEntry.realName}
                        </g:if>
                        <g:else>
                            <otp:editorSwitch roles="ROLE_OPERATOR" value="${userEntry.realName}"
                                              link="${g.createLink(controller: 'projectUser', action: 'updateName', params: ["user.id": userEntry.user.id])}"/>
                        </g:else>
                    </td>
                    <td class="csv_export"><g:render template="securedLinkedUsername" model="[user: userEntry.user]"/></td>
                    <td class="csv_export">${userEntry.department}</td>
                    <td>
                        <otp:editorSwitch roles="ROLE_OPERATOR" value="${userEntry.user.email}"
                                          link="${g.createLink(controller: 'projectUser', action: 'updateEmail', params: ["user.id": userEntry.user.id])}"/>
                    </td>
                    <td>
                        <div class="loader"></div>

                        <div class="loaded-content bootstrapped" style="display: none">
                            <sec:access
                                    expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                <g:each in="${userEntry.projectRoleNames}" var="projectRoleName">
                                    <otp:editorSwitch template="remove" value="${projectRoleName}" confirmation="${confirmationText}" name="${projectRoleName}"
                                                      link="${g.createLink(controller: "projectUser", action: "deleteProjectRole", params: ['userProjectRole.id': userEntry.userProjectRole.id, 'currentRole': projectRoleName])}"/>
                                </g:each>
                                <div class="submit-container">
                                    <g:select id="${userEntry.user.id}-selectorNewRoles"
                                              name="newRoles"
                                              class="use-select-2"
                                              multiple="true"
                                              from="${userEntry.availableRoles}"
                                              style="width: 200px"
                                              data-placeholder="${g.message(code: "projectUser.addMember.roleSelection")}"/>
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
                    <td class="csv_export">${userEntry.user.email}</td>
                    <td class="csv_export">
                        ${userEntry.projectRoleNames.join(',')}
                    </td>
                    <td class="accessToOtp">
                        <g:if test="${userEntry.inLdap}">
                            <sec:access
                                    expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                <otp:editorSwitch
                                        template="toggle"
                                        link="${g.createLink(controller: "projectUser", action: "setAccessToOtp", params: ['userProjectRole.id': userEntry.userProjectRole.id])}"
                                        value="${userEntry.otpAccess.toBoolean()}"
                                        confirmation="${confirmationText}"/>
                            </sec:access>
                            <sec:noAccess
                                    expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                <span class="icon-${userEntry.otpAccess}"></span>
                            </sec:noAccess>
                        </g:if>
                        <g:else>
                            <span class="icon-${userEntry.otpAccess}"></span>
                        </g:else>
                    </td>
                    <g:if test="${userEntry.inLdap}">
                        <td class="bootstrapped accessToFiles">
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
                                            <button class="btn btn-primary" onclick="onToggleAccessToFiles(this)"><g:message
                                                    code="default.button.toggle.label"/></button><br>
                                            <button class="btn btn-primary" onclick="hideEditorAndShowLabel(this)"><g:message
                                                    code="default.button.cancel.label"/></button>
                                        </div>

                                        <p class="modal-editor-switch-label" data-placement="top"
                                           title="${message(code: userEntry.fileAccess.toolTipKey) ?: ''}">
                                            <span class="icon-${userEntry.fileAccess}"></span>
                                            <button class="btn btn-primary edit" onclick="onFileAccessEditClick(this)">&nbsp;</button>
                                        </p>
                                    </div>
                                </sec:access>
                                <sec:noAccess
                                        expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
                                    <span></span>
                                </sec:noAccess>
                            </div>
                        </td>
                        <td class="bootstrapped manageUsers">
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
                            <td class="bootstrapped delegateManagement">
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="toggle"
                                        link="${g.createLink(controller: "projectUser", action: "setManageUsersAndDelegate", params: ['userProjectRole.id': userEntry.userProjectRole.id])}"
                                        value="${userEntry.manageUsersAndDelegate.toBoolean()}"
                                        confirmation="${confirmationText}"/>
                            </td>
                        </sec:access>
                        <sec:noAccess expression="hasRole('ROLE_OPERATOR')">
                            <td><span class="icon-${userEntry.manageUsersAndDelegate}"></span></td>
                        </sec:noAccess>
                    </g:if>
                    <g:else>
                        <td><span class="icon-${userEntry.fileAccess}" title="${g.message(code: "${userEntry.fileAccess.toolTipKey}")}"></span></td>
                        <td><span class="icon-${userEntry.manageUsers}"></span></td>
                        <td><span class="icon-${userEntry.manageUsersAndDelegate}"></span></td>
                    </g:else>
                    <td class="bootstrapped receivesNotifications">
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
                    <td class="csv_export">${userEntry.otpAccess}</td>
                    <td class="csv_export">${userEntry.fileAccess}</td>
                    <td class="csv_export">${userEntry.manageUsers}</td>
                    <td class="csv_export">${userEntry.manageUsersAndDelegate}</td>
                    <td class="csv_export">${userEntry.receivesNotifications}</td>
                    <td>
                        <g:set var="disabled" value="disabled"/>
                        <sec:access
                                expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS') or ${userEntry.user.id == currentUser.id}">
                            <g:set var="disabled" value=""/>
                        </sec:access>
                        <div class="submit-container">
                            <input type="hidden" name="changeProjectAccessButton"
                                   value="${g.createLink(controller: "projectUser", action: "setEnabled", params: ["userProjectRole.id": userEntry.userProjectRole.id, "value": false])}"/>
                            <button class="btn btn-primary changeProjectAccess js-add" ${disabled} data-confirmation="${confirmationTextHtml ?:
                                    g.message(code: "projectUser.deactivateConfirmation", args: [userEntry.user.username, selectedProject.name])}">
                                <g:message code="projectUser.table.deactivateUser"/>
                            </button>
                        </div>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>

        <p><span class="icon-asterisk"></span>${g.message(code: 'projectUser.table.fileAccess.legend')}</p>
        </g:if>
        <g:else>
            <g:message code="projectUser.noUsers"/>
        </g:else>
    </div>
    <sec:access expression="hasRole('ROLE_OPERATOR')">
        <button class="btn btn-primary listEmails" title="${g.message(code: 'projectUser.table.tooltip.copyEmail')}"
                data-project="${selectedProject?.name}" data-emails="${emailsIfEnabled}">
            <g:message code="projectUser.table.copyEmail"/>
        </button>
        <button class="btn btn-primary listEmails" title="${g.message(code: 'projectUser.table.tooltip.copyEmailAll')}"
                data-project="${selectedProject?.name}" data-emails="${emailsIfDisabled}">
            <g:message code="projectUser.table.copyEmailAll"/>
        </button>
    </sec:access>
    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${selectedProject.id}, 'de.dkfz.tbi.otp.project.Project', 'MANAGE_USERS')">
        <div class="otpDataTables projectUserTable" id="formerProjectMemberTable">
            <h5><strong><g:message code="projectUser.formerUsers"/></strong></h5>
            <g:if test="${disabledProjectUsers}">
            <table class="table table-sm table-striped table-hover fixed-table-header">
                <g:render template="userListingTableHeaderRow" model="[mode: 'disabled', project: selectedProject]"/>
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
            </g:if>
            <g:else>
                <g:message code="projectUser.noUsers"/>
            </g:else>
        </div>
    </sec:access>
</div>
<otp:otpModal modalId="confirmationModal" title="Warning" type="dialog" closeText="Cancel" confirmText="Confirm" closable="false">
</otp:otpModal>
</body>
</html>
