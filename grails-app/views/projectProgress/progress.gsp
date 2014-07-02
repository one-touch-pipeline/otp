<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="projectProgress.progress.title" /></title>
</head>
<body>
  <div class="body">
    <form class="blue_label" id="projectsGroupbox">
        <span class="blue_label">
            <g:message code="search.from.date"/>:<g:datePicker name="startDate" value="${startDate}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
        </span>
        <br>
        <span class="blue_label">
            <g:message code="search.to.date"/>:<g:datePicker name="endDate" value="${endDate}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
        </span>
        <g:select class="projectSelectMultiple blue_label"
            name="projects"
            value="${projects}"
            from="${de.dkfz.tbi.otp.ngsdata.Project.list(sort: "name", order: "asc")}"
            optionKey="name"
            multiple="true"
        />

        <input id="display" type="button" class="blue_label" name="progress" value=" Display "/>
    </form>
  <div class="progressId laneOverviewTable" >
        <otp:dataTable codes="${[
                    'projectProgress.progress.runs',
                    'projectProgress.progress.center',
                    'projectProgress.progress.samples', ]}"
                    id="progressId"/>
</div>
</div>

<r:script>
        $(function() {
            $.otp.projectProgressTable.registerProjectProgressId();
    });
</r:script>
</body>
</html>
