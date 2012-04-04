<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Server Shutdown</title>
</head>
<body>
<!-- TODO: global info messages -->
    <p style="display: none" id="shutdownInfo"></p>
    <p>
        <label for="shutdownReason">Reason for Shutdown:</label><input type="text" id="shutdownReason"/>
        <button id="planShutdown">Plan Shutdown</button>
    </p>
    <r:script>
    $("#planShutdown").click(function () {
        $.getJSON("${g.createLink(action: 'planShutdown')}", {reason: $("#shutdownReason").val()}, function (data) {
            if (data.success === true) {
                $("#shutdownInfo").text("Server shutdown planned");
                $("#shutdownInfo").show();
            } else {
                $("#shutdownInfo").text("Server shutdown could not be scheduled!");
                $("#shutdownInfo").show();
            }
        });
    });
    </r:script>
</body>
</html>
