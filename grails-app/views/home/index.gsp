<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="otp.welcome.title"/></title>
    <r:require module="lightbox"/>
</head>
<body>
    <div class="body">
        <div class="homeTable">
            <table>
                <tr>
                    <th><g:message code="index.project"/></th>
                    <th><g:message code="index.seqType"/></th>
                </tr>
                <g:each var="row" in="${projectQuery}">
                    <tr>
                        <td><b>${row.key}</b></td>
                        <td><b>${row.value}</b><td>
                    </tr>
                </g:each>
            </table>
        </div>
        <br>
    </div>
    <r:script>
        $(function() {
            $.otp.resizeBodyInit_nTable(".homeTable", 20);
        });
    </r:script>
</body>
</html>