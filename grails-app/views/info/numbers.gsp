<html>
<head>
    <meta name="layout" content="info"/>
    <title><g:message code="info.numbers.title" /></title>
    <asset:javascript src="modules/lightbox"/>
    <asset:stylesheet src="modules/lightbox"/>
    <asset:javascript src="modules/graph"/>
</head>

<body>
    <h2><g:message code="info.numbers.title" /></h2>
    <g:message code="info.numbers.text"/>
    <div class="homeGraph" style="clear: both;" >
        <div class="plotContainer" style="position: abvsolute; top: 0px; left: 0px;">
            <canvas id="projectCountPerDate" width="530" height="380">[No canvas support]</canvas>
        </div>
        <div class="plotContainer">
            <canvas id="laneCountPerDate" width="530" height="380">[No canvas support]</canvas>
        </div>
    </div>
<asset:script type="text/javascript">
    $(function() {
        $.otp.graph.info.init();
    });
</asset:script>
</body>
</html>
