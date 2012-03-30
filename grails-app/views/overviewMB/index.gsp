<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Insert title here</title>
</head>
<body>
   <div class="body">
    <div class="myHeader">
    Centers Overview
    </div>
    <div class="myContent">
    <table>
    <g:each var="line" in="${centers}">
        <tr>
            <g:each var="token" in="${line}">
                <td>${token}</td>
            </g:each>
        </tr>
    </g:each>
    </table>
    </div>

    <div class="body">
    <div class="myHeader">
    Sequencing Types Overview
    </div>
    <div class="myContent">
    <table>
    <g:each var="line" in="${types}">
        <tr>
            <g:each var="token" in="${line}">
                <td>${token}</td>
            </g:each>
        </tr>
    </g:each>
    </table>
    </div>


  </div>
</body>
</html>

