%{--
  - Copyright 2011-2026 The OTP authors
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

<html>
<head>
    <title><g:message code="referenceGenome.title"/></title>
    <asset:javascript src="pages/referenceGenome/datatable.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:stylesheet src="pages/referenceGenome/index.less"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <g:render template="/metaDataFields/tabMenu"/>
    <div class="otpDataTables">
    <br>
    <h3><g:message code="referenceGenome.title"/></h3>
        <table id="referenceGenome-datatable" class="table table-sm table-striped table-hover table-bordered" style="width:100%">
            <thead>
            <tr class="flex-row">
                <th>
                    <div class="d-sm-flex">
                        ${g.message(code: "referenceGenome.name")}
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.name.tooltip")}"></i>
                        </div>
                    </div>
                <th>
                    <div class="d-sm-flex">
                        ${g.message(code: "referenceGenome.fileNamePrefix")}
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.fileNamePrefix.tooltip")}"></i>
                        </div>
                    </div>
                </th>
                <th>
                    <div class="d-sm-flex">
                        <g:message code="referenceGenome.species"/>
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.species.tooltip")}"></i>
                        </div>
                    </div>
                </th>
                <th>
                    <div class="d-sm-flex">
                        <g:message code="referenceGenome.length"/>
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.length.tooltip")}"></i>
                        </div>
                    </div>
                </th>
                <th>
                    <div class="d-sm-flex">
                    <g:message code="referenceGenome.lengthWithoutN"/>
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.lengthWithoutN.tooltip")}"></i>
                        </div>
                    </div>
                </th>
                <th>
                    <div class="d-sm-flex">
                    <g:message code="referenceGenome.folder"/>
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.folder.tooltip")}"></i>
                        </div>
                    </div>
                </th>
                <th>
                    <div class="d-sm-flex">
                    <g:message code="referenceGenome.chromosomePrefix"/>
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.chromosomePrefix.tooltip")}"></i>
                        </div>
                    </div>
                </th>
                <th>
                    <div class="d-sm-flex">
                    <g:message code="referenceGenome.chromosomeSuffix"/>
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.chromosomeSuffix.tooltip")}"></i>
                        </div>
                    </div>
                </th>
                <th>
                    <div class="d-sm-flex">
                        <g:message code="referenceGenome.fingerPrintingFileName"/>
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.fingerPrintingFileName.tooltip")}"></i>
                        </div>
                    </div>
                </th>
                <th>
                    <div class="d-sm-flex">
                        <g:message code="referenceGenome.dateCreated"/>
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.dateCreated.tooltip")}"></i>
                        </div>
                    </div>
                </th>
                <th>
                    <div class="d-sm-flex">
                        <g:message code="referenceGenome.legacy"/>
                        <div class="px-1">
                            <i class="helper-icon bi bi-question-circle-fill" title="${g.message(code: "referenceGenome.legacy.tooltip")}"></i>
                        </div>
                    </div>
                </th>
            </tr>
            </thead>
            <tbody>
            <g:each status="i" var="referenceGenome" in="${referenceGenomes}">
                <tr class="${referenceGenome.legacy ? 'text-muted' : ''}">
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'referenceGenome', action: 'updateName', id: referenceGenome.id)}"
                                value="${referenceGenome.name}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'referenceGenome', action: 'updateFileNamePrefix', id: referenceGenome.id)}"
                                value="${referenceGenome.fileNamePrefix}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDownMulti"
                                link="${g.createLink(controller: 'referenceGenome', action: 'updateSpecies', id: referenceGenome.id)}"
                                values="${allSpecies}"
                                value="${referenceGenome.species}"
                                optionKey="id"
                        />
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDownMulti"
                                link="${g.createLink(controller: 'referenceGenome', action: 'updateSpeciesWithStrain', id: referenceGenome.id)}"
                                values="${allSpeciesWithStrain}"
                                value="${referenceGenome.speciesWithStrain}"
                                optionKey="id"
                        />
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'referenceGenome', action: 'updateLength', id: referenceGenome.id)}"
                                value="${referenceGenome.length}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'referenceGenome', action: 'updateLengthWithoutN', id: referenceGenome.id)}"
                                value="${referenceGenome.lengthWithoutN}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'referenceGenome', action: 'updatePath', id: referenceGenome.id)}"
                                value="${referenceGenome.path}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'referenceGenome', action: 'updateChromosomePrefix', id: referenceGenome.id)}"
                                value="${referenceGenome.chromosomePrefix}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'referenceGenome', action: 'updateChromosomeSuffix', id: referenceGenome.id)}"
                                value="${referenceGenome.chromosomeSuffix}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'referenceGenome', action: 'updateFingerPrintingFileName', id: referenceGenome.id)}"
                                value="${referenceGenome.fingerPrintingFileName}"/>
                    </td>
                    <td>${referenceGenome.dateCreated}</td>
                    <td>
                        <g:render template="/templates/slider" model="[
                                targetAction: 'changeReferenceGenomeLegacyState',
                                objectName  : 'referenceGenome',
                                object      : referenceGenome,
                                i           : i,
                        ]"/>
                        <span hidden>${referenceGenome.legacy}</span>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
