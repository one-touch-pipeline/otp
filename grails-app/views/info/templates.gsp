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

<%@ page import="de.dkfz.tbi.otp.administration.DocumentController; de.dkfz.tbi.otp.administration.Document" %>
<html>
<head>
    <meta name="layout" content="info"/>
    <title><g:message code="info.templates.link"/></title>
</head>

<body>
<br>
<%--<otp:annotation type="info">
    ${g.message(code: "info.template.banner1")}
    <g:link controller="projectRequest" action="index">${g.message(code: "info.template.projectRequestLink")}</g:link>${g.message(code: "info.template.banner2")}
</otp:annotation>
--%>
<h1>${g.message(code: "info.templates.link")}</h1>
<ul>
<%--<li>
        ${g.message(code: "info.template.projectRequestDescription")}<br>
        <g:link controller="projectRequest" action="index">
            ${g.message(code: "info.template.projectRequestLink")}
        </g:link>
    </li>--%>
    <g:each in="${availableTemplates}" var="template">
        <li>
            ${template.document.documentType.description}<br>
            <g:if test="${template.document.formatType == de.dkfz.tbi.otp.administration.Document.FormatType.LINK}">
                <a href="${template.document.link}"><g:message code="info.template.open" args="[template.fullFileName]"/></a>
            </g:if>
            <g:else>
                <g:link controller="document" action="download"
                        params="['document.id': template.document.id, to: DocumentController.Action.DOWNLOAD]">
                    <g:message code="info.template.download" args="[template.fullFileName]"/>
                </g:link>
            </g:else>
        </li>
    </g:each>
</ul>

${g.message(code: "info.template.request")} <a href="mailto:${contactDataSupportEmail}">${contactDataSupportEmail}</a>
</body>
</html>
