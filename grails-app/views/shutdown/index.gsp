<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Server Shutdown</title>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>
        <h1><g:message code="serverShutdown.title"/></h1>
        <p>
            <g:if test="${shutdownSucceeded}">
                <g:message code="serverShutdown.successful"/>
            </g:if>
            <g:else>
                <g:form action="planShutdown">
                    <label for="shutdownReason"><g:message code="serverShutdown.reason"/></label>
                    <input type="text" name="reason" id="shutdownReason"/>
                    <g:submitButton name="Plan Shutdown"/>
                </g:form>
            </g:else>
        </p>
    </div>
</body>
</html>
