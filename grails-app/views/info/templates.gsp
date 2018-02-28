<%@ page import="de.dkfz.tbi.otp.administration.Document" %>
<html>
<head>
    <meta name="layout" content="info"/>
    <title><g:message code="info.templates.link" /></title>
</head>

<body>
<h2><g:message code="info.templates.link" /></h2>

<ul>
    <li>
        ${g.message(code: "info.template.projectForm.info")}<br>
        <g:link controller="document" action="download" params="[file: Document.Name.PROJECT_FORM]">${g.message(code: "info.template.projectForm.link")}</g:link>
    </li>
    <li>
        ${g.message(code: "info.template.metadataTemplate.info")}<br>
        <g:link controller="document" action="download" params="[file: Document.Name.METADATA_TEMPLATE]">${g.message(code: "info.template.metadataTemplate.link")}</g:link>
    </li>
    <li>
        ${g.message(code: "info.template.processingInformation.info")}<br>
        <g:link controller="document" action="download" params="[file: Document.Name.PROCESSING_INFORMATION]">${g.message(code: "info.template.processingInformation.link")}</g:link>
    </li>
</ul>
${g.message(code: "info.template.request")} <a href="mailto:${contactDataSupportEmail}">${contactDataSupportEmail}</a>
</body>
</html>
