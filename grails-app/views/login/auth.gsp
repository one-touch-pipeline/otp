<html>
<head>
    <meta name='layout' content='main'/>
    <title><g:message code="springSecurity.login.title"/></title>
    <asset:stylesheet src="modules/info"/>
    <asset:javascript src="modules/info"/>
</head>

<body>
<div class="body">
    <div id='login'>
        <h2><g:message code="springSecurity.login.header"/></h2>

        <g:if test='${flash.message}'>
            <div class='login_message'>${flash.message}</div>
        </g:if>

        <g:render template="login"/>
    </div>
</div>
</body>
</html>
