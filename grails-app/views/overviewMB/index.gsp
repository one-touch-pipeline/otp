<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Insert title here</title>
</head>
<body>
    <div class="body">
        <div class="tableBlock">
            <h1>Centers Overview</h1>
            <table>
                <tbody>
                    <g:each var="line" in="${centers}">
                        <tr>
                            <g:each var="token" in="${line}">
                                <td>${token}</td>
                            </g:each>
                        </tr>
                    </g:each>
                </tbody>
            </table>
        </div>
        <div class="tableBlock">
            <h1>Sequencing Types Overview</h1>
            <table>
                <tbody>
                    <g:each var="line" in="${types}">
                        <tr>
                            <g:each var="token" in="${line}">
                                <td>${token}</td>
                            </g:each>
                        </tr>
                    </g:each>
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>
