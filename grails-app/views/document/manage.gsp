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
        <h2>${g.message(code: "otp.menu.documents")}</h2>
        ${g.message(code: "document.notice")}
        <table>
            <tr>
                <th>${g.message(code: "document.name")}</th>
                <th>${g.message(code: "document.type")}</th>
                <th>${g.message(code: "document.document")}</th>
                <th>${g.message(code: "document.update")}</th>
            </tr>
            <g:each in="${documents}" var="document">
                <tr>
                    <td>${document.key}</td>
                    <g:if test="${document.value}">
                        <td>${document.value.type}</td>
                        <td>
                            <g:link action="download" params="[file: document.key, to: DocumentController.Action.VIEW]">${g.message(code: "document.view")}</g:link> |
                            <g:link action="download" params="[file: document.key, to: DocumentController.Action.DOWNLOAD]">${g.message(code: "document.download")}</g:link>
                        </td>
                    </g:if>
                    <g:else>
                        <td>${g.message(code: "document.notAvailable")}</td>
                        <td>${g.message(code: "document.notAvailable")}</td>
                    </g:else>
                    <td>
                        <g:uploadForm action="upload" useToken="true">
                            <input type="hidden" name="name" value="${document.key}"/>
                            <input type="file" name="content" />
                            <g:select name="type" from="${Document.Type}" optionValue="displayName" noSelection="${[(""): "Select type"]}" />
                            <g:submitButton name="${g.message(code: "document.update")}"/>
                        </g:uploadForm>
                    </td>
                </tr>
            </g:each>
        </table>
    </div>
</div>
</body>
</html>
