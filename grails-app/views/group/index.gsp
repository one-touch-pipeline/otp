<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="group.administration.header"/></title>
</head>
<body>
    <div class="body">
        <h1><g:message code="group.administration.header"/></h1>
        <ul>
        <g:each var="group" in="${usersByGroup}">
            <li>
                <h3>${group.key.name}</h3>
                Read project: ${group.key.readProject},&nbsp;
                write project: ${group.key.writeProject},&nbsp;
                read job system: ${group.key.readJobSystem},&nbsp;
                write job system: ${group.key.writeJobSystem},&nbsp;
                read sequence center: ${group.key.readSequenceCenter},&nbsp;
                write sequence center: ${group.key.writeSequenceCenter}
                <ul>
                <g:each var="user" in="${group.value}">
                    <li>
                        ${user.username}
                    </li>
                </g:each>
                </ul>
            </li>
        </g:each>
        </ul>
    </div>
</body>
</html>
