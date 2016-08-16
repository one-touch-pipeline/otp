<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="metadataImport.blackListedIlseNumbers.title"/></title>
    <asset:javascript src="modules/editorSwitch"/>
</head>

<body>
<div class="body">
    <div id="create">
        <h3>
            <g:message code="metadataImport.blackListedIlseNumbers.create.description"/>
        </h3>
        <g:if test="${command?.addButton}">
            <ul>
                <g:each in="${command.errors.fieldErrors}" var="error">
                    <li>
                        <g:message error="${error}"/>
                    </li>
                </g:each>
            </ul>
        </g:if>


        <g:form action="blacklistedIlseNumbers">
            <table class="table">
                <tr>
                    <td>
                        <g:message code="metadataImport.blackListedIlseNumbers.ilse"/>
                        <g:textField name="ilse" size="15" value="${command?.ilse}"/>
                    </td>
                    <td>
                        <g:message code="metadataImport.blackListedIlseNumbers.comment"/>
                        <g:textArea name="comment" size="130" style="height:50px;" value="${command?.comment}"/>
                    </td>
                </tr>
            </table>
            <g:submitButton name="addButton" value="Add"/>
        </g:form>
    </div>

    <div id="show">
        <h3>
            <g:message code="metadataImport.blackListedIlseNumbers.table.description"/>
        </h3>
        <table class="table">
            <tr>
                <th>
                    <g:message code="metadataImport.blackListedIlseNumbers.ilse"/>
                </th>
                <th>
                    <g:message code="metadataImport.blackListedIlseNumbers.comment"/>
                </th>
            </tr>

            <g:each in="${ilseSubmissions}" var="ilseSubmission">
                <tr>
                    <td>
                        ${ilseSubmission.ilseNumber}
                    </td>
                    <td>
                        <pre>${ilseSubmission.comment.displayString()}</pre>
                    </td>
                </tr>
            </g:each>
        </table>
    </div>
</div>
</body>
</html>
