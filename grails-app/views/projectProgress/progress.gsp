<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="projectProgress.progress.title" /></title>
    <asset:javascript src="pages/projectProgress/progress/progress.js"/>
</head>
<body>
  <div class="body">
    <form class="blue_label" id="projectsGroupbox">
        <span class="blue_label">
            <g:message code="search.from.date"/>:<input type="text" class="datePicker" id="startDate" value="${startDate}">
        </span>
        <br>
        <span class="blue_label">
            <g:message code="search.to.date"/>:<input type="text" class="datePicker" id="endDate" value="${endDate}">
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
  <div class="otpDataTables" >
        <otp:dataTable codes="${[
                    'projectProgress.progress.runs',
                    'projectProgress.progress.center',
                    'projectProgress.progress.samples', ]}"
                    id="progressId"/>
</div>
</div>

<asset:script type="text/javascript">
        $(function() {
            $.otp.projectProgressTable.registerProjectProgressId();
    });
</asset:script>
</body>
</html>
