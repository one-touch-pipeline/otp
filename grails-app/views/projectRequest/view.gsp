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

<%@ page import="de.dkfz.tbi.otp.project.StoragePeriod" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${g.message(code: "projectRequest.view.title", args: [projectRequest.name])}</title>
    <asset:javascript src="taglib/NoSwitchedUser.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <g:render template="tabMenu"/>

    <h1>${g.message(code: "projectRequest.view.title", args: [projectRequest.name])}</h1>
    <div class="basic-flex-box">
        <g:form class="item basic-right-padding">
            <g:if test="${!projectRequest.status.editableStatus}">
                <otp:annotation type="info">
                    <g:message code="projectRequest.view.completed"/>:
                    <strong><span style="color: ${projectRequest.status.color}">${projectRequest.formattedStatus}</span></strong>
                </otp:annotation>
            </g:if>

            <table class="key-value-table key-input">
                <tr>
                    <td>${g.message(code: "projectRequest.requester")}</td>
                    <td>${projectRequest.requester}</td>
                </tr>

                <tr>
                    <td>${g.message(code: "project.name")}</td>
                    <td>${projectRequest.name}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "project.description")}</td>
                    <td><div class="project-multiline-wrapper">${projectRequest.description}</div></td>
                </tr>
                <tr>
                    <td>${g.message(code: "project.keywords")}</td>
                    <td>
                        <g:each var="keyword" in="${projectRequest.keywords}">
                            ${keyword}<br>
                        </g:each>
                    </td>
                </tr>
                <tr>
                    <td>${g.message(code: "project.organizationalUnit")}</td>
                    <td>${projectRequest.organizationalUnit}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "project.costCenter")}</td>
                    <td>${projectRequest.costCenter}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "project.fundingBody")}</td>
                    <td>${projectRequest.fundingBody}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "project.grantId")}</td>
                    <td>${projectRequest.grantId}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "project.endDate")}</td>
                    <td>${projectRequest.endDate}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "project.storageUntil")}</td>
                    <td>${projectRequest.storageUntil ?: StoragePeriod.INFINITELY.description}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "project.relatedProjects")}</td>
                    <td>${projectRequest.relatedProjects}</td>
                </tr>
                %{--
                <tr>
                    <td>${g.message(code: "project.tumorEntity")}</td>
                    <td>${projectRequest.tumorEntity}</td>
                </tr>
                --}%
                <tr>
                    <td>${g.message(code: "project.speciesWithStrain")}</td>
                    <td>${projectRequest.speciesWithStrain ?: projectRequest.customSpeciesWithStrain ? g.message(code: "project.speciesWithStrain.custom", args: [projectRequest.customSpeciesWithStrain]) : ""}</td>
                </tr>
                <tr>
                    <td>${g.message(code: "project.projectType")}</td>
                    <td>${projectRequest.projectType}</td>
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
                    <td><div class="project-multiline-wrapper">${projectRequest.comments}</div></td>
                </tr>
                <tr>
                    <td>${g.message(code: "projectRequest.users")}</td>
                    <td>
                        <g:each var="user" in="${projectRequest.users}">
                            <g:render template="projectRequestUser" model="[user: user]"/>
                        </g:each>
                    </td>
                </tr>

                <g:if test="${projectRequest.status.editableStatus}">
                    <g:if test="${eligibleToAccept}">
                        <tr>
                            <td></td>
                            <td>
                                <label>
                                    <g:checkBox name="confirmConsent" id="confirmConsent"/>
                                    ${g.message(code: "projectRequest.view.confirmConsent")}
                                </label>
                                <br>
                                <label>
                                    <g:checkBox name="confirmRecordOfProcessingActivities" id="confirmRecordOfProcessingActivities"/>
                                    ${g.message(code: "projectRequest.view.confirmRecordOfProcessingActivities")}
                                </label>
                            </td>
                        </tr>
                    </g:if>
                    <tr>
                        <td></td>
                        <td>
                            <otpsecurity:noSwitchedUser>
                                <div class="project-request-actions">
                                    <g:if test="${eligibleToAccept}">
                                        <g:actionSubmit action="approve" name="approve" value="${g.message(code: "projectRequest.view.approve")}" />
                                        <g:actionSubmit action="deny" name="deny" value="${g.message(code: "projectRequest.view.deny")}" />
                                    </g:if>
                                    <g:if test="${eligibleToEdit}">
                                        <g:actionSubmit action="edit" name="edit" value="${g.message(code: "projectRequest.view.edit")}" />
                                    </g:if>
                                    <g:if test="${eligibleToClose}">
                                        <g:actionSubmit action="close" name="close" value="${g.message(code: "projectRequest.view.close")}" />
                                    </g:if>
                                </div>
                            </otpsecurity:noSwitchedUser>
                        </td>
                    </tr>
                </g:if>
                <g:hiddenField name="request.id" value="${projectRequest.id}"/>
            </table>
        </g:form>
        <div class="item basic-right-padding">
            <g:render template="approverOverview" model="[projectRequestUsers: projectRequest.users.findAll { it.approver }]"/>
        </div>
    </div>
</div>
</body>
</html>
