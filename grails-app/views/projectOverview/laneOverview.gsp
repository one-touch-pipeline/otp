<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title>OTP - project overview</title>
    <asset:javascript src="pages/projectOverview/laneOverview/datatable.js"/>
</head>
<body>
    <div class="body">
        <div class= "searchCriteriaTableSequences">
            <table id="searchCriteriaTable2">
                <tr>
                    <td>
                        <span class="blue_label"><g:message code="home.projectfilter"/> :</span>
                    </td>
                    <td>
                        <form>
                            <g:select class="criteria" id="project" name='project'
                            from='${projects}' value='${project}' onChange='submit();'></g:select>
                        </form>
                    </td>
                    <td>
                        <span class="blue_label"><g:message code="projectOverview.filter.sampletype"/> :</span>
                    </td>
                    <td>
                        <div class="searchCriteriaTableSampleTypes">
                            <table id="searchCriteriaTable3">
                                <tr>
                                    <td class="attribute">
                                        <g:select class="criteria" name="criteria2"
                                        from='${sampleTypes}' noSelection="${["none": message(code:"otp.filter.sampleType")]}" ></g:select>
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </td>
                    <td>
                        <span class="blue_label"><g:message code="projectOverview.filter.seqtype"/> :</span>
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
                                    <input class="blue_labelForPlus" type="button" value="${g.message(code: "projectOverview.filter.seqtype.remove")}" style="display: none"/>
                                </td>
                                <td class="add">
                                    <input class="blue_labelForPlus" type="button" value="${g.message(code: "projectOverview.filter.seqtype.add")}" style="display: none"/>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
            <p id="withdrawn_description">
                Withdrawn data is colored gray.
                <div></div>
            </p>
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
                            <th><g:message code="projectOverview.index.registeredLanes"/></th>
                            <g:each var="workflow" in="${pipelines}">
                                <th>${raw(workflow.html)}<br><g:message code="projectOverview.index.lanesAndCoverage"/></th>
                            </g:each>
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
    <asset:script>
        $(function() {
            $.otp.projectOverviewTable.registerLaneOverviewId();
            $('.dataTables_scrollBody').height($('.body').height()-265);
        });
    </asset:script>
</body>
</html>
