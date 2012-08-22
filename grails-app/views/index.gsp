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
                    <li><g:link controller="crashRecovery"><g:message code="otp.welcome.administration.crashRecovery"/></g:link></li>
                    <li><g:link controller="shutdown"><g:message code="otp.welcome.administration.shutdown"/></g:link></li>
                    <li><g:link controller="notification"><g:message code="otp.welcome.administration.notification"/></g:link></li>
                    </ul>
                </div>
            </sec:ifAnyGranted>
        </div>
    </body>
</html>
