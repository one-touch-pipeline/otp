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

<%@ page import="de.dkfz.tbi.otp.project.additionalField.AbstractFieldValue" contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <title><g:message code="user.administration.show.title"/></title>
        <meta name="layout" content="main" />
        <asset:javascript src="pages/userAdministration/show/show.js"/>
    </head>
    <body>
        <div class="body">
            <g:render template="/templates/messages"/>
            <g:render template="detailCommonHeader" model="[user: user, userExistsInLdap: userExistsInLdap]"/>

            <g:form controller="login" action="impersonate" method="POST" params='["username": user.username]'>
                <g:submitButton name="${g.message(code: "user.administration.show.switch")}"/>
            </g:form>

            <g:form controller="userAdministration" action="editUser" params='["user": user.id]'>
                <table class="key-value-table key-input">
                    <thead></thead>
                    <tbody>
                        <tr>
                            <td><g:message code="user.administration.user.fields.username"/>:</td>
                            <td>${user.username}</td>
                        </tr>
                        <tr>
                            <td><g:message code="user.administration.user.fields.realName"/>:</td>
                            <td><input type="text" name="realName" value="${cmd?.realName ?: user.realName}"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="user.administration.user.fields.email"/>:</td>
                            <td><input type="text" name="email" value="${cmd?.email ?: user.email}"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="user.administration.user.fields.plannedDeactivationDate"/>:</td>
                            <td>${user.formattedPlannedDeactivationDate}</td>
                        </tr>
                        <tr>
                            <td></td>
                            <td><g:submitButton name="${g.message(code: "user.administration.show.update")}"/></td>
                        </tr>
                    </tbody>
                </table>
            </g:form>
            <br>
            <h2><g:message code="user.administration.projectOverview.heading" args="[user.username]"/></h2>
            <table class="otpDataTables">
                <tr>
                    <th><g:message code="user.administration.projectOverview.projectName"/></th>
                    <th><g:message code="user.administration.projectOverview.unixGroup"/></th>
                    <th><g:message code="user.administration.projectOverview.department"/></th>
                    <th><g:message code="user.administration.projectOverview.role"/></th>
                    <th><g:message code="user.administration.projectOverview.otpAccess"/></th>
                    <th><g:message code="user.administration.projectOverview.fileAccess"/></th>
                    <th><g:message code="user.administration.projectOverview.manageUsers"/></th>
                    <th><g:message code="user.administration.projectOverview.manageUsersAndDelegate"/></th>
                    <th><g:message code="user.administration.projectOverview.receivesNotifications"/></th>
                    <th><g:message code="user.administration.projectOverview.enabled"/></th>
                </tr>
                <g:if test="${!userProjectRoles}">
                    <tr>
                        <td><g:message code="user.administration.placeholder.none"/></td>
                    </tr>
                </g:if>
                <g:each var="userProjectRole" in="${userProjectRoles}">
                    <tr>
                        <td>
                            <g:link controller="projectUser" action="index" params="[(projectParameter): userProjectRole.project.name]">
                                ${userProjectRole.project.name}
                            </g:link>
                        </td>
                        <td>${userProjectRole.project.unixGroup}</td>
                        <td>${userProjectRole.project.projectFields?.find { it.definition.name == 'Cost Center'}?.displayValue ?: "Not Set"}</td>
                        <td>
                            <g:each var="projectRoles" in="${userProjectRole.projectRoles}">
                                ${projectRoles.name} <br />
                            </g:each>
                        </td>
                        <td><span class="icon-${userProjectRole.accessToOtp}"></span></td>
                        <td><span class="icon-${userProjectRole.accessToFiles}"></span></td>
                        <td><span class="icon-${userProjectRole.manageUsers}"></span></td>
                        <td><span class="icon-${userProjectRole.manageUsersAndDelegate}"></span></td>
                        <td><span class="icon-${userProjectRole.receivesNotifications}"></span></td>
                        <td><span class="icon-${userProjectRole.enabled}"></span></td>
                    </tr>
                </g:each>
            </table>
            <br>
            <h2><g:message code="user.administration.groups.heading" args="[user.username]"/></h2>
            ${ldapGroups.sort().join(", ")}
            <br>
            <h2><g:message code="user.administration.groups.userAccountControl" args="[userAccountControlValue]"/></h2>
            <table class="key-value-table key-input user-account-control-table">
                <thead>
                    <tr>
                        <th><g:message code="user.administration.groups.userAccountControl.field"/></th>
                        <th><g:message code="user.administration.groups.userAccountControl.value"/></th>
                    </tr>
                </thead>
                <tbody>
                    <g:each in="${userAccountControlMap.sort()}" var="entry">
                        <tr>
                            <td>${entry.key}</td>
                            <td><span style="${entry.value ? "font-weight: bold" : ""}">${entry.value}</span></td>
                        </tr>
                    </g:each>
                </tbody>
            </table>
            <br>
            <g:each var="type" in="${["Group", "Role"]}">
                <h3 id="${type}_anchor"><g:message code="user.administration.role.heading.manage${type}s" args="[user.username]"/></h3>
                <g:each var="status" in="${["user", "available"]}">
                <div id="${status}${type}">
                    <h4><g:message code="user.administration.role.heading.${status}${type}s"/></h4>
                    <table>
                        <tbody>
                        <g:each var="role" in="${roleLists["${status}${type}"]}">
                            <tr>
                                <td style="width: 20%">${role.authority}</td>
                                <td>
                                    <g:if test="${status == "available"}">
                                        <a href="#${type}_anchor" class="add" onclick="$.otp.userAdministration.editUser.addOrRemoveUserRole(this, ${user.id}, ${role.id}, '${type}')">
                                        <g:message code="user.administration.role.add${type}"/>
                                    </g:if>
                                    <g:else>
                                        <a href="#${type}_anchor" class="remove" onclick="$.otp.userAdministration.editUser.addOrRemoveUserRole(this, ${user.id}, ${role.id}, '${type}')">
                                        <g:message code="user.administration.role.remove${type}"/>
                                    </g:else>
                                    </a>
                                </td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
                </g:each>
                <br>
            </g:each>
        </div>
    </body>
</html>
