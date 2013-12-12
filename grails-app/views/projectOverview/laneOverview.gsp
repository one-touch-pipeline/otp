<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title>OTP - project overview</title>
<r:require module="core" />
<r:require module="jqueryDatatables" />
</head>
<body>
    <div class="body">
        <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="home.projectfilter"/> :</span>
            <g:select class="criteria" id="project" name='project'
                from='${projects}' value='${project}' onChange='submit();'></g:select>
        </form>
        <div id="laneOverviewTable">
            <table id="laneOverviewId" border="1" >
                <thead>
                    <tr>
                        <th><g:message code="projectOverview.index.PID"/></th>
                        <th><g:message code="projectOverview.index.sampleType"/></th>
                        <th><g:message code="projectOverview.index.sampleID"/></th>
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