<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <title><g:message code="user.administration.header"/></title>
        <meta name="layout" content="main" />
        <r:require module="userAdministration"/>
        <r:require module="jqueryDatatables"/>
    </head>
    <body>
        <h2><g:message code="user.administration.header"/></h2>
        <div>
        <form id="switch-user-form" action="${request.contextPath}/j_spring_security_switch_user" method="POST">
            <input type="hidden" name="j_username"/>
        </form>
        <otp:dataTable codes="${[
                'user.administration.list.id',
                'user.administration.list.username',
                'user.administration.list.email',
                'user.administration.list.jabber',
                'user.administration.list.enabled',
                'user.administration.list.accountExpired',
                'user.administration.list.accountLocked',
                'user.administration.list.passwordExpired',
                'otp.blank'
            ]}" id="userTable"/>
        </div>
        <div class="buttons">
            <g:link action="create"><g:message code="user.administration.createUser"/></g:link>
        </div>
        </form>
        <r:script>
$(function() {
    $.otp.userAdministration.loadUserList();
});
        </r:script>
    </body>
</html>
