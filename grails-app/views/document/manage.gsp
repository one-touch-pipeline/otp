<%@ page import="de.dkfz.tbi.otp.administration.DocumentController; de.dkfz.tbi.otp.administration.Document" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="otp.menu.documents"/></title>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <div>
        <h2><g:message code="otp.menu.documents"/></h2>
        <g:message code="document.notice"/>
        <table>
            <tr>
                <th><g:message code="document.name"/></th>
                <th><g:message code="document.type"/></th>
                <th><g:message code="document.document"/></th>
                <th><g:message code="document.update"/></th>
                <th><g:message code="document.delete"/></th>

            </tr>
            <g:each in="${documents}" var="document">
                <tr>
                    <td title="${document.key.description}">${document.key.title}</td>
                    <g:if test="${document.value}">
                        <td>${document.value.formatType}</td>
                        <td>
                            <g:link action="download" params="['document.id': document.value.id, to: DocumentController.Action.VIEW]">${g.message(code: "document.view")}</g:link> |
                            <g:link action="download" params="['document.id': document.value.id, to: DocumentController.Action.DOWNLOAD]">${g.message(code: "document.download")}</g:link>
                        </td>
                    </g:if>
                    <g:else>
                        <td>${g.message(code: "document.notAvailable")}</td>
                        <td>${g.message(code: "document.notAvailable")}</td>
                    </g:else>
                    <td>
                        <g:uploadForm action="upload" useToken="true">
                            <input type="hidden" name="documentType.id" value="${document.key.id}"/>
                            <input type="file" name="content" />
                            <g:select name="formatType" from="${Document.FormatType}" optionValue="displayName" noSelection="${[(""): "Select format type"]}" />
                            <g:submitButton name="${g.message(code: "document.update")}"/>
                        </g:uploadForm>
                    </td>
                    <td>
                        <g:uploadForm action="delete" useToken="true">
                            <input type="hidden" name="documentType.id" value="${document.key.id}"/>
                            <g:submitButton name="${g.message(code: "document.delete")}"/>
                        </g:uploadForm>
                    </td>
                </tr>
            </g:each>
        </table>
        <h2><g:message code="document.documentType"/></h2>
        <g:form action="createDocumentType" useToken="true">
            <label for="title_input"><g:message code="document.title"/></label>
            <input id="title_input" type="text" name="title">
            <label for="description_input"><g:message code="document.description"/></label>
            <input id="description_input" type="text" name="description" style="width: 700px">
            <g:submitButton name="${g.message(code: "document.createType")}"/>
        </g:form>
    </div>
</div>
</body>
</html>
