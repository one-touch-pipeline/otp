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
    <title>${g.message(code: "workflowSelection.title")}</title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/workflowSelection/index.js"/>
    <asset:javascript src="pages/workflowSelection/alignmentTable.js"/>
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

    <div>
        <g:render template="/projectConfig/tabMenu"/>
    </div>
    <br>

    <h1>${g.message(code: "workflowSelection.title")}</h1>

    <h2>${g.message(code: "workflowSelection.fastqc")}</h2>
    <otp:annotation type="info">
        ${g.message(code: "workflowSelection.fastqc.info", args: [contactDataSupportEmail, faqLink])}
    </otp:annotation>
    <table id="fastqc" class="table table-sm table-striped">
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

    <h2>${g.message(code: "workflowSelection.alignment")}</h2>
    <table id="alignment" class="table table-sm table-striped">
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
        <g:each in="${alignmentConf}" var="config">
            <tr>
                <td>${config.workflow.name}</td>
                <td>${config.seqType}</td>
                <td>${config.version.workflowVersion}</td>
                <td>${config.refGen.referenceGenome.name}</td>
                <td>${config.refGen.species.join(' + ')}</td>
                <sec:ifAllGranted roles="ROLE_OPERATOR">
                    <td>
                        <button class="btn float-right btn-primary remove-alignment-config-btn" data-ref-genome-selector="${config.refGen.id}"
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
                              noSelection="${['': 'Unselected']}" from="${alignmentWorkflows}" optionKey="id" optionValue="displayName"/>
                </td>
                <td>
                    <g:select required="true" class="use-select-2 w-100" id="alignment-seq-type-select" name="seq-type-select"
                              noSelection="${['': 'Unselected']}" from="${seqTypes}" optionKey="id" optionValue="displayNameWithLibraryLayout"/>
                </td>
                <td>
                    <g:select required="true" class="use-select-2 w-100" id="alignment-version-select" name="version-select"
                              noSelection="${['': 'Unselected']}" from="${alignmentVersions}" optionKey="id" optionValue="nameWithDefaultAndWorkflow"/>
                </td>
                <td>
                    <g:select required="true" class="use-select-2 w-100" id="alignment-ref-genome-select" name="alignment-ref-genome-select"
                              noSelection="${['': 'Unselected']}" from="${referenceGenomes}" optionKey="id" optionValue="name"/>
                </td>
                <td>
                    <g:select required="true" class="use-select-2 w-100" id="alignment-species-select" name="alignment-species-select" multiple="true"
                              from="${species}" optionKey="id" optionValue="displayName"/>
                </td>
                <td>
                    <button type="submit" class="btn float-right btn-primary add-alignment-config-btn"><g:message
                            code="workflowSelection.addOrUpdateConfig"/></button>
                </td>
            </tr>
            </tfoot>
        </sec:ifAllGranted>
    </table>

    <h2>${g.message(code: "workflowSelection.analysis")}</h2>
    <table id="analysis" class="table table-sm table-striped">
        <thead>
        <tr>
            <th>${g.message(code: "workflowSelection.workflow")}</th>
            <th>${g.message(code: "workflowSelection.seqType")}</th>
            <th>${g.message(code: "workflowSelection.version")}</th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${analysisConf}" var="config">
            <tr>
                <td>${config.workflow.name}</td>
                <td>${config.seqType}</td>
                <td><otp:editorSwitch
                        roles="ROLE_OPERATOR"
                        template="dropDown"
                        optionKey="id"
                        optionValue="workflowVersion"
                        link="${g.createLink(controller: 'workflowSelection', action: 'updateVersion', params: ['seqType.id' : config.seqType.id,
                                                                                                                'workflow.id': config.workflow.id])}"
                        values="${config.versions}"
                        value="${config.version?.id}"
                        noSelection="${["": g.message(code: "workflowSelection.notConfigured")]}"/>
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>

    <h2>${g.message(code: 'workflowSelection.mergingCriteria')}</h2>
    <table id="mergingCriteria" class="table table-sm table-striped">
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
</body>
</html>
