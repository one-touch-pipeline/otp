<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="otp.menu.roles"/></title>
</head>
<body>
    <div class="body">
        <h3><g:message code="roles.rolesAndGroups.header"/></h3>
        <g:each in="${roleLists}" var="roleList">
            <g:each in="${roleList}" var="roleAndUsers">
                <h4>${roleAndUsers.role.authority}</h4>
                <g:if test="${roleAndUsers.users.isEmpty()}">
                    ${g.message(code: "roles.noMembers")}
                </g:if>
                <g:else>
                    ${g.message(code: "roles.members")}
                    <ul>
                        <g:each in="${roleAndUsers.users}" var="user">
                            <li>${user.username} (${user.realName})</li>
                        </g:each>
                    </ul>
                </g:else>
            </g:each>
            <g:if test="${roleList != roleLists.last()}">
                <hr>
            </g:if>
        </g:each>
    </div>
</body>
</html>
