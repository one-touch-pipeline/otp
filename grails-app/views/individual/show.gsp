<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Insert title here</title>
</head>
<body>
  <div class="body">

    <table>
       <tr> 
            <td class="myKey">PID</td>
            <td class="myValue">${ind.pid}</td>
       </tr>
       <tr>
            <td class="myKey">Mock PID</td>
            <td class="myValue">${ind.mockPid}</td>
       </tr>
       <tr>
            <td class="myKey">Mock Full Name</td>
            <td class="myValue">${ind.mockFullName}</td>
       </tr>
       <tr>
            <td class="myKey">Type</td>
            <td class="myValue">${ind.type}</td>
       </tr>
       <tr>
            <td class="myKey">Project</td>
            <td class="myValue">${ind.project}</td>
       </tr> 
    </table>


    <g:each var="type" in="${seqTypes}">
        <h1>${type}</h1>






    </g:each>


  </div>
</body>
</html>