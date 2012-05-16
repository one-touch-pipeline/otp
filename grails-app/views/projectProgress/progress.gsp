<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>New Runs</title>
</head>
<body>
  <div class="body">
    <h1>Runs from: ${projects} since: <g:formatDate format="yyyy-MM-dd" date="${startDate}"/></h1>

    <g:form>
        <td><g:datePicker name="startDate" value="${startDate}" precision="day" years="[2010, 2011,2012]"/>
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
            <th>center</th>
            <th>name</th>
            <th>samples</th>
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
</html>