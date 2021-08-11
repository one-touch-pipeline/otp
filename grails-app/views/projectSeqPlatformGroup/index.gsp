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

<%@ page import="de.dkfz.tbi.otp.ngsdata.mergingCriteria.SelectorViewState; de.dkfz.tbi.otp.dataprocessing.MergingCriteria" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>${g.message(code: "mergingCriteria.title", args: [selectedProject.name])}</title>
    <asset:stylesheet src="pages/projectSeqPlatformGroup/index.less"/>
    <asset:javascript src="CommentBox.js"/>
    <asset:javascript src="pages/projectSeqPlatformGroup/index.js"/>
</head>

<body>

<div class="container-fluid mb-5">

    %{-- first row: table form--}%

    <h1>${g.message(code: "mergingCriteria.title", args: [selectedProject.name])}</h1>

    <g:form action="update" class="my-5">
        <g:hiddenField name="seqType.id" value="${seqType.id}"/>
        <table>
            <tr>
                <th>${g.message(code: 'projectOverview.mergingCriteria.seqType')}</th>
                <th>${g.message(code: 'projectOverview.mergingCriteria.libPrepKit')}</th>
                <th>${g.message(code: 'projectOverview.mergingCriteria.seqPlatformGroup')}</th>
            </tr>
            <tr>
                <td>
                    ${seqType}
                </td>
                <td>
                    <g:if test="${seqType.isExome()}">
                        true
                        <g:hiddenField name="useLibPrepKit" value="on"/>
                    </g:if>
                    <g:elseif test="${seqType.isWgbs()}">
                        false
                    %{-- no hidden field needed --}%
                    </g:elseif>
                    <g:else>
                        <sec:ifAllGranted roles="ROLE_OPERATOR">
                            <g:checkBox name="useLibPrepKit" checked="${mergingCriteria.useLibPrepKit}" value="true" id="libPrepKit"/>
                        </sec:ifAllGranted>
                        <sec:ifNotGranted roles="ROLE_OPERATOR">
                            ${mergingCriteria.useLibPrepKit}
                        </sec:ifNotGranted>
                    </g:else>
                </td>
                <td>
                    <sec:ifAllGranted roles="ROLE_OPERATOR">
                        <g:select name="useSeqPlatformGroup" class="use-select-2" value="${mergingCriteria.useSeqPlatformGroup}"
                                  from="${MergingCriteria.SpecificSeqPlatformGroups}" id="useSeqPlatformGroup"/>
                    </sec:ifAllGranted>
                    <sec:ifNotGranted roles="ROLE_OPERATOR">
                        ${mergingCriteria.useSeqPlatformGroup}
                    </sec:ifNotGranted>

                </td>
            </tr>
        </table>
    </g:form>

%{-- second row: create new and search bar --}%

    <g:if test="${mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.IGNORE_FOR_MERGING}">
        ${g.message(code: "mergingCriteria.seqPlatformDefinition.ignoreSeqPlatform")}
    </g:if>
    <g:else>
        <g:set var="useDefaultGroups" value="${mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_OTP_DEFAULT}"/>
        <g:set var="selectedSeqPlatformGroups" value="${useDefaultGroups ? seqPlatformGroups : seqPlatformGroupsPerProjectAndSeqType}"/>

        <div class="row mb-3">
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <div class="col-6">
                    <h2 class="d-block mb-3">${g.message(code: "mergingCriteria.ConfigureMergingCriteriaTitle")}</h2>
                    <g:if test="${!useDefaultGroups}">
                        <h5>${g.message(code: "mergingCriteria.createNewGroup")}</h5>
                        <g:render template="/templates/bootstrap/seqPlatformGroupSelector/seqPlatformGroupSelector"
                                  model="${[mergingCriteria            : mergingCriteria,
                                            allSeqPlatformsWithoutGroup: allSeqPlatformsWithoutGroup,
                                            selectorState              : SelectorViewState.CREATE,
                                            selectedProjectToCopyForm  : selectedProjectToCopyForm,
                                            selectedSeqTypeToCopyFrom  : selectedSeqTypeToCopyFrom
                                  ]}"/>
                    </g:if>
                </div>
            </sec:ifAllGranted>

            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <div class="col-6">
                    <g:if test="${mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC}">
                        <h2 class="d-block mb-3">${g.message(code: "mergingCriteria.copyFromTemplateTitle")}</h2>
                        <h5>${g.message(code: "mergingCriteria.searchForTemplate")}</h5>
                        <g:form action="searchForSeqPlatformGroups" class="my-3">
                            <g:hiddenField name="seqType.id" value="${seqType.id}"/>
                            <div class="combined-search-bar">
                                <div class="input-group">
                                    <g:select id="selectedProjectToCopyFrom"
                                              value="${selectedProjectToCopyForm ? selectedProjectToCopyForm.id : null}"
                                              name="selectedProjectToCopyForm.id"
                                              class="use-select-2 form-control"
                                              autocomplete="off"
                                              from="${availableProjects}"
                                              optionKey="id"
                                              noSelection="${[null: "${g.message(code: 'mergingCriteria.search.defaultSelection')}"]}"/>
                                    <g:select id="selectedSeqTypeToCopyFrom"
                                              value="${selectedSeqTypeToCopyFrom ? selectedSeqTypeToCopyFrom.id : null}"
                                              name="selectedSeqTypeToCopyFrom.id"
                                              class="use-select-2 form-control"
                                              autocomplete="off"
                                              from="${availableSeqTypes}"
                                              optionKey="id"
                                              disabled="${!selectedProjectToCopyForm}"
                                              noSelection="${[null: "${g.message(code: 'mergingCriteria.search.seqTypeSelection')}"]}"/>
                                </div>
                            </div>
                        </g:form>
                    </g:if>
                </div>
            </sec:ifAllGranted>
        </div>

    %{-- third row: copy all button --}%

        <div class="row">
            <div class="col-6">
                <h5>${g.message(code: "mergingCriteria.currentConfigTitle")}</h5>
                <sec:ifAllGranted roles="ROLE_OPERATOR">
                    <g:if test="${mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC}">
                        <g:form action="emptyAllSeqPlatformGroups">
                            <g:hiddenField name="seqType.id" value="${seqType.id}"/>
                            <g:hiddenField name="selectedProjectToCopyForm.id" value="${selectedProjectToCopyForm?.id}"/>
                            <g:hiddenField name="selectedSeqTypeToCopyFrom.id" value="${selectedSeqTypeToCopyFrom?.id}"/>
                            <g:each in="${selectedSeqPlatformGroups}" status="i" var="it">
                                <g:hiddenField name="seqPlatformGroupList[${i}]" value="${it.id}"/>
                            </g:each>
                            <div class="row my-2 justify-content-start">
                                <div class="col-2">
                                    <button class="btn btn-outline-danger float-left" type="submit" ${(!selectedSeqPlatformGroups) ? "disabled" : ""}>
                                        <i class="bi bi-trash"></i> All
                                    </button>
                                </div>
                            </div>
                        </g:form>
                    </g:if>
                </sec:ifAllGranted>
            </div>

            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <div class="col-6">
                    <g:if test="${mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC}">
                        <h5>${g.message(code: "mergingCriteria.foundTemplatesTitle")}</h5>
                        <g:form action="copyAllSeqPlatformGroups">
                            <g:hiddenField name="seqType.id" value="${seqType.id}"/>
                            <g:hiddenField name="selectedProjectToCopyForm.id" value="${selectedProjectToCopyForm?.id}"/>
                            <g:hiddenField name="selectedSeqTypeToCopyFrom.id" value="${selectedSeqTypeToCopyFrom?.id}"/>
                            <g:each in="${seqPlatformGroups}" status="i" var="it">
                                <g:hiddenField name="seqPlatformGroupList[${i}]" value="${it.id}"/>
                            </g:each>
                            <g:hiddenField name="mergingCriteria.id" value="${mergingCriteria.id}"/>
                            <div class="row my-2 justify-content-end">
                                <div class="col-2">
                                    <button class="btn btn-outline-secondary float-right" type="submit" ${(dontAllowCopyingAll ||
                                            (!seqPlatformGroups || seqPlatformGroups.empty)) ? "disabled" : ""}>
                                        <i class="bi bi-arrow-left"></i> <i class="bi bi-clipboard"></i> All
                                    </button>
                                </div>
                            </div>
                        </g:form>
                    </g:if>
                </div>
            </sec:ifAllGranted>
        </div>

    %{-- fourth row: project specific and found SeqPlatformGroup lists --}%

        <div class="row">
            <div class="col-6">
        <g:if test="${selectedSeqPlatformGroups != null && !selectedSeqPlatformGroups.empty}">
            <g:each in="${selectedSeqPlatformGroups}" var="seqPlatformGroup">
                <g:render template="/templates/bootstrap/seqPlatformGroupSelector/seqPlatformGroupSelector"
                          model="${[mergingCriteria            : mergingCriteria,
                                    seqPlatformGroup           : seqPlatformGroup,
                                    allSeqPlatformsWithoutGroup: allSeqPlatformsWithoutGroup,
                                    selectorState              : useDefaultGroups ? SelectorViewState.SHOW : SelectorViewState.EDIT,
                                    selectedProjectToCopyForm  : selectedProjectToCopyForm,
                                    selectedSeqTypeToCopyFrom  : selectedSeqTypeToCopyFrom,
                          ]}"/>
            </g:each>
        </g:if>
        <g:else>
            <div class="alert alert-info text-center" role="alert">
                ${g.message(code: "mergingCriteria.noGroupConfigured", args: [selectedProject.name, seqType])}
            </div>
        </g:else>
        </div>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <div class="col-6">
            <g:if test="${mergingCriteria.useSeqPlatformGroup == MergingCriteria.SpecificSeqPlatformGroups.USE_PROJECT_SEQ_TYPE_SPECIFIC}">
                <g:if test="${seqPlatformGroups && !seqPlatformGroups.empty}">
                    <g:each in="${seqPlatformGroups}" var="seqPlatformGroup">
                        <g:render template="/templates/bootstrap/seqPlatformGroupSelector/seqPlatformGroupSelector"
                                  model="${[mergingCriteria             : mergingCriteria,
                                            seqPlatformGroup            : seqPlatformGroup,
                                            selectorState               : SelectorViewState.COPY,
                                            seqPlatformGroupAlreadyInUse: allUsedSpecificSeqPlatforms.intersect(seqPlatformGroup.seqPlatforms),
                                            selectedProjectToCopyForm   : selectedProjectToCopyForm,
                                            selectedSeqTypeToCopyFrom   : selectedSeqTypeToCopyFrom
                                  ]}"/>
                    </g:each>
                </g:if>
                <g:else>
                    <div class="alert alert-info text-center" role="alert">
                        ${g.message(code: "mergingCriteria.noGroupFound")}
                    </div>
                </g:else>
                </div>
            </g:if>
            </div>
        </sec:ifAllGranted>
    </g:else>
</div>
</body>
</html>

