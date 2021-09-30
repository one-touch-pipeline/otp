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

<html>
<head>
    <title><g:message code="otp.menu.alignmentQuality"/></title>
    <asset:javascript src="pages/alignmentQualityOverview/index/alignmentQualityOverview.js"/>
    <asset:stylesheet src="pages/alignmentQualityOverview/index.css"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <g:render template="/templates/messages"/>

    <g:render template="/templates/bootstrap/projectSelection"/>

    <g:if test="${seqType}">
        <form>
            <div class="input-group mb-3" style="max-width: 500px;">
                <div class="input-group-prepend">
                    <label class="input-group-text" for="seqType"><g:message code="alignment.quality.seqType"/></label>
                </div>

                <g:hiddenField name="project" value="${selectedProject.name}"/>
                <g:select id="seqType" name='seqType' class="custom-select use-select-2"
                          data-columns="${columns}"
                          from='${seqTypes}' value='${seqType.id}' optionKey='id' optionValue='displayNameWithLibraryLayout' onChange='submit()'/>
            </div>
        </form>
    </g:if>

    <h5><g:message code="otp.menu.alignmentQuality"/></h5>

    <div id="sample" data-sample="${sample?.id}">
        <g:if test="${sample}">
            <otp:annotation type="info">
                ${g.message(code: "alignment.quality.selectedSample", args: [sample])}
                <g:link action="index" params="[seqType: seqType.id]">${g.message(code: "alignment.quality.showAll")}</g:link>
            </otp:annotation>
        </g:if>
    </div>

    <div class="alert alert-info" role="alert" style="width: 100%">
        ${g.message(code: "alignment.quality.qcTrafficLightStatus.warning", args: [supportEmail])}
    </div>


    <table id="overviewTableProcessedMergedBMF" class="table table-sm table-striped">
        <thead>
            <tr>
                <g:each in="${header}" var="it" status="i">
                    <th><g:message code="${it}"/></th>
                </g:each>
            </tr>
        </thead>
        <tbody></tbody>
    </table>

    <div id="alignmentQualityOverviewSpinner" class="text-center" style="display: none">
        <div class="spinner-border" role="status">
            <span class="sr-only">Loading...</span>
        </div>
    </div>
</div>

<otp:otpModal modalId="confirmModal" title="QC status change" type="dialog" closeText="Cancel" confirmText="Confirm" closable="false">
    <label for="modalInput" style="margin-bottom: 10px;">
        <g:message code="alignment.quality.modal.message"/>
    </label>
    <input id="modalInput" data-mode="" type="text" class="form-control" placeholder="Comment...">
</otp:otpModal>

</body>
</html>
