<%@ page import="de.dkfz.tbi.otp.administration.DocumentController; de.dkfz.tbi.otp.administration.Document" %>
<html>
<head>
    <meta name="layout" content="info"/>
    <title><g:message code="info.templates.link"/></title>
</head>

<body>
<h2><g:message code="info.templates.link"/></h2>
${g.message(code: "info.template.request")} <a href="mailto:${contactDataSupportEmail}">${contactDataSupportEmail}</a>

<ul>
    <g:each in="${availableTemplates}" var="template">
        <li>
            ${template.documentType.description}<br>
            <g:link controller="document" action="download"
                    params="['document.id': template.id, to: DocumentController.Action.DOWNLOAD]">
                <g:message code="info.template.download" args="[template.getFileNameWithExtension()]"/>
            </g:link>
        </li>
    </g:each>
</ul>
</body>
</html>
