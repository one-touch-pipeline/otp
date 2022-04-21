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

<%@ page import="de.dkfz.tbi.otp.project.projectRequest.StoragePeriod" %>
<%@ page import="de.dkfz.tbi.otp.project.projectRequest.ProjectRequestStateProvider" %>
<%@ page import="de.dkfz.tbi.otp.project.projectRequest.Approval" %>
<%@ page import="de.dkfz.tbi.util.TimeFormats" %>


<html>
<head>
    <title>${g.message(code: "projectRequest.view.title", args: [projectRequest.name])}</title>
    <asset:javascript src="taglib/NoSwitchedUser.js"/>
    <asset:stylesheet src="pages/projectRequest/index.less"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <g:render template="templates/tabs"/>
    <h3 class="mb-3">${g.message(code: "projectRequest.view.title", args: [projectRequest.name])}
        <span class="h4">(${g.message(code: stateDisplayName)})</span>
    </h3>

    <g:form method="POST" useToken="true">
        <g:hiddenField name="projectRequest.id" value="${projectRequest.id}"/>
        <g:if test="${projectRequest.state.beanName == "created"}">
            <otp:annotation type="info">
                <g:message code="projectRequest.view.completed"/>:
                <strong>
                    ${g.message(code: stateDisplayName)}
                </strong>
            </otp:annotation>
        </g:if>

        <div class="row">
            <div class="col-sm">
                <table class="table table-sm table-striped table-hover">
                    <tbody>
                    <tr>
                        <td>${g.message(code: "projectRequest.requester")}</td>
                        <td>${projectRequest.requester}</td>
                    </tr>
                    <tr>
                        <td>${g.message(code: "project.projectType")}</td>
                        <td>${projectRequest.projectType}</td>
                    </tr>
                    <tr>
                        <td>${g.message(code: "project.name")}</td>
                        <td>${projectRequest.name}</td>
                    </tr>
                    <tr>
                        <td>${g.message(code: "project.description")}</td>
                        <td class="text-break" style="white-space: pre-line">${projectRequest.description}</td>
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
                        <td>${projectRequest.speciesWithStrains?.join(", ")}</td>
                    </tr>
                    <g:if test="${projectRequest.customSpeciesWithStrains}">
                        <tr>
                            <td>${g.message(code: "project.customSpeciesWithStrain")}</td>
                            <td>${projectRequest.customSpeciesWithStrains}</td>
                        </tr>
                    </g:if>

                    <tr>
                        <td>${g.message(code: "projectRequest.sequencingCenter")}</td>
                        <td>${projectRequest.sequencingCenters?.join(", ")}</td>
                    </tr>
                    <tr>
                        <td>${g.message(code: "projectRequest.approxNoOfSamples")}</td>
                        <td>${projectRequest.approxNoOfSamples}</td>
                    </tr>
                    <tr>
                        <td>${g.message(code: "projectRequest.seqTypes")}</td>
                        <td>${projectRequest.seqTypes?.join(", ")}</td>
                    </tr>
                    <g:each in="${abstractFields}" var="abstractField" status="index">
                        <tr>
                            <td><g:message code="${abstractField.name}"/></td>
                            <td>${abstractValues.get(Long.toString(abstractField.id))}</td>
                        </tr>
                    </g:each>
                    <tr>
                        <td>${g.message(code: "projectRequest.requesterComment")}</td>
                        <td>
                            <div style="white-space: pre-line" id="comments">${projectRequest.requesterComment}</div>
                        </td>
                    </tr>

                    <g:set var="enableAdditionalCommentField" value="${false}"/>
                    <g:if test="${["check", "approval", "approved"].contains(projectRequest.state.beanName)}">
                        <g:set var="enableAdditionalCommentField" value="${currentUserIsProjectAuthority}"/>
                        <sec:ifAnyGranted roles="ROLE_OPERATOR">
                            <g:set var="enableAdditionalCommentField" value="${true}"/>
                        </sec:ifAnyGranted>
                    </g:if>

                    <g:if test="${enableAdditionalCommentField || projectRequest.comments}">
                        <tr>
                            <td>${g.message(code: "projectRequest.additionalComments")}</td>
                            <td>
                            <!-- Table containing the existing comments -->
                                <g:if test="${projectRequest.comments}">
                                    <table class="table table-sm table-striped table-hover">
                                        <thead>
                                        <tr>
                                            <td>${g.message(code: "comment.author")}</td>
                                            <td>${g.message(code: "comment.comment")}</td>
                                            <td>${g.message(code: "comment.lastModified")}</td>
                                        </tr>

                                        </thead>
                                        <tbody>
                                        <g:each in="${projectRequest.comments}" var="comment">
                                            <tr>
                                                <td>${comment.author}</td>
                                                <td class="wrap-line-breaks">${comment.comment}</td>
                                                <td>${TimeFormats.DATE.getFormattedDate(comment.modificationDate)}</td>
                                            </tr>
                                        </g:each>
                                    </table>
                                </g:if>
                                <g:if test="${enableAdditionalCommentField}">
                                    <textarea class="form-control"
                                              name="additionalComment" id="additionalComment"></textarea>
                                </g:if>
                            </td>
                        </tr>
                    </g:if>

                    <tr>
                        <td>${g.message(code: "projectRequest.users")}</td>
                        <td>
                            <g:render template="/projectRequest/templates/projectRequestUserTable" model="[users: projectRequest.users]"/>
                        </td>
                    </tr>
                    </tbody>
                </table>

                <g:if test="${buttonActions*.action.contains("approve")}">
                    <div class="alert alert-warning">
                        <label>
                            <g:checkBox name="confirmConsent" id="confirmConsent"/>
                            ${g.message(code: "projectRequest.view.confirmConsent")}
                        </label>

                        <br>
                        <label>
                            <g:checkBox name="confirmRecordOfProcessingActivities" id="confirmRecordOfProcessingActivities"/>
                            ${g.message(code: "projectRequest.view.confirmRecordOfProcessingActivities")}
                        </label>
                    </div>
                </g:if>

            <!-- form actions -->
                <g:render template="templates/submitArea" model="[buttonActions: buttonActions]"/>
            </div>


            <div class="col-sm-3">
                <g:if test="${projectRequest.state.beanName == "approval"}">
                    <h4>Approvals</h4>
                    <g:each var="userThatNeedsToApprove" in="${projectRequest?.state?.usersThatNeedToApprove}">
                        ${userThatNeedsToApprove.username} (${userThatNeedsToApprove.realName}): Waiting for Approval<br>
                    </g:each>
                    <g:each var="userThatAlreadyApproved" in="${projectRequest?.state?.usersThatAlreadyApproved}">
                        ${userThatAlreadyApproved.username} (${userThatAlreadyApproved.realName}): Approved<br>
                    </g:each>
                </g:if>
            </div>
        </div>

    </g:form>
</div>
</body>
</html>
