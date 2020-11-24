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
    <title><g:message code="otp.menu.alignmentQuality"/></title>
    <asset:javascript src="pages/alignmentQualityOverview/index/dataTable.js"/>
    <asset:stylesheet src="modules/modal.css"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <div class="project-selection-header-container">
        <div class="grid-element">
            <g:render template="/templates/projectSelection"/>
        </div>

        <div class="grid-element">
            <g:if test="${seqType}">
                <div class="rounded-page-header-box">
                    <span><g:message code="alignment.quality.seqType"/>:</span>

                    <form style="display: inline">
                        <g:hiddenField name="project" value="${selectedProject.name}"/>
                        <g:select id="seqType" name='seqType' class="use-select-2" style="width: 40ch;"
                                  data-columns="${columns}"
                                  from='${seqTypes}' value='${seqType.id}' optionKey='id' optionValue='displayNameWithLibraryLayout' onChange='submit();'/>
                    </form>
                </div>
            </g:if>
        </div>
    </div>

    <h1><g:message code="otp.menu.alignmentQuality"/></h1>

    <div id="sample" data-sample="${sample?.id}">
        <g:if test="${sample}">
            <otp:annotation type="info">
                ${g.message(code: "alignment.quality.selectedSample", args: [sample])}
                <g:link action="index" params="[seqType: seqType.id]">${g.message(code: "alignment.quality.showAll")}</g:link>
            </otp:annotation>
        </g:if>
    </div>

    <div class="otpDataTables alignmentQualityOverviewTable">
        <table id="overviewTableProcessedMergedBMF">
            <thead>
                <tr>
                    <g:each in="${header}" var="it" status="i">
                        <g:if test="${i == 2}">
                            <th><g:message code="${it}"/></th>
                        </g:if>
                        <g:else>
                            <th class="export_column"><g:message code="${it}"/></th>
                        </g:else>
                    </g:each>
                </tr>
            </thead>
            <tbody></tbody>
        </table>
    </div>
</div>
<otp:otpModal modalId="wait">
    <div class="modal-wait-message"><g:message code="alignment.quality.modal.message"/></div>
</otp:otpModal>
</body>
</html>
