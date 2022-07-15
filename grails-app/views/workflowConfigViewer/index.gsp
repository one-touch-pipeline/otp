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
    <title>${g.message(code: "workflowConfigViewer")}</title>
    <asset:stylesheet src="pages/workflowConfig/viewer/styles.less"/>
    <asset:stylesheet src="pages/workflowConfig/selector/styles.less"/>
    <asset:javascript src="pages/workflowConfigViewer/index.js"/>
    <asset:javascript src="pages/workflowConfigViewer/selector.js"/>
</head>

<body>
<div class="container-fluid outer-container">

    <h3>${g.message(code: "workflowConfigViewer")}</h3>

    <div class="row align-items-start">
        <div class="col-8">
            <h6><strong>${g.message(code: "workflowConfig.selector")}</strong></h6>
            <g:form class="selector" autocomplete="off">
                ${g.message(code: "workflowConfigViewer.selector.note")}
                <table class="key-value-table key-input">
                    <tr>
                        <td><label for="workflowSelector">${g.message(code: "workflowConfigViewer.workflow")}</label></td>
                        <td>
                            <g:select id="workflowSelector"
                                      name="workflow"
                                      from="${workflows}"
                                      class="use-select-2"
                                      noSelection="${['': '']}"
                                      optionKey="id"
                                      optionValue="displayName"
                                      data-placeholder="${g.message(code: "workflowConfigViewer.workflowSelection")}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><label for="workflowVersionSelector">${g.message(code: "workflowConfigViewer.workflowVersion")}</label></td>
                        <td>
                            <g:select id="workflowVersionSelector"
                                      name="workflowVersion"
                                      from="${workflowVersions}"
                                      class="use-select-2"
                                      noSelection="${['': '']}"
                                      optionKey="id"
                                      optionValue="displayName"
                                      data-placeholder="${g.message(code: "workflowConfigViewer.workflowVersionSelection")}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><label for="projectSelector">${g.message(code: "workflowConfigViewer.project")}</label></td>
                        <td>
                            <g:select id="projectSelector"
                                      name="project"
                                      from="${projects}"
                                      class="use-select-2"
                                      noSelection="${['': '']}"
                                      optionKey="id"
                                      optionValue="name"
                                      data-placeholder="${g.message(code: "workflowConfigViewer.projectSelection")}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><label for="seqTypeSelector">${g.message(code: "workflowConfigViewer.seqType")}</label></td>
                        <td>
                            <g:select id="seqTypeSelector"
                                      name="seqType"
                                      from="${seqTypes}"
                                      class="use-select-2"
                                      noSelection="${['': '']}"
                                      optionKey="id"
                                      optionValue="displayNameWithLibraryLayout"
                                      data-placeholder="${g.message(code: "workflowConfigViewer.seqTypeSelection")}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><label for="refGenSelector">${g.message(code: "workflowConfigViewer.refGen")}</label></td>
                        <td>
                            <g:select id="refGenSelector"
                                      name="refGen"
                                      from="${referenceGenomes}"
                                      class="use-select-2"
                                      noSelection="${['': '']}"
                                      optionKey="id"
                                      optionValue="name"
                                      data-placeholder="${g.message(code: "workflowConfigViewer.refGenSelection")}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><label for="libPrepKitSelector">${g.message(code: "workflowConfigViewer.libPrepKit")}</label></td>
                        <td>
                            <g:select id="libPrepKitSelector"
                                      name="libPrepKit"
                                      from="${libraryPreparationKits}"
                                      class="use-select-2"
                                      noSelection="${['': '']}"
                                      optionKey="id"
                                      optionValue="name"
                                      data-placeholder="${g.message(code: "workflowConfigViewer.libPrepKitSelection")}"/>
                        </td>
                    </tr>
                    <tr>
                        <td><label for="configValue">${g.message(code: "workflowConfigViewer.fragment.value")}</label></td>
                        <td>
                            <textarea id="configValue" name="value" class="code" rows="10" readonly></textarea>
                        </td>
                    </tr>
                </table>
            </g:form>
        </div>

        <div class="col-4" id="right-side">
            <h6><strong>${g.message(code: "workflowConfig.config.related")}</strong></h6>

            ${g.message(code: "workflowConfig.selector.type")}
            <g:select id="relatedSelectorType"
                      name="relatedSelectorType"
                      from="${selectorTypes}"
                      multiple="true"
                      class="use-select-2"
                      autocomplete="off"
                      style="width: 50%"
                      data-placeholder="${g.message(code: "workflowConfig.selector.type.filter")}"/>

            <div id="relatedSelectors" class="my-2"></div>
        </div>
    </div>
</div>
</body>
</html>
