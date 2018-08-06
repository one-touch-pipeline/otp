<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title><g:message code="user.administration.header"/></title>
    <meta name="layout" content="main" />
    <asset:javascript src="modules/userAdministration"/>
    <asset:javascript src="modules/jqueryDatatables"/>
    <asset:stylesheet src="modules/jqueryDatatables"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>

        <h1><g:message code="user.administration.header"/></h1>
        <div>
            <form id="switch-user-form" action="${request.contextPath}/j_spring_security_switch_user" method="POST">
                <input type="hidden" name="j_username"/>
            </form>
            <div class="otpDataTables">
                <otp:dataTable codes="${[
                    'user.administration.list.id',
                    'user.administration.list.username',
                    'user.administration.list.email',
                    'user.administration.list.enabled',
                    'user.administration.list.accountExpired',
                    'user.administration.list.accountLocked',
                    'otp.blank'
                ]}" id="userTable"/>
            </div>
        </div>
        <div class="buttons">
            <g:link action="create"><g:message code="user.administration.createUser"/></g:link>
        </div>
    </div>
    <asset:script type="text/javascript">
        $(function() {
            $.otp.userAdministration.loadUserList();
        });
    </asset:script>
</body>
</html>
