<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title>OTP - project overview</title>
</head>
<body>
    <div class="body">
        <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="home.projectfilter"/> :</span>
            <g:select class="criteria" id="project" name='project'
                from='${projects}' value='${project}' onChange='submit();'></g:select>
        </form>
        <div class= "searchCriteriaTableSequences">
            <table id="searchCriteriaTable2">
                <tr>
                    <td>
                        <span class="blue_label"><g:message code="extended.search"/> :</span>
                    </td>
                    <td>
                        <table id="searchCriteriaTable">
                            <tr>
                                <td class="attribute">
                                    <g:select class="criteria" name="criteria" from="${seqTypes}" optionKey="naturalId" noSelection="${["none": message(code:"otp.filter.none")]}"/>
                                </td>
                                <td class="value">
                                </td>
                                <td class="remove">
                                    <input class="blue_labelForPlus" type="button" value="${g.message(code: "otp.filter.remove")}" style="display: none"/>
                                </td>
                                <td class="add">
                                    <input class="blue_labelForPlus" type="button" value="${g.message(code: "otp.filter.add")}" style="display: none"/>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </div>

        <div id="laneOverviewTable">
            <table id="laneOverviewId" border="1" data-ignore-filter-columns="${hideSampleIdentifier ? 2 : 3}">
                <thead>
                    <tr>
                        <th><g:message code="projectOverview.index.PID"/></th>
                        <th><g:message code="projectOverview.index.sampleType"/></th>
                        <g:if test="${!hideSampleIdentifier}">
                            <th><g:message code="projectOverview.index.sampleID"/></th>
                        </g:if>
                        <g:each var="seqType" in="${seqTypes}">
                            <th>${seqType}<br><g:message code="projectOverview.index.laneCount"/></th>
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
    <r:script>
        $(function() {
            $.otp.projectOverviewTable.registerLaneOverviewId();
        });
    </r:script>
</body>
</html>
