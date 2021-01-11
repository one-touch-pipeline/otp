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
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <asset:stylesheet src="pages/workflowArtefact/index/styles.less"/>
    <title><g:message code="workflowArtefact.title" args="[artefact?.displayName ?: 'Artefact']"/></title>
</head>

<body>
    <div class="body">
        <sec:access expression="hasRole('ROLE_OPERATOR')">
            <g:render template="/templates/messages"/>

            <div class="container-fluid">
                <g:if test="${artefact}">
                    <span class="badge rounded-pill workflow-run-${artefact.state.toString().toLowerCase()}-color text-white workflow-artefact-status">${artefact.state}</span>
                    <h4>${artefact.displayName}</h4>

                    <ul class="list-group list-group-horizontal">
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
                                <g:link controller="workflowRunDetails" action="index" id="${artefact.producedBy.id}">${artefact.producedBy.displayName} (${artefact.producedBy.id})</g:link>
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
                                    <g:link controller="workflowRunDetails" action="index" id="${it.id}">${it.workflowRun.displayName}</g:link> <g:message code="workflowArtefact.as"/> ${it.role}<br>
                                    <g:message code="workflowArtefact.outputs"/>:
                                    <g:if test="${it.workflowRun.outputArtefacts.isEmpty()}">
                                        <i>--<g:message code="workflowArtefact.noOutputs"/>--</i>
                                    </g:if>
                                    <div class="workflow-artefacts-outputs">
                                        <g:each status="index" in="${it.workflowRun.outputArtefacts}" var="output">
                                            <g:link controller="workflowArtefact" action="index" id="${output.value.properties.id}">${output.value.properties.displayName}</g:link>
                                            <g:if test="${it.workflowRun.outputArtefacts.size() - 1 != index}">,</g:if>
                                        </g:each>
                                    </div>
                                </div>
                            </g:each>
                        </li>
                    </ul>
                </g:if>
            </div>
        </sec:access>
    </div>
</body>
</html>
