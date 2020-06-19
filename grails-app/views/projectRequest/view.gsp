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
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <g:render template="tabMenu"/>

    <h1>${g.message(code: "projectRequest.view.title", args: [projectRequest.name])}</h1>
    <g:form action="update">
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
            <td>${projectRequest.keywords?.join(", ")}</td>
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
            <td>${g.message(code: "projectRequest.pi")}</td>
            <td>${projectRequest.pi}</td>
        </tr>
        <tr>
            <td>${g.message(code: "projectRequest.leadBioinformatician")}</td>
            <td>${projectRequest.leadBioinformaticians?.join(", ")}</td>
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
            <td></td>
            <td><g:checkBox name="confirmConsent" id="confirmConsent"/><label for="confirmConsent">${g.message(code: "projectRequest.view.confirmConsent")}</label></td>
        </tr>
        <tr>
            <td></td>
            <td><g:checkBox name="confirmRecordOfProcessingActivities" id="confirmRecordOfProcessingActivities"/><label for="confirmRecordOfProcessingActivities">${g.message(code: "projectRequest.view.confirmRecordOfProcessingActivities")}</label></td>
        </tr>
        <tr>
            <td></td>
            <td>
                <g:submitButton name="approve" value="${g.message(code: "projectRequest.view.approve")}" />
                <g:submitButton name="deny" value="${g.message(code: "projectRequest.view.deny")}" />
                <g:link class="btn" action="index" params="[id: projectRequest.id]">${g.message(code: "projectRequest.view.edit")}</g:link>
            </td>
        </tr>
        <g:hiddenField name="request.id" value="${projectRequest.id}"/>
    </table>
    </g:form>
</div>
</body>
</html>
