<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="user.administration.createUser"/></title>
        <r:require module="userAdministration"/>
    </head>
<body>
    <div class="body">
        <ul>
            <li class="button"><g:link action="index"><g:message code="user.administration.backToOverview"/></g:link></li>
        </ul>
        <form id="create-user-form" action="createUser">
        <table class="form">
            <tbody>
                <tr>
                    <td><label for="create_user_name"><g:message code="user.administration.ui.username"/></label></td>
                    <td><input type="text" name="username" id="create_user_name"/></td>
                    <td>&nbsp;</td>
                </tr>
                <tr>
                    <td><label for="create_user_email"><g:message code="user.administration.ui.email"/></label></td>
                    <td><input type="text" name="email" id="create_user_email"/></td>
                    <td>&nbsp;</td>
                </tr>
                <tr>
                    <td><label for="create_user_jabber"><g:message code="user.administration.ui.jabber"/></label></td>
                    <td><input type="text" name="jabber" id="create_user_jabber"/></td>
                    <td>&nbsp;</td>
                </tr>
            </tbody>
        </table>
        <div id="roles_container">
            <h2><g:message code="user.administration.ui.heading.roles"/></h2>
            <ul>
                <g:each in="${roles}">
                    <li><input type="checkbox" name="role" value="${it.id}"/>${it.authority}</li>
                </g:each>
            </ul>
        </div>
        <div id="groups_container">
            <h2><g:message code="user.administration.ui.heading.grous"/></h2>
            <ul>
                <g:each in="${groups}">
                    <li><input type="checkbox" name="group" value="${it.id}"/>${it.name}</li>
                </g:each>
            </ul>
        </div>
        <div class="buttons">
            <input type="submit" value="${g.message(code: 'user.administration.createUser')}"/>
        </div>
        </form>
    </div>
    <r:script>
        $(function () {
            $.otp.userAdministration.create.register();
        });
    </r:script>
</body>
</html>
