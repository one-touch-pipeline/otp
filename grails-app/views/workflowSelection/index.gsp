%{--
  - Copyright 2011-2022 The OTP authors
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
    <title>${g.message(code: "workflowSelection.title")}</title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="common/CommentBox.js"/>
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

    <h2>${g.message(code: "workflowSelection.alignment")}</h2>
    <table class="table table-sm table-striped">
        <tr>
            <th>${g.message(code: "workflowSelection.workflow")}</th>
            <th>${g.message(code: "workflowSelection.seqType")}</th>
            <th>${g.message(code: "workflowSelection.version")}</th>
            <th>${g.message(code: "workflowSelection.species")}</th>
            <th>${g.message(code: "workflowSelection.referenceGenome")}</th>
        </tr>
        <g:each in="${alignmentConf}" var="workflow">
            <tr>
                <td rowspan="${workflow.refGens.species.size() ?: 1}">${workflow.workflow.name}</td>
                <td rowspan="${workflow.refGens.species.size() ?: 1}">${workflow.seqType}</td>
                <td rowspan="${workflow.refGens.species.size() ?: 1}"><otp:editorSwitch
                        roles="ROLE_OPERATOR"
                        template="dropDown"
                        optionKey="id"
                        optionValue="workflowVersion"
                        link="${g.createLink(controller: 'workflowSelection', action: 'updateVersion', params: ['seqType.id' : workflow.seqType.id,
                                                                                                                'workflow.id': workflow.workflow.id])}"
                        values="${workflow.versions}"
                        value="${workflow.version?.id}"
                        noSelection="${["": g.message(code: "workflowSelection.notConfigured")]}"/>
                </td>

                <g:each in="${workflow.refGens.take(1)}" var="r">
                    <td>${r.species.join(" + ")}</td>
                    <td><otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            template="dropDown"
                            optionKey="id"
                            optionValue="name"
                            link="${g.createLink(controller: 'workflowSelection', action: 'updateReferenceGenome', params: [
                                    'seqType.id' : workflow.seqType.id,
                                    'species'    : r.species*.id,
                                    'workflow.id': workflow.workflow.id,
                            ])}"
                            values="${r.referenceGenomes}"
                            value="${r.referenceGenome?.id}"
                            noSelection="${[(null): g.message(code: "workflowSelection.notConfigured")]}"/>
                    </td>
                </g:each>
            <g:if test="${!workflow.refGens}"><td>${g.message(code: "workflowSelection.noSpecies")}</td><td></td></g:if>
            </tr>
            <g:each in="${workflow.refGens.drop(1)}" var="r">
                <tr>
                    <td>${r.species.join(" + ")}</td>
                    <td><otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            template="dropDown"
                            optionKey="id"
                            optionValue="name"
                            link="${g.createLink(controller: 'workflowSelection', action: 'updateReferenceGenome', params: [
                                    'seqType.id' : workflow.seqType.id,
                                    'species'    : r.species*.id,
                                    'workflow.id': workflow.workflow.id,
                            ])}"
                            values="${r.referenceGenomes}"
                            value="${r.referenceGenome?.id}"
                            noSelection="${[(null): g.message(code: "workflowSelection.notConfigured")]}"/>
                    </td>
                </tr>
            </g:each>
        </g:each>
        <g:if test="${!alignmentConf}">
            <tr><td colspan="5">${g.message(code: "workflowSelection.notFound")}</td></tr>
        </g:if>
    </table>

    <h2>${g.message(code: "workflowSelection.analysis")}</h2>
    <table class="table table-sm table-striped">
        <tr>
            <th>${g.message(code: "workflowSelection.workflow")}</th>
            <th>${g.message(code: "workflowSelection.seqType")}</th>
            <th>${g.message(code: "workflowSelection.version")}</th>
        </tr>
        <g:each in="${analysisConf}" var="workflow">
            <tr>
                <td>${workflow.workflow.name}</td>
                <td>${workflow.seqType}</td>
                <td><otp:editorSwitch
                        roles="ROLE_OPERATOR"
                        template="dropDown"
                        optionKey="id"
                        optionValue="workflowVersion"
                        link="${g.createLink(controller: 'workflowSelection', action: 'updateVersion', params: ['seqType.id' : workflow.seqType.id,
                                                                                                                'workflow.id': workflow.workflow.id])}"
                        values="${workflow.versions}"
                        value="${workflow.version?.id}"
                        noSelection="${["": g.message(code: "workflowSelection.notConfigured")]}"/>
                </td>
            </tr>
        </g:each>
        <g:if test="${!analysisConf}">
            <tr><td colspan="3">${g.message(code: "workflowSelection.notFound")}</td></tr>
        </g:if>
    </table>

    <h2>${g.message(code: 'workflowSelection.mergingCriteria')}</h2>
    <table class="table table-sm table-striped">
        <tr>
            <th>${g.message(code: 'workflowSelection.seqType')}</th>
            <th>${g.message(code: 'workflowSelection.libPrepKit')}</th>
            <th>${g.message(code: 'workflowSelection.seqPlatformGroup')}</th>
        </tr>
        <g:each in="${seqTypeMergingCriteria}" var="m">
            <tr>
                <td><g:link controller="projectSeqPlatformGroup" action="index" params='["seqType.id": m.key.id]'>${m.key}</g:link></td>
                <td>
                    <g:if test="${m.key.isExome()}">
                        ${g.message(code: 'workflowSelection.yes')}
                    </g:if>
                    <g:elseif test="${m.key.isWgbs()}">
                        ${g.message(code: 'workflowSelection.no')}
                    </g:elseif>
                    <g:else>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                optionKey="key"
                                optionValue="value"
                                link="${g.createLink(controller: 'workflowSelection', action: 'updateMergingCriteriaLPK', params: ['mergingCriteria.id': m.value.id])}"
                                values="${[(true): g.message(code: 'workflowSelection.yes'), (false): g.message(code: 'workflowSelection.no')]}"
                                value="${m.value?.useLibPrepKit}"/>
                    </g:else>
                </td>
                <td><otp:editorSwitch
                        roles="ROLE_OPERATOR"
                        template="dropDown"
                        link="${g.createLink(controller: 'workflowSelection', action: 'updateMergingCriteriaSPG', params: ['mergingCriteria.id': m.value.id])}"
                        values="${de.dkfz.tbi.otp.dataprocessing.MergingCriteria.SpecificSeqPlatformGroups.values()}"
                        value="${m.value?.useSeqPlatformGroup}"/>
                </td>
            </tr>
        </g:each>
    </table>
</body>
</html>
