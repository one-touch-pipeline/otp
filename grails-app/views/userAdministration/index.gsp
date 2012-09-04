<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <title><g:message code="user.administration.header"/></title>
        <meta name="layout" content="main" />
        <r:require module="userAdministration"/>
        <jqDT:resources type="js"/>
    </head>
    <body>
        <h2><g:message code="user.administration.header"/></h2>
        <div>
        <form id="switch-user-form" action="${request.contextPath}/j_spring_security_switch_user" method="POST">
            <input type="hidden" name="j_username"/>
        </form>
        <table id="userTable">
            <thead>
            <tr>
                <th><g:message code="user.administration.list.id"/></th>
                <th><g:message code="user.administration.list.username"/></th>
                <th><g:message code="user.administration.list.email"/></th>
                <th><g:message code="user.administration.list.jabber"/></th>
                <th><g:message code="user.administration.list.enabled"/></th>
                <th><g:message code="user.administration.list.accountExpired"/></th>
                <th><g:message code="user.administration.list.accountLocked"/></th>
                <th><g:message code="user.administration.list.passwordExpired"/></th>
                <th>&nbsp;</th>
            </tr>
            </thead>
            <tbody></tbody>
            <tfoot>
            <tr>
                <th><g:message code="user.administration.list.id"/></th>
                <th><g:message code="user.administration.list.username"/></th>
                <th><g:message code="user.administration.list.email"/></th>
                <th><g:message code="user.administration.list.jabber"/></th>
                <th><g:message code="user.administration.list.enabled"/></th>
                <th><g:message code="user.administration.list.accountExpired"/></th>
                <th><g:message code="user.administration.list.accountLocked"/></th>
                <th><g:message code="user.administration.list.passwordExpired"/></th>
                <th>&nbsp;</th>
            </tr>
            </tfoot>
        </table>
        </div>
        <r:script>
$(function() {
    $.otp.userAdministration.loadUserList();
});
        </r:script>
    </body>
</html>
