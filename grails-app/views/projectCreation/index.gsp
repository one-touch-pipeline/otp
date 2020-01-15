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
<%@ page import="java.time.format.DateTimeFormatter" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="otp.menu.projectCreation"/></title>
    <asset:javascript src="common/MultiInputField.js"/>
    <asset:javascript src="pages/projectCreation/index.js"/>
</head>
<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <g:form class="projectRequestSelection" action="index" method="GET">
        <table class="key-value-table">
            <tr>
                <td>${g.message(code: "projectCreation.projectRequest")}</td>
                <td>
                    <g:select id="fromRequest" name="fromRequest.id" value="${projectRequest?.id}" from="${projectRequests}" noSelection="${[null:""]}" optionKey="id" optionValue="name"/>
                </td>
            </tr>
        </table>
    </g:form>

    <g:uploadForm controller="projectCreation" action="save">
        <table class="key-value-table">
            <g:if test="${projectRequest}">
                <g:hiddenField name="projectRequest.id" value="${projectRequest.id}"/>
                <tr>
                    <td>${g.message(code: "projectRequest.requester")}</td>
                    <td>${projectRequest.requester}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "projectRequest.pi")}</td>
                    <td>${projectRequest.pi}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "projectRequest.deputyPi")}</td>
                    <td>${projectRequest.deputyPis?.join(", ")}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "projectRequest.responsibleBioinformatician")}</td>
                    <td>${projectRequest.responsibleBioinformaticians?.join(", ")}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "projectRequest.bioinformatician")}</td>
                    <td>${projectRequest.bioinformaticians?.join(", ")}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "projectRequest.submitter")}</td>
                    <td>${projectRequest.submitters?.join(", ")}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "projectRequest.sequencingCenter")}</td>
                    <td>${projectRequest.sequencingCenter}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "projectRequest.approxNoOfSamples")}</td>
                    <td>${projectRequest.approxNoOfSamples}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "projectRequest.seqTypes")}</td>
                    <td>${projectRequest.seqTypes?.join(", ")}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "projectRequest.comments")}</td>
                    <td>${projectRequest.comments}</td>
                </tr>
            </g:if>
            <tr>
                <td><g:message code="project.name"/></td>
                <td><g:textField name="name" value="${cmd?.name ?: projectRequest?.name}" required="true"/></td>
            </tr>
            <tr>
                <td><g:message code="project.individualPrefix"/></td>
                <td><g:textField name="individualPrefix" value="${cmd?.individualPrefix}" required="true"/></td>
            </tr>
            <tr>
                <td><g:message code="project.directory"/></td>
                <td><g:textField name="directory" value="${cmd?.directory}" required="true"/></td>
            </tr>
            <tr>
                <td><g:message code="project.analysisDirectory"/></td>
                <td><g:textField name="analysisDirectory" value="${cmd?.analysisDirectory}" required="true"/></td>
            </tr>
            <tr>
                <td><g:message code="project.keywords"/></td>
                <td class="multi-input-field">
                    <g:each in="${cmd?.keywords*.name ?: projectRequest?.keywords ?: [""]}" var="keyword" status="i">
                        <div class="field">
                            <g:textField list="keywordList" name="keywordNames" value="${keyword}" />
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
                <td><g:message code="project.description"/></td>
                <td><g:textArea name="description" value="${cmd?.description ?: projectRequest?.description}" required="true"/></td>
            </tr>
            <tr>
                <td><g:message code="project.relatedProjects"/></td>
                <td class="multi-input-field">
                    <g:each in="${(cmd?.relatedProjects ?: projectRequest?.relatedProjects)?.split(",") ?: [""]}" var="relatedProject" status="i">
                        <div class="field">
                            <g:textField list="projectList" name="relatedProjectName" value="${relatedProject}"/>
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
            <datalist id="projectList">
                <g:each in="${projects}" var="project">
                    <option value="${project.name}">${project.name}</option>
                </g:each>
            </datalist>
            <tr>
                <td><g:message code="project.tumorEntity"/></td>
                <td><g:select name='tumorEntityName' from='${tumorEntities}' value="${cmd?.tumorEntity ?: projectRequest?.tumorEntity}"/></td>
            </tr>
            <tr>
                <td><g:message code="project.speciesWithStrain"/></td>
                <td><g:select name='speciesWithStrain' from='${allSpeciesWithStrains}' value="${cmd?.speciesWithStrain?: projectRequest?.speciesWithStrain}" optionKey="id" noSelection="${['': 'None']}"/></td>
            </tr>
            <tr>
                <td><g:message code="project.unixGroup"/></td>
                <td><g:textField name="unixGroup" value="${cmd?.unixGroup}" required="true"/></td>
            </tr>
            <tr>
                <td><g:message code="project.costCenter"/></td>
                <td><g:textField name="costCenter" value="${cmd?.costCenter ?: projectRequest?.costCenter}"/></td>
            </tr>
            <tr>
                <td><g:message code="project.organizationalUnit"/></td>
                <td><g:textField name="organizationalUnit" value="${cmd?.organizationalUnit ?: projectRequest?.organizationalUnit}"/></td>
            </tr>
            <tr>
                <td><g:message code="project.fundingBody"/></td>
                <td><g:textField name="fundingBody" value="${cmd?.fundingBody ?: projectRequest?.fundingBody}"/></td>
            </tr>
            <tr>
                <td><g:message code="project.grantId"/></td>
                <td><g:textField name="grantId" value="${cmd?.grantId ?: projectRequest?.grantId}"/></td>
            </tr>
            <tr>
                <td><g:message code="project.projectType"/></td>
                <td><g:select id="group" name='projectType' from='${projectTypes}' value="${cmd?.projectType ?: projectRequest?.projectType ?: defaultProjectType}" required="true"/></td>
            </tr>
            <tr>
                <td><g:message code="project.forceCopyFiles"/></td>
                <td><g:checkBox name="forceCopyFiles" checked="${cmd == null || cmd?.forceCopyFiles}" value="true"/></td>
            </tr>
            <tr>
                <td><g:message code="project.endDate"/></td>
                <td><input type="date" name="endDateInput" value="${(cmd?.endDate ?: projectRequest?.endDate)?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: ""}"/></td>
            </tr>
            <tr>
                <td><g:message code="project.storageUntil"/></td>
                <td><input type="date" name="storageUntilInput" value="${(cmd?.storageUntil ?: projectRequest?.storageUntil)?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: defaultDate}" required/></td>
            </tr>
            <tr>
                <td><g:message code="project.nameInMetadata"/></td>
                <td><g:textField name="nameInMetadataFiles" value="${cmd?.nameInMetadataFiles}"/></td>
            </tr>
            <tr>
                <td><g:message code="project.processingPriority"/></td>
                <td><g:select id="priority" name='processingPriority' from='${processingPriorities}' value="${cmd?.processingPriority ?: defaultProcessingPriority}" required="true"/></td>
            </tr>
            <tr>
                <td><g:message code="project.projectGroup"/></td>
                <td><g:select id="group" name='projectGroup' from='${projectGroups}' value="${cmd?.projectGroup}"/></td>
            </tr>
            <tr>
                <td><g:message code="project.sampleParser"/></td>
                <td><g:select id="group" name='sampleIdentifierParserBeanName' from='${sampleIdentifierParserBeanNames}' value="${cmd?.sampleIdentifierParserBeanName}" optionValue="displayName"/></td>
            </tr>
            <tr>
                <td><g:message code="project.qcThresholdHandling"/></td>
                <td><g:select id="group" name='qcThresholdHandling' from='${qcThresholdHandlings}' value="${cmd?.qcThresholdHandling ?: defaultQcThresholdHandling}"/></td>
            </tr>
            <tr>
                <td><g:message code="project.projectInfo"/></td>
                <td>
                    <input type="file" name="projectInfoFile" id="projectInfoFile"/>
                </td>
            </tr>
            <tr>
                <td><g:message code="project.internalNotes"/></td>
                <td><g:textArea name="internalNotes" value="${cmd?.internalNotes}"/></td>
            </tr>
            <tr>
                <td></td>
                <td><g:submitButton name="submit" value="Submit"/></td>
            </tr>

            %{--if someone want to change this it is hidden now--}%
            <tr hidden>
                <td><g:message code="project.fingerPrinting"/></td>
                <td><g:checkBox name="fingerPrinting" checked="${cmd == null ? true : cmd?.fingerPrinting}" value="true"/></td>
            </tr>
        </table>
    </g:uploadForm>
</div>
</body>
</html>
