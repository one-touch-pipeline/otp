%{--
  - Copyright 2011-2020 The OTP authors
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
<%@ page import="de.dkfz.tbi.otp.ngsdata.UserProjectRole" %>
<%@ page import="de.dkfz.tbi.otp.ngsdata.TumorEntity" %>
<%@ page import="de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain" %>
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

    <h1><g:message code="otp.menu.projectCreation"/></h1>

    <h2><g:message code="projectCreation.basis.header"/></h2>

    <otp:annotation type="info">
        <span class="keep-whitespace"><g:message code="projectCreation.basis.annotation"/></span>
    </otp:annotation>

    <g:form class="projectCreationBasisSelection" action="index" method="GET">
        <table class="key-value-table key-input">
            <tr>
                <td><g:message code="projectCreation.basis.projectRequest"/></td>
                <td>
                    <g:select id="projectRequest" name="projectRequest.id" class="use-select-2"
                              value="${projectRequest?.id}" from="${projectRequests}" noSelection="${[null:"No project request selected"]}"
                              optionKey="id" optionValue="name"/>
                </td>
            </tr>
            <tr>
                <td><g:message code="projectCreation.basis.project"/></td>
                <td>
                    <g:select id="baseProject" name="baseProject.id" class="use-select-2"
                              value="${baseProject?.id}" from="${availableProjects}" noSelection="${[null:"No project selected"]}"
                              optionKey="id" optionValue="name"/>
                </td>
            </tr>
        </table>
    </g:form>

    <h2><g:message code="projectCreation.form.header"/></h2>
    <g:uploadForm controller="projectCreation" action="save">
        <g:if test="${showIgnoreUsersFromBaseObjects}">
            <otp:annotation type="warning">
                <label class="vertical-align-middle">
                    <strong><g:message code="projectCreation.ignoreUsersFromBaseObjects"/>:</strong>
                    <g:checkBox name="ignoreUsersFromBaseObjects" checked="false" value="true"/>
                </label>
                <br>
                <span class="keep-whitespace"><g:message code="projectCreation.ignoreUsersFromBaseObjects.information"/></span>
            </otp:annotation>
        </g:if>
        <g:if test="${projectRequest}">
            <h3><g:message code="projectCreation.projectRequest.header" args="[projectRequest.name]"/></h3>
            <g:hiddenField name="projectRequest.id" value="${projectRequest.id}"/>
            <table class="key-value-table key-input">
                <tr>
                    <td><g:message code="projectRequest.requester"/></td>
                    <td>${projectRequest.requester}</td>
                </tr>
                <tr>
                    <td><g:message code="projectRequest.pi"/></td>
                    <td>${projectRequest.pi}</td>
                </tr>
                <tr>
                    <td><g:message code="projectRequest.leadBioinformatician"/></td>
                    <td>${projectRequest.leadBioinformaticians?.join(", ")}</td>
                </tr>
                <tr>
                    <td><g:message code="projectRequest.bioinformatician"/></td>
                    <td>${projectRequest.bioinformaticians?.join(", ")}</td>
                </tr>
                <tr>
                    <td><g:message code="projectRequest.submitter"/></td>
                    <td>${projectRequest.submitters?.join(", ")}</td>
                </tr>
                <tr>
                    <td><g:message code="projectRequest.sequencingCenter"/></td>
                    <td>${projectRequest.sequencingCenter}</td>
                </tr>
                <tr>
                    <td><g:message code="projectRequest.approxNoOfSamples"/></td>
                    <td>${projectRequest.approxNoOfSamples}</td>
                </tr>
                <tr>
                    <td><g:message code="projectRequest.seqTypes"/></td>
                    <td>${projectRequest.seqTypes?.join(", ")}</td>
                </tr>
                <g:if test="${projectRequest.customSpeciesWithStrain}">
                <tr>
                    <td><g:message code="projectRequest.customSpeciesWithStrain"/></td>
                    <td>
                        <span style="margin-right: 20px">${projectRequest.customSpeciesWithStrain}</span>
                        <otp:annotation type="warning" variant="inline">
                            <g:message code="projectRequest.customSpeciesWithStrain.link"/>
                            <g:link controller="speciesWithStrain" action="index" params="[helper: projectRequest.customSpeciesWithStrain]">here</g:link>
                        </otp:annotation>
                    </td>
                </tr>
                </g:if>
                <tr>
                    <td><g:message code="projectRequest.comments"/></td>
                    <td><div class="project-multiline-wrapper">${projectRequest.comments}</div></td>
                </tr>
            </table>
        </g:if>
        <g:if test="${baseProject}">
            <g:hiddenField name="baseProject.id" value="${baseProject.id}"/>
            <h3><g:message code="projectCreation.baseProject.header" args="[baseProject.name]"/></h3>
            <table class="key-value-table key-input">
                <tr>
                    <td><g:message code="projectCreation.baseProject.users"/></td>
                    <td>
                        <g:set var="selectedUsers" value="${(source.getByFieldName("usersToCopyFromBaseProject") as List<UserProjectRole>).sort { it.user.username } ?: []}"/>
                        <g:each var="userProjectRole" in="${baseProjectUsers}" status="i">
                            <label class="vertical-align-middle">
                                <g:checkBox name="usersToCopyFromBaseProject.id"
                                            checked="${userProjectRole.id in selectedUsers*.id}"
                                            value="${userProjectRole.id}"/>
                                ${userProjectRole.user.username}
                            </label>
                            <br>
                        </g:each>
                    </td>
                </tr>
            </table>
        </g:if>
        <h3><g:message code="projectCreation.general.header"/></h3>
        <table class="key-value-table key-input-text-text">
            <tr>
                <th><g:message code="otp.blank"/></th>
                <th><g:message code="otp.blank"/></th>
                <th><g:message code="project.base.values.request"/></th>
                <th><g:message code="project.base.values.project"/></th>
            </tr>
            <tr>
                <td><g:message code="project.name"/></td>
                <td><g:textField name="name" value="${source.getByFieldName("name")}" required="true"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'name', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.individualPrefix"/></td>
                <td><g:textField name="individualPrefix" value="${source.getByFieldName("individualPrefix")}" required="true"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'individualPrefix', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.directory"/></td>
                <td><g:textField name="dirName" value="${source.getByFieldName("dirName")}" required="true"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'dirName', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.analysisDirectory"/></td>
                <td><g:textField name="dirAnalysis" value="${source.getByFieldName("dirAnalysis")}" required="true"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'dirAnalysis', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.keywords"/></td>
                <td class="multi-input-field">
                    <g:each in="${(projectCreationCmd?.keywords ?: source.getAllByFieldName("keywords")).flatten().findResults { it ? it.toString() : null }.unique().sort() ?: [""]}" var="keyword" status="i">
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
                <g:render template="baseValueColumns" model="[fieldName: 'keywords', cmd: cmd, type: 'sorted-list']"/>
            </tr>
            <tr>
                <td><g:message code="project.description"/></td>
                <td><g:textArea class="resize-vertical" name="description" value="${source.getByFieldName("description")}" required="true"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'description', cmd: cmd, type: 'multi-line-string']"/>
            </tr>
            <tr>
                <td><g:message code="project.relatedProjects"/></td>
                <td class="multi-input-field">
                    <g:each in="${(projectCreationCmd?.relatedProjects ?: source.getAllByFieldName("relatedProjects").flatten().findAll().join(",")).split(",").toUnique()}" var="relatedProject" status="i">
                        <div class="field">
                            <g:textField list="projectList" name="relatedProjectNames" value="${relatedProject}"/>
                            <g:if test="${i == 0}">
                                <button class="add-field">+</button>
                            </g:if>
                            <g:else>
                                <button class="remove-field">-</button>
                            </g:else>
                        </div>
                    </g:each>
                </td>
                <g:render template="baseValueColumns" model="[fieldName: 'relatedProjects', cmd: cmd, type: 'comma-separated-sorted-list']"/>
            </tr>
            <datalist id="projectList">
                <g:each in="${projects}" var="project">
                    <option value="${project.name}">${project.name}</option>
                </g:each>
            </datalist>
            <tr>
                <td><g:message code="project.tumorEntity"/></td>
                <td><g:select name="tumorEntity" class="use-select-2"
                              from="${tumorEntities}" value="${(source.getByFieldName("tumorEntity") as TumorEntity)?.id}" optionKey="id"
                              noSelection="${['': 'No Tumor Entity']}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'tumorEntity', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.speciesWithStrain"/></td>
                <td><g:select name="speciesWithStrain" class="use-select-2"
                              from="${allSpeciesWithStrains}" value="${(source.getByFieldName("speciesWithStrain") as SpeciesWithStrain)?.id}" optionKey="id"
                              noSelection="${['': 'None']}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'speciesWithStrain', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.unixGroup"/></td>
                <td><g:textField name="unixGroup" value="${source.getByFieldName("unixGroup")}" required="true"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'unixGroup', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.costCenter"/></td>
                <td><g:textField name="costCenter" value="${source.getByFieldName("costCenter")}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'costCenter', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.organizationalUnit"/></td>
                <td><g:textField name="organizationalUnit" value="${source.getByFieldName("organizationalUnit")}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'organizationalUnit', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.fundingBody"/></td>
                <td><g:textField name="fundingBody" value="${source.getByFieldName("fundingBody")}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'fundingBody', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.grantId"/></td>
                <td><g:textField name="grantId" value="${source.getByFieldName("grantId")}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'grantId', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.projectType"/></td>
                <td><g:select name='projectType' class="use-select-2"
                              from='${projectTypes}' value="${source.getByFieldName("projectType")}" required="true"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'projectType', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.forceCopyFiles"/></td>
                <td><g:checkBox name="forceCopyFiles" checked="${source.getByFieldName("forceCopyFiles")}" value="true"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'forceCopyFiles', cmd: cmd, type: 'boolean']"/>
            </tr>
            <tr>
                <td><g:message code="project.endDate"/></td>
                <td><input type="date" name="endDateInput" value="${(source.getFieldAsLocalDate("endDate"))?.format(DateTimeFormatter.ISO_LOCAL_DATE)}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'endDate', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.storageUntil"/></td>
                <td><input type="date" name="storageUntilInput" value="${source.getFieldAsLocalDate("storageUntil")?.format(DateTimeFormatter.ISO_LOCAL_DATE)}" required/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'storageUntil', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.nameInMetadata"/></td>
                <td><g:textField name="nameInMetadataFiles" value="${source.getByFieldName("nameInMetadataFiles")}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'nameInMetadataFiles', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.processingPriority"/></td>
                <td><g:select id="priority" name="processingPriority" class="use-select-2"
                              from="${processingPriorities}" optionKey="id" optionValue="name" required="true"
                              value="${source.getByFieldName("processingPriority")?.id}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'processingPriority', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.group"/></td>
                <td><g:select name="projectGroup" class="use-select-2"
                              from="${projectGroups}" value="${source.getByFieldName("projectGroup")}" noSelection="${['': 'No Group']}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'projectGroup', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.sampleParser"/></td>
                <td><g:select id="parserBeanName" name="sampleIdentifierParserBeanName" class="use-select-2"
                              from="${sampleIdentifierParserBeanNames}" value="${source.getByFieldName("sampleIdentifierParserBeanName")}"
                              optionValue="displayName"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'sampleIdentifierParserBeanName', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.qcThresholdHandling"/></td>
                <td><g:select name="qcThresholdHandling" class="use-select-2"
                              from="${qcThresholdHandlings}" value="${source.getByFieldName("qcThresholdHandling")}" required="true"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'qcThresholdHandling', cmd: cmd]"/>
            </tr>
            <tr>
                <td><g:message code="project.projectInfo"/></td>
                <td><input type="file" name="projectInfoFile" id="projectInfoFile" ${projectCreationCmd?.projectInfoFile as boolean ? "required" : ""}/></td>
                <td></td>
                <td>
                    <g:if test="${baseProjectInfos}">
                        <g:select name="projectInfoToCopy" class="use-select-2"
                                  from="${baseProjectInfos}" value="${projectCreationCmd?.projectInfoToCopy?.id}"
                                  optionKey="id" optionValue="path"
                                  noSelection="${['': 'No project info selected']}"/>
                    </g:if>
                </td>
            </tr>
            <tr>
                <td><g:message code="project.internalNotes"/></td>
                <td><g:textArea class="resize-vertical" name="internalNotes" value="${source.getByFieldName("internalNotes")}"/></td>
                <g:render template="baseValueColumns" model="[fieldName: 'internalNotes', cmd: cmd, type: 'multi-line-string']"/>
            </tr>
            <tr>
                <td></td>
                <td><g:submitButton name="submit" value="Submit"/></td>
                <td></td>
                <td></td>
            </tr>

            %{-- if someone wants to change this, it is now hidden --}%
            <tr hidden>
                <td><g:message code="project.fingerPrinting"/></td>
                <td><g:checkBox name="fingerPrinting" checked="${source.getByFieldName("fingerPrinting")}" value="true"/></td>
                <td></td>
                <td></td>
            </tr>
        </table>
    </g:uploadForm>
</div>
</body>
</html>
