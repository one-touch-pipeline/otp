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
        <title><g:message code="user.administration.ui.heading.user"/></title>
        <meta name="layout" content="main" />
        <asset:javascript src="modules/userAdministration.js"/>
    </head>
    <body>
        <div class="body_grow">
            <g:render template="/templates/messages"/>
            <ul>
                <li class="button"><g:link action="index"><g:message code="user.administration.backToOverview"/></g:link></li>
            </ul>
            <h2><g:message code="user.administration.ui.heading.user"/></h2>
            <div>
            <g:form controller="userAdministration" action="editUser" params='["user": user.id]'>
                <table>
                    <thead></thead>
                    <tbody>
                    <tr>
                        <td><g:message code="user.administration.ui.username"/></td>
                        <td><input type="hidden" id="edit-user-username" name="username" value="${user.username}"/>${user.username}</td>
                    </tr>
                    <tr>
                        <td><g:message code="user.administration.ui.email"/></td>
                        <td><span><input type="text" id="edit-user-email" name="email" value="${user.email}"/></span></td>
                    </tr>
                    <tr>
                        <td><g:message code="user.administration.ui.realName"/></td>
                        <td><span><input type="text" id="edit-user-realname" name="realName" value="${user.realName}"/></span></td>
                    </tr>
                    <tr>
                        <td><g:message code="user.administration.ui.asperaAccount"/></td>
                        <td><span><input type="text" id="edit-user-asperaaccount" name="asperaAccount" value="${user.asperaAccount}"/></span></td>
                    </tr>
                    </tbody>
                </table>
                <div class="buttons">
                    <input type="reset" value="${g.message(code: 'user.administration.cancel')}"/>
                    <input type="submit" value="${g.message(code: 'user.administration.save')}"/>
                </div>
            </g:form>
            </div>
            <br>
            <h2><g:message code="user.administration.projectOverview.heading" args="[user.username]"/></h2>
            <table class="otpDataTables projectOverviewTable">
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
                <g:if test="${!userProjectRolesSortedByProjectName}">
                    <tr>
                        <td><g:message code="user.administration.placeholder.none"/></td>
                    </tr>
                </g:if>
                <g:each var="userProjectRole" in="${userProjectRolesSortedByProjectName}">
                    <tr>
                        <td>${userProjectRole.project.name}</td>
                        <td>${userProjectRole.project.unixGroup}</td>
                        <td>${userProjectRole.project.costCenter}</td>
                        <td>${userProjectRole.projectRole.name}</td>
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
            <h2><g:message code="user.administration.ldapGroups.heading" args="[user.username]"/></h2>
            ${ldapGroups.sort().join(", ")}
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
    <asset:script type="text/javascript">
        $(function() {
            $.otp.userAdministration.editUser.register.call($.otp.userAdministration.editUser);
            $.otp.growBodyInit(240);
        });
    </asset:script>
</html>
