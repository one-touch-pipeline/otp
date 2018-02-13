<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <title><g:message code="user.administration.ui.heading.user"/></title>
        <meta name="layout" content="main" />
        <asset:javascript src="modules/userAdministration"/>
    </head>
    <body>
        <div class="body_grow">
            <div id="create-group-dialog" style="display: none" title="${g.message(code: 'group.create.ui.dialog.title') }">
                <form>
                <table>
                    <tbody>
                        <tr>
                            <td><label for="create-group-name"><g:message code="group.create.ui.name"/></label></td>
                            <td><input type="text" name="name" id="create-group-name"/></td>
                        </tr>
                        <tr>
                            <td><label for="create-group-description"><g:message code="group.create.ui.description"/></label></td>
                            <td><input type="text" name="description" id="create-group-description"/></td>
                        </tr>
                    </tbody>
                </table>
                <table>
                    <thead>
                        <tr>
                            <th>&nbsp;</th>
                            <th><g:message code="group.create.ui.permission.read"/></th>
                            <th><g:message code="group.create.ui.permission.write"/></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <th><g:message code="group.create.ui.project"/></th>
                            <td><input type="checkbox" name="readProject"/></td>
                            <td><input type="checkbox" name="writeProject"/></td>
                        </tr>
                        <tr>
                            <th><g:message code="group.create.ui.jobSystem"/></th>
                            <td><input type="checkbox" name="readJobSystem"/></td>
                            <td><input type="checkbox" name="writeJobSystem"/></td>
                        </tr>
                        <tr>
                            <th><g:message code="group.create.ui.seqeuenceCenter"/></th>
                            <td><input type="checkbox" name="readSequenceCenter"/></td>
                            <td><input type="checkbox" name="writeSequenceCenter"/></td>
                        </tr>
                    </tbody>
                </table>
                </form>
            </div>
            <ul>
                <li class="button"><g:link action="index"><g:message code="user.administration.backToOverview"/></g:link></li>
            </ul>
            <h1><g:message code="user.administration.ui.heading.user"/></h1>
            <div>
            <form id="edit-user-form" method="POST">
                <input type="hidden" id="edit-user-userid" name="userid" value="${user.id}"/>
                <table>
                    <thead></thead>
                    <tbody>
                    <tr>
                        <td><label for="edit-user-username"><g:message code="user.administration.ui.username"/>:</label></td>
                        <td><input type="hidden" id="edit-user-username" name="username" value="${user.username}"/>${user.username}</td>
                    </tr>
                    <tr>
                        <td><label for="edit-user-email"><g:message code="user.administration.ui.email"/>:</label></td>
                        <td><span><input type="text" id="edit-user-email" name="email" value="${user.email}"/></span></td>
                    </tr>
                    <tr>
                        <td><label for="edit-user-realname"><g:message code="user.administration.ui.realName"/>:</label></td>
                        <td><span><input type="text" id="edit-user-realname" name="realName" value="${user.realName}"/></span></td>
                    </tr>
                    <tr>
                        <td><label for="edit-user-asperaaccount"><g:message code="user.administration.ui.asperaAccount"/>:</label></td>
                        <td><span><input type="text" id="edit-user-asperaaccount" name="asperaAccount" value="${user.asperaAccount}"/></span></td>
                    </tr>
                    </tbody>
                </table>
                <div class="buttons">
                    <input type="reset" value="${g.message(code: 'user.administration.cancel')}"/>
                    <input type="submit" value="${g.message(code: 'user.administration.save')}"/>
                </div>
            </form>
            </div>
            <div id="user-group-management">
                <h1><g:message code="user.administration.userGroup.ui.heading"/></h1>
                <div id="userGroups">
                    <h3><g:message code="user.administration.userGroup.ui.heading.userGroups"/></h3>
                    <input type="hidden" value="${user.id}"/>
                    <input type="hidden" value="removeGroup"/>
                    <table>
                        <tbody>
                        <g:each var="group" in="${userGroups}">
                            <tr><td title="${group.description}">${group.name}</td><td><input type="hidden" value="${group.id}"/><a href="#" rel="#userGroups-${group.id}"><g:message code="user.administration.userGroup.ui.removeGroup"/></a></td></tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
                <div id="availableGroups">
                    <h3><g:message code="user.administration.userGroup.ui.heading.availableGroups"/></h3>
                    <input type="hidden" value="${user.id}"/>
                    <input type="hidden" value="addGroup"/>
                    <table>
                        <tbody>
                        <g:each var="group" in="${groups}">
                            <tr><td title="${group.description}">${group.name}</td><td><input type="hidden" value="${group.id}"/><a href="#" rel="#userGroups-${group.id}"><g:message code="user.administration.userGroup.ui.addGroup"/></a></td></tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
                <div class="buttons">
                <button id="create-new-group"><g:message code="user.administration.userGroup.ui.heading.newGroup"/></button>
                </div>
            </div>
            <div id="user-role-management">
                <h1><g:message code="user.administration.userRole.ui.heading" args="[user.username]"/></h1>
                <div id="userRoles">
                    <h3><g:message code="user.administration.userRole.ui.heading.usersRoles"/></h3>
                    <input type="hidden" value="${user.id}"/>
                    <input type="hidden" value="removeRole"/>
                    <table>
                        <tbody>
                        <g:each var="role" in="${userRoles}">
                            <tr><td>${role.authority}</td><td><input type="hidden" value="${role.id}"/><a href="#" rel="#userRoles-${role.id}"><g:message code="user.administration.userRole.ui.removeRole"/></a></td></tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
                <div id="availableRoles">
                    <h3><g:message code="user.administration.userRole.ui.heading.availableRoles"/></h3>
                    <input type="hidden" value="${user.id}"/>
                    <input type="hidden" value="addRole"/>
                    <table>
                        <tbody>
                <%
                    for (def role in roles) {
                        if (userRoles.find { it.id == role.id }) {
                            continue
                        }
                %>
                        <tr><td>${role.authority}</td><td><input type="hidden" value="${role.id}"/><a href="#" rel="#availableRoles-${role.id}"><g:message code="user.administration.userRole.ui.addRole"/></a></td></tr>
                <%
                    }
                %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </body>
    <asset:script type="text/javascript">
        $(function() {
            $.otp.userAdministration.editUser.register.call($.otp.userAdministration.editUser);
            $.otp.growBodyInit(240);
        });
    </asset:script>
</html>
