<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="aceseq.plots.title" args="[pid]" /></title>
</head>
<body>
    <g:if test="${error}">
        <h1><g:message code="error.404.title"/></h1>
        <p><g:message code="${error}"/></p>
    </g:if>
    <g:else>
        <h1><g:message code="aceseq.plots.title" args="[pid]" /></h1>
        <div>
            <g:each in="${plot}" var="p">
                <div>
                    <img src="${g.createLink(controller: "aceseq", action: "plotImage",
                            params: ["aceseqInstance.id": aceseqInstance.id, aceseqPlot: p])}"/>
                </div>
            </g:each>
            <g:each in="${plotNumber}" var="p">
                <g:each in="${0..p.value}" var="i">
                    <div>
                        <img src="${g.createLink(controller: "cnv", action: "plotImages",
                                params: ["aceseqInstance.id": aceseqInstance.id, aceseqPlots: p.key, index: i])}"/>
                    </div>
                </g:each>
            </g:each>
        </div>
    </g:else>
</body>
</html>
