%{--
  - Copyright 2011-2021 The OTP authors
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
<%@ page import="de.dkfz.tbi.otp.project.projectRequest.StoragePeriod" %>
<%@ page import="de.dkfz.tbi.otp.project.additionalField.ProjectFieldType;" %>
<%@ page import="de.dkfz.tbi.otp.config.TypeValidators;" %>
<%@ page import="de.dkfz.tbi.otp.project.additionalField.FieldExistenceType;" %>
<html>
<head>
    <title>${g.message(code: "projectRequest.title")}</title>
    <asset:javascript src="common/UserAutoComplete.js"/>
    <asset:javascript src="pages/projectRequest/index.js"/>
    <asset:javascript src="pages/projectRequest/userFormAdd.js"/>
    <asset:javascript src="common/CloneField.js"/>
    <asset:stylesheet src="pages/projectRequest/index.less"/>
</head>

<body>

<div class="container-fluid otp-main-container">
    <g:render template="templates/tabs"/>
    <h3 class="mb-3">${g.message(code: "projectRequest.title")}</h3>

    <otp:annotation type="info">
        <g:if test="${faqLink}">
            ${g.message(code: "projectRequest.new.support.faq", args: [contactDataSupportEmail, faqLink])}
        </g:if>
        <g:else>
            ${g.message(code: "projectRequest.new.support", args: [contactDataSupportEmail])}
        </g:else>
    </otp:annotation>

    <g:form method="POST" useToken="true">
        <g:hiddenField id="projectRequestId" name="projectRequest.id" value="${cmd?.projectRequest?.id}"/>

        <!-- Project Type* -->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" for="projectType">${g.message(code: "project.projectType")}*</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "projectRequest.projectType.detail")}"></i>
            </div>

            <div class="col-sm-10">
                <g:select id="projectType" name="projectType" class="use-select-2 form-control"
                          from="${projectTypes}" value="${cmd?.projectType}"
                          noSelection="${['':'']}"
                          data-placeholder="Select a project type"
                          autocomplete="off"
                          required="true"/>
            </div>
        </div>

        <!-- Project Name* -->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" for="name">${g.message(code: "project.name")}*</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${projectNameDescription}"></i>
            </div>

            <div class="col-sm-10">
                <g:if test="${projectNamePattern}">
                    <input name="name"
                           class="form-control"
                           id="name"
                           title="${projectNameDescription}"
                           value="${cmd?.name}"
                           pattern=${projectNamePattern} required/>
                </g:if>
                <g:else>
                    <input name="name" class="form-control" id="name" value="${cmd?.name}" required/>
                </g:else>
            </div>
        </div>

        <!-- Description* -->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" for="description">${g.message(code: "project.description")}*</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "projectRequest.description.detail")}"></i>
            </div>

            <div class="col-sm-10">
                <textarea class="form-control"
                          name="description"
                          id="description"
                          minlength="50" required>${cmd?.description}</textarea>
            </div>
        </div>

        <!-- Keyword(s)* -->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" for="keywords">${g.message(code: "project.keywords")}*</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "projectRequest.keywords.detail")}"></i>
            </div>

            <div class="col-sm-10">
                <select class="use-select-2 tag-select form-control"
                        name="keywords"
                        name="keywords"
                        id="keywords"
                        multiple="multiple"
                        required>
                    <g:each in="${keywords}" var="keyword">
                        <g:set var="selected" value="${keyword in cmd?.keywords ? "selected" : ""}"/>
                        <option value="${keyword.name}" ${selected}>${keyword.name}</option>
                    </g:each>
                </select>
            </div>
        </div>

        <!-- End Date -->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" for="endDate">${g.message(code: "project.endDate")}</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "projectRequest.endDate.detail")}"></i>
            </div>

            <div class="col-sm-10">
                <input class="form-control" name="endDate" id="endDate" value="${cmd?.endDate}" type="date"/>
            </div>
        </div>

        <!-- Storage Until* -->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" for="storagePeriod">${g.message(code: "project.storageUntil")}*</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "projectRequest.storageUntil.detail")}"></i>
            </div>

            <div class="col-sm-5">
                <g:select class="use-select-2 form-control"
                          id="storagePeriod"
                          name="storagePeriod"
                          from="${storagePeriod}"
                          value="${cmd?.storagePeriod}"
                          noSelection="${['':'']}"
                          data-placeholder="Select a project type"
                          optionKey="name"
                          optionValue="description"
                          required="true"/>
            </div>

            <div class="col-sm-5">
                <g:set var="disabled" value="${cmd?.storagePeriod == StoragePeriod.USER_DEFINED ? "" : "disabled"}"/>
                <input class="form-control" name="storageUntil" id="storageUntil" value="${cmd?.storageUntil}" type="date" ${disabled}/>
            </div>
        </div>

        <!-- Related Projects -->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" for="relatedProjects">${g.message(code: "project.relatedProjects")}</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "projectRequest.relatedProjects.detail")}"></i>
            </div>

            <div class="col-sm-10">
                <input class="form-control" name="relatedProjects" id="relatedProjects" value="${cmd?.relatedProjects}"/>
            </div>
        </div>

        <!-- Species [with Strain]-->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" for="speciesWithStrainList">${g.message(code: "project.speciesWithStrain")}</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "projectRequest.speciesWithStrain.detail")}"></i>
            </div>

            <div class="col-sm-10">
                <select class="use-select-2 tag-select form-control" name="speciesWithStrainList" id="speciesWithStrainList" multiple="multiple" from="speciesWithStrainList">
                    <g:each in="${speciesWithStrains}" var="species">
                        <g:set var="selected" value="${species in cmd?.speciesWithStrains ? "selected" : ""}"/>
                        <option value="${species.id}" ${selected}>${species.displayString}</option>
                    </g:each>
                    <g:each in="${cmd?.customSpeciesWithStrains}" var="customSpecies">
                        <option value="${customSpecies}" selected>${customSpecies}</option>
                    </g:each>
                </select>
            </div>
        </div>

        <!-- Sequencing Center -->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" for="sequencingCenters">${g.message(code: "projectRequest.sequencingCenter")}</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "projectRequest.sequencingCenter.detail")}"></i>
            </div>

            <div class="col-sm-10">
                <select class="use-select-2 form-control tag-select"
                        name="sequencingCenters"
                        id="sequencingCenters"
                        multiple="multiple">
                    <g:each in="${sequencingCenters}" var="sequencingCenter">
                        <g:set var="selected" value="${sequencingCenter in cmd?.sequencingCenters ? "selected" : ""}"/>
                        <option value="${sequencingCenter}" ${selected}>${sequencingCenter}</option>
                    </g:each>
                </select>
            </div>
        </div>

        <!-- Approximate Number of Samples* -->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" id="approxNoOfSamplesLabel" for="approxNoOfSamples">${g.message(code: "projectRequest.approxNoOfSamples")}*</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "projectRequest.approxNumberOfSamples.detail")}"></i>
            </div>

            <div class="col-sm-10">
                <input class="form-control" name="approxNoOfSamples" id="approxNoOfSamples" type="number" min="0" value="${cmd?.approxNoOfSamples}" required/>
            </div>
        </div>

        <!-- Sequencing Type(s)* -->
        <div class="form-group row">
            <div class="col-sm-2">
                <label class="col-form-label" for="seqTypes" id="seqTypesLabel">${g.message(code: "projectRequest.seqTypes")}</label>
                <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "projectRequest.seqTypes.detail")}"></i>
            </div>

            <div class="col-sm-10">
                <select class="use-select-2 form-control" name="seqTypes" id="seqTypes" multiple="multiple">
                    <g:each in="${seqTypes}" var="seqType">
                        <g:set var="selected" value="${seqType in cmd?.seqTypes ? "selected" : ""}"/>
                        <option value="${seqType.id}" ${selected}>${seqType.displayNameWithLibraryLayout}</option>
                    </g:each>
                </select>
            </div>
        </div>

        <!-- abstract Fields -->
        <div class="abstract-fields-container">
            <!-- logic is implemented in the javascript index file-->
            <g:each var="abstractValue" in="${cmd?.additionalFieldValue}">
                <g:hiddenField id="tempAbstractValue_${abstractValue.key}" name="tempAbstractValue_${abstractValue.key}" value="${abstractValue.value}"/>
            </g:each>

        </div>

        <!-- Comments -->
        <div class="form-group row">
            <label class="col-sm-2 col-form-label" for="comments">${g.message(code: "projectRequest.comments")}</label>

            <div class="col-sm-10">
                <textarea class="form-control" name="comments" id="comments">${cmd?.comments}</textarea>
            </div>
        </div>

        <!-- User adding -->
        <div class="container user-form">
            <div class="row">
                <div class="col-sm-9">
                    <h3>Add user</h3>
                    <p>${g.message(code: "projectRequest.users.detail")}</p>
                </div>

                <div class="col-sm-3">
                    <button class="btn btn-primary user-add-button" id="clone-add">
                        <i class="bi bi-plus"></i>
                    </button>
                </div>
            </div>

            <div class="clone-target" id="accordion" data-highest-index="${cmd?.users?.size() ?: 1}">
                <g:if test="${cmd?.users}">
                    <g:each in="${cmd?.users}" var="user" status="i">
                        <g:if test="${user}">
                            <g:render template="templates/userFormItem" model="[i: i, user: user, availableRoles: availableRoles]"/>
                        </g:if>
                    </g:each>
                </g:if>
                <g:else>
                    <g:render template="templates/userFormItem" model="[i: 1, emptyForm: true, availableRoles: availableRoles]"/>
                </g:else>
            </div>
        </div>

        <!-- submit/draft form actions -->
        <g:render template="templates/submitArea" model="[buttonActions: buttonActions]"/>

    </g:form>

    <div class="clone-template hidden">
        <g:render template="templates/userFormItem" model="[i: 'template-index', availableRoles: availableRoles]"/>
    </div>
</div>



</body>
</html>
