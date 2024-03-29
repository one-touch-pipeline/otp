%{--
  - Copyright 2011-2024 The OTP authors
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
