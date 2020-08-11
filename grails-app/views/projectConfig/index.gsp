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
    <meta name="layout" content="main"/>
    <title><g:message code="projectOverview.title" args="[selectedProject?.name]"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>
        <div class="project-selection-header-container">
            <div class="grid-element">
                <g:render template="/templates/projectSelection"/>
            </div>
            <div class="grid-element comment-box">
                <g:render template="/templates/commentBox" model="[
                        commentable     : selectedProject,
                        targetController: 'projectConfig',
                        targetAction    : 'saveProjectComment',
                ]"/>
            </div>
        </div>
        <br>

        <h1><g:message code="projectOverview.title" args="[selectedProject?.name]"/></h1>
        <div id="projectOverviewDates">
            <table class="key-value-table key-help-input">
                <tr>
                    <td><g:message code="project.individualPrefix"/></td>
                    <td></td>
                    <td>${selectedProject.individualPrefix}</td>
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
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['fieldName': 'dirAnalysis'])}"
                                value="${selectedProject.dirAnalysis}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.keywords"/></td>
                    <td></td>
                    <td>
                        ${selectedProject.keywords*.name.join(", ") ?: g.message(code: "project.keywords.empty")}
                        <sec:access expression="hasRole('ROLE_OPERATOR')">
                            <g:link controller="keyword" action="index">
                                <button class="edit-button" title="${g.message(code: "project.keywords.addKeywords")}">&nbsp;&nbsp;&nbsp;</button>
                            </g:link>
                        </sec:access>
                    </td>
                </tr>
                <tr>
                    <td style="padding-top: 1em; padding-bottom: 1em; vertical-align: 1em"><g:message code="project.description"/></td>
                    <td></td>
                    <td>
                        <div class="scrollable" style="max-height: 20em;">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="textArea"
                                    link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['fieldName': 'description'])}"
                                    value="${selectedProject.description}"/>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.relatedProjects"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['fieldName': 'relatedProjects'])}"
                                value="${selectedProject.relatedProjects}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.tumorEntity"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: "projectConfig", action: 'updateTumorEntity', params: ['fieldName': 'tumorEntity'])}"
                                noSelection="${['': 'None']}"
                                values="${tumorEntities}"
                                value="${selectedProject.tumorEntity}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.speciesWithStrain"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: "projectConfig", action: 'updateSpeciesWithStrain', params: ['fieldName': 'speciesWithStrain'])}"
                                optionKey="id"
                                noSelection="${['': 'None']}"
                                values="${allSpeciesWithStrain}"
                                value="${selectedProject.speciesWithStrain}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.unixGroup"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['fieldName': 'unixGroup'])}"
                                value="${selectedProject.unixGroup}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.costCenter"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['fieldName': 'costCenter'])}"
                                value="${selectedProject.costCenter}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.organizationalUnit"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['fieldName': 'organizationalUnit'])}"
                                value="${selectedProject.organizationalUnit}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.fundingBody"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['fieldName': 'fundingBody'])}"
                                value="${selectedProject.fundingBody}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.grantId"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['fieldName': 'grantId'])}"
                                value="${selectedProject.grantId}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.projectType"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: "projectConfig", action: 'updateProjectField', params: ['fieldName': 'projectType'])}"
                                values="${projectTypes}"
                                value="${selectedProject.projectType}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.forceCopyFiles"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateCopyFiles', params: ['fieldName': 'forceCopyFiles'])}"
                                values="${["true", "false"]}"
                                value="${selectedProject.forceCopyFiles}"/>
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
                                    link="${g.createLink(controller: 'projectConfig', action: 'updateProjectFieldDate', params: ['fieldName': 'endDate'])}"
                                    value="${selectedProject.endDate}"/>
                        </sec:ifAllGranted>
                        <sec:ifNotGranted roles="ROLE_OPERATOR">
                            ${selectedProject.endDate ?: g.message(code: "project.endDate.empty")}
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
                                    link="${g.createLink(controller: 'projectConfig', action: 'updateProjectFieldDate', params: ['fieldName': 'storageUntil'])}"
                                    value="${selectedProject.storageUntil}"/>
                        </sec:ifAllGranted>
                        <sec:ifNotGranted roles="ROLE_OPERATOR">
                            ${selectedProject.storageUntil ?: g.message(code: "project.storageUntil.empty")}
                        </sec:ifNotGranted>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.nameInMetadata"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['fieldName': 'nameInMetadataFiles'])}"
                                value="${selectedProject.nameInMetadataFiles}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.processingPriority"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                optionKey="id"
                                optionValue="name"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProcessingPriority', params: ['fieldName': 'processingPriority'])}"
                                values="${processingPriorities}"
                                value="${processingPriority}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.group"/></td>
                    <td></td>
                    <td><otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            template="dropDown"
                            link="${g.createLink(controller: 'projectConfig', action: 'updateProjectGroup', params: ['fieldName': 'projectGroup'])}"
                            noSelection="['':'None']"
                            values="${allProjectGroups}"
                            value="${selectedProject.projectGroup}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.sampleParser"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateSampleIdentifierParserBeanName', params: ['fieldName': 'sampleIdentifierParserBeanName'])}"
                                values="${sampleIdentifierParserBeanNames}"
                                value="${selectedProject.sampleIdentifierParserBeanName}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.qcThresholdHandling"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateQcThresholdHandling', params: ['fieldName': 'qcThresholdHandling'])}"
                                values="${qcThresholdHandlingDropdown}"
                                value="${selectedProject.qcThresholdHandling}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.fingerPrinting"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateFingerPrinting')}"
                                values="${["true", "false"]}"
                                value="${selectedProject.fingerPrinting}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.processingNotification.message"/></td>
                    <td class="help" title="${g.message(code: "project.processingNotification.message.detail")}"></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateProcessingNotification')}"
                                values="${["true", "false"]}"
                                value="${selectedProject.processingNotification}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.qcTrafficLightNotification.message"/></td>
                    <td class="help" title="${g.message(code: "project.qcTrafficLightNotification.message.detail")}"></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateQcTrafficLightNotification')}"
                                values="${["true", "false"]}"
                                value="${selectedProject.qcTrafficLightNotification}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.customFinalNotification.message"/></td>
                    <td class="help" title="${g.message(code: "project.customFinalNotification.message.detail")}"></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateCustomFinalNotification')}"
                                values="${["true", "false"]}"
                                value="${selectedProject.customFinalNotification}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.publiclyAvailable"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: "updatePubliclyAvailable", params: ['fieldName': 'publiclyAvailable'])}"
                                values="${["true", "false"]}"
                                value="${publiclyAvailable}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.closed"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: "updateClosed", params: ['fieldName': 'closed'])}"
                                values="${["true", "false"]}"
                                value="${closed}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="project.requestAvailable"/></td>
                    <td></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: "updateRequestAvailable", params: ['fieldName': 'projectRequestAvailable'])}"
                                values="${["true", "false"]}"
                                value="${projectRequestAvailable}"/>
                    </td>
                </tr>
                <sec:ifAllGranted roles="ROLE_OPERATOR">
                    <tr>
                        <td style="padding-top: 1em; padding-bottom: 1em; vertical-align: 1em"><g:message code="project.internalNotes"/></td>
                        <td></td>
                        <td>
                            <div class="scrollable" style="max-height: 20em">
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="textArea"
                                        link="${g.createLink(controller: 'projectConfig', action: 'updateProjectField', params: ['fieldName': 'internalNotes'])}"
                                        value="${selectedProject.internalNotes}"/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding-top: 1em; padding-bottom: 1em; vertical-align: 1em"><g:message code="project.requestComment"/></td>
                        <td></td>
                        <td><div class="project-multiline-wrapper" style="max-height: 20em; max-width: none">${projectRequestComments}</div></td>
                    </tr>
                    <tr>
                        <td><g:message code="projectOverview.projectInfos"/></td>
                        <td></td>
                        <td>
                            <g:link controller='projectInfo' action='list'>
                                <g:message code="projectOverview.projectInfos.link" args="[selectedProject.projectInfos.size()]"/>
                            </g:link>
                        </td>
                    </tr>
                </sec:ifAllGranted>
            </table>
        </div>
    </div>
</body>
</html>
