<%@ page import="de.dkfz.tbi.otp.administration.DocumentController; de.dkfz.tbi.otp.administration.Document" %>
<html>
<head>
    <meta name="layout" content="info"/>
    <title><g:message code="info.templates.link" /></title>
</head>

<body>
<h2><g:message code="info.templates.link" /></h2>

<ul>
    <g:if test="${Document.Name.PROJECT_FORM in availableTemplates}">
        <li>
            ${g.message(code: "info.template.projectForm.info")}<br>
            <g:link controller="document" action="download" params="[file: Document.Name.PROJECT_FORM, to: DocumentController.Action.DOWNLOAD]">${g.message(code: "info.template.projectForm.link")}</g:link>
        </li>
    </g:if>
    <g:if test="${Document.Name.METADATA_TEMPLATE in availableTemplates}">
        <li>
            ${g.message(code: "info.template.metadataTemplate.info")}<br>
            <g:link controller="document" action="download" params="[file: Document.Name.METADATA_TEMPLATE, to: DocumentController.Action.DOWNLOAD]">${g.message(code: "info.template.metadataTemplate.link")}</g:link>
        </li>
    </g:if>
    <g:if test="${Document.Name.PROCESSING_INFORMATION in availableTemplates}">
        <li>
            ${g.message(code: "info.template.processingInformation.info")}<br>
            <g:link controller="document" action="download" params="[file: Document.Name.PROCESSING_INFORMATION, to: DocumentController.Action.DOWNLOAD]">${g.message(code: "info.template.processingInformation.link")}</g:link>
        </li>
    </g:if>
    <g:if test="${availableTemplates.empty}">
        <li>
            ${g.message(code: "info.template.none")}
        </li>
    </g:if>
</ul>
${g.message(code: "info.template.request")} <a href="mailto:${contactDataSupportEmail}">${contactDataSupportEmail}</a>
</body>
</html>
