<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page import="de.dkfz.tbi.otp.ngsdata.Project.Snv" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="projectOverview.title" args="[project?.name]"/></title>
    <asset:javascript src="pages/projectConfig/index/init_description.js"/>
    <asset:javascript src="pages/projectConfig/index/functions.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
    <div class="body">
    <g:render template="/templates/messages"/>
    <g:if test="${projects}">
        <g:if test="${hasErrors == true}">
            <div class="errors"> <li>${message}</li></div>
        </g:if>
        <g:elseif test="${message}">
            <div class="message">${message}</div>
        </g:elseif>
        <div id="projectCommentBox" class="commentBoxContainer">
            <div id="commentLabel">Comment:</div>
            <sec:ifNotGranted roles="ROLE_OPERATOR">
                <textarea id="commentBox" readonly>${comment?.comment}</textarea>
            </sec:ifNotGranted>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <textarea id="commentBox">${comment?.comment}</textarea>
                <div id="commentButtonArea">
                    <button id="saveComment" disabled>&nbsp;&nbsp;&nbsp;<g:message code="commentBox.save" /></button>
                    <button id="cancelComment" disabled><g:message code="commentBox.cancel" /></button>
                </div>
            </sec:ifAllGranted>
            <div id="commentDateLabel">${comment?.modificationDate?.format('EEE, d MMM yyyy HH:mm')}</div>
            <div id="commentAuthorLabel">${comment?.author}</div>
        </div>
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
        <div id="projectOverviewDates">
            <table>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.creationDate"/></td>
                    <td id="creation-date">${creationDate}</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.lastDate"/></td>
                    <td id="last-received-date">${lastReceivedDate}</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.directory"/></td>
                    <td id="projectDirectory">${directory}</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.analysisDirectory"/></td>
                    <td>
                        <otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            link="${g.createLink(controller: 'projectConfig', action: 'updateAnalysisDirectory', params: ['project.id': project.id])}"
                            value="${analysisDirectory}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.processingPriority"/></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: "updateProcessingPriority", params: ['project.id': project.id, 'fieldName': 'processingPriority'])}"
                                values="${processingPriorities}"
                                value="${project?(project.processingPriority == 0 ? "NORMAL" : "FAST_TRACK"):""}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.category"/></td>
                    <td>
                        <otp:editorSwitchCheckboxes
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateCategory', params: ['project.id': project.id])}"
                                availableValues="${projectCategories}"
                                selectedValues="${project?project.projectCategories*.name:""}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.nameInMetadata"/></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateNameInMetadataFiles', params: ['project.id': project.id])}"
                                value="${nameInMetadata}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.group"/></td>
                    <td id="group">${projectGroup}</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.copyFiles"/></td>
                    <td id="copyFiles">${copyFiles}</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.fingerPrinting"/></td>
                    <td id="fingerPrinting">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: "updateFingerPrinting", id: project.id)}"
                                values="${["true","false"]}"
                                value="${fingerPrinting}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.mailingListName"/></td>
                    <td>
                        <otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            link="${g.createLink(controller: 'projectConfig', action: 'updateMailingListName', params: ['project.id': project.id, 'fieldName': 'mailingListName'])}"
                            value="${mailingListName}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.phabricatorAlias"/></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_USER"
                                link="${g.createLink(controller: 'projectConfig', action: 'updatePhabricatorAlias', params: ['project.id': project.id])}"
                                value="${project?.phabricatorAlias}"/>
                    </td>
                </tr>
                <tr id="descriptionRow">
                    <td  class="myKey" id="descriptionHeader" style="padding-top: 1em; vertical-align: 1em"><g:message code="projectOverview.description"/> ↓</td>
                    <td id="descriptionContent" style="height: 3em; overflow: hidden;">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="textArea"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateDescription', params: ['project.id': project.id, 'fieldName': 'description'])}"
                                value="${description}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.unixGroup"/></td>
                    <td id="unixGroup">${unixGroup}</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.costCenter"/></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateCostCenter', params: ['project.id': project.id, 'fieldName': 'costCenter'])}"
                                value="${costCenter}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.tumorEntity"/></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: "projectConfig", action: "updateTumorEntity", params: ['project.id': project.id, 'fieldName': 'tumorEntity'])}"
                                values="${tumorEntities}"
                                value="${tumorEntity}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.customFinalNotification.message"/></td>
                    <td id="customFinalNotification">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: "updateCustomFinalNotification", id: project.id)}"
                                values="${["true","false"]}"
                                value="${customFinalNotification}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.projectInfos"/></td>
                    <td id="projectInfo">
                        <g:each var="projectInfo" in="${projectInfos}">
                            <p>
                                <g:message code="projectOverview.projectInfo.creationDate"/>: <g:formatDate date="${projectInfo.dateCreated}" format="yyyy-MM-dd HH:mm:ss" /><br>
                                <g:message code="projectOverview.projectInfo.path"/>: ${projectInfo.getPath()}
                            </p>
                        </g:each>
                        <p>
                        <g:uploadForm action="addProjectInfo" useToken="true">
                            <input type="file" name="projectInfoFile" id="projectInfoFile" />
                            <input type="hidden" name="project.id" value="${project.id}"/>
                            <g:submitButton name="${g.message(code: "projectOverview.projectInfo.add")}"/>
                        </g:uploadForm>
                        </p>
                    </td>
                </tr>
            </table>
        </div>

        <div class="otpDataTables">
            <h3>
                <g:message code="projectOverview.contactPerson.headline" />
            </h3>
            <table>
                <tr>
                    <th><g:message code="projectOverview.contactPerson.name"/></th>
                    <th><g:message code="projectOverview.contactPerson.email"/></th>
                    <th><g:message code="projectOverview.contactPerson.aspera"/></th>
                    <th><g:message code="projectOverview.contactPerson.role"/></th>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <th></th>
                    </sec:ifAllGranted>
                </tr>
               <g:each in="${projectContactPersons}" var="projectContactPerson">
                    <tr>
                        <td>
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateName', params: ["contactPerson.id": projectContactPerson.contactPerson.id])}"
                                value="${projectContactPerson.contactPerson.fullName}"
                                values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateEmail', params: ["contactPerson.id": projectContactPerson.contactPerson.id])}"
                                value="${projectContactPerson.contactPerson.email}"
                                values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectConfig', action: 'updateAspera', params: ["contactPerson.id": projectContactPerson.contactPerson.id])}"
                                    value="${projectContactPerson.contactPerson.aspera}"
                                    values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="dropDown"
                                    link="${g.createLink(controller: 'projectConfig', action: 'updateRole', params: ["projectContactPerson.id": projectContactPerson.id])}"
                                    value="${projectContactPerson.contactPersonRole?.name ?: ''} "
                                    values="${roleDropDown}"/>
                        </td>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <td>
                                <input type="button" class="deletePerson" value="Delete" data-id="${projectContactPerson.id}"/>
                            </td>
                        </sec:ifAllGranted>
                    </tr>
               </g:each>
            </table>
        </div>
        <p>
            <otp:editorSwitchNewValues
                    roles="ROLE_OPERATOR"
                    labels="${["Name", "E-Mail", "Aspera Account", "Role"]}"
                    textFields="${["name", "email", "aspera"]}"
                    dropDowns="${[role: roleDropDown]}"
                    link="${g.createLink(controller: 'projectConfig', action: "createContactPersonOrAddProject", params: ['project.id': project.id])}"
            />
        </p>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <br>
            <div>
                <h3><g:message code="projectOverview.accessPerson.headline" /></h3>
                <a id="toggleLink" href="javascript:void(0)" onclick="$.otp.projectConfig.toggle('controlElement', 'toggleLink')">Show list</a>
                <ul id="controlElement" style="display: none">
                    <g:each in="${accessPersons}">
                        <li>
                            ${it}
                        </li>
                    </g:each>
                </ul>
            </div>
        </sec:ifAllGranted>
        <h2>${g.message(code: 'projectOverview.alignmentInformation.title')}</h2>
        <div>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <h3><g:message code="projectOverview.alignmentInformation.configure"/></h3>
                <div class="show_button">
                    <ul>
                        <g:each in="${seqTypes}" var="seqType">
                            <li>
                                <g:if test="${seqType.isRna()}">
                                    <g:link controller='configurePipeline' action='rnaAlignment' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                        ${seqType.displayNameWithLibraryLayout}
                                    </g:link>
                                </g:if>
                                <g:else>
                                    <g:link controller='configurePipeline' action='alignment' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
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
                            ${m.value?.libPrepKit != null ? m.value.libPrepKit : "Not configured"}
                        </td>
                        <td>
                            ${m.value?.seqPlatformGroup ?: "Not configured"}
                        </td>
                    </tr>
                </g:each>
            </table>
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
                ] }"
                id="listReferenceGenome" />
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
                            <g:link controller='configurePipeline' action='snv' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                ${seqType.displayNameWithLibraryLayout}
                            </g:link>
                        </li>
                    </g:each>
                </ul>
                <table class="snv">
                    <tr>
                        <td><g:message code="projectOverview.snv"/></td>
                        <td class="myValue typeDropDown">
                            <otp:editorSwitch roles="ROLE_OPERATOR"
                                              template="dropDown"
                                              link="${g.createLink(controller: 'projectConfig', action: 'updateSnv', params: ['project.id': project.id, 'fieldName': 'snv'])}"
                                              values="${snvDropDown}"
                                              value="${snv}"/>
                        </td>
                    </tr>
                </table>
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
                            <g:link controller='configurePipeline' action='indel' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
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
                    <li>
                        <g:if test="${!checkSophiaReferenceGenome}">
                            <g:each in="${sophiaSeqType}" var="seqType">
                                <g:link controller='configurePipeline' action='sophia' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                    ${seqType.displayNameWithLibraryLayout}
                                </g:link>
                            </g:each>
                        </g:if>
                        <g:else>
                            ${checkSophiaReferenceGenome}
                        </g:else>
                    </li>
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
                    <li>
                        <g:if test="${!checkAceseqReferenceGenome}">
                            <g:each in="${aceseqSeqType}" var="seqType">
                                <g:link controller='configurePipeline' action='aceseq' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                    ${seqType.displayNameWithLibraryLayout}
                                </g:link>
                            </g:each>
                        </g:if>
                        <g:else>
                            ${checkAceseqReferenceGenome}
                        </g:else>
                    </li>
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
    <asset:script>
        $(function() {
            $.otp.projectConfig.referenceGenome();
            $.otp.projectConfig.asynchronousCallAlignmentInfo();
            $.otp.projectConfig.deleteUser();
            $.otp.initialiseSpecificOverview.toggleDescription();
            $.otp.initCommentBox(${project.id}, "#projectCommentBox");
            $("#descriptionContent").children().css('height', '3em');
            $("#descriptionContent").find(':button').css('display', 'block');
        });
    </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
