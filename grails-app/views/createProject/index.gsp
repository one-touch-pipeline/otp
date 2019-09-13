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
    <asset:javascript src="pages/metadataImport/index/metaDataImport.js"/>
</head>
<body>
    <div class="body">
    <g:if test="${hasErrors}">
        <div class="errors">${message}</div>
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
                <td><g:textField name="name" size="130" value="${cmd.name}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.projectPrefix"/></td>
                <td><g:textField name="projectPrefix" size="130" value="${cmd.projectPrefix}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.directory"/></td>
                <td><g:textField name="directory" size="130" value="${cmd.directory}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.analysisDirectory"/></td>
                <td><g:textField name="analysisDirectory" size="130" value="${cmd.analysisDirectory}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.keywords"/></td>
                <td id="input-fields-keywords">
                    <g:if test="${cmd.keywords}">
                        <g:each var="keyword" in="${cmd.keywords}">
                            <g:textField list="keywordList" name="keywordNames" size="130" value="${keyword.name}"/>
                        </g:each>
                    </g:if>
                    <g:textField list="keywordList" name="keywordNames" size="130"/>
                    <button class="add-button keywords-button">+</button>
                    <datalist id="keywordList">
                        <g:each in="${keywords}" var="keyword">
                            <option value="${keyword.name}">${keyword.name}</option>
                        </g:each>
                    </datalist>
                </td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.description"/></td>
                <td><g:textArea name="description" cols="130" value="${cmd.description}" style="width: auto" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.connectedProjects"/></td>
                <td id="input-fields-connected-projects">
                    <g:if test="${cmd.connectedProjects}">
                        <g:each var="connectedProject" in="${cmd.connectedProjects.split(",")}">
                            <g:textField list="projectList" name="connectedProjectNames" size="130" value="${connectedProject}"/>
                        </g:each>
                    </g:if>
                    <g:textField list="projectList" name="connectedProjectNames" size="130"/>
                    <button class="add-button connected-projects-button">+</button>
                </td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.subsequentApplication"/></td>
                <td><g:textField list="projectList" name="subsequentApplication" size="130" value="${cmd.subsequentApplication}"/></td>
            </tr>
            <datalist id="projectList">
                <g:each in="${projects}" var="project">
                    <option value="${project.name}">${project.name}</option>
                </g:each>
            </datalist>
            <tr>
                <td class="myKey"><g:message code="createProject.tumorEntity"/></td>
                <td><g:select class="criteria" name='tumorEntityName' from='${tumorEntities}' value="${cmd.tumorEntity}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.speciesWithStrain"/></td>
                <td><g:select class="criteria" name='speciesWithStrain' from='${allSpeciesWithStrains}' value="${cmd.speciesWithStrain}" optionKey="id" noSelection="${['': 'None']}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.unixGroup"/></td>
                <td><g:textField name="unixGroup" size="130" value="${cmd.unixGroup}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.costCenter"/></td>
                <td><g:textField name="costCenter" size="130" value="${cmd.costCenter}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.organisationUnit"/></td>
                <td><g:textField name="organisationUnit" size="130" value="${cmd.organisationUnit}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.projectType"/></td>
                <td><g:select class="criteria" id="group" name='projectType' from='${projectTypes}' value="${cmd.projectType ?: defaultProjectType}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.forceCopyFiles"/></td>
                <td><g:checkBox name="forceCopyFiles" checked="${cmd == null || cmd.forceCopyFiles}" value="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.endDate"/></td>
                <td><input type="date" name="endDateInput" value="${cmd.endDate?.format("yyyy-MM-dd") ?: ""}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.storageUntil"/></td>
                <td><input type="date" name="storageUntilInput" value="${cmd.storageUntil?.format("yyyy-MM-dd") ?: defaultDate}" required/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.metadata.name"/></td>
                <td><g:textField name="nameInMetadataFiles" size="130" value="${cmd.nameInMetadataFiles}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.processingPriority"/></td>
                <td><g:select class="criteria" id="priority" name='processingPriority' from='${processingPriorities}' value="${cmd.processingPriority ?: defaultProcessingPriority}" required="true"/></td>
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
                <td class="myKey"><g:message code="createProject.projectInfo"/></td>
                <td>
                    <input type="file" name="projectInfoFile" id="projectInfoFile"/>
                </td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="createProject.internalNotes"/></td>
                <td><g:textArea name="internalNotes" cols="130" value="${cmd.internalNotes}" style="width: auto"/></td>
            </tr>
            <tr>
                <td></td>
                <td><g:submitButton name="submit" value="Submit"/></td>
            </tr>

            %{--if someone want to change this it is hidden now--}%
            <tr hidden>
                <td class="myKey"><g:message code="createProject.fingerPrinting"/></td>
                <td><g:checkBox name="fingerPrinting" checked="${cmd == null ? true : cmd.fingerPrinting}" value="true"/></td>
            </tr>
        </table>
    </g:uploadForm>
    </div>
    <asset:script type="text/javascript">
        $.otp.metaDataImport.addNewField()
    </asset:script>
</body>
</html>
