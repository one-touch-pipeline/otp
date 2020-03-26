%{--
  - Copyright 2011-2019 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

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
        <h1><g:message code="otp.menu.documents"/></h1>
        <otp:annotation type="info">
            <g:message code="document.notice"/>
        </otp:annotation>
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
                            <g:select id="formatType-${document.key.title}" class="use-select-2" style="min-width: 20ch;"
                                      name="formatType" from="${Document.FormatType}" optionValue="displayName"
                                      noSelection="${[(""): "Select format type"]}" />
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
