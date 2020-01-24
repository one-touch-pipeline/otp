%{--
  - Copyright 2011-2019 The OTP authors
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
    <meta name="layout" content="main"/>
    <title><g:message code="projectOverview.title" args="[project?.name]"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/projectConfig/index/functions.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <g:if test="${projects}">
        <div class="project-selection-header-container">
            <div class="grid-element">
                <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]"/>
            </div>
            <div class="grid-element comment-box">
                <g:render template="/templates/commentBox" model="[
                        commentable     : project,
                        targetController: 'projectConfig',
                        targetAction    : 'saveProjectComment',
                ]"/>
            </div>
        </div>
        <br>
        <h1><g:message code="projectOverview.title" args="[project?.name]"/></h1>
        <div id="projectOverviewDates">
            <table class="key-value-table key-help-input">
                <tr>
                    <td><g:message code="project.individualPrefix"/></td>
                    <td></td>
                    <td>${project.individualPrefix}</td>
                </tr>
                <tr>
                    <td><g:message code="project.directory"/></td>
                    <td></td>
                    <td>${directory}</td>
                </tr>
                <tr>
                    <td><g:message code="project.analysisDirectory"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'dirAnalysis'])}"
                                value="${project.dirAnalysis}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.keywords"/></td>
                    <td></td>
                    <td>
                        <g:if test="${project.keywords}">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectConfig', action: 'updateKeywords', params: ['project.id': project.id])}"
                                    value="${project.listAllKeywords()}"/>
                        </g:if>
                        <g:else>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectConfig', action: 'updateKeywords', params: ['project.id': project.id])}"
                                    value="${g.message(code: 'project.keywords.empty')}"/>
                        </g:else>
                    </td>
                </tr>
                <tr>
                    <td style="padding-top: 1em; padding-bottom: 1em; vertical-align: 1em"><g:message code="project.description"/></td>
                    <td></td>
                    <td>
                        <div style="overflow: auto; max-height: 20em;">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="textArea"
                                    link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'description'])}"
                                    value="${project.description}"/>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.relatedProjects"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'relatedProjects'])}"
                                value="${project.relatedProjects}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.tumorEntity"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: "projectConfig", action: 'updateTumorEntity', params: ['project.id': project.id, 'fieldName': 'tumorEntity'])}"
                                noSelection="${['': 'None']}"
                                values="${tumorEntities}"
                                value="${project.tumorEntity}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.speciesWithStrain"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: "projectConfig", action: 'updateSpeciesWithStrain', params: ['project.id': project.id, 'fieldName': 'speciesWithStrain'])}"
                                optionKey="id"
                                noSelection="${['': 'None']}"
                                values="${allSpeciesWithStrain}"
                                value="${project.speciesWithStrain}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.unixGroup"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'unixGroup'])}"
                                value="${project.unixGroup}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.costCenter"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'costCenter'])}"
                                value="${project.costCenter}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.organizationalUnit"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'organizationalUnit'])}"
                                value="${project.organizationalUnit}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.fundingBody"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'fundingBody'])}"
                                value="${project.fundingBody}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.grantId"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'grantId'])}"
                                value="${project.grantId}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.projectType"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: "projectConfig", action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'projectType'])}"
                                values="${projectTypes}"
                                value="${project.projectType}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.forceCopyFiles"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateCopyFiles', params: ['project.id': project.id, 'fieldName': 'forceCopyFiles'])}"
                                values="${["true", "false"]}"
                                value="${project.forceCopyFiles}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.creationDate"/></td>
                    <td></td>
                    <td>${creationDate}</td>
                </tr>
                <tr>
                    <td><g:message code="project.lastDate"/></td>
                    <td></td>
                    <td>${lastReceivedDate}</td>
                </tr>
                <tr>
                    <td><g:message code="project.endDate"/></td>
                    <td></td>
                    <td>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="date"
                                    link="${g.createLink(controller: 'projectConfig', action: 'updateProjectFieldDate', params: ['project.id': project.id, 'fieldName': 'endDate'])}"
                                    value="${project.endDate}"/>
                        </sec:ifAllGranted>
                        <sec:ifNotGranted roles="ROLE_OPERATOR">
                            ${project.endDate ?: g.message(code: "project.endDate.empty")}
                        </sec:ifNotGranted>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.storageUntil"/></td>
                    <td></td>
                    <td>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="date"
                                    link="${g.createLink(controller: 'projectConfig', action: 'updateProjectFieldDate', params: ['project.id': project.id, 'fieldName': 'storageUntil'])}"
                                    value="${project.storageUntil}"/>
                        </sec:ifAllGranted>
                        <sec:ifNotGranted roles="ROLE_OPERATOR">
                            ${project.storageUntil ?: g.message(code: "project.storageUntil.empty")}
                        </sec:ifNotGranted>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.nameInMetadata"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'nameInMetadataFiles'])}"
                                value="${project.nameInMetadataFiles}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.processingPriority"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProcessingPriority', params: ['project.id': project.id, 'fieldName': 'processingPriority'])}"
                                values="${processingPriorities}"
                                value="${processingPriority ?: ""}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.group"/></td>
                    <td></td>
                    <td><otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            template="dropDown"
                            link="${g.createLink(controller: 'projectConfig', action: 'updateProjectGroup', params: ['project.id': project.id, 'fieldName': 'projectGroup'])}"
                            noSelection="['':'None']"
                            values="${allProjectGroups}"
                            value="${project.projectGroup}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.sampleParser"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateSampleIdentifierParserBeanName', params: ['project.id': project.id, 'fieldName': 'sampleIdentifierParserBeanName'])}"
                                values="${sampleIdentifierParserBeanNames}"
                                value="${project.sampleIdentifierParserBeanName}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.qcThresholdHandling"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateQcThresholdHandling', params: ['project.id': project.id, 'fieldName': 'qcThresholdHandling'])}"
                                values="${qcThresholdHandlingDropdown}"
                                value="${project.qcThresholdHandling}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.fingerPrinting"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateFingerPrinting', id: project.id)}"
                                values="${["true", "false"]}"
                                value="${project.fingerPrinting}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.processingNotification.message"/></td>
                    <td class="help" title="${g.message(code: "project.processingNotification.message.detail")}"></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProcessingNotification', id: project.id)}"
                                values="${["true", "false"]}"
                                value="${project.processingNotification}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.qcTrafficLightNotification.message"/></td>
                    <td class="help" title="${g.message(code: "project.qcTrafficLightNotification.message.detail")}"></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateQcTrafficLightNotification', id: project.id)}"
                                values="${["true", "false"]}"
                                value="${project.qcTrafficLightNotification}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.customFinalNotification.message"/></td>
                    <td class="help" title="${g.message(code: "project.customFinalNotification.message.detail")}"></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateCustomFinalNotification', id: project.id)}"
                                values="${["true", "false"]}"
                                value="${project.customFinalNotification}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.closed"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: "updateClosed", params: ['project.id': project.id, 'fieldName': 'closed'])}"
                                values="${["true", "false"]}"
                                value="${closed}"/>
                    </td>
                </tr>
                <sec:ifAllGranted roles="ROLE_OPERATOR">
                    <tr>
                        <td style="padding-top: 1em; padding-bottom: 1em; vertical-align: 1em"><g:message
                                code="project.internalNotes"/></td>
                        <td></td>
                        <td>
                            <div style="overflow: auto; max-height: 20em;">
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="textArea"
                                        link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['project.id': project.id, 'fieldName': 'internalNotes'])}"
                                        value="${project.internalNotes}"/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td><g:message code="projectOverview.projectInfos"/></td>
                        <td></td>
                        <td>
                            <g:link controller='projectInfo' action='list'>
                                <g:message code="projectOverview.projectInfos.link" args="[project.projectInfos.size()]"/>
                            </g:link>
                        </td>
                    </tr>
                </sec:ifAllGranted>
            </table>
        </div>

        <h2>${g.message(code: 'projectOverview.alignmentInformation.title')}</h2>

        <div>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <h3><g:message code="projectOverview.alignmentInformation.configureRoddy"/></h3>

                <div class="show_button">
                    <ul>
                        <g:each in="${roddySeqTypes}" var="seqType">
                            <li>
                                <g:if test="${seqType.isRna()}">
                                    <g:link controller='configurePipeline' action='rnaAlignment' params='["project.id": project.id, "seqType.id": seqType.id]'
                                            class="configure">
                                        ${seqType.displayNameWithLibraryLayout}
                                    </g:link>
                                </g:if>
                                <g:else>
                                    <g:link controller='configurePipeline' action='alignment' params='["project.id": project.id, "seqType.id": seqType.id]'
                                            class="configure">
                                        ${seqType.displayNameWithLibraryLayout}
                                    </g:link>
                                </g:else>
                            </li>
                        </g:each>
                    </ul>
                </div>
            </sec:ifAllGranted>
            <div id="alignment_info">
                <table style="visibility: hidden" id="alignment_info_table">
                    <tr>
                        <th>${g.message(code: 'projectOverview.alignmentInformation.tool')}</th>
                        <th>${g.message(code: 'projectOverview.alignmentInformation.version')}</th>
                        <th>${g.message(code: 'projectOverview.alignmentInformation.arguments')}</th>
                    </tr>

                </table>
            </div>
        </div>

        <div>
            <h3>${g.message(code: 'projectOverview.mergingCriteria')}</h3>
            <table>
                <tr>
                    <th>${g.message(code: 'projectOverview.mergingCriteria.seqType')}</th>
                    <th>${g.message(code: 'projectOverview.mergingCriteria.libPrepKit')}</th>
                    <th>${g.message(code: 'projectOverview.mergingCriteria.seqPlatformGroup')}</th>
                </tr>
                <g:each in="${seqTypeMergingCriteria}" var="m">
                    <tr>
                        <td>
                            <g:link controller="mergingCriteria" action="projectAndSeqTypeSpecific"
                                    params='["project.id": project.id, "seqType.id": m.key.id]'>
                                ${m.key}
                            </g:link>
                        </td>
                        <td>
                            ${m.value?.useLibPrepKit != null ? m.value.useLibPrepKit : "Not configured"}
                        </td>
                        <td>
                            ${m.value?.useSeqPlatformGroup ?: "Not configured"}
                        </td>
                    </tr>
                </g:each>
            </table>
        </div>

        <div>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <h3><g:message code="projectOverview.alignmentInformation.configureCellRanger"/></h3>

                <div class="show_button">
                    <ul>
                        <g:each in="${cellRangerSeqTypes}" var="seqType">
                            <li>
                                <g:link controller='configureCellRangerPipeline' action='index' params='["project.id": project.id, "seqType.id": seqType.id]'
                                        class="configure">
                                    ${seqType.displayNameWithLibraryLayout}
                                </g:link>
                            </li>
                        </g:each>
                    </ul>
                </div>
            </sec:ifAllGranted>
            <table>
                <tr>
                    <th>${g.message(code: 'projectOverview.alignmentInformation.cellRanger.seqType')}</th>
                    <th>${g.message(code: 'projectOverview.alignmentInformation.cellRanger.version')}</th>
                </tr>
                <g:each in="${cellRangerOverview}" var="m">
                    <tr>
                        <td>
                            ${m.seqType?.getDisplayNameWithLibraryLayout()}
                        </td>
                        <td>
                            ${m.config?.programVersion ?: "Not configured"}
                        </td>
                    </tr>
                </g:each>
            </table>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:link controller="cellRanger">${g.message(code: 'projectOverview.alignmentInformation.cellRanger.link')}</g:link>
            </sec:ifAllGranted>
        </div>
        <br>

        <div class="otpDataTables">
            <h3>${g.message(code: 'projectOverview.listReferenceGenome.title')}</h3>
            <otp:dataTable
                    codes="${[
                            'projectOverview.index.referenceGenome.sequenceTypeName',
                            'projectOverview.index.referenceGenome.sampleTypeName',
                            'projectOverview.index.referenceGenome',
                            'projectOverview.index.statSizeFile',
                            'projectOverview.index.adapterTrimming',
                    ]}"
                    id="listReferenceGenome"/>
        </div>
        <br>

        <h2>${g.message(code: 'projectOverview.analysis.title')}</h2>

        <div>
            <h3>${g.message(code: 'projectOverview.qc.thresholds')}</h3>

            <div>
                <g:link controller="qcThreshold" action="projectConfiguration">${g.message(code: 'projectOverview.qc.link')}</g:link>
            </div>

            <h3>${g.message(code: 'projectOverview.analysis.thresholds')}</h3>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <ul>
                    <li>
                        <g:link controller='configureAnalysis' params='["project.id": project.id]' class="configure">
                            ${g.message(code: 'projectOverview.analysis.link')}
                        </g:link>
                    </li>
                </ul>
            </sec:ifAllGranted>
            <g:if test="${thresholdsTable}">
                <table>
                    <g:each var="row" in="${thresholdsTable}" status="i">
                        <tr>
                            <g:each var="cell" in="${row}">
                                <g:if test="${i == 0}">
                                    <th>${cell}</th>
                                </g:if>
                                <g:else>
                                    <td class="tableEntry">${cell}</td>
                                </g:else>
                            </g:each>
                        </tr>
                    </g:each>
                </table>
            </g:if>
            <g:else>
                ${g.message(code: 'projectOverview.analysis.noThresholds')}
            </g:else>
        </div>

        <div>
            <h3>${g.message(code: 'projectOverview.snv.title')}</h3>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:message code="projectOverview.snv.configure"/>
                <ul>
                    <g:each in="${snvSeqTypes}" var="seqType">
                        <li>
                            <g:link controller='ConfigureSnvPipeline' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                ${seqType.displayNameWithLibraryLayout}
                            </g:link>
                        </li>
                    </g:each>
                </ul>
                <br>
            </sec:ifAllGranted>
            <table>
                <g:each var="row" in="${snvConfigTable}" status="i">
                    <tr>
                        <g:each var="cell" in="${row}">
                            <g:if test="${i == 0}">
                                <th>${cell}</th>
                            </g:if>
                            <g:else>
                                <td class="tableEntry">${cell}</td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:each>
            </table>
            <br>
        </div>

        <div>
            <h3>${g.message(code: 'projectOverview.indel.title')}</h3>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:message code="projectOverview.indel.configure"/>
                <ul>
                    <g:each in="${indelSeqTypes}" var="seqType">
                        <li>
                            <g:link controller='ConfigureIndelPipeline' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                ${seqType.displayNameWithLibraryLayout}
                            </g:link>
                        </li>
                    </g:each>
                </ul>
            </sec:ifAllGranted>
            <table>
                <g:each var="row" in="${indelConfigTable}" status="i">
                    <tr>
                        <g:each var="cell" in="${row}">
                            <g:if test="${i == 0}">
                                <th>${cell}</th>
                            </g:if>
                            <g:else>
                                <td class="tableEntry">${cell}</td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:each>
            </table>
            <br>
        </div>

        <div>
            <h3>${g.message(code: 'projectOverview.sophia.title')}</h3>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:message code="projectOverview.sophia.configure"/>
                <ul>
                    <g:each in="${sophiaSeqTypes}" var="seqType">
                        <li>
                            <g:if test="${!checkSophiaReferenceGenome[seqType]}">
                                <g:link controller='ConfigureSophiaPipeline' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                    ${seqType.displayNameWithLibraryLayout}
                                </g:link>
                            </g:if>
                            <g:else>
                                ${seqType.displayNameWithLibraryLayout}: ${checkSophiaReferenceGenome[seqType]}
                            </g:else>
                        </li>
                    </g:each>
                </ul>
            </sec:ifAllGranted>
            <table>
                <g:each var="row" in="${sophiaConfigTable}" status="i">
                    <tr>
                        <g:each var="cell" in="${row}">
                            <g:if test="${i == 0}">
                                <th>${cell}</th>
                            </g:if>
                            <g:else>
                                <td class="tableEntry">${cell}</td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:each>
            </table>
            <br>
        </div>

        <div>
            <h3>${g.message(code: 'projectOverview.aceseq.title')}</h3>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:message code="projectOverview.aceseq.configure"/>
                <ul>
                    <g:each in="${aceseqSeqTypes}" var="seqType">
                        <li>
                            <g:if test="${!checkAceseqReferenceGenome[seqType]}">
                                <g:link controller='ConfigureAceseqPipeline' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                    ${seqType.displayNameWithLibraryLayout}
                                </g:link>
                            </g:if>
                            <g:else>
                                ${seqType.displayNameWithLibraryLayout}: ${checkAceseqReferenceGenome[seqType]}
                            </g:else>
                        </li>
                    </g:each>
                </ul>
            </sec:ifAllGranted>
            <table>
                <g:each var="row" in="${aceseqConfigTable}" status="i">
                    <tr>
                        <g:each var="cell" in="${row}">
                            <g:if test="${i == 0}">
                                <th>${cell}</th>
                            </g:if>
                            <g:else>
                                <td class="tableEntry">${cell}</td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:each>
            </table>
            <br>
        </div>

        <div>
            <h3>${g.message(code: 'projectOverview.runYapsa.title')}</h3>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:message code="projectOverview.runYapsa.configure"/>
                <ul>
                    <g:each in="${runYapsaSeqTypes}" var="seqType">
                        <li>
                            <g:link controller='configureRunYapsaPipeline' action='index' params='["project.id": project.id, "seqType.id": seqType.id]'
                                    class="configure">
                                ${seqType.displayNameWithLibraryLayout}
                            </g:link>
                        </li>
                    </g:each>
                </ul>
            </sec:ifAllGranted>
            <table>
                <g:each var="row" in="${runYapsaConfigTable}" status="i">
                    <tr>
                        <g:each var="cell" in="${row}">
                            <g:if test="${i == 0}">
                                <th>${cell}</th>
                            </g:if>
                            <g:else>
                                <td class="tableEntry">${cell}</td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:each>
            </table>
            <br>
        </div>
        <asset:script type="text/javascript">
            $(function() {
                $.otp.projectConfig.referenceGenome();
                $.otp.projectConfig.asynchronousCallAlignmentInfo();
            });
        </asset:script>
    </g:if>
    <g:else>
        <g:render template="/templates/noProject"/>
    </g:else>
</div>
</body>
</html>
