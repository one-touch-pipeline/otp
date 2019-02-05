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
<title><g:message code="otp.menu.createProject"/></title>
</head>
<body>
    <div class="body">
    <g:if test="${hasErrors}">
        <div class="errors"> <li>${message}</li></div>
    </g:if>
    <g:elseif test="${message}">
        <div class="message">${message}</div>
    </g:elseif>
    <g:else>
        <div class="empty"><br></div>
    </g:else>
    <g:uploadForm controller="createProject" action="index">
        <table>
            <tr>
                <td class="myKey"><g:message code="createProject.name"/></td>
                <td><g:textField name="name" size="130" value="${cmd.name}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.phabricatorAlias"/></td>
                <td><g:textField name="phabricatorAlias" size="130" value="${cmd.phabricatorAlias}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.directory"/></td>
                <td><g:textField name="directory" size="130" value="${cmd.directory}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.analysisDirectory"/></td>
                <td><g:textField name="analysisDirectory" size="130" value="${cmd.analysisDirectory}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.processingPriority"/></td>
                <td><g:select class="criteria" id="priority" name='processingPriority' from='${processingPriorities}' value="${cmd.processingPriority ?: defaultProcessingPriority}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.metadata.name"/></td>
                <td><g:textField name="nameInMetadataFiles" size="130" value="${cmd.nameInMetadataFiles}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.unixGroup"/></td>
                <td><g:textField name="unixGroup" size="130" value="${cmd.unixGroup}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.costCenter"/></td>
                <td><g:textField name="costCenter" size="130" value="${cmd.costCenter}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.projectGroup"/></td>
                <td><g:select class="criteria" id="group" name='projectGroup' from='${projectGroups}' value="${cmd.projectGroup}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.sampleParser"/></td>
                <td><g:select class="criteria" id="group" name='sampleIdentifierParserBeanName' from='${sampleIdentifierParserBeanNames}' value="${cmd.sampleIdentifierParserBeanName}" optionValue="displayName"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.qcThresholdHandling"/></td>
                <td><g:select class="criteria" id="group" name='qcThresholdHandling' from='${qcThresholdHandlings}' value="${cmd.qcThresholdHandling ?: defaultQcThresholdHandling}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.tumorEntity"/></td>
                <td><g:select class="criteria" name='tumorName' from='${tumorEntities}' value="${cmd.tumorEntity}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.category"/></td>
                <td>
                    <g:each in="${projectCategories}" var="category">
                        <g:checkBox name="projectCategories" value="${category}" id="category-${category}" checked="false" /><label for="category-${category}">${category}</label>
                    </g:each>
                </td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.copyFiles"/></td>
                <td><g:checkBox name="copyFiles" checked="${cmd == null || cmd.copyFiles}" value="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.fingerPrinting"/></td>
                <td><g:checkBox name="fingerPrinting" checked="${cmd == null ? true : cmd.fingerPrinting}" value="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.projectInfo"/></td>
                <td>
                    <input type="file" name="projectInfoFile" id="projectInfoFile"/>
                </td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.description"/></td>
                <td><g:textArea name="description" value="${cmd.description}"/></td>
            </tr>
            <tr>
                <td></td>
                <td><g:submitButton name="submit" value="Submit"/></td>
            </tr>
        </table>
    </g:uploadForm>
    </div>
</body>
</html>
