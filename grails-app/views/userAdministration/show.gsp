<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <title><g:message code="user.administration.ui.heading.user"/></title>
        <meta name="layout" content="main" />
        <r:require module="userAdministration"/>
    </head>
    <body>
        <div class="body_grow">
            <div id="create-group-dialog" style="display: none" title="${g.message(code: 'group.create.ui.dialog.title') }">
                <form>
                <table>
                    <tbody>
                        <tr>
                            <td><label for="create-group-name"><g:message code="group.create.ui.name"/></label></td>
                            <td><input type="text" name="name" id="create-group-name"/><span class="error-marker"></span></td>
                        </tr>
                        <tr>
                            <td><label for="create-group-description"><g:message code="group.create.ui.description"/></label></td>
                            <td><input type="text" name="description" id="create-group-description"/><span class="error-marker"></span></td>
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
                            <td><input type="checkbox" name="writeProject"/><span class="error-marker"></span></td>
                        </tr>
                        <tr>
                            <th><g:message code="group.create.ui.jobSystem"/></th>
                            <td><input type="checkbox" name="readJobSystem"/></td>
                            <td><input type="checkbox" name="writeJobSystem"/><span class="error-marker"></span></td>
                        </tr>
                        <tr>
                            <th><g:message code="group.create.ui.seqeuenceCenter"/></th>
                            <td><input type="checkbox" name="readSequenceCenter"/></td>
                            <td><input type="checkbox" name="writeSequenceCenter"/><span class="error-marker"></span></td>
                        </tr>
                    </tbody>
                </table>
                </form>
            </div>
            <ul>
                <li class="button"><g:link action="index"><g:message code="user.administration.backToOverview"/></g:link></li>
                <g:if test="${next}">
                    <li class="button"><g:link action="show" id="${next.id}"><g:message code="user.administration.nextUser"/></g:link></li>
                </g:if>
                <g:if test="${previous}">
                    <li class="button"><g:link action="show" id="${previous.id}"><g:message code="user.administration.previousUser"/></g:link></li>
                </g:if>
            </ul>
            <h1><g:message code="user.administration.ui.heading.user"/></h1>
            <div>
            <form id="edit-user-form" method="POST">
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
                        <td><label for="edit-user-jabber"><g:message code="user.administration.ui.jabber"/>:</label></td>
                        <td><span><input type="text" id="edit-user-jabber" name="jabber" value="${user.jabberId}"/></span></td>
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
        <r:script>
            $(function() {
                $.otp.userAdministration.editUser.register.call($.otp.userAdministration.editUser);
            });
        </r:script>
    </body>
    <r:script>
        $(function() {
            $.otp.growBodyInit(240);
        });
    </r:script>
</html>
