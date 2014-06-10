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
        <g:submitButton class="blue_label" name="progress" value=" Display " action="progress" />
    </form>

    <div id="laneOverviewTable">
        <table border="1">
        <thead>
            <tr>
                <th><g:message code="projectProgress.progress.runs"/></th>
                <th><g:message code="projectProgress.progress.center"/></th>
                <th><g:message code="projectProgress.progress.samples"/></th>
            </tr>
        </thead>
        <g:each var="row" in="${data}">
            <tr>
                <td><b><g:link controller="run" action="show" id="${row.get(0)}">${row.get(3)}</g:link></b></td>
                <td>${row.get(2)}</td>
                <td>
                    <g:each var="sampleIdentifier" in="${row.get(4)}">
                    <g:link controller="individual" action="show" id="${sampleIdentifier.sample.individual.id}"> ${sampleIdentifier.name},</g:link></g:each>
                </td>
            </tr>
        </g:each>
        </table>
     </div>
  </div>


<r:script>
    $(function() {
        $.otp.growBodyInit(240);
    });
</r:script>
  </body>
</html>
