<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="projectProgress.progress.title"/></title>
</head>
<body>
  <div class="body_grow">
    <g:form>
        <td><g:datePicker name="startDate" value="${startDate}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
        <g:select
            name="projects"
            value="${projects}"
            from="${de.dkfz.tbi.otp.ngsdata.Project.list()}"
            optionKey="name"
            multiple="true"
        />
        <g:submitButton name="progress" value=" Display " action="progress" />
       </tr>
       </table>
    </g:form>
    <table>
        <tr>
            <th></th>
            <th><g:message code="projectProgress.progress.center"/></th>
            <th><g:message code="projectProgress.progress.name"/></th>
            <th><g:message code="projectProgress.progress.samples"/></th>
        </tr>
    <g:each var="row" in="${data}">
        <tr>
            <td><b>${row.get(1)}</b></td>
            <td>${row.get(2)}</td>
            <td><g:link controller="run" action="show" id="${row.get(0)}">${row.get(3)}</g:link></td>
            <td>
                <g:each var="sample" in="${row.get(4)}">${sample}, </g:each>
            </td>
        </tr>
    </g:each>
    </table>
  </div>
</body>
<r:script>
    $(function() {
        $.otp.growBodyInit(240);
    });
</r:script>
</html>
