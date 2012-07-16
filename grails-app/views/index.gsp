<!doctype html>
<html>
    <head>
        <meta name="layout" content="main"/>
        <title><g:message code="otp.welcome.title"/></title>
    </head>
    <body>
        <div class="body">
            <h1><g:message code="otp.welcome.title"/></h1>
            <p><g:message code="otp.welcome.introduction"/></p>
            <sec:ifAnyGranted roles="ROLE_ADMIN">
                <h2><g:message code="otp.welcome.administration.title"/></h2>
                <div>
                    <ul>
                    <li><g:link controller="userAdministration"><g:message code="otp.welcome.administration.userAdmin"/></g:link></li>
                    <li><g:link controller="crashRecover"><g:message code="otp.welcome.administration.crashRecover"/></g:link></li>
                    <li><g:link controller="shutdownController"><g:message code="otp.welcome.administration.shutdown"/></g:link></li>
                    </ul>
                </div>
            </sec:ifAnyGranted>
        </div>
    </body>
</html>
