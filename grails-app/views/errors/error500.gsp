<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta name="layout" content="main" />
        <title><g:message code="error.500.title"/></title>
    </head>
    <body>
        <div class="body" style="overflow: auto;">
            <h1><g:message code="error.500.title"/></h1>
            <p><g:message code="error.500.explanation" args="${[code]}"/></p>
            <sec:ifAllGranted roles="ROLE_ADMIN">
                <pre>${message}</pre>
                <pre>${stackTrace}</pre>
            </sec:ifAllGranted>
        </div>
    </body>
</html>
