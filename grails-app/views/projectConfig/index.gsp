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
<%@ page import="de.dkfz.tbi.otp.config.TypeValidators; de.dkfz.tbi.otp.project.additionalField.ProjectFieldType" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title><g:message code="projectOverview.title" args="[selectedProject?.name]"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="pages/project/projectConfigPage.js"/>
</head>
<body>
    <div class="container-fluid otp-main-container">
        <g:render template="/templates/messages"/>
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
        <br>

        <h1><g:message code="projectOverview.title" args="[selectedProject?.name]"/></h1>

        <table class="table table-sm table-striped key-value-table key-help-input">
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
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <div class="input-group">
                            <div class="input-group-append">
                                <input disabled type="text" id="analysisDirInput" class="form-control form-control-sm"
                                       data-fixed="${selectedProject.dirAnalysis}" value="${selectedProject.dirAnalysis}">
                                <button class="btn btn-success btn-sm" type="button" id="button-save-analysisDir"
                                        onclick="onSaveAnalysisDir()" style="display: none;">
                                    <i class="bi bi-save"></i>
                                    <g:message code="default.button.save.label"/>
                                </button>
                                <button class="btn btn-outline-secondary btn-sm otp-background-white" type="button" id="button-edit-analysisDir"
                                        onclick="onEditAnalysisDir()" title="Edit">
                                    <i class="bi bi-pencil"></i>
                                    <g:message code="default.button.edit.label"/>
                                </button>
                            </div>
                        </div>
                    </sec:ifAllGranted>
                    <sec:ifNotGranted roles="ROLE_OPERATOR">
                        ${selectedProject.dirAnalysis}
                    </sec:ifNotGranted>
                </td>
            </tr>
            <tr>
                <td><g:message code="project.unixGroup"/></td>
                <td></td>
                <td>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <div class="input-group">
                            <div class="input-group-append">
                                <input disabled type="text" id="unixGroupInput" class="form-control form-control-sm" data-fixed="${selectedProject.unixGroup}" value="${selectedProject.unixGroup}">
                                <button class="btn btn-success btn-sm" type="button" id="button-save-unixGroup" onclick="onSaveUnixGroup()" style="display: none;">
                                    <i class="bi bi-save"></i>
                                    <g:message code="default.button.save.label"/>
                                </button>
                                <button class="btn btn-outline-secondary btn-sm otp-background-white" type="button" id="button-edit-unixGroup" onclick="onEditUnixGroup()" title="Edit">
                                    <i class="bi bi-pencil"></i>
                                    <g:message code="default.button.edit.label"/>
                                </button>
                            </div>
                        </div>
                    </sec:ifAllGranted>
                    <sec:ifNotGranted roles="ROLE_OPERATOR">
                        ${selectedProject.unixGroup}
                    </sec:ifNotGranted>
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
                <td><g:message code="project.speciesWithStrain"/></td>
                <td></td>
                <td>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDownMulti"
                                link="${g.createLink(controller: "projectConfig", action: 'updateSpeciesWithStrains', params: ['fieldName': 'speciesWithStrains'])}"
                                values="${allSpeciesWithStrain}"
                                optionKey="id"
                                value="${selectedProject?.speciesWithStrains ?: ""}"/>
                    </sec:ifAllGranted>
                    <sec:ifNotGranted roles="ROLE_OPERATOR">
                        ${selectedProject?.speciesWithStrains?.join(", ") ?: ""}
                    </sec:ifNotGranted>
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
                            link="${g.createLink(controller: 'projectConfig', action: 'updateProcessingPriority', params: ['fieldName': 'processingPriority'])}"
                            values="${processingPriorities*.name}"
                            value="${processingPriority.name}"/>
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
                    <td><div class="project-multiline-wrapper" style="max-height: 20em; max-width: none">${projectRequestComment}</div></td>
                </tr>
            </sec:ifAllGranted>
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
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <tr>
                    <td><g:message code="projectOverview.projectInfos"/></td>
                    <td></td>
                    <td>
                        <g:link controller='projectInfo' action='list'>
                            <g:message code="projectOverview.projectInfos.link" args="[selectedProject.projectInfos.size()]"/>
                        </g:link>
                    </td>
                </tr>
                <g:each in="${abstractFields}" var="abstractField" status="index">
                    <g:if test="${abstractField.projectDisplayOnConfigPage.toString() != 'HIDE'}">
                        <tr>
                            <td><g:message code="${abstractField.name}"/></td>
                            <td class="help" title="${g.message(code: "${abstractField.descriptionConfig}")}"></td>
                            <g:if test="${abstractField.projectFieldType == ProjectFieldType.INTEGER}">
                                <td>
                                    <otp:editorSwitch
                                            roles="ROLE_OPERATOR"
                                            template="integer"
                                            link="${g.createLink(controller: 'projectConfig', action: 'updateAbstractField',
                                                    params: ['fieldName': abstractField.id.toString()])}"
                                            value="${abstractValues.get(Long.toString(abstractField.id))}"/>
                                </td>
                            </g:if>
                            <g:elseif test="${abstractField.typeValidator == TypeValidators.MULTI_LINE_TEXT}">
                                <td>
                                    <otp:editorSwitch
                                            roles="ROLE_OPERATOR"
                                            template="textArea"
                                            link="${g.createLink(controller: 'projectConfig', action: 'updateAbstractField',
                                                    params: ['fieldName': abstractField.id.toString()])}"
                                            value="${abstractValues.get(Long.toString(abstractField.id))}"/>
                                </td>
                            </g:elseif>
                            <g:else>
                                <td>
                                    <otp:editorSwitch
                                            roles="ROLE_OPERATOR"
                                            link="${g.createLink(controller: 'projectConfig', action: 'updateAbstractField',
                                                    params: ['fieldName': abstractField.id.toString()])}"
                                            value="${abstractValues.get(Long.toString(abstractField.id))}"/>
                                </td>
                            </g:else>
                        </tr>
                    </g:if>
                </g:each>
            </sec:ifAllGranted>
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
            <tr>
                <td><g:message code="project.archived.message"/></td>
                <td class="help" title="${g.message(code: "project.archived.message.detail")}"></td>
                <td>
                    <otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            template="dropDown"
                            link="${g.createLink(controller: 'projectConfig', action: 'updateArchived')}"
                            values="${["true", "false"]}"
                            value="${selectedProject.archived}"/>
                </td>
            </tr>
        </table>
    </div>
<otp:otpModal modalId="confirmationUserGroupModal" title="Warning" type="dialog" closeText="Cancel" confirmText="Update" closable="false">
</otp:otpModal>
</body>
</html>
