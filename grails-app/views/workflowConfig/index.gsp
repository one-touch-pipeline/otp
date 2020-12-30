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
<%@ page import="de.dkfz.tbi.otp.workflowExecution.SelectorType" %>
<html>
<head>
    <title>${g.message(code: "workflowConfig.config")}</title>
    <meta name="layout" content="main"/>
    <asset:javascript src="taglib/Expandable.js"/>
    <asset:javascript src="pages/workflowConfig/index.js"/>
</head>

<body>
<div class="body" style='display: grid; grid-template-areas: "header header" "main related"'>
    <h1 style='grid-area: header'>${g.message(code: "workflowConfig.config")}</h1>

    <div style='grid-area: main'>
        <g:render template="/templates/messages"/>
        <h2>${g.message(code: "workflowConfig.selector")}</h2>

        <g:form action="index" method="GET" class="selector">
            ${g.message(code: "workflowConfig.selector.note")}
            <table class="key-value-table key-input">
                <tr>
                    <td><label for="selectorName">${g.message(code: "workflowConfig.selector.name")}</label></td>
                    <td>
                        <g:select id="selectorName" name="selector.id" from="${allSelectors}" class="use-select-2" optionKey="id" optionValue="name"
                                  value="${selector?.id}"
                                  noSelection="${['': '']}" autocomplete="off"/>
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td>${g.message(code: "workflowConfig.selector.or")}</td>
                </tr>
            </table>
        </g:form>
        <g:form action="index" method="GET" class="selector">

            <table class="key-value-table key-input">
                <tr>
                    <td><label for="selectorWorkflows">${g.message(code: "workflowConfig.selector.workflows")}</label></td>
                    <td><g:select id="selectorWorkflows" name="workflows" from="${workflows}" multiple="true" class="use-select-2" optionKey="id" optionValue="displayName"
                                  value="${selectedWorkflows}" noSelection="${['': g.message(code: "workflowConfig.selector.workflows.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.workflows.all")}" autocomplete="off"/></td>
                </tr>
                <tr>
                    <td><label for="selectorWorkflowVersions">${g.message(code: "workflowConfig.selector.versions")}</label></td>
                    <td><g:select id="selectorWorkflowVersions" name="workflowVersions" from="${workflowVersions}" multiple="true" class="use-select-2" optionKey="id" optionValue="displayName"
                                  value="${selectedWorkflowVersions}" noSelection="${['': g.message(code: "workflowConfig.selector.versions.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.versions.all")}" autocomplete="off"/></td>
                </tr>
                <tr>
                    <td><label for="selectorProjects">${g.message(code: "workflowConfig.selector.projects")}</label></td>
                    <td><g:select id="selectorProjects" name="projects" from="${projects}" multiple="true" class="use-select-2" optionKey="id" optionValue="name"
                                  value="${selectedProjects}" noSelection="${['': g.message(code: "workflowConfig.selector.projects.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.projects.all")}" autocomplete="off"/></td>
                </tr>
                <tr>
                    <td><label for="selectorSeqTypes">${g.message(code: "workflowConfig.selector.seqTypes")}</label></td>
                    <td><g:select id="selectorSeqTypes" name="seqTypes" from="${seqTypes}" multiple="true" class="use-select-2" optionKey="id" optionValue="displayNameWithLibraryLayout"
                                  value="${selectedSeqTypes}" noSelection="${['': g.message(code: "workflowConfig.selector.seqTypes.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.seqTypes.all")}" autocomplete="off"/></td>
                </tr>
                <tr>
                    <td><label for="selectorReferenceGenomes">${g.message(code: "workflowConfig.selector.referenceGenomes")}</label></td>
                    <td><g:select id="selectorReferenceGenomes" name="referenceGenomes" from="${referenceGenomes}" multiple="true" class="use-select-2" optionKey="id" optionValue="name"
                                  value="${selectedReferenceGenomes}" noSelection="${['': g.message(code: "workflowConfig.selector.referenceGenomes.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.referenceGenomes.all")}" autocomplete="off"/></td>
                </tr>
                <tr>
                    <td><label for="selectorLibraryPreparationKits">${g.message(code: "workflowConfig.selector.libPrepKits")}</label></td>
                    <td><g:select id="selectorLibraryPreparationKits" name="libraryPreparationKits" from="${libraryPreparationKits}" multiple="true" class="use-select-2" optionKey="id" optionValue="name"
                                  value="${selectedLibraryPreparationKits}" noSelection="${['': g.message(code: "workflowConfig.selector.libPrepKits.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.libPrepKits.all")}" autocomplete="off"/></td>
                </tr>
            </table>
        </g:form>

        <h2>${g.message(code: "workflowConfig.fragment")}</h2>
        <g:if test="${selector}">
            <g:form action="index" method="GET" class="selector">
                <label for="fragment">${g.message(code: "workflowConfig.fragment.version")}</label>
                <g:select id="fragment" name="fragment.id" from="${fragments}" class="use-select-2" optionKey="id"
                          optionValue="${{ it ? (it.deprecationDate ? "deprecated (${it.dateCreated}…${it.deprecationDate})" : "current (since ${it.dateCreated})") : "none" }}"
                          value="${fragment.id}" autocomplete="off"/>
            </g:form>
        </g:if>
        <g:else>
            ${g.message(code: "workflowConfig.fragment.none")}
        </g:else>

        <br>

        <g:form controller="workflowConfig" method="POST" useToken="true">
            <input type="hidden" name="selector.id" value="${selector?.id}">
            <input type="hidden" name="fragment.id" value="${fragment?.id}">
            <table class="key-value-table key-input">
                <tr>
                    <td><label for="selectorName1">${g.message(code: "workflowConfig.selector.name")}</label></td>
                    <td><input id="selectorName1" name="selectorName" type="text" value="${editedCmd ? editedCmd?.selectorName : selector?.name}" autocomplete="off"
                               required/></td>
                </tr>
                <tr>
                    <td><label for="workflows">${g.message(code: "workflowConfig.selector.workflows")}</label></td>
                    <td><g:select name="workflows" from="${workflows}" multiple="true" class="use-select-2" optionKey="id" optionValue="displayName"
                                  value="${editedWorkflows}" noSelection="${['': g.message(code: "workflowConfig.selector.workflows.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.workflows.all")}" autocomplete="off"/></td>
                </tr>
                <tr>
                    <td><label for="workflowVersions">${g.message(code: "workflowConfig.selector.versions")}</label></td>
                    <td><g:select name="workflowVersions" from="${workflowVersions}" multiple="true" class="use-select-2" optionKey="id" optionValue="displayName"
                                  value="${editedWorkflowVersions}" noSelection="${['': g.message(code: "workflowConfig.selector.workflows.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.workflows.all")}" autocomplete="off"/></td>

                </tr>
                <tr>
                    <td><label for="projects">${g.message(code: "workflowConfig.selector.projects")}</label></td>
                    <td><g:select name="projects" from="${projects}" multiple="true" class="use-select-2" optionKey="id" optionValue="name"
                                  value="${editedProjects}" noSelection="${['': g.message(code: "workflowConfig.selector.projects.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.projects.all")}" autocomplete="off"/></td>

                </tr>
                <tr>
                    <td><label for="seqTypes">${g.message(code: "workflowConfig.selector.seqTypes")}</label></td>
                    <td><g:select name="seqTypes" from="${seqTypes}" multiple="true" class="use-select-2" optionKey="id" optionValue="displayNameWithLibraryLayout"
                                  value="${editedSeqTypes}" noSelection="${['': g.message(code: "workflowConfig.selector.seqTypes.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.seqTypes.all")}" autocomplete="off"/></td>

                </tr>
                <tr>
                    <td><label for="referenceGenomes">${g.message(code: "workflowConfig.selector.referenceGenomes")}</label></td>
                    <td><g:select name="referenceGenomes" from="${referenceGenomes}" multiple="true" class="use-select-2" optionKey="id" optionValue="name"
                                  value="${editedReferenceGenomes}" noSelection="${['': g.message(code: "workflowConfig.selector.referenceGenomes.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.referenceGenomes.all")}" autocomplete="off"/></td>

                </tr>
                <tr>
                    <td><label for="libraryPreparationKits">${g.message(code: "workflowConfig.selector.libPrepKits")}</label></td>
                    <td><g:select name="libraryPreparationKits" from="${libraryPreparationKits}" multiple="true" class="use-select-2" optionKey="id" optionValue="name"
                                  value="${editedLibraryPreparationKits}" noSelection="${['': g.message(code: "workflowConfig.selector.libPrepKits.all")]}"
                                  data-placeholder="${g.message(code: "workflowConfig.selector.libPrepKits.all")}" autocomplete="off"/></td>

                </tr>
                <tr>
                    <td><label for="type">${g.message(code: "workflowConfig.selector.type")}</label></td>
                    <td><g:select name="type" from="${selectorTypes}" class="use-select-2" value="${editedCmd ? editedCmd?.type : selector?.selectorType ?: SelectorType.GENERIC}"
                                  autocomplete="off"/></td>
                </tr>
                <tr>
                    <td><label for="fragmentName">${g.message(code: "workflowConfig.fragment.name")}</label></td>
                    <td><input id="fragmentName" name="fragmentName" type="text" value="${editedCmd ? editedCmd?.fragmentName : fragment?.name}" autocomplete="off"/></td>
                </tr>

                <tr>
                    <td><label for="customPriority">${g.message(code: "workflowConfig.selector.customPriority")}</label></td>
                    <td><input id="customPriority" name="customPriority" type="text" value="${editedCmd ? editedCmd?.customPriority : selector?.customPriority}" autocomplete="off" required/></td>
                </tr>

                <tr>
                    <td><label>${g.message(code: "workflowConfig.selector.suggestedPriority")}</label></td>
                    <td><label>${selector?.calculateSuggestedPriority()}</label></td>
                </tr>

                <tr>
                    <td><label for="configValue">${g.message(code: "workflowConfig.fragment.value")}</label></td>
                    <td>
                        <textarea id="configValue" name="value" class="code"
                                  rows="10">${editedCmd ? editedCmd.value : fragment?.configValues ? grails.converters.JSON.parse(fragment?.configValues).toString(2) : ""}</textarea>
                        <button class="format">${g.message(code: "workflowConfig.button.format")}</button>
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td>
                        <g:actionSubmit action="create" name="create" value="${g.message(code: "workflowConfig.button.create")}"/>
                        <g:if test="${selector}">
                            <g:actionSubmit action="update" name="update" value="${g.message(code: "workflowConfig.button.update")}"/>
                            <g:actionSubmit action="deprecate" name="deprecate" value="${g.message(code: "workflowConfig.button.deprecate")}"/>
                        </g:if>
                    </td>
                </tr>
            </table>
        </g:form>
    </div>

    <div style='grid-area: related; padding: 1ch'>
        <h2>${g.message(code: "workflowConfig.config.related")}</h2>

        ${g.message(code: "workflowConfig.selector.type")}
        <g:select id="relatedSelectorType" name="relatedSelectorType" from="${selectorTypes}" multiple="true" class="use-select-2" autocomplete="off"/>

        <div id="relatedSelectors">
            <g:each in="${relatedSelectors}" var="selector">
                <div data-type="${selector.selectorType}">
                    <g:render template="selector" model="[selector: selector]"/>
                </div>
            </g:each>
            <g:if test="${!relatedSelectors}">
                ${g.message(code: "workflowConfig.config.related.none")}
            </g:if>
        </div>
    </div>
</div>
</body>
</html>
