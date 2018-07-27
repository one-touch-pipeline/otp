<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processingOption.insert.title"/></title>
</head>
<body>
    <div class="body">
        <g:if test="${hasErrors}">
            <div class="errors"> <li>${message}</li></div>
        </g:if>
        <g:elseif test="${message}">
            <div class="message">${message}</div>
        </g:elseif>
        <g:else>
            <div class="empty"><br></div>
        </g:else>
        <h1><g:message code="processingOption.insert.title"/></h1>
        <g:form controller="processingOption" action="insert">
            <table>
                <tr>
                    <td class="myKey"><g:message code="processingOption.insert.name"/></td>
                    <td valign="top" class="value">
                        <g:select name="optionName" from="${optionNames}" id="optionName" value="${cmd?.optionName}" style="width: 500px"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="processingOption.insert.type"/></td>
                    <td valign="top" class="value">
                        <input type="text" id="type" name="type"  value="${cmd?.type}" style="width: 500px"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey" ><g:message code="processingOption.insert.value"/></td>
                    <td valign="top" class="value">
                        <input type="text" id="value" name="value"value="${cmd?.value}" style="width: 500px"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="processingOption.insert.project"/></td>
                    <td valign="top" class="value">
                        <g:select name="project" from="${projects}" id="project" value="${cmd?.project}"/>
                    </td>
                </tr>
            </table>
            <g:submitButton name="submit" value="${g.message(code: 'processingOption.insert.save')}"/>
        </g:form>
    </div>
</body>
</html>
