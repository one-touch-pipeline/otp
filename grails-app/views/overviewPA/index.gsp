<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Insert title here</title>
</head>
<body>
  <div class="body">
    <h1>Astrocytome overview</h1>
    <table>
        <thead>
            <td class="microHeader">Individual</td>
            <g:each var="scan" in="${scans}"> 
                <td class="microHeader">${scan}</td>
            </g:each>
        </thead>
        <g:each var="individual" in="${table}">
            <tr>
                <g:each var="field" in="${individual}">
                    <td class=${field}>${field}</td>
                </g:each>
            </tr>
        </g:each>
    </table>
  </div>
</body>
</html>