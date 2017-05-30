<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="sophia.plots.title" args="[pid]" /></title>
</head>
<body>
    <g:if test="${error}">
        <h1><g:message code="error.404.title"/></h1>
        <p><g:message code="${error}"/></p>
    </g:if>
    <g:else>
        <div class="body_pdf">
            <object data="${g.createLink(controller: "sophia",
                action: "renderPDF",
                params: ["sophiaInstance.id": id])}" type="application/pdf"  height="650" width="1250">
                <a href="${g.createLink(controller: "sophia",
                    action: "renderPDF",
                    params: ["sophiaInstance.id": id])}"><g:message code="sophia.plots.view"/></a>
            </object>
        </div>
    </g:else>
</body>
</html>
