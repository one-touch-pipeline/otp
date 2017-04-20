<%@ page contentType="text/html;charset=UTF-8"%>
<%@ page import="de.dkfz.tbi.otp.ngsdata.Project.Snv" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="projectOverview.title" args="[project.name]"/></title>
    <asset:javascript src="pages/projectOverview/index/datatable.js"/>
    <asset:javascript src="pages/projectOverview/index/init_description.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
    <div class="body">
        <div id="projectCommentBox" class="commentBoxContainer">
            <div id="commentLabel">Comment:</div>
            <textarea id="commentBox">${comment?.comment}</textarea>
            <div id="commentButtonArea">
                <button id="saveComment" disabled>&nbsp;&nbsp;&nbsp;<g:message code="commentBox.save" /></button>
                <button id="cancelComment" disabled><g:message code="commentBox.cancel" /></button>
            </div>
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
                            link="${g.createLink(controller: 'projectOverview', action: 'updateAnalysisDirectory', params: ['project.id': project.id])}"
                            value="${analysisDirectory}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.processingPriority"/></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                link="${g.createLink(controller: "projectOverview", action: "updateProcessingPriority", params: ['project.id': project.id])}"
                                values="${processingPriorities}"
                                value="${(project.processingPriority == 0) ? "NORMAL" : "FAST_TRACK"}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.category"/></td>
                    <td>
                        <otp:editorSwitchCheckboxes
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectOverview', action: 'updateCategory', params: ['project.id': project.id])}"
                                availableValues="${projectCategories*.name}"
                                selectedValues="${project.projectCategories*.name}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.nameInMetadata"/></td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectOverview', action: 'updateNameInMetadataFiles', params: ['project.id': project.id])}"
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
                                link="${g.createLink(controller: "projectOverview", action: "updateFingerPrinting", id: project.id)}"
                                values="${["true","false"]}"
                                value="${fingerPrinting}"/>
                    </td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="projectOverview.mailingListName"/></td>
                    <td>
                        <otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            link="${g.createLink(controller: 'projectOverview', action: 'updateMailingListName', params: ['project.id': project.id])}"
                            value="${mailingListName}"/>
                    </td>
                </tr>
                <tr id="descriptionRow">
                    <td  class="myKey" id="descriptionHeader" style="padding-top: 1em; vertical-align: 1em"><g:message code="projectOverview.description"/> ↓</td>
                    <td id="descriptionContent" style="height: 3em; overflow: hidden;">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="textArea"
                                link="${g.createLink(controller: 'projectOverview', action: 'updateDescription', params: ['project.id': project.id])}"
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
                                link="${g.createLink(controller: 'projectOverview', action: 'updateCostCenter', params: ['project.id': project.id])}"
                                value="${costCenter}"/>
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
                    <th></th>
                </tr>
               <g:each in="${projectContactPersons}" var="projectContactPerson">
                    <tr>
                        <td>
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectOverview', action: 'updateName', params: ["contactPerson.id": projectContactPerson.contactPerson.id])}"
                                value="${projectContactPerson.contactPerson.fullName}"
                                values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectOverview', action: 'updateEmail', params: ["contactPerson.id": projectContactPerson.contactPerson.id])}"
                                value="${projectContactPerson.contactPerson.email}"
                                values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectOverview', action: 'updateAspera', params: ["contactPerson.id": projectContactPerson.contactPerson.id])}"
                                    value="${projectContactPerson.contactPerson.aspera}"
                                    values="${projectContactPersons}"/>
                        </td>
                        <td>
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="dropDown"
                                    link="${g.createLink(controller: 'projectOverview', action: 'updateRole', params: ["projectContactPerson.id": projectContactPerson.id])}"
                                    value="${projectContactPerson.contactPersonRole?.name ?: ''} "
                                    values="${roleDropDown}"/>
                        </td>
                        <td><input type="button" class="deletePerson" value="Delete" data-id="${projectContactPerson.id}"/></td>
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
                    link="${g.createLink(controller: "projectOverview", action: "createContactPersonOrAddProject", params: ['project.id': project.id])}"
            />
        </p>
        <br>
        <div>
            <h3><g:message code="projectOverview.accessPerson.headline" /></h3>
            <ul>
                <g:each in="${accessPersons}">
                    <li>
                        ${it}
                    </li>
                </g:each>
            </ul>
        </div>
        <div>
            <h3>${g.message(code: 'projectOverview.alignmentInformation.title')}</h3>
            <div class="show_button">
                <g:message code="projectOverview.alignmentInformation.configure"/>
                <ul>
                    <g:each in="${seqTypes}" var="seqType">
                        <li>
                            <g:if test="${seqType.displayName == "RNA"}">
                                <g:link controller='configurePipeline' action='rnaAlignment' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                    ${seqType.displayName}
                                </g:link>
                            </g:if>
                            <g:else>
                                <g:link controller='configurePipeline' action='alignment' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                    ${seqType.displayName}
                                </g:link>
                            </g:else>
                        </li>
                    </g:each>
                </ul>
            </div>
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
        <div>
            <h3>${g.message(code: 'projectOverview.analysis.title')}</h3>
            <ul>
                <li>
                    <g:link controller='configureAnalysis' params='["project.id": project.id]' class="configure">
                        ${g.message(code: 'projectOverview.analysis.link')}
                    </g:link>
                </li>
            </ul>
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
        </div>
        <div>
            <h3>${g.message(code: 'projectOverview.snv.title')}</h3>
            <g:message code="projectOverview.snv.configure"/>
            <ul>
                <g:each in="${snvSeqTypes}" var="seqType">
                    <li>
                        <g:link controller='configurePipeline' action='snv' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                            ${seqType.displayName}
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
                                            link="${g.createLink(controller: 'ProjectOverview', action: 'updateSnv', params: ['project.id': project.id])}"
                                            values="${snvDropDown}"
                                            value="${snv}"/>
                    </td>
                </tr>
            </table>
            <br>
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
            <g:message code="projectOverview.indel.configure"/>
            <ul>
                <g:each in="${indelSeqTypes}" var="seqType">
                    <li>
                        <g:link controller='configurePipeline' action='indel' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                            ${seqType.displayName}
                        </g:link>
                    </li>
                </g:each>
            </ul>
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
            <g:message code="projectOverview.sophia.configure"/>
            <ul>
                <li>
                    <g:if test="${!checkSophiaReferenceGenome}">
                        <g:each in="${sophiaSeqType}" var="seqType">
                            <g:link controller='configurePipeline' action='sophia' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                ${seqType.displayName}
                            </g:link>
                        </g:each>
                    </g:if>
                    <g:else>
                        ${checkSophiaReferenceGenome}
                    </g:else>
                </li>
            </ul>
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
            <g:message code="projectOverview.aceseq.configure"/>
            <ul>
                <li>
                    <g:if test="${!checkAceseqReferenceGenome}">
                        <g:each in="${aceseqSeqType}" var="seqType">
                            <g:link controller='configurePipeline' action='aceseq' params='["project.id": project.id, "seqType.id": seqType.id]' class="configure">
                                ${seqType.displayName}
                            </g:link>
                        </g:each>
                    </g:if>
                    <g:else>
                        ${checkAceseqReferenceGenome}
                    </g:else>
                </li>
            </ul>
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
    </div>
    <asset:script>
        $(function() {
            $.otp.projectOverviewTable.referenceGenome();
            $.otp.projectOverviewTable.asynchronCallAlignmentInfo();
            $.otp.initialiseSpecificOverview.toggleDescription();
            $.otp.initCommentBox(${project.id}, "#projectCommentBox");
            $.otp.projectOverviewTable.deleteUser();
            $("#descriptionContent").children().css('height', '3em');
            $("#descriptionContent").find(':button').css('display', 'block');
        });
    </asset:script>
</body>
</html>
