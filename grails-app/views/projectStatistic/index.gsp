<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="projectStatistic.title"/></title>
    <r:require module="core"/>
    <r:require module="graph"/>
</head>

<body>
    <div class="body">
        <div class="overviewMenu">
            <div class="projectLabel">project:</div>
            <div class="projectSelect"><g:select id="project_select" name='project_select' from='${projects}'></g:select></div>
        </div>
        <h2><g:message code="projectStatistic.pageTitle"/></h2>
        <div class="homeGraph">
            <div style="margin-top: 40px"></div>
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