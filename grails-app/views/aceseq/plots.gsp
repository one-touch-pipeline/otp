<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="aceseq.plots.title" args="[bamFilePairAnalysis.individual.pid]" /></title>
</head>
<body>
    <g:if test="${error}">
        <h1><g:message code="error.404.title"/></h1>
        <p><g:message code="${error}"/></p>
    </g:if>
    <g:else>
        <h1><g:message code="aceseq.plots.title" args="[bamFilePairAnalysis.individual.pid]" /></h1>
        <div class="body">
            <g:each in="${plotType}" var="p">
                <g:set var="url" value="${g.createLink(controller: "aceseq", action: "plotImages",
                        params: ["bamFilePairAnalysis.id": bamFilePairAnalysis.id, "plotType": p])}" />
                <div>
                    <a target="_blank" href="${url}">
                        <img class="img-plots" src="${url}"/>
                   </a>
                </div>
            </g:each>
            <g:each in="${plotNumber}" var="p">
                <g:each in="${p.value}" var="i">
                    <g:set var="url" value="${g.createLink(controller: "aceseq", action: "plotImages",
                            params: ["bamFilePairAnalysis.id": bamFilePairAnalysis.id, "plotType": p.key, index: i])}" />
                    <div>
                        <a target="_blank" href="${url}">
                            <img class="img-plots" src="${url}"/>
                        </a>
                    </div>
                </g:each>
            </g:each>
        </div>
    </g:else>
</body>
</html>
