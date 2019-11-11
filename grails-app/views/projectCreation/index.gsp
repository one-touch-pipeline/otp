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
    <title><g:message code="otp.menu.projectCreation"/></title>
    <asset:javascript src="common/MultiInputField.js"/>
</head>
<body>
    <div class="body">
    <g:render template="/templates/messages"/>

    <g:uploadForm controller="projectCreation" action="save">
        <table>
            <tr>
                <td class="myKey"><g:message code="projectCreation.name"/></td>
                <td><g:textField name="name" size="130" value="${cmd?.name}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.individualPrefix"/></td>
                <td><g:textField name="individualPrefix" size="130" value="${cmd?.individualPrefix}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.directory"/></td>
                <td><g:textField name="directory" size="130" value="${cmd?.directory}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.analysisDirectory"/></td>
                <td><g:textField name="analysisDirectory" size="130" value="${cmd?.analysisDirectory}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.keywords"/></td>
                <td class="multi-input-field">
                    <g:each in="${cmd?.keywords*.name ?: [""]}" var="keyword" status="i">
                        <div class="field">
                            <g:textField list="keywordList" name="keywordNames" size="130" value="${keyword}" />
                            <g:if test="${i == 0}">
                                <button class="add-field">+</button>
                            </g:if>
                            <g:else>
                                <button class="remove-field">-</button>
                            </g:else>
                        </div>
                    </g:each>
                    <datalist id="keywordList">
                        <g:each in="${keywords}" var="keyword">
                            <option value="${keyword.name}">${keyword.name}</option>
                        </g:each>
                    </datalist>
                </td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.description"/></td>
                <td><g:textArea name="description" cols="130" value="${cmd?.description}" style="width: auto" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.connectedProjects"/></td>
                <td class="multi-input-field">
                    <g:each in="${cmd?.connectedProjects?.split(",") ?: [""]}" var="connectedProject" status="i">
                        <div class="field">
                            <g:textField list="projectList" name="connectedProjectNames" size="130" value="${connectedProject}"/>
                            <g:if test="${i == 0}">
                                <button class="add-field">+</button>
                            </g:if>
                            <g:else>
                                <button class="remove-field">-</button>
                            </g:else>
                        </div>
                    </g:each>
                </td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.subsequentApplication"/></td>
                <td><g:textField list="projectList" name="subsequentApplication" size="130" value="${cmd?.subsequentApplication}"/></td>
            </tr>
            <datalist id="projectList">
                <g:each in="${projects}" var="project">
                    <option value="${project.name}">${project.name}</option>
                </g:each>
            </datalist>
            <tr>
                <td class="myKey"><g:message code="projectCreation.tumorEntity"/></td>
                <td><g:select class="criteria" name='tumorEntityName' from='${tumorEntities}' value="${cmd?.tumorEntity}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.speciesWithStrain"/></td>
                <td><g:select class="criteria" name='speciesWithStrain' from='${allSpeciesWithStrains}' value="${cmd?.speciesWithStrain}" optionKey="id" noSelection="${['': 'None']}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.unixGroup"/></td>
                <td><g:textField name="unixGroup" size="130" value="${cmd?.unixGroup}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.costCenter"/></td>
                <td><g:textField name="costCenter" size="130" value="${cmd?.costCenter}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.organisationUnit"/></td>
                <td><g:textField name="organisationUnit" size="130" value="${cmd?.organisationUnit}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.projectType"/></td>
                <td><g:select class="criteria" id="group" name='projectType' from='${projectTypes}' value="${cmd?.projectType ?: defaultProjectType}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.forceCopyFiles"/></td>
                <td><g:checkBox name="forceCopyFiles" checked="${cmd == null || cmd?.forceCopyFiles}" value="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.endDate"/></td>
                <td><input type="date" name="endDateInput" value="${cmd?.endDate?.format("yyyy-MM-dd") ?: ""}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.storageUntil"/></td>
                <td><input type="date" name="storageUntilInput" value="${cmd?.storageUntil?.format("yyyy-MM-dd") ?: defaultDate}" required/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.metadata.name"/></td>
                <td><g:textField name="nameInMetadataFiles" size="130" value="${cmd?.nameInMetadataFiles}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.processingPriority"/></td>
                <td><g:select class="criteria" id="priority" name='processingPriority' from='${processingPriorities}' value="${cmd?.processingPriority ?: defaultProcessingPriority}" required="true"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.projectGroup"/></td>
                <td><g:select class="criteria" id="group" name='projectGroup' from='${projectGroups}' value="${cmd?.projectGroup}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.sampleParser"/></td>
                <td><g:select class="criteria" id="group" name='sampleIdentifierParserBeanName' from='${sampleIdentifierParserBeanNames}' value="${cmd?.sampleIdentifierParserBeanName}" optionValue="displayName"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.qcThresholdHandling"/></td>
                <td><g:select class="criteria" id="group" name='qcThresholdHandling' from='${qcThresholdHandlings}' value="${cmd?.qcThresholdHandling ?: defaultQcThresholdHandling}"/></td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.projectInfo"/></td>
                <td>
                    <input type="file" name="projectInfoFile" id="projectInfoFile"/>
                </td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="projectCreation.internalNotes"/></td>
                <td><g:textArea name="internalNotes" cols="130" value="${cmd?.internalNotes}" style="width: auto"/></td>
            </tr>
            <tr>
                <td></td>
                <td><g:submitButton name="submit" value="Submit"/></td>
            </tr>

            %{--if someone want to change this it is hidden now--}%
            <tr hidden>
                <td class="myKey"><g:message code="projectCreation.fingerPrinting"/></td>
                <td><g:checkBox name="fingerPrinting" checked="${cmd == null ? true : cmd?.fingerPrinting}" value="true"/></td>
            </tr>
        </table>
    </g:uploadForm>
    </div>
</body>
</html>
