<html>
<head>
    <meta name="layout" content="info"/>
    <title><g:message code="info.contact.title" /></title>
</head>

<body>
    <h2><g:message code="info.contact.title" /></h2>
    <p>${g.message(code: "info.contact.operatedBy")}</p>
    <p class="keep-whitespace">${contactDataOperatedBy}</p>
    <h3>${g.message(code: "info.contact.personInCharge")}</h3>
    <p class="keep-whitespace">${contactDataPersonInCharge}</p>
    <h3>${g.message(code: "info.contact.postalAddress")}</h3>
    <p class="keep-whitespace">${contactDataPostalAddress}</p>
    <h3>${g.message(code: "info.contact.support")}</h3>
    <p class="keep-whitespace"><a href="mailto:${contactDataSupportEmail}">${contactDataSupportEmail}</a></p>
    <h3>${g.message(code: "info.contact.picture")}</h3>
    <p>Login button &copy; unizyne, istockphoto.com</p>
</body>
</html>
