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
<%@ page import="de.dkfz.tbi.util.TimeFormats; de.dkfz.tbi.otp.project.dta.DataTransfer" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title><g:message code="dataTransfer.page.title" args="[selectedProject?.name]"/></title>
    <asset:stylesheet src="pages/dataTransfer/index.less"/>
    <asset:javascript src="pages/dataTransfer/index/functions.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <g:render template="/templates/messages"/>

    <g:render template="/templates/bootstrap/projectSelection"/>

    <g:render template="templates/addDtaForm" bean="${docDtaCmd}"/>

    <hr>

    <h4><g:message code="dataTransfer.dta.header"/></h4>

    <div id="dtaAccordion" class="accordion">
        <g:if test="${!dataTransferAgreements}">
            <div class="alert alert-secondary" role="alert">
                <g:message code="dataTransfer.noDTAs"/>
            </div>
        </g:if>
        <g:each var="dta" in="${dataTransferAgreements}" status="i">
            <div id="doc${dta.id}" class="card">
                <div class="card-header" id="header-${i}">
                    <div class="collapse-title-container">
                        <button class="btn btn-link btn-block text-left collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-dta-${i}"
                                aria-controls="collapse-dta-${i}" aria-expanded="false">
                            ${dta.dtaId ? "${dta.dtaId}, " : ""}
                            <strong>${dta.peerInstitution}</strong>
                            ${dta.transfers*.peerPerson.toUnique()}
                            (${dta.legalBasis?.name()})
                            ${dta.transfers*.direction.contains(DataTransfer.Direction.INCOMING) ? '- INCOMING' : ''}
                        </button>
                        <g:if test="${dta.transfers*.completionDate.contains(null)}">
                            <span class="badge badge-pill ongoing-color">ONGOING</span>
                        </g:if>
                        <g:else>
                            <span class="badge badge-pill completed-color">COMPLETED</span>
                        </g:else>
                    </div>
                </div>

                <div id="collapse-dta-${i}" class="collapse" aria-labelledby="header-${i}">
                    <div class="card-body">
                        <span><i class="bi bi-info-square"></i> <g:message code="dataTransfer.upload.overview"/>:</span><br>
                        ${dta.dtaId ? "${dta.dtaId}, " : ""}with <strong>${dta.peerInstitution}</strong> (${dta.legalBasis?.name()?.toLowerCase()}),
                        <g:message code="dataTransfer.dta.transfer.created"/>
                        ${TimeFormats.DATE.getFormattedDate(dta.dateCreated)}<br>
                        <br>
                        <span><i class="bi bi-files"></i> <g:message code="dataTransfer.upload.files"/>:</span>
                        <ul id="dtaDocuments-${i}">
                            <g:each var="dtaDocument" in="${dta.dataTransferAgreementDocuments}">
                                <li>
                                    <g:link action="downloadDataTransferAgreementDocument" params='["dataTransferAgreementDocument.id": dtaDocument.id]'>
                                        ${dtaDocument.fileName}</g:link>
                                </li>
                            </g:each>
                        </ul>
                        <button type="button" class="btn btn-sm btn-primary alignment-add-files-btn" data-bs-toggle="collapse" data-bs-target="#addFileToDta-${i}" aria-expanded="false" aria-controls="addFileToDta-${i}">
                            <i class="bi bi-folder-plus"></i> Add
                        </button>

                        <div class="collapse container-sm left-aligned" id="addFileToDta-${i}">
                            <g:render template="templates/uploadFiles" model="[id: i, parentName: 'dataTransferAgreement.id', parentValue: dta.id, formAction: 'addFilesToTransferAgreement']"/>
                        </div>

                        <br><br>

                        <span><i class="bi bi-card-text"></i> <g:message code="dataTransfer.upload.comment"/>:</span>
                        <div class="comment-box-wrapper">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR" template="textArea" rows="3" cols="100"
                                    link="${g.createLink(controller: 'dataTransfer', action: 'updateDataTransferAgreementComment', params: ["dataTransferAgreement.id": dta.id])}"
                                    value="${dta.comment}"/>
                        </div>
                        <br>

                        <g:if test="${!dta.transfers}">
                            <div class="alert alert-secondary" role="alert">
                                <g:message code="dataTransfer.dta.transfer.noTransfers"/>
                            </div>
                        </g:if>

                        <g:render template="templates/transferList" model="['dta': dta]"/>

                        <button class="btn btn-sm btn-primary alignment-new-transfer-btn" data-bs-toggle="collapse" data-bs-target="#addTransfer-${i}" aria-expanded="${!cachedTransferCmd}" aria-controls="addTransfer-${i}">
                            ${g.message(code: 'dataTransfer.dta.transfer.new.expand')}
                        </button>

                        <div id="addTransfer-${i}" class="collapse container-sm left-aligned">
                            <g:render template="templates/addTransfer"
                                      model="['cachedTransferCmd': cachedTransferCmd, 'dta': dta, 'directions': directions, 'selectedDirection': selectedDirection]"/>
                        </div>

                        <br><br>

                        <g:form action="deleteDataTransferAgreement" useToken="true"
                                onSubmit="confirmDataTransferDeletion(event)">
                            <input type="hidden" name="dataTransferAgreement.id" value="${dta.id}"/>
                            <button type="submit" class="btn btn-sm btn-outline-danger">
                                <i class="bi bi-trash"></i>
                                ${g.message(code: "dataTransferAgreement.dta.delete")}
                            </button>
                        </g:form>
                    </div>
                </div>
            </div>
        </g:each>
    </div>
</div>
</body>
</html>
