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

<%@ page import="de.dkfz.tbi.util.TimeFormats; de.dkfz.tbi.otp.project.ProjectInfo" contentType="text/html;charset=UTF-8" %>
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
            <g:if test="${!projectInfos && !projectRequest}">
                <li><g:message code="projectInfo.noDocuments"/></li>
            </g:if>
            <g:if test="${projectRequest}">
                <li>
                    <g:link controller="projectRequest" action="view" params='["id": projectRequest.id]'>Project request</g:link>
                </li>
            </g:if>
            <g:each var="doc" in="${projectInfos}">
                <li id="doc${doc.id}">
                    <g:link action="downloadProjectInfoDocument" params='["projectInfo.id": doc.id]'>${doc.fileName}</g:link>
                    <br>
                    <g:message code="dataTransfer.dta.transfer.created"/>
                    <g:formatDate date="${doc.dateCreated}" format="${dateFormat}"/>
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
</div>
</body>
</html>
