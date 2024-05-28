%{--
  - Copyright 2011-2024 The OTP authors
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
    <title><g:message code="speciesWithStrain.title"/></title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:stylesheet src="pages/speciesWithStrain/styles.less"/>
</head>

<body>
<div class="body">
    <g:render template="/metaDataFields/tabMenu"/>

    <h1 class="my-3"><g:message code="speciesWithStrain.header"/></h1>

    <g:if test="${helperParams.helper}">
        <otp:annotation type="info">
            <strong><g:message code="speciesWithStrain.helper.prefix"/></strong> ${helperParams.helper}
        </otp:annotation>
    </g:if>

    <div class="card-group mb-3">
        <div class="card">
            <div class="card-header">
                <h2><g:message code="speciesWithStrain.header.createSpeciesWithStrain"/></h2>
            </div>

            <div class="card-body">
                <g:form controller="speciesWithStrain" action="createSpeciesWithStrain" params="${helperParams}">
                    <div class="mb-3 row">
                        <label for="species-select" class="col-sm-2 col-form-label">
                            <g:message code="speciesWithStrain.label.species"/>
                        </label>

                        <div class="col-sm-10">
                            <g:if test="${!allSpecies}">
                                <g:message code="speciesWithStrain.label.noSpeciesAvailable"/>
                            </g:if>
                            <g:else>
                                <g:select id="species-select" name="speciesId" class="use-select-2 form-control"
                                          from="${allSpecies}" optionKey="id"/>
                            </g:else>
                        </div>
                    </div>

                    <div class="mb-3 row">
                        <label for="strain-select" class="col-sm-2 col-form-label">
                            <g:message code="speciesWithStrain.label.strain"/>
                        </label>

                        <div class="col-sm-10">
                            <g:if test="${!strains}">
                                <g:message code="speciesWithStrain.label.noStrainsAvailable"/>
                            </g:if>
                            <g:else>
                                <g:select id="strain-select" name="strainId" class="use-select-2 form-control"
                                          from="${strains}" optionKey="id"/>
                            </g:else>
                        </div>
                    </div>
                    <g:submitButton class="btn btn-primary" value="Create" name="create-species-with-strain"/>
                </g:form>
            </div>
        </div>

        <div class="card">
            <div class="card-header">
                <h2><g:message code="speciesWithStrain.header.existingSpeciesWithStrains"/></h2>
            </div>

            <div class="card-body listing-content" id="species-with-strain-list">
                <g:if test="${!speciesWithStrainsBySpecies}">
                    <g:message code="speciesWithStrain.label.noSpeciesWithStrainAvailable"/>
                </g:if>
                <ul class="list-group">
                    <g:set var="i" value="${0}"/>
                    <g:each var="species" in="${speciesWithStrainsBySpecies.keySet().sort()}">
                        <li class="list-group-item"><b>${species}</b>
                            <ul class="list-group mt-2">
                                <g:each var="speciesWithStrain" in="${speciesWithStrainsBySpecies[species].sort()}">
                                    <li class="list-group-item">
                                        <div class="row">
                                            <div class="col-sm-4 align-content-center">${speciesWithStrain}</div>
                                            <div class="col-sm-6 align-content-center" title="${g.message(code: 'speciesWithStrain.tooltip.importAlias')}">
                                                ${g.message(code: 'speciesWithStrain.label.importAlias')}: ${speciesWithStrain.importAlias.join(", ")}
                                                <otp:editorSwitchNewValues
                                                    roles="ROLE_OPERATOR"
                                                    labels="${["Add Alias"]}"
                                                    textFields="${["importAlias"]}"
                                                    link="${g.createLink(
                                                            controller: 'speciesWithStrain',
                                                            action: 'createImportAlias',
                                                            id: speciesWithStrain.id
                                                    )}"/>
                                            </div>
                                            <div class="col-sm-2 align-content-center legacy-container" title="${g.message(code: 'speciesWithStrain.tooltip.legacy')}"> 
                                                <g:render template="/templates/slider" model="[
                                                    targetAction: 'changeLegacyState',
                                                    objectName  : 'species',
                                                    object      : speciesWithStrain,
                                                    i           : i++,
                                            ]"/>
                                            </div>
                                        </div>
                                    </li>
                                </g:each>
                            </ul>
                        </li>
                    </g:each>
                </ul>
            </div>
        </div>
    </div>

    <div class="card-group mb-3">
        <div class="card">
            <div class="card-header">
                <h2><g:message code="speciesWithStrain.header.createSpecies"/></h2>
            </div>

            <div class="card-body">
                <g:form controller="speciesWithStrain" action="createSpecies" params="${helperParams}">
                    <div class="mb-3 row">
                        <label for="common-name-input" class="col-sm-2 col-form-label">
                            <g:message code="speciesWithStrain.label.commonName"/>
                        </label>

                        <div class="col-sm-10">
                            <input type="text" id="common-name-input" class="form-control" list="commonNameList" name="speciesCommonName"
                                   value="${cachedCommonName}" autocomplete="off"/>
                            <datalist id="commonNameList">
                                <g:each var="speciesCommonName" in="${speciesCommonNames}">
                                    <option>${speciesCommonName}</option>
                                </g:each>
                            </datalist>
                        </div>
                    </div>

                    <div class="mb-3 row">
                        <label for="scientific-name-input" class="col-sm-2 col-form-label">
                            <g:message code="speciesWithStrain.label.scientificName"/>
                        </label>

                        <div class="col-sm-10">
                            <input type="text" class="form-control" id="scientific-name-input" name="scientificName" value="${cachedScientificName}"
                                   autocomplete="off"/>
                        </div>
                    </div>
                    <g:submitButton class="btn btn-primary" value="Create" name="create-scientific-name"/>
                </g:form>
            </div>
        </div>

        <div class="card">
            <div class="card-header">
                <h2><g:message code="speciesWithStrain.header.existingSpecies"/></h2>
            </div>

            <div class="card-body listing-content" id="species-list">
                <g:if test="${!speciesCommonNames}">
                    <g:message code="speciesWithStrain.label.noSpeciesAvailable"/>
                </g:if>
                <ul class="list-group">
                    <g:each var="speciesCommonName" in="${speciesCommonNames}">
                        <li class="list-group-item">
                            <b>${speciesCommonName.name}</b>
                            <ul class="list-group mt-2">
                                <g:each var="species" in="${speciesBySpeciesCommonName[speciesCommonName]}">
                                    <li class="list-group-item">${species.scientificName}</li>
                                </g:each>
                            </ul>
                        </li>
                    </g:each>
                </ul>
            </div>
        </div>
    </div>

    <div class="card-group mb-3">
        <div class="card">
            <div class="card-header">
                <h2><g:message code="speciesWithStrain.header.createStrain"/></h2>
            </div>

            <div class="card-body">
                <g:form controller="speciesWithStrain" action="createStrain" params="${helperParams}">
                    <div class="mb-3 row">
                        <label for="strain-input" class="col-sm-2 col-form-label">
                            <g:message code="speciesWithStrain.label.strainName"/>
                        </label>

                        <div class="col-sm-10">
                            <input id="strain-input" type="text" class="form-control" name="newStrainName" value="${cachedStrainName}" autocomplete="off"/>
                        </div>
                    </div>
                    <g:submitButton value="Create" class="btn btn-primary" name="create-strain"/></td>
                </g:form>
            </div>
        </div>

        <div class="card">
            <div class="card-header">
                <h2><g:message code="speciesWithStrain.header.existingStrains"/></h2>
            </div>

            <div class="card-body listing-content" id="strains-list">
                <g:if test="${!strains}">
                    <g:message code="speciesWithStrain.label.noStrainsAvailable"/>
                </g:if>
                <g:if test="${!speciesCommonNames}">
                    <g:message code="speciesWithStrain.label.noSpeciesAvailable"/>
                </g:if>
                <ul class="list-group">
                    <g:each var="strain" in="${strains}">
                        <li class="list-group-item">${strain}</li>
                    </g:each>
                </ul>
            </div>
        </div>
    </div>
</div>

</body>
</html>
