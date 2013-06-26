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
    <h3><g:message code="home.pageTitle"/></h3>
        <div class="homeTable">
            <table>
                <tr>
                    <th><g:message code="index.project"/></th>
                    <th><g:message code="index.seqType"/></th>
                </tr>
                <g:each var="row" in="${projectQuery}">
                    <tr>
                        <td><b><g:link controller="projectOverview" action="index" params="[projectName: row.key]">${row.key}</g:link></b></td>
                        <td><b>${row.value}</b><td>
                    </tr>
                </g:each>
            </table>
        </div>
        <br>
        <div style="width: 10px; height: 5px;"></div>
        <h3><g:message code="home.pageTitle.graph"/></h3>
        <div class="homeGraph">
            <div style="float: left;">
                <canvas id="projectCountPerDate" width="540" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: right;">
                <canvas id="laneCountPerDate" width="540" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: left;">
                <canvas id="sampleCountPerSequenceTypePie" width="540" height="380">[No canvas support]</canvas>
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