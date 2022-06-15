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
<%@ page import="de.dkfz.tbi.otp.ngsdata.SeqTypeNames" contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title>${g.message(code: "sampleOverview.title")}</title>
    <asset:javascript src="common/DataTableFilter.js"/>
    <asset:javascript src="pages/sampleOverview/index/datatable.js"/>
</head>
<body>
    <div class="body">
        <div class="project-selection-header-container">
            <div class="grid-element">
                <g:render template="/templates/projectSelection"/>
            </div>

            <div class="grid-element rounded-page-header-box" style="display: table;">
                <span style="display:table-cell; vertical-align: middle; white-space: nowrap;"><g:message code="sampleOverview.filter.sampleType"/>:</span>
                <table id="searchCriteriaTableSampleType" style="display:table-cell;">
                    <tr>
                        <td class="value">
                            <g:select id="" name="sampleTypeSelection" class="use-select-2" style="min-width: 18ch;"
                                      from='${sampleTypes}' noSelection="${["none": message(code: "otp.filter.sampleType")]}"
                                      autocomplete="off"/>
                        </td>
                    </tr>
                </table>
                <div style="width: 20px"></div>
                <span style="display:table-cell; vertical-align: middle; white-space: nowrap;"><g:message code="sampleOverview.filter.seqType"/>:</span>
                <table id="searchCriteriaTableSeqType" style="display:table-cell;">
                    <tr class="dtf_row">
                        %{-- css class: little hack: the seqType select is actually the "value" of the filter,
                             but should behave like "attribute" (that is: add a new row for the next filter).
                             since "attribute"-handling also triggers the value-updates, all is good. --}%
                        <td class="attribute">
                            <span class="dtf_value_span">
                                <g:select id="" name="seqTypeSelection" class="dtf_criterium use-select-2" style="min-width: 24ch;"
                                          from="${seqTypes}" optionKey="id"
                                          noSelection="${["none": message(code: "otp.filter.seqType")]}"
                                          autocomplete="off"/>
                            </span>
                        </td>
                        <td class="remove" style="display: none">
                            <input type="button" value="${g.message(code: "sampleOverview.filter.seqType.remove")}"/>
                        </td>
                        <td class="add" style="display: none">
                            <input type="button" value="${g.message(code: "sampleOverview.filter.seqType.add")}"/>
                        </td>
                    </tr>
                </table>
            </div>
        </div>

        <otp:annotation type="info" id="withdrawn_description">
            ${g.message(code: "sampleOverview.information.withdrawn")}
        </otp:annotation>

        <div class="otpDataTables">
            <table id="laneOverviewId" data-ignore-filter-columns="2" data-workflow-size="${pipelines.size()}" data-seq-type-size="${seqTypes.size()}">
                <thead>
                    <tr>
                        <th></th>
                        <th></th>
                        <g:each var="seqType" in="${seqTypes}">
                            <th colspan="${pipelines.size() + 1}">${seqType}</th>
                        </g:each>
                    </tr>
                    <tr>
                        <th><g:message code="sampleOverview.PID"/></th>
                        <th><g:message code="sampleOverview.sampleType"/></th>
                        <g:each var="seqType" in="${seqTypes}">
                            <th title="<g:message code="sampleOverview.registeredLanes.tooltip"/>"><span hidden>${seqType.displayNameWithLibraryLayout}: </span><g:message code="sampleOverview.registeredLanes"/></th>
                            <g:if test="${seqType.name == SeqTypeNames.RNA.seqTypeName}">
                                <g:each var="workflow" in="${pipelines}">
                                    <th class="custom-sort"><span hidden>${seqType.displayNameWithLibraryLayout}: </span>${workflow.displayName}<br><g:message code="sampleOverview.alignedAndAllLanes"/></th>
                                </g:each>
                            </g:if><g:else>
                                <g:each var="workflow" in="${pipelines}">
                                    <th class="custom-sort"><span hidden>${seqType.displayNameWithLibraryLayout}: </span>${workflow.displayName}<g:if test="${workflow.displayName == 'external'}"><g:message code="sampleOverview.existsAndCoverage"/></g:if><g:else><g:message code="sampleOverview.lanesAndCoverage"/></g:else></th>
                                </g:each>
                            </g:else>
                        </g:each>
                    </tr>
                </thead>
                <tbody>
                </tbody>
                <tfoot>
                </tfoot>
            </table>
        </div>
    </div>
</body>
</html>
