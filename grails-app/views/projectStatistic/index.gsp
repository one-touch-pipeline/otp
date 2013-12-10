<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="projectStatistic.title" /></title>
<r:require module="core" />
<r:require module="graph" />
</head>

<body>
    <div class="body">
       <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="home.projectfilter"/> :</span>
            <g:select class="criteria" id="project_select" name='project_select'
                from='${projects}' value='${project}'></g:select>
        </form>
        <h3 class="statisticTitle">
            <g:message code="projectStatistic.pageTitle" />
        </h3>
        <div class="homeGraph">
            <div style="margin-top: 20px"></div>
            <div id="laneNumberByProject"></div>
        </div>
    </div>

    <r:script>
        $(function() {
            $.otp.projectOverviewStatistic.register();
            $.otp.graph.projectStatistic.init();
        });
    </r:script>
</body>
</html>