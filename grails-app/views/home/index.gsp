<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="otp.welcome.title"/></title>
    <r:require module="lightbox"/>
    <r:require module="graph"/>
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
        <div class="homeGraph">
            <div style="float: left;">
                <canvas id="projectCountPerDate" width="540" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: right;">
                <canvas id="laneCountPerDate" width="540" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: left;">
                <canvas id="projectCountPerSequenceTypePie" width="540" height="360">[No canvas support]</canvas>
            </div>
            <div style="float: right;">
                <canvas id="projectCountPerSequenceTypeBar" width="540" height="440">[No canvas support]</canvas>
            </div>
        </div>
    </div>
    <r:script>
        $.otp.graph.overview.init();
    </r:script>
</body>
</html>