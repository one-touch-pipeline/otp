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
                                value="${processingPriority ?: ""}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.category"/></td>
                    <td>
                        <otp:editorSwitchCheckboxes
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateCategory', params: ['project.id': project.id])}"
                                availableValues="${projectCategories}"
                                selectedValues="${project ? project.projectCategories*.name : ""}"/>
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
                    <td class="myKey"><g:message code="projectOverview.sampleParser"/></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: "updateSampleIdentifierParserBeanName", params: ['project.id': project.id, 'fieldName': 'sampleIdentifierParserBeanName'])}"
                                values="${sampleIdentifierParserBeanNames}"
                                value="${sampleIdentifierParserBeanName}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.qcThresholdHandling"/></td>
                    <td id="qcThresholdHandling">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: 'projectConfig', action: "updateQcThresholdHandling", params: ['project.id': project.id, 'fieldName': 'qcThresholdHandling'])}"
                                values="${qcThresholdHandlingDropdown}"
                                value="${qcThresholdHandling}"/>
                    </td>
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
                    <td class="myKey"><g:message code="projectOverview.phabricatorAlias"/></td>
                    <td>
                        <otp:editorSwitch
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
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectConfig', action: 'updateUnixGroup', params: ['project.id': project.id, 'fieldName': 'unixGroup'])}"
                                value="${unixGroup}"/>
                    </td>
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
                <sec:ifAllGranted roles="ROLE_OPERATOR">
                    <tr>
                        <td class="myKey"><g:message code="projectOverview.projectInfos"/></td>
                        <td id="projectInfo">
                            <g:each var="projectInfo" in="${projectInfos}">
                                <p>
                                    <g:message code="projectOverview.projectInfo.creationDate"/>: <g:formatDate date="${projectInfo.dateCreated}" format="yyyy-MM-dd HH:mm:ss" /><br>
                                    <g:message code="projectOverview.projectInfo.path"/>: <g:link action="download" params='["projectInfo.id": projectInfo.id]'>${projectInfo.getPath()}</g:link>
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
                                <g:link controller='configureCellRangerPipeline' action='index' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
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
                    <th>${g.message(code: 'projectOverview.alignmentInformation.cellRanger.referenceGenomeIndex')}</th>
                </tr>
                <g:each in="${cellRangerOverview}" var="m">
                    <tr>
                        <td>
                            ${m.seqType?.getDisplayNameWithLibraryLayout()}
                        </td>
                        <td>
                            ${m.config?.programVersion ?: "Not configured"}
                        </td>
                        <td>
                            ${m.config?.referenceGenomeIndex ?: "Not configured"}
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
                            <g:link controller='ConfigureSnvPipeline' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
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
                            <g:link controller='configureRunYapsaPipeline' action='index' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
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
    <asset:script>
        $(function() {
            $.otp.projectConfig.referenceGenome();
            $.otp.projectConfig.asynchronousCallAlignmentInfo();
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
