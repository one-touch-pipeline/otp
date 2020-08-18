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
<%@ page import="de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain" %>
<%@ page import="de.dkfz.tbi.otp.config.GuiAnnotation" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${g.message(code: "projectRequest.title")}</title>
    <asset:javascript src="common/UserAutoComplete.js"/>
    <asset:javascript src="common/MultiInputField.js"/>
    <asset:javascript src="common/CloneField.js"/>
    <asset:javascript src="pages/projectRequest/index.js"/>
    <asset:javascript src="taglib/NoSwitchedUser.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <g:render template="tabMenu"/>

    <h2>${g.message(code: "projectRequest.title")}</h2>
    <otp:annotation type="info">
        <g:if test="${faqLink}">
            ${g.message(code: "projectRequest.new.support.faq", args: [contactDataSupportEmail, faqLink])}
        </g:if>
        <g:else>
            ${g.message(code: "projectRequest.new.support", args: [contactDataSupportEmail])}
        </g:else>
    </otp:annotation>
    <otp:annotationPO option-type="${GuiAnnotation.PROJECT_REQUEST}" type="info"/>
    <g:if test="${projectRequestToEdit}">
        <otp:annotation type="warning">
            ${g.message(code: "projectRequest.edit.warning", args: [projectRequestToEdit.name, projectRequestToEdit.requester])}
        </otp:annotation>
    </g:if>
    <br>

    <div class="clone-template hidden">
        <g:render template="projectRequestUserForm" model="[i: 'template-index', availableRoles: availableRoles]"/>
    </div>

    <g:form>
    <table class="key-value-table key-help-input">
        <g:hiddenField name="request.id" value="${projectRequestToEdit?.id}"/>
        <tr>
            <td><label for="name">${g.message(code: "project.name")}*</label></td>
            <td class="help" title="${g.message(code: "projectRequest.name.detail")}"></td>
            <td><input name="name" id="name" value="${source.getByFieldName("name")}" required/></td>
        </tr>
        <tr>
            <td><label for="description">${g.message(code: "project.description")}*</label></td>
            <td class="help" title="${g.message(code: "projectRequest.description.detail")}"></td>
            <td><textarea class="resize-vertical" name="description" id="description" required>${source.getByFieldName("description")}</textarea></td>
        </tr>
        <tr>
            <td><label for="keyword">${g.message(code: "project.keywords")}*</label></td>
            <td class="help" title="${g.message(code: "projectRequest.keywords.detail")}"></td>
            <td class="multi-input-field">
                <g:each in="${source.getByFieldName("keywords")?.sort()}" var="keyword" status="i">
                    <div class="field">
                        <input list="keywordList" name="keywords" id="keyword" type="text" value="${keyword}" required>
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
            <td><label for="organizationalUnit">${g.message(code: "project.organizationalUnit")}*</label></td>
            <td class="help" title="${g.message(code: "projectRequest.organizationalUnit.detail")}"></td>
            <td><input name="organizationalUnit" id="organizationalUnit" value="${source.getByFieldName("organizationalUnit")}" required/></td>
        </tr>
        <tr>
            <td><label for="costCenter">${g.message(code: "project.costCenter")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.costCenter.detail")}"></td>
            <td><input name="costCenter" id="costCenter" value="${source.getByFieldName("costCenter")}"/></td>
        </tr>
        <tr>
            <td><label for="fundingBody">${g.message(code: "project.fundingBody")}</label></td>
            <td></td>
            <td><input name="fundingBody" id="fundingBody" value="${source.getByFieldName("fundingBody")}"/></td>
        </tr>
        <tr>
            <td><label for="grantId">${g.message(code: "project.grantId")}</label></td>
            <td></td>
            <td><input name="grantId" id="grantId" value="${source.getByFieldName("grantId")}"/></td>
        </tr>
        <tr>
            <td><label for="endDate">${g.message(code: "project.endDate")}</label></td>
            <td></td>
            <td><input name="endDate" id="endDate" value="${source.getByFieldName("endDate")}" type="date"/></td>
        </tr>
        <tr>
            <td><label for="storagePeriod">${g.message(code: "project.storageUntil")}*</label></td>
            <td class="help" title="${g.message(code: "projectRequest.storageUntil.detail")}"></td>
            <td>
                <g:select name="storagePeriod" class="use-select-2"
                          value="${source.getByFieldName("storagePeriod")}" from="${storagePeriods}"
                          optionKey="name" optionValue="description"
                          noSelection="${["": ""]}" required="true" />
                <br>
                <input name="storageUntil" id="storageUntil" value="${source.getByFieldName("storageUntil")}" type="date" style="display: none"/>
            </td>
        </tr>
        <tr>
            <td><label for="relatedProjects">${g.message(code: "project.relatedProjects")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.relatedProjects.detail")}"></td>
            <td><input name="relatedProjects" id="relatedProjects" value="${source.getByFieldName("relatedProjects")}"/></td>
        </tr>
%{--
        <tr>
            <td><label for="tumorEntity">${g.message(code: "project.tumorEntity")}</label></td>
            <td></td>
            <td>
                <g:select name="tumorEntity.id" id="tumorEntity" class="use-select-2"
                          value="${cmd?.tumorEntity?.id}" from="${tumorEntities}" optionValue="name" optionKey="id"
                          noSelection="${[null: ""]}" /></td>
        </tr>
--}%
        <tr>
            <g:set var="customSpeciesWithStrain" value="${source.getByFieldName("customSpeciesWithStrain")}"/>
            <td><label for="speciesWithStrain">${g.message(code: "project.speciesWithStrain")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.speciesWithStrain.detail")}"></td>
            <td>
                <g:select id="speciesWithStrain" name="speciesWithStrain.id" class="use-select-2"
                          from="${species}" value="${customSpeciesWithStrain ? "other" : (source.getByFieldName("speciesWithStrain") as SpeciesWithStrain)?.id}"
                          optionKey="id" optionValue="displayString"
                          noSelection="${['': 'None']}" />
                <br>
                <input name="customSpeciesWithStrain" id="customSpeciesWithStrain" value="${customSpeciesWithStrain}" type="text" disabled/>
            </td>
        </tr>
        <tr>
            <td><label for="projectType">${g.message(code: "project.projectType")}</label></td>
            <td></td>
            <td><g:select id="projectType" name="projectType" class="use-select-2"
                          from="${projectTypes}" value="${source.getByFieldName("projectType")}"/></td>
        </tr>
        <tr>
            <td><label for="sequencingCenter">${g.message(code: "projectRequest.sequencingCenter")}</label></td>
            <td></td>
            <td><input name="sequencingCenter" id="sequencingCenter" value="${source.getByFieldName("sequencingCenter")}"/></td>
        </tr>
        <tr>
            <td><label for="approxNoOfSamples">${g.message(code: "projectRequest.approxNoOfSamples")}</label></td>
            <td></td>
            <td><input name="approxNoOfSamplesString" id="approxNoOfSamples" type="number" value="${source.getByFieldName("approxNoOfSamples")}"/></td>
        </tr>
        <tr>
            <td>${g.message(code: "projectRequest.seqTypes")}</td>
            <td class="help" title="${g.message(code: "projectRequest.seqTypes.detail")}"></td>
            <td class="multi-input-field">
                <g:each in="${source.getByFieldName("seqTypes") ?: [null]}" var="seqType" status="i">
                    <div class="field">
                        <g:select id="" name="seqType.id" class="use-select-2"
                                  from="${seqTypes}" value="${seqType?.id ?: ""}"
                                  optionKey="id" optionValue="displayNameWithLibraryLayout"
                                  noSelection="${["": "None"]}"/>
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
            <td><label for="comments">${g.message(code: "projectRequest.comments")}</label></td>
            <td></td>
            <td><textarea class="resize-vertical" name="comments" id="comments">${source.getByFieldName("comments")}</textarea></td>
        </tr>
    </table>

    <h3>${g.message(code: "projectRequest.users")}</h3>
    ${g.message(code: "projectRequest.users.detail")}
    <br><br>

    <g:set var="users" value="${source.getByFieldName('users').findAll() ?: []}"/>
    <div class="clone-target" data-highest-index="${users.size()}">
        <g:each in="${users}" var="user" status="i">
            <g:render template="projectRequestUserForm" model="[i: i, user: user, availableRoles: availableRoles]"/>
        </g:each>
    </div>
    <button class="clone-add"><g:message code="projectRequest.users.add"/></button>

    <br><br>

    <table class="key-value-table key-help-input">
        <tr>
            <td></td>
            <td></td>
            <td>${g.message(code: "projectRequest.explain.submitter")}<br></td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td>
                <otpsecurity:noSwitchedUser>
                    <g:if test="${projectRequestToEdit}">
                        <div class="element-with-annotation-flex-container">
                            <div>
                                <g:actionSubmit action="saveEdit" name="saveEdit" value="${g.message(code: "projectRequest.submit.edit")}" />
                            </div>
                            <otp:annotation type="warning" variant="inline">
                                ${g.message(code: "projectRequest.edit.warning.submit")}
                            </otp:annotation>
                        </div>
                    </g:if>
                    <g:else>
                        <g:actionSubmit action="save" name="save" value="${g.message(code: "projectRequest.submit")}" />
                    </g:else>
                </otpsecurity:noSwitchedUser>
            </td>
        </tr>
        <tr>
            <td colspan="3"><br></td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td>${g.message(code: "projectRequest.explain.requiredFields")}</td>
        </tr>
    </table>
    </g:form>
</div>
</body>
</html>
