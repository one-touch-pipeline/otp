%{--
  - Copyright 2011-2020 The OTP authors
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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <asset:stylesheet src="pages/workflowArtefact/index/styles.less"/>
    <asset:javascript src="pages/workflowRunList/common.js"/>
    <title><g:message code="workflowArtefact.title" args="[artefact.id.toString()]"/></title>
</head>

<body>
<div class="container-fluid otp-main-container">
    <sec:access expression="hasRole('ROLE_OPERATOR')">
        <g:render template="/templates/messages"/>
        <g:if test="${artefact}">
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb">
                    <li class="breadcrumb-item"><g:link controller="workflowRunList" action="index">${g.message(code: "workflow.navigation.list")}</g:link></li>
                    <g:if test="${artefact.producedBy}">
                        <li class="breadcrumb-item"><g:link controller="workflowRunDetails" action="index" id="${artefact.producedBy.id}">
                            ${g.message(code: "workflow.navigation.details")} (${artefact.producedBy.id})</g:link>
                        </li>
                    </g:if>
                    <g:if test="${!artefactUsedBy.empty}">
                        <li class="breadcrumb-item"><g:link controller="workflowRunDetails" action="index" id="${artefactUsedBy.first().workflowRun.id}">
                            ${g.message(code: "workflow.navigation.details")} (${artefactUsedBy.first().workflowRun.id})</g:link>
                        </li>
                    </g:if>
                    <li class="breadcrumb-item active" aria-current="page">${g.message(code: "workflow.navigation.artefact")}</li>
                </ol>
            </nav>

            <nav class="navbar">
                <div class="navbar-brand">
                    <div id="statusDot" title="${artefact.state}" data-status="${artefact.state}" class="d-inline-block"></div>
                    <span class="d-inline-flex align-top"><g:message code="workflowArtefact.title" args="[artefact.id.toString()]"/></span>
                </div>
            </nav>

            <div class="dropdown-divider"></div>

            ${raw(artefact.displayName.replace("\n", "<br>"))}

            <ul class="list-group list-group-horizontal mt-2">
                <li class="list-group-item hlg-item-key"><g:message code="workflowArtefact.title.project"/>:</li>
                <li class="list-group-item flex-fill">${artefact.project.name}</li>
            </ul>
            <ul class="list-group list-group-horizontal">
                <li class="list-group-item hlg-item-key"><g:message code="workflowArtefact.title.pid"/>:</li>
                <li class="list-group-item flex-fill">${artefact.individual.pid}</li>
            </ul>
            <ul class="list-group list-group-horizontal">
                <li class="list-group-item hlg-item-key"><g:message code="workflowArtefact.title.seqtype"/>:</li>
                <li class="list-group-item flex-fill">${artefact.seqType}</li>
            </ul>
            <ul class="list-group list-group-horizontal">
                <li class="list-group-item hlg-item-key"><g:message code="workflowArtefact.title.producedBy"/>:</li>
                <li class="list-group-item flex-fill">
                    <g:if test="${artefact.producedBy}">
                        <g:message code="workflowArtefact.title.workflowRun"/> ${artefact.producedBy.id}<br>
                        <g:link controller="workflowRunDetails" action="index"
                                id="${artefact.producedBy.id}">${raw(artefact.producedBy.displayName.replace("\n", "<br>"))}</g:link>
                    </g:if>
                </li>
            </ul>
            <ul class="list-group list-group-horizontal">
                <li class="list-group-item hlg-item-key"><g:message code="workflowArtefact.title.outputRole"/>:</li>
                <li class="list-group-item flex-fill">${artefact.outputRole}</li>
            </ul>
            <ul class="list-group list-group-horizontal">
                <li class="list-group-item hlg-item-key"><g:message code="workflowArtefact.title.usedBy"/>:</li>
                <li class="list-group-item flex-fill">
                    <g:each in="${artefactUsedBy}">
                        <div class="workflow-artefacts-used-by-col">
                            <g:link controller="workflowRunDetails" action="index" id="${it.workflowRun.id}">${raw(it.workflowRun.displayName.replace("\n", "<br>"))}</g:link> <g:message
                                    code="workflowArtefact.as"/> ${it.role}<br>
                            <p class="mt-2"><g:message code="workflowArtefact.outputs"/>:</p>
                            <g:if test="${it.workflowRun.outputArtefacts.isEmpty()}">
                                <i>--<g:message code="workflowArtefact.noOutputs"/>--</i>
                            </g:if>
                            <ul class="list-group list-group-horizontal">
                                <div class="workflow-artefacts-outputs">
                                    <g:each status="index" in="${it.workflowRun.outputArtefacts}" var="output">
                                        <li class="list-group-item">
                                            <g:link controller="workflowArtefact" action="index"
                                                    id="${output.value.properties.id}">${raw(output.value.properties.displayName.replace("\n", "<br>"))}</g:link>
                                        </li>
                                    </g:each>
                                </div>
                            </ul>
                        </div>
                    </g:each>
                </li>
            </ul>
        </g:if>
    </sec:access>
</div>
</body>
</html>
