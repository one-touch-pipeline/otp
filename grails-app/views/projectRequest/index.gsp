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
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${g.message(code: "projectRequest.title")}</title>
    <asset:javascript src="common/UserAutoComplete.js"/>
    <asset:javascript src="common/MultiInputField.js"/>
    <asset:javascript src="pages/projectRequest/index.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <h1>${g.message(code: "projectRequest.header.waiting")}</h1>
    <g:if test="${awaitingRequests}">
        <table>
            <tr>
                <th>${g.message(code: "project.name")}</th>
                <th>${g.message(code: "projectRequest.dateCreated")}</th>
                <th>${g.message(code: "projectRequest.lastUpdated")}</th>
                <th>${g.message(code: "projectRequest.status")}</th>
                <th>${g.message(code: "projectRequest.requester")}</th>
                <th>${g.message(code: "projectRequest.viewApprove")}</th>
            </tr>
            <g:each var="req" in="${awaitingRequests}">
                <tr>
                    <td>${req.name}</td>
                    <td>${req.dateCreated.format("yyyy-MM-dd HH:mm")}</td>
                    <td>${req.lastUpdated.format("yyyy-MM-dd HH:mm")}</td>
                    <td>${req.status}</td>
                    <td>${req.requester}</td>
                    <td><g:link action="view" id="${req.id}">${g.message(code: "projectRequest.viewApprove")}</g:link></td>
                </tr>
            </g:each>
        </table>
    </g:if>
    <g:else>
        <ul>
            <li>${g.message(code: "projectRequest.none")}</li>
        </ul>
    </g:else>
    <br>

    <h1>${g.message(code: "projectRequest.header.createdApproved")}</h1>
    <g:if test="${createdAndApprovedRequests}">
        <table>
            <tr>
                <th>${g.message(code: "project.name")}</th>
                <th>${g.message(code: "projectRequest.dateCreated")}</th>
                <th>${g.message(code: "projectRequest.lastUpdated")}</th>
                <th>${g.message(code: "projectRequest.status")}</th>
                <th>${g.message(code: "projectRequest.requester")}</th>
                <th>${g.message(code: "projectRequest.pi")}</th>
            </tr>
            <g:each var="req" in="${createdAndApprovedRequests}">
                <tr>
                    <td>${req.name}</td>
                    <td>${req.dateCreated.format("yyyy-MM-dd HH:mm")}</td>
                    <td>${req.lastUpdated.format("yyyy-MM-dd HH:mm")}</td>
                    <td>${req.status}</td>
                    <td>${req.requester}</td>
                    <td>${req.pi}</td>
                </tr>
            </g:each>
        </table>
    </g:if>
    <g:else>
        <ul>
            <li>${g.message(code: "projectRequest.none")}</li>
        </ul>
    </g:else>
    <br>

    <h1>${g.message(code: "projectRequest.header.new")}</h1>
    <p>${g.message(code: "projectRequest.new.support", args: [contactDataSupportEmail])}</p>
    <g:form action="save">
    <table class="key-value-table">
        <tr>
            <td><label for="name">${g.message(code: "project.name")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.name.detail")}"></td>
            <td><input name="name" id="name" value="${cmd?.name}" required/></td>
        </tr>
        <tr>
            <td><label for="description">${g.message(code: "project.description")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.description.detail")}"></td>
            <td><textarea name="description" id="description" required>${cmd?.description}</textarea></td>
        </tr>
        <tr>
            <td><label for="keyword">${g.message(code: "project.keywords")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.keywords.detail")}"></td>
            <td class="multi-input-field">
                <g:each in="${cmd?.keywords ?: [""]}" var="keyword" status="i">
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
            <td><label for="organizationalUnit">${g.message(code: "project.organizationalUnit")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.organizationalUnit.detail")}"></td>
            <td><input name="organizationalUnit" id="organizationalUnit" value="${cmd?.organizationalUnit}" required/></td>
        </tr>
        <tr>
            <td><label for="costCenter">${g.message(code: "project.costCenter")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.costCenter.detail")}"></td>
            <td><input name="costCenter" id="costCenter" value="${cmd?.costCenter}"/></td>
        </tr>
        <tr>
            <td><label for="fundingBody">${g.message(code: "project.fundingBody")}</label></td>
            <td></td>
            <td><input name="fundingBody" id="fundingBody" value="${cmd?.fundingBody}"/></td>
        </tr>
        <tr>
            <td><label for="grantId">${g.message(code: "project.grantId")}</label></td>
            <td></td>
            <td><input name="grantId" id="grantId" value="${cmd?.grantId}"/></td>
        </tr>
        <tr>
            <td><label for="endDate">${g.message(code: "project.endDate")}</label></td>
            <td></td>
            <td><input name="endDate" id="endDate" value="${cmd?.endDate}" type="date"/></td>
        </tr>
        <tr>
            <td><label for="storagePeriod">${g.message(code: "project.storageUntil")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.storageUntil.detail")}"></td>
            <td>
                <g:select name="storagePeriod" id="storagePeriod" value="${cmd?.storagePeriod}" from="${storagePeriods}" optionKey="name" optionValue="description" noSelection="${["": ""]}" required="true" />
                <br>
                <input name="storageUntil" id="storageUntil" value="${cmd?.storageUntil}" type="date" style="display: none"/>
            </td>
        </tr>
        <tr>
            <td><label for="relatedProjects">${g.message(code: "project.relatedProjects")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.relatedProjects.detail")}"></td>
            <td><input name="relatedProjects" id="relatedProjects" value="${cmd?.relatedProjects}"/></td>
        </tr>
%{--
        <tr>
            <td><label for="tumorEntity">${g.message(code: "project.tumorEntity")}</label></td>
            <td></td>
            <td><g:select name="tumorEntity.id" id="tumorEntity" value="${cmd?.tumorEntity?.id}" from="${tumorEntities}" optionValue="name" optionKey="id" noSelection="${[null: ""]}" /></td>
        </tr>
--}%
        <tr>
            <td><label for="speciesWithStrain">${g.message(code: "project.speciesWithStrain")}</label></td>
            <td></td>
            <td><g:select name="speciesWithStrain.id" id="speciesWithStrain" value="${cmd?.speciesWithStrain?.id}" from="${species}" optionKey="id" noSelection="${[null: ""]}" /></td>
        </tr>
        <tr>
            <td><label for="projectType">${g.message(code: "project.projectType")}</label></td>
            <td></td>
            <td><g:select name="projectType" id="projectType" value="${cmd?.projectType}" from="${projectTypes}"/></td>
        </tr>
        <tr>
            <td><label for="sequencingCenter">${g.message(code: "projectRequest.sequencingCenter")}</label></td>
            <td></td>
            <td><input name="sequencingCenter" id="sequencingCenter" value="${cmd?.sequencingCenter}"/></td>
        </tr>
        <tr>
            <td><label for="approxNoOfSamples">${g.message(code: "projectRequest.approxNoOfSamples")}</label></td>
            <td></td>
            <td><input name="approxNoOfSamplesString" id="approxNoOfSamples" type="number" value="${cmd?.approxNoOfSamples}"/></td>
        </tr>
        <tr>
            <td><label for="seqType">${g.message(code: "projectRequest.seqTypes")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.seqTypes.detail")}"></td>
            <td class="multi-input-field">
                <g:each in="${cmd?.seqType ?: [null]}" var="seqType" status="i">
                    <div class="field">
                        <g:select name="seqType.id" id="seqType" from="${seqTypes}" value="${seqType?.id ?: ""}" noSelection="${["": "None"]}" optionKey="id" optionValue="displayNameWithLibraryLayout" required="true" />
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
            <td><textarea name="comments" id="comments">${cmd?.comments}</textarea></td>
        </tr>

        <tr class="user-auto-complete">
            <td><label for="pi">${g.message(code: "projectRequest.pi")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.pi.detail")}"></td>
            <td><input name="pi" id="pi" type="text" autocomplete="off" placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}" value="${cmd?.pi}" required></td>
        </tr>
        <tr>
            <td><label for="deputyPi">${g.message(code: "projectRequest.deputyPi")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.deputyPi.detail")}"></td>
            <td class="multi-input-field user-auto-complete">
                <g:each in="${cmd?.deputyPis ?: [""]}" var="deputyPi" status="i">
                    <div class="field">
                        <input name="deputyPis" id="deputyPi" type="text" autocomplete="off"
                               placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}" value="${deputyPi}">
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
            <td><label for="responsibleBioinformatician">${g.message(code: "projectRequest.responsibleBioinformatician")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.responsibleBioinformatician.detail")}"></td>
            <td class="multi-input-field user-auto-complete">
                <g:each in="${cmd?.responsibleBioinformaticians ?: [""]}" var="responsibleBioinformatician" status="i">
                    <div class="field">
                        <input name="responsibleBioinformaticians" id="responsibleBioinformatician" type="text" autocomplete="off"
                               placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}" value="${responsibleBioinformatician}">
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
            <td><label for="bioinformatician">${g.message(code: "projectRequest.bioinformatician")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.bioinformatician.detail")}"></td>
            <td class="multi-input-field user-auto-complete">
                <g:each in="${cmd?.bioinformaticians ?: [""]}" var="bioinformatician" status="i">
                    <div class="field">
                        <input name="bioinformaticians" id="bioinformatician" type="text" autocomplete="off"
                               placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}" value="${bioinformatician}">
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
            <td><label for="submitter">${g.message(code: "projectRequest.submitter")}</label></td>
            <td class="help" title="${g.message(code: "projectRequest.submitter.detail")}"></td>
            <td class="multi-input-field user-auto-complete">
                <g:each in="${cmd?.submitters ?: [""]}" var="submitter" status="i">
                    <div class="field">
                        <input name="submitters" id="submitter" type="text" autocomplete="off"
                               placeholder="${g.message(code: 'projectUser.addMember.ldapSearchValues')}" value="${submitter}">
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
            <td></td>
            <td></td>
            <td>${g.message(code: "projectRequest.explain.submitter")}<br></td>
        </tr>
        <tr>
            <td></td>
            <td></td>
            <td><g:submitButton name="${g.message(code: "projectRequest.submit")}"/></td>
        </tr>
    </table>
    </g:form>

</div>
</body>
</html>
