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

<%@ page import="de.dkfz.tbi.otp.ngsdata.ProjectInfo" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="projectInfo.title" args="[project?.name]"/></title>
    <asset:javascript src="pages/projectInfo/list/functions.js"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>
        <g:if test="${projects}">
            <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]"/>
            <br><br><br>
            <h3><g:message code="projectInfo.header.listing"/></h3>
            <div class="project-info-listing">
                <g:each var="projectInfo" in="${project.projectInfos.sort { [it.recipientInstitution == null, it.id] }}">
                    <div style="margin-top: 1em">
                        <g:message code="projectInfo.upload.dta.creationDate"/>
                        <g:formatDate date="${projectInfo.dateCreated}" format="yyyy-MM-dd"/>
                        <br><g:message code="projectInfo.upload.dta.path"/>
                        <g:link action="download" params='["projectInfo.id": projectInfo.id]'>${projectInfo.getPath()}</g:link>
                        <g:form action="deleteProjectInfo" useToken="true" style="display: inline"
                                onSubmit="\$.otp.projectInfo.confirmProjectInfoDelete(event);">
                            |
                            <input type="hidden" name="projectInfo.id" value="${projectInfo.id}"/>
                            <g:submitButton name="${g.message(code: "projectInfo.upload.dta.deleteProjectInfo")}"/>
                        </g:form>
                        <g:if test="${projectInfo.hasAdditionalInfos()}">
                            <br><g:message code="projectInfo.upload.dta.additional"/> ${projectInfo.additionalInfos}
                            <g:if test="${!projectInfo.deletionDate}">
                                <g:uploadForm action="markDtaDataAsDeleted" useToken="true" style="display: inline"
                                              onSubmit="\$.otp.projectInfo.confirmDtaDelete(event);">
                                    |
                                    <input type="hidden" name="projectInfo.id" value="${projectInfo.id}"/>
                                    <g:submitButton name="${g.message(code: "projectInfo.upload.dta.markDtaDataAsDeleted")}"/>
                                </g:uploadForm>
                            </g:if>
                        </g:if>
                    </div>
                </g:each>
            </div>

            <h3><g:message code="projectInfo.header.upload"/></h3>
            <div class="project-info-upload">
                <g:uploadForm action="addProjectInfo" useToken="true">
                    <input type="hidden" name="project.id" value="${project.id}"/>
                    <input type="file" name="projectInfoFile" required="required">
                    <g:submitButton name="${g.message(code: "projectInfo.upload.dta.add")}"/>
                </g:uploadForm>
            </div>

            <h3><g:message code="projectInfo.header.upload.dta"/></h3>
            <div class="project-info-upload-dta">
                <g:uploadForm action="addProjectDta" useToken="true">
                    <input type="hidden" name="project.id" value="${project.id}"/>
                    <table class="project-info-input">
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.path"/></td>
                            <td><input type="file" name="projectInfoFile" required="required"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.recipient.institution"/></td>
                            <td><input type="text" value="${addProjectInfos?.recipientInstitution}" name="recipientInstitution" required="required"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.recipient.person"/></td>
                            <td><input type="text" value="${addProjectInfos?.recipientPerson}" name="recipientPerson" required="required"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.recipient.account"/></td>
                            <td><input type="text" value="${addProjectInfos?.recipientAccount}" name="recipientAccount"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.transferDate"/></td>
                            <td><input type="date" value="${addProjectInfos?.transferDate?.format("yyyy-MM-dd")}" name="transferDateInput"
                                       required="true"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.validityDate"/></td>
                            <td><input type="date" value="${addProjectInfos?.validityDate?.format("yyyy-MM-dd")}" name="validityDateInput"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.transferMode"/></td>
                            <td>
                                <g:select name="transferMode"
                                          noSelection="['': '']"
                                          from="${transferModes}"
                                          value="${addProjectInfos?.transferMode}"
                                          class="dropDown"
                                          required="true"/>
                            </td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.legalBasis"/></td>
                            <td>
                                <g:select name="legalBasis"
                                          noSelection="['': '']"
                                          from="${legalBasis}"
                                          value="${addProjectInfos?.legalBasis}"
                                          class="dropDown"
                                          required="true"/>
                            </td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.dtaId"/></td>
                            <td><input type="text" value="${addProjectInfos?.dtaId}" name="dtaId"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.requester"/></td>
                            <td><input type="text" value="${addProjectInfos?.requester}" name="requester" required="required"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.ticketID"/></td>
                            <td><input type="text" value="${addProjectInfos?.ticketID}" name="ticketID"/></td>
                        </tr>
                        <tr>
                            <td><g:message code="projectInfo.upload.dta.comment"/></td>
                            <td><input type="text" value="${addProjectInfos?.comment}" name="comment"/></td>
                        </tr>
                    </table>
                    <g:submitButton name="${g.message(code: "projectInfo.upload.dta.add")}"/>
                </g:uploadForm>
            </div>
            <br>
        </g:if>
        <g:else>
            <g:render template="/templates/noProject"/>
        </g:else>
    </div>
</body>
</html>
