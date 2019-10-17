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
<%@ page import="de.dkfz.tbi.otp.ngsdata.PermissionStatus" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="projectUser.title" args="[project?.name]"/></title>
    <asset:javascript src="common/UserAutoComplete.js"/>
    <asset:javascript src="pages/projectUser/index/functions.js"/>
    <asset:javascript src="modules/editorSwitch.js"/>
</head>
<body>
    <div class="body">
    <g:if test="${projects}">
        <g:set var="sharesUnixGroup" value="${projectsOfUnixGroup.size() > 1}"/>
        <g:set var="projectsWithSharedUnixGroupListing" value="${projectsOfUnixGroup.sort { it.name }.collect { "\n  - "+it }.join("")}"/>
        <g:set var="confirmationText" value="${sharesUnixGroup ? g.message(code: "projectUser.sharedUnixGroupConfirmation", args: [projectsWithSharedUnixGroupListing]) : ''}"/>
        <g:set var="confirmationTextJS" value="${confirmationText.replaceAll("\n", "\\\\n")}"/>

        <g:render template="/templates/messages"/>

        <div class="project-user-grid-wrapper">
            <div class="grid-element" style="grid-column: 1">
                <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
            </div>
            <div class="grid-element" style="grid-column: 2">
                <g:if test="${sharesUnixGroup}">
                    <span style="white-space: pre-wrap"><g:message code="projectUser.annotation.projectWithSharedUnixGroup" args="[projectsWithSharedUnixGroupListing]"/></span>
                </g:if>
            </div>
        </div>

        <div class="otpDataTables projectUserTable enabled">
            <h3><g:message code="projectUser.activeUsers" args="[project.displayName]"/></h3>
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
                            <g:if test="${userEntry.thumbnailPhoto}">
                                <img class="ldapThumbnail" src="data:image/png;base64,${userEntry.thumbnailPhoto}"/>
                            </g:if>
                        </td>
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
                                        value="${userEntry.projectRoleName}"
                                        confirmation="${confirmationText}"/>
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
                                            link="${g.createLink(controller: "projectUser", action: "setAccessToOtp", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                            value="${userEntry.otpAccess.toBoolean()}"
                                            confirmation="${confirmationText}"/>
                                </sec:access>
                                <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                    <span class="icon-${userEntry.otpAccess}"></span>
                                </sec:noAccess>
                            </td>
                            <td class="small">
                                <div class="filesAccessHandler-${userEntry.fileAccess}">
                                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                        <otp:editorSwitch
                                                template="toggle"
                                                tooltip="${g.message(code: "${userEntry.fileAccess.toolTipKey}")}"
                                                link="${g.createLink(controller: "projectUser", action: "setAccessToFiles", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                                value="${userEntry.fileAccess.toBoolean()}"
                                                confirmation="${confirmationText}"/>
                                    </sec:access>
                                    <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                                        <span></span>
                                    </sec:noAccess>
                                </div>
                            </td>
                            <td class="small">
                                <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'DELEGATE_USER_MANAGEMENT')">
                                    <otp:editorSwitch
                                            template="toggle"
                                            link="${g.createLink(controller: "projectUser", action: "setManageUsers", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                            value="${userEntry.manageUsers.toBoolean()}"
                                            confirmation="${confirmationText}"/>
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
                                            link="${g.createLink(controller: "projectUser", action: "setManageUsersAndDelegate", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                            value="${userEntry.manageUsersAndDelegate.toBoolean()}"
                                            confirmation="${confirmationText}"/>
                                </td>
                            </sec:access>
                        </g:if>
                        <g:else>
                            <td class="small"><span class="icon-${userEntry.otpAccess}"></span></td>
                            <td class="small"><span class="icon-${userEntry.fileAccess}" title="${g.message(code: "${userEntry.fileAccess.toolTipKey}")}"></span></td>
                            <td class="small"><span class="icon-${userEntry.manageUsers}"></span></td>
                            <sec:access expression="hasRole('ROLE_OPERATOR')">
                                <td class="large"><span class="icon-${userEntry.manageUsersAndDelegate}"></span></td>
                            </sec:access>
                        </g:else>
                        <td class="large">
                            <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS') or ${userEntry.user.username == currentUser.username}">
                                <otp:editorSwitch
                                        template="toggle"
                                        link="${g.createLink(controller: "projectUser", action: "setReceivesNotifications", params: ['userProjectRole.id': userEntry.userProjectRole.id] )}"
                                        value="${userEntry.receivesNotifications.toBoolean()}"
                                        confirmation="${confirmationText}"/>
                            </sec:access>
                            <sec:noAccess expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS') or ${userEntry.user.username == currentUser.username}">
                                <span class="icon-${userEntry.receivesNotifications}"></span>
                            </sec:noAccess>
                        </td>
                        <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                            <td>
                                <g:form action="setEnabled" params='["userProjectRole.id": userEntry.userProjectRole.id, "value": false]'>
                                    <input type="submit" value="${g.message(code: 'projectUser.table.deactivateUser')}"
                                           title="${g.message(code: 'projectUser.table.tooltip.activateSwitchButton', args: ["Deactivate"])}"
                                           onclick='return onclickEnableSwitchButton(this, "${confirmationTextJS}")'/>
                                </g:form>
                            </td>
                        </sec:access>
                    </tr>
                </g:each>
            </table>
        </div>
        <sec:access expression="hasRole('ROLE_OPERATOR')">
            <button onclick='getEmails("${project}", "${emails}")' title="${g.message(code: 'projectUser.table.tooltip.copyEmail')}">
                <g:message code="projectUser.table.copyEmail" /></button>
        </sec:access>
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
                        <g:if test="${userEntry.thumbnailPhoto}">
                            <img class="ldapThumbnail" src="data:image/png;base64,${userEntry.thumbnailPhoto}"/>
                        </g:if>
                    </td>
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
                    <td>${userEntry.user.username}</td>
                    <td>${userEntry.department}</td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectUser', action: 'updateEmail', params: ["user.id": userEntry.user.id])}"
                                value="${userEntry.user.email}"/>
                    </td>
                    <td>${userEntry.projectRoleName}</td>
                    <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
                        <td>
                            <g:form action="setEnabled" params='["userProjectRole.id": userEntry.userProjectRole.id]'>
                                <input type="submit" value="${g.message(code: 'projectUser.table.reactivateUser', "value": true)}"
                                       title="${g.message(code: 'projectUser.table.tooltip.activateSwitchButton', args: ["Activate"])}"
                                       onclick='return onclickEnableSwitchButton(this, "${confirmationTextJS}")'/>
                            </g:form>
                        </td>
                    </sec:access>
                </tr>
                </g:each>
            </table>
        </div>
        </sec:access>

        <sec:access expression="hasRole('ROLE_OPERATOR')">
            <g:if test="${usersWithoutUserProjectRole || unknownUsersWithFileAccess}">
                <h3><g:message code="projectUser.additionalUsers.header" args="[project.unixGroup]"/></h3>
                <h4><g:message code="projectUser.additionalUsers.notConnected"/></h4>
                ${usersWithoutUserProjectRole.join(", ") ?: 'None'}

                <h4><g:message code="projectUser.additionalUsers.unregisteredUsers"/></h4>
                ${unknownUsersWithFileAccess.join(", ") ?: 'None'}
            </g:if>
        </sec:access>

        <sec:access expression="hasRole('ROLE_OPERATOR') or hasPermission(${project.id}, 'de.dkfz.tbi.otp.ngsdata.Project', 'MANAGE_USERS')">
            <h3><g:message code="projectUser.addMember.title"/></h3>
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
                            <td><g:message code="projectUser.addMember.username"/></td>
                            <td class="user-auto-complete"><input name="searchString" type="text" class="inputField ldapUser autocompleted" autocomplete="off" placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}"></td>
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
                <div class="addButtonContainer">
                    <input type="hidden" name="projectName" value="${project}">
                    <input type="submit" class="addButton" value="${g.message(code: 'projectUser.addMember.add')}"
                           onclick='return confirmation("${confirmationTextJS}")'/>

                    <span class="legalNotice"><g:message code="projectUser.annotation.legalNotice"/></span>
                </div>
            </g:form>
            <br>
        </sec:access>
        <asset:script type="text/javascript">
            $(function() {
                $.otp.projectUser.formSelect();
            });
        </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
    <script>
        function getEmails(project, emails) {
            prompt("Emails for ${project}", emails);
        }
        function onclickEnableSwitchButton(ref, text) {
            ref.disabled = true;

            if (!confirmation(text)) {
                ref.disabled = false;
                return false;
            }
            return true
        }
        function confirmation(text) {
            if (text) {
                return confirm(text);
            }
            return true
        }
    </script>
</body>
</html>
