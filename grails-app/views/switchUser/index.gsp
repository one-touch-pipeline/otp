<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Switch User</title>
</head>
<body>
    <div class="body">
        <g:form action="switchUser">
            <g:select name="id" from="${users}" optionValue="username" optionKey="id"/>
            <g:submitButton name="switchUser"/>
        </g:form>
    </div>
</body>
</html>