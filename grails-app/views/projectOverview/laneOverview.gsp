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
    <title>OTP - project overview</title>
    <asset:javascript src="pages/projectOverview/laneOverview/datatable.js"/>
</head>
<body>
    <div class="body">
    <g:if test="${projects}">
        <div class= "searchCriteriaTableSequences">
            <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]" />
            <table id="searchCriteriaTable2" style="display: inline-block">
                <tr>
                    <td>
                        <span class="blue_label"><g:message code="projectOverview.filter.sampleType"/> :</span>
                    </td>
                    <td>
                        <div class="searchCriteriaTableSampleTypes">
                            <table id="searchCriteriaTable3">
                                <tr>
                                    <td class="attribute">
                                        <g:select class="criteria" name="criteria2"
                                        from='${sampleTypes}' noSelection="${["none": message(code:"otp.filter.sampleType")]}" />
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </td>
                    <td>
                        <span class="blue_label"><g:message code="projectOverview.filter.seqType"/> :</span>
                    </td>
                    <td>
                        <table id="searchCriteriaTable">
                            <tr>
                                <td class="attribute">
                                    <g:select class="criteria" name="criteria" from="${seqTypes}" optionKey="id" noSelection="${["none": message(code:"otp.filter.seqType")]}"/>
                                </td>
                                <td class="value">
                                </td>
                                <td class="remove">
                                    <input class="blue_labelForPlus" type="button" value="${g.message(code: "projectOverview.filter.seqType.remove")}" style="display: none"/>
                                </td>
                                <td class="add">
                                    <input class="blue_labelForPlus" type="button" value="${g.message(code: "projectOverview.filter.seqType.add")}" style="display: none"/>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
            <div>
                <p id="withdrawn_description">
                    Withdrawn data is colored gray.
                </p>
            </div>
        </div>
        <div class="otpDataTables">
            <table id="laneOverviewId"  data-ignore-filter-columns="${hideSampleIdentifier ? 2 : 3}" data-workflow-size="${pipelines.size()}">
                <thead>
                    <tr>
                        <th></th>
                        <th></th>
                        <g:if test="${!hideSampleIdentifier}">
                            <th></th>
                        </g:if>
                        <g:each var="seqType" in="${seqTypes}">
                            <th colspan="${pipelines.size() + 1}">${seqType}</th>
                        </g:each>
                    </tr>
                    <tr>
                        <th><g:message code="projectOverview.index.PID"/></th>
                        <th><g:message code="projectOverview.index.sampleType"/></th>
                        <g:if test="${!hideSampleIdentifier}">
                            <th><g:message code="projectOverview.index.sampleID"/></th>
                        </g:if>
                        <g:each var="seqType" in="${seqTypes}">
                            <th title="<g:message code="projectOverview.mouseOver.lane"/>"><g:message code="projectOverview.index.registeredLanes"/></th>
                            <g:if test="${seqType.name == SeqTypeNames.RNA.seqTypeName}">
                                <g:each var="workflow" in="${pipelines}">
                                    <th>${workflow.displayName}<br><g:message code="projectOverview.index.alignedAndAllLanes"/></th>
                                </g:each>
                            </g:if><g:else>
                                <g:each var="workflow" in="${pipelines}">
                                    <th>${workflow.displayName}
                                        <g:if test="${workflow.displayName == 'external'}"><g:message code="projectOverview.index.existsAndCoverage"/></g:if>
                                        <g:else><g:message code="projectOverview.index.lanesAndCoverage"/></g:else>
                                    </th>
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
    <asset:script type="text/javascript">
        $(function() {
            $.otp.projectOverviewTable.registerLaneOverviewId();
            $('.dataTables_scrollBody').height($('.body').height()-265);
        });
    </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
