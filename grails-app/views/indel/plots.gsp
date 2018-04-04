<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="indel.plots.title" args="[pid]"/></title>
</head>
<body>
    <g:if test="${error}">
        <h1><g:message code="error.404.title"/></h1>
        <p><g:message code="${error}"/></p>
    </g:if>
    <g:else>
        <g:if test="${plotType.name() == 'INDEL'}">
            <div class="body_pdf">
                <object data="${g.createLink(controller: "indel",
                        action: "renderPlot",
                        params: ["bamFilePairAnalysis.id": id, "plotType": plotType])}" type="application/pdf"
                        height="650" width="1250">
                    <a href="${g.createLink(controller: "indel",
                            action: "renderPlot",
                            params: ["bamFilePairAnalysis.id": id, "plotType": plotType])}"><g:message
                            code="indel.plots.view"/></a>
                </object>
            </div>
        </g:if>
        <g:else>
            <div class="body">
                <g:set var="url" value="${g.createLink(controller: "indel", action: "renderPlot",
                        params: ["bamFilePairAnalysis.id": id, "plotType": plotType])}"/>
                <a target="_blank" href="${url}">
                    <img class="img-plots" src="${url}"/>
                </a>
            </div>
        </g:else>
    </g:else>
</body>
</html>
