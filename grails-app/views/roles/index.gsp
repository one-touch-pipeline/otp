<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="otp.menu.roles"/></title>
</head>

<body>
<div class="body">
    <div>
        <h2>${g.message(code: "otp.menu.roles")}</h2>

        <g:each in="${rolesAndUsers}" var=" roleAndUsers">
            <h3>${roleAndUsers.role.authority}</h3>
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
        <hr>
        <g:each in="${groupsAndUsers}" var=" roleAndUsers">
            <h3>${roleAndUsers.role.authority}</h3>
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
    </div>
</div>
</body>
</html>
