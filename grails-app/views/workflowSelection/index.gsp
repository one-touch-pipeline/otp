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
<%@ page import="de.dkfz.tbi.otp.dataprocessing.MergingCriteria" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${g.message(code: "workflowSelection.title")}</title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="pages/workflowSelection/index.js"/>
    <asset:javascript src="pages/workflowSelection/alignmentTable.js"/>
    <asset:javascript src="pages/workflowSelection/analysisTable.js"/>
    <asset:javascript src="pages/workflowSelection/initialize.js"/>
    <asset:stylesheet src="pages/workflowSelection/index.less"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <div class="project-selection-header-container">
        <div class="grid-element">
            <g:render template="/templates/bootstrap/projectSelection"/>
        </div>

        <div class="grid-element comment-box">
            <g:render template="/templates/commentBox" model="[
                    commentable     : selectedProject,
                    targetController: 'projectConfig',
                    targetAction    : 'saveProjectComment',
            ]"/>
        </div>
    </div>

    <div class="mb-4">
        <g:render template="/projectConfig/tabMenu"/>
    </div>

    <h1>${g.message(code: "workflowSelection.title")}</h1>

    <div class="accordion" id="workflowSelectionAccordion">
        <div class="accordion-item">
            <h2 class="accordion-header" id="headingFastqc">
                <button class="accordion-button" type="button" data-bs-toggle="collapse" data-bs-target="#collapseFastqc" aria-expanded="true"
                        aria-controls="collapseFastqc">
                    ${g.message(code: "workflowSelection.fastqc")}
                </button>
            </h2>

            <div id="collapseFastqc" class="accordion-collapse collapse show" aria-labelledby="headingFastqc">
                <div class="accordion-body">
                    <otp:annotation type="info">
                        ${g.message(code: "workflowSelection.fastqc.info", args: [contactDataSupportEmail, faqLink])}
                    </otp:annotation>
                    <table id="fastqcTable" class="table table-sm table-striped table-bordered">
                        <thead>
                        <tr>
                            <th>${g.message(code: "workflowSelection.workflow")}</th>
                            <th>${g.message(code: "workflowSelection.version")}</th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${fastqcVersions}" var="fastqcVersion">
                            <tr>
                                <td>${fastqcVersion.workflow.name}</td>
                                <td><otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="dropDown"
                                        optionKey="id"
                                        optionValue="nameWithDefault"
                                        link="${g.createLink(controller: 'workflowSelection', action: 'updateVersion', params: ['workflow': fastqcVersion.workflow.id])}"
                                        sucessHandler="workflowSelectionUpdateSuccessHandler"
                                        values="${fastqcVersion.versions}"
                                        value="${fastqcVersion.version?.id}"
                                        noSelection="${["": g.message(code: "workflowSelection.notConfigured")]}"/>
                                </td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div class="accordion-item">
            <h2 class="accordion-header" id="headingAlignment">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse"
                        data-bs-target="#collapseAlignment" aria-expanded="false" aria-controls="collapseAlignment">
                    ${g.message(code: "workflowSelection.alignment")}
                </button>
            </h2>

            <div id="collapseAlignment" class="accordion-collapse collapse" aria-labelledby="headingAlignment">
                <div class="accordion-body">
                    <table id="alignmentTable" class="table table-sm table-striped table-bordered">
                        <thead>
                        <tr>
                            <th>${g.message(code: "workflowSelection.workflow")}</th>
                            <th>${g.message(code: "workflowSelection.seqType")}</th>
                            <th>${g.message(code: "workflowSelection.version")}</th>
                            <th>${g.message(code: "workflowSelection.referenceGenome")}</th>
                            <th>${g.message(code: "workflowSelection.species")}</th>
                            <sec:ifAllGranted roles="ROLE_OPERATOR">
                                <th></th>
                            </sec:ifAllGranted>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${alignment.conf}" var="config">
                            <tr>
                                <td>${config.workflow.name}</td>
                                <td>${config.seqType}</td>
                                <td>${config.version.workflowVersion}</td>
                                <td>${config.refGen.referenceGenome.name}</td>
                                <td>${config.refGen.species.join(' + ')}</td>
                                <sec:ifAllGranted roles="ROLE_OPERATOR">
                                    <td>
                                        <button class="btn float-end btn-primary remove-config-btn" data-ref-genome-selector="${config.refGen.id}"
                                                data-version-selector="${config.workflowVersionSelectorId}">
                                            <g:message code="workflowSelection.removeConfig"/></button>
                                    </td>
                                </sec:ifAllGranted>
                            </tr>
                        </g:each>
                        </tbody>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <tfoot>
                            <tr>
                                <td>
                                    <g:select required="true" class="use-select-2 w-100" id="alignment-workflow-select" name="workflow-select"
                                              noSelection="${['': 'Unselected']}" from="${alignment.workflows}" optionKey="id" optionValue="displayName"/>
                                </td>
                                <td>
                                    <g:select required="true" class="use-select-2 w-100" id="alignment-seq-type-select" name="seq-type-select"
                                              noSelection="${['': 'Unselected']}" from="${alignment.seqTypes}" optionKey="id"
                                              optionValue="displayNameWithLibraryLayout"/>
                                </td>
                                <td>
                                    <g:select required="true" class="use-select-2 w-100" id="alignment-version-select" name="version-select"
                                              noSelection="${['': 'Unselected']}" from="${alignment.versions}" optionKey="id"
                                              optionValue="nameWithDefaultAndWorkflow"/>
                                </td>
                                <td>
                                    <g:select required="true" class="use-select-2 w-100" id="alignment-ref-genome-select" name="alignment-ref-genome-select"
                                              noSelection="${['': 'Unselected']}" from="${alignment.referenceGenomes}" optionKey="id" optionValue="name"/>
                                </td>
                                <td>
                                    <g:select required="true" class="use-select-2 w-100" id="alignment-species-select" name="alignment-species-select"
                                              multiple="true"
                                              from="${alignment.species}" optionKey="id" optionValue="displayName"/>
                                </td>
                                <td>
                                    <button type="submit" class="btn float-end btn-primary" id="add-alignment-config-btn"><g:message
                                            code="workflowSelection.addOrUpdateConfig"/></button>
                                </td>
                            </tr>
                            </tfoot>
                        </sec:ifAllGranted>
                    </table>
                </div>
            </div>
        </div>

        <div class="accordion-item">
            <h2 class="accordion-header" id="headingAnalysis">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapseAnalysis" aria-expanded="false"
                        aria-controls="collapseAnalysis">
                    ${g.message(code: "workflowSelection.analysis")}
                </button>
            </h2>

            <div id="collapseAnalysis" class="accordion-collapse collapse" aria-labelledby="headingAnalysis">
                <div class="accordion-body">
                    <table id="analysisTable" class="table table-sm table-striped table-bordered">
                        <thead>
                        <tr>
                            <th>${g.message(code: "workflowSelection.workflow")}</th>
                            <th>${g.message(code: "workflowSelection.seqType")}</th>
                            <th>${g.message(code: "workflowSelection.version")}</th>
                            <sec:ifAllGranted roles="ROLE_OPERATOR">
                                <th></th>
                            </sec:ifAllGranted>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${analysis.conf}" var="config">
                            <tr>
                                <td>${config.workflow.name}</td>
                                <td>${config.seqType}</td>
                                <td>${config.version.workflowVersion}</td>
                                <sec:ifAllGranted roles="ROLE_OPERATOR">
                                    <td>
                                        <button class="btn float-end btn-primary remove-config-btn"
                                                data-version-selector="${config.workflowVersionSelectorId}">
                                            <g:message code="workflowSelection.removeConfig"/>
                                        </button>
                                    </td>
                                </sec:ifAllGranted>
                            </tr>
                        </g:each>
                        </tbody>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <tfoot>
                            <tr>
                                <td>
                                    <g:select required="true" class="use-select-2 w-100" id="analysis-workflow-select" name="workflow-select"
                                              noSelection="${['': 'Unselected']}" from="${analysis.workflows}" optionKey="id" optionValue="displayName"/>
                                </td>
                                <td>
                                    <g:select required="true" class="use-select-2 w-100" id="analysis-seq-type-select" name="seq-type-select"
                                              noSelection="${['': 'Unselected']}" from="${analysis.seqTypes}" optionKey="id"
                                              optionValue="displayNameWithLibraryLayout"/>
                                </td>
                                <td>
                                    <g:select required="true" class="use-select-2 w-100" id="analysis-version-select" name="version-select"
                                              noSelection="${['': 'Unselected']}" from="${analysis.versions}" optionKey="id"
                                              optionValue="nameWithDefaultAndWorkflow"/>
                                </td>
                                <td>
                                    <button type="submit" class="btn float-end btn-primary" id="add-analysis-config-btn">
                                        ${g.message(code: "workflowSelection.addOrUpdateConfig")}
                                    </button>
                                </td>
                            </tr>
                            </tfoot>
                        </sec:ifAllGranted>
                    </table>
                </div>
            </div>
        </div>

        <div class="accordion-item">
            <h2 class="accordion-header" id="headingCriteria">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapseCriteria" aria-expanded="false"
                        aria-controls="collapseCriteria">
                    ${g.message(code: 'workflowSelection.mergingCriteria')}
                </button>
            </h2>

            <div id="collapseCriteria" class="accordion-collapse collapse" aria-labelledby="headingCriteria">
                <div class="accordion-body">
                    <table id="mergingCriteriaTable" class="table table-sm table-striped table-bordered">
                        <thead>
                        <tr>
                            <th>${g.message(code: 'workflowSelection.seqType')}</th>
                            <th>${g.message(code: 'workflowSelection.libPrepKit')}</th>
                            <th>${g.message(code: 'workflowSelection.seqPlatformGroup')}</th>
                        </tr>
                        </thead>
                        <tbody>
                        <g:each in="${seqTypeMergingCriteria}" var="mergingCriteria">
                            <tr>
                                <td><g:link controller="projectSeqPlatformGroup" action="index"
                                            params='["seqType.id": mergingCriteria.key.id]'>${mergingCriteria.key}</g:link></td>
                                <td>
                                    <g:if test="${mergingCriteria.key.needsBedFile}">
                                        ${g.message(code: 'workflowSelection.yes')}
                                    </g:if>
                                    <g:elseif test="${mergingCriteria.key.isWgbs()}">
                                        ${g.message(code: 'workflowSelection.no')}
                                    </g:elseif>
                                    <g:else>
                                        <otp:editorSwitch
                                                roles="ROLE_OPERATOR"
                                                template="dropDown"
                                                optionKey="key"
                                                optionValue="value"
                                                link="${g.createLink(controller: 'workflowSelection', action: 'updateMergingCriteriaLPK', params: ['mergingCriteria.id': mergingCriteria.value.id])}"
                                                values="${[(true): g.message(code: 'workflowSelection.yes'), (false): g.message(code: 'workflowSelection.no')]}"
                                                value="${mergingCriteria.value?.useLibPrepKit}"/>
                                    </g:else>
                                </td>
                                <td><otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="dropDown"
                                        link="${g.createLink(controller: 'workflowSelection', action: 'updateMergingCriteriaSPG', params: ['mergingCriteria.id': mergingCriteria.value.id])}"
                                        values="${MergingCriteria.SpecificSeqPlatformGroups.values()}"
                                        value="${mergingCriteria.value?.useSeqPlatformGroup}"/>
                                </td>
                            </tr>
                        </g:each>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
