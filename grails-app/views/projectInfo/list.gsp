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

<%@ page import="de.dkfz.tbi.otp.administration.ProjectInfo" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="projectInfo.title" args="[selectedProject?.name]"/></title>
    <asset:javascript src="pages/projectInfo/list/functions.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="taglib/Expandable.js"/>
</head>
<body>
    <div class="body">
            <g:render template="/templates/messages"/>

            <g:render template="/templates/projectSelection"/>

            <h1><g:message code="projectInfo.title" args="[selectedProject?.name]"/></h1>

            <h2><g:message code="projectInfo.header.document.upload"/></h2>
            <div class="project-info-form-container">
                <g:uploadForm action="addProjectInfo" useToken="true">
                    <table class="key-value-table key-input">
                        <tr>
                            <td><g:message code="projectInfo.upload.path"/></td>
                            <td><input type="file" name="projectInfoFile" required></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.comment"/></td>
                            <td><g:textArea name="comment" rows="3" value="${docCmd?.comment}"/></td>
                        </tr>
                    </table>
                    <g:submitButton name="${g.message(code: "projectInfo.upload.add")}"/>
                </g:uploadForm>
            </div>

            <hr>

            <h2><g:message code="projectInfo.document.header"/></h2>
            <div>
                <ul>
                    <g:if test="${!projectInfos["NonDta"]}">
                        <li><g:message code="projectInfo.noDocuments"/></li>
                    </g:if>
                    <g:each var="doc" in="${projectInfos["NonDta"]}">
                        <li id="doc${doc.id}">
                            <g:link action="download" params='["projectInfo.id": doc.id]'>${doc.path}</g:link>
                            <br>
                            <g:message code="projectInfo.dta.transfer.created"/>
                            <g:formatDate date="${doc.dateCreated}" format="yyyy-MM-dd"/>
                            <g:form action="deleteProjectInfo" useToken="true" style="display: inline"
                                    onSubmit="\$.otp.projectInfo.confirmProjectInfoDelete(event);">
                                <input type="hidden" name="projectInfo.id" value="${doc.id}"/>
                                <g:submitButton name="${g.message(code: "projectInfo.document.delete")}"/>
                            </g:form>
                            <br>
                            <g:message code="projectInfo.upload.comment"/>:
                            <div class="comment-box-wrapper">
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR" template="textArea" rows="3" cols="100"
                                        link="${g.createLink(controller: 'projectInfo', action: 'updateProjectInfoComment', params: ["projectInfo.id": doc.id])}"
                                        value="${doc.comment}"/>
                            </div>
                        </li>
                    </g:each>
                </ul>
            </div>

            <hr>

            <h2><g:message code="projectInfo.dta.header.upload"/></h2>
            <div class="project-info-form-container">
                <g:uploadForm action="addProjectInfo" useToken="true">
                    <table class="key-value-table key-input">
                        <tr>
                            <td><g:message code="projectInfo.upload.path"/></td>
                            <td><input type="file" name="projectInfoFile" required/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.dta.transfer.peerInstitution"/></td>
                            <td><input type="text" value="${docCmd?.peerInstitution}" name="peerInstitution" required/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.dta.transfer.legalBasis"/></td>
                            <td>
                                <g:select name="legalBasis"
                                          from="${legalBases}"
                                          value="${docCmd?.legalBasis ?: defaultLegalBasis}"
                                          class="use-select-2"
                                          required="true"/>
                            </td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.dta.transfer.validityDate"/></td>
                            <td><input type="date" value="${docCmd?.validityDate?.format("yyyy-MM-dd")}" name="validityDateInput"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.dta.transfer.dtaId"/></td>
                            <td><input type="text" value="${docCmd?.dtaId}" name="dtaId"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.comment"/></td>
                            <td><g:textArea name="comment" value="${docCmd?.comment}"/></td>
                        </tr>
                    </table>
                    <g:submitButton name="${g.message(code: "projectInfo.upload.add")}"/>
                </g:uploadForm>
            </div>

            <hr>

            <h2><g:message code="projectInfo.dta.header"/></h2>
            <div>
                <ul>
                    <g:if test="${!projectInfos["Dta"]}">
                        <li><g:message code="projectInfo.noDocuments"/></li>
                    </g:if>
                    <g:each var="doc" in="${projectInfos["Dta"]}">
                    <li id="doc${doc.id}" style="margin-top: 2em">
                        <g:link action="download" params='["projectInfo.id": doc.id]'>${doc.path}</g:link>
                        <br>
                        ${doc.dtaId ? "${doc.dtaId}, " : ""}with <strong>${doc.peerInstitution}</strong> (${doc.legalBasis?.name()?.toLowerCase()}),
                        <g:message code="projectInfo.dta.transfer.created"/>
                        <g:formatDate date="${doc.dateCreated}" format="yyyy-MM-dd"/>
                        <g:form action="deleteProjectInfo" useToken="true" style="display: inline"
                                onSubmit="\$.otp.projectInfo.confirmProjectInfoDelete(event);">
                            <input type="hidden" name="projectInfo.id" value="${doc.id}"/>
                            <g:submitButton name="${g.message(code: "projectInfo.dta.delete")}"/>
                        </g:form>
                        <br>
                        <g:message code="projectInfo.upload.comment"/>:
                        <div class="comment-box-wrapper">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR" template="textArea" rows="3" cols="100"
                                    link="${g.createLink(controller: 'projectInfo', action: 'updateProjectInfoComment', params: ["projectInfo.id": doc.id])}"
                                    value="${doc.comment}"/>
                        </div>
                        <ul class="transfer-listing">
                            <g:if test="${!doc.transfers}">
                                <li class="other"><g:message code="projectInfo.dta.transfer.noTransfers"/></li>
                            </g:if>
                            <g:each var="xfer" in="${doc.transfersSortedByDateCreatedDesc}" >
                                <li class="${xfer.completionDate ? "completed" : "ongoing"}">
                                    <g:message code="projectInfo.dta.transfer.transfer" /> ${xfer.id}, ${xfer.direction.adjective} <strong>${xfer.peerPerson} (${xfer.peerAccount ?: "N/A"})</strong>
                                    via ${xfer.transferMode}
                                    <br>
                                    <g:message code="projectInfo.dta.transfer.requestedBy"/> ${xfer.requester} via <a ${xfer.ticketLinkable ? "href=${xfer.ticketLink}" : ""}>${xfer.ticketID}</a>
                                    <br>
                                    <g:message code="projectInfo.dta.transfer.new.handledBy"/> ${xfer.performingUser.realName} (${xfer.performingUser.username})
                                    <br>
                                    <g:message code="projectInfo.dta.transfer.started"/> <g:formatDate date="${xfer.transferDate}" format="yyyy-MM-dd"/>,
                                    <g:if test="${xfer.completionDate}">
                                        <g:message code="projectInfo.dta.transfer.completionDate"/> <g:formatDate date="${xfer.completionDate}" format="yyyy-MM-dd"/>
                                    </g:if>
                                    <g:else>
                                        <g:message code="projectInfo.dta.transfer.completionDate.none"/>
                                        <g:form action="markTransferAsCompleted" useToken="true" style="display: inline"
                                                onSubmit="\$.otp.projectInfo.confirmCompleteTransfer(event);">
                                            <input type="hidden" name="dataTransfer.id" value="${xfer.id}"/>
                                            <g:submitButton name="${g.message(code:"projectInfo.dta.transfer.new.complete")}"/>
                                        </g:form>
                                    </g:else>
                                    <br>
                                    <g:message code="projectInfo.upload.comment"/>:
                                    <div class="comment-box-wrapper">
                                        <otp:editorSwitch
                                                roles="ROLE_OPERATOR" template="textArea" rows="3" cols="100"
                                                link="${g.createLink(controller: 'projectInfo', action: 'updateDataTransferComment', params: ["dataTransfer.id": xfer.id])}"
                                                value="${xfer.comment}"/>
                                    </div>
                                </li>
                            </g:each>
                            <li>
                                <g:set var="cachedXfer" value="${xferCmd?.parentDocument?.id == doc.id}"/>
                                <otp:expandable value="${g.message(code: 'projectInfo.dta.transfer.new.expand')}" wrapperClass='new-transfer-wrapper' collapsed="${!cachedXfer}">
                                    <g:form action="addTransfer" useToken="true">
                                        <%-- since we have this form multiple times, was the previous form content for THIS document? --%>
                                        <input type="hidden" name="parentDocument.id" value="${doc.id}">

                                        <table class="key-value-table key-input project-info-form-container">
                                            <tr>
                                                <td><g:message code="projectInfo.dta.transfer.requestedBy"/></td>
                                                <td><input type="text" value="${cachedXfer ? xferCmd.requester : ""}" name="requester" required/></td>
                                            </tr>
                                            <tr>
                                                <td><g:message code="projectInfo.dta.transfer.ticketId"/></td>
                                                <td><input type="text" value="${cachedXfer ? xferCmd.ticketID : ""}" name="ticketID" required/></td>
                                            </tr>
                                            <tr>
                                                <td><g:message code="projectInfo.dta.transfer.new.peerPerson"/></td>
                                                <td><input type="text" value="${cachedXfer ? xferCmd.peerPerson : ""}" name="peerPerson" required/></td>
                                            </tr>
                                            <tr>
                                                <td><g:message code="projectInfo.dta.transfer.new.peerAccount"/></td>
                                                <td><input type="text" value="${cachedXfer ? xferCmd.peerAccount : ""}" name="peerAccount"/></td>
                                            </tr>
                                            <tr>
                                                <td><g:message code="projectInfo.dta.transfer.new.direction"/></td>
                                                <td>
                                                    <g:select id="" name="direction"
                                                              from="${directions}"
                                                              value="${cachedXfer ? xferCmd.direction : defaultDirection}"
                                                              class="use-select-2"
                                                              required="true"/>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td><g:message code="projectInfo.dta.transfer.transferMode"/></td>
                                                <td>
                                                    <g:select id="" name="transferMode"
                                                              from="${transferModes}"
                                                              value="${cachedXfer ? xferCmd.transferMode : defaultTransferMode}"
                                                              class="use-select-2"
                                                              required="true"/>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td><g:message code="projectInfo.dta.transfer.new.transferStarted"/></td>
                                                <td><input type="date" value="${(cachedXfer ? xferCmd.transferDate : new Date()).format("yyyy-MM-dd")}"
                                                           name="transferDateInput" required/></td>
                                            </tr>
                                            <tr>
                                                <td><g:message code="projectInfo.dta.transfer.completionDate"/></td>
                                                <td><input type="date" value="${cachedXfer ? xferCmd?.completionDate?.format("yyyy-MM-dd") : ""}"
                                                           name="transferDateInput"/></td>
                                            </tr>
                                            <tr>
                                                <td><g:message code="projectInfo.upload.comment"/></td>
                                                <td><g:textArea name="comment" value="${cachedXfer ? xferCmd.comment : ""}"/></td>
                                            </tr>
                                        </table>
                                        <g:submitButton name="${g.message(code:"projectInfo.dta.transfer.new.submit")}"/>
                                    </g:form>
                                </otp:expandable>
                            </li>
                        </ul>
                    </li>
                </g:each>
                </ul>
            </div>
    </div>
</body>
</html>
