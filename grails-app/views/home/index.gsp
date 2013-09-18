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
                 <thead>
                    <tr>
                        <th><g:message code="index.project"/></th>
                        <th class="tableHeaderHome"><g:message code="index.seqType"/></th>
                    </tr>
                </thead>
            </table>
            <div class="table-body-box" style="margin-top:-10px;">
                <table>
                    <g:each var="row" in="${projectQuery}">
                        <tr>
                            <td><b><g:link controller="projectOverview" action="index" params="[projectName: row.key]">${row.key}</g:link></b></td>
                            <td><b>${row.value}</b><td>
                        </tr>
                    </g:each>
                </table>
            </div>
        </div>
        <br>
        <div style="width: 10px; height: 5px;"></div>
        <h3><g:message code="home.pageTitle.graph"/></h3>
        <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="home.projectGroupfilter"/> :</span>
            <g:select class="criteria" id="projectGroup_select" name='projectGroup_select'
                    from='${projectGroups}' value='${projectGroup}'></g:select>
        </form>
        <div class="homeGraph" style="clear: both;" >
            <div style="float: left; margin-top: 20px; border: 25px solid #E1F1FF;">
                <canvas id="sampleCountPerSequenceTypePie" width="530" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: right; margin-top: 20px; border: 25px solid #E1F1FF;" >
                <canvas id="patientsCountPerSequenceType" width="530" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: left; margin-top: 20px; border: 25px solid #E1F1FF;">
                <canvas id="projectCountPerDate" width="530" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: right; margin-top: 20px; border: 25px solid #E1F1FF;">
                <canvas id="projectCountPerSequenceTypeBar" width="530" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: left; margin-top: 20px; border: 25px solid #E1F1FF;">
                <canvas id="laneCountPerDate" width="530" height="380">[No canvas support]</canvas>
            </div>
        </div>
    </div>
    <r:script>
     $(function() {
        $.otp.projectOverviewHome.register();
        $.otp.graph.overview.init();
         });
    </r:script>
</body>
</html>