<%@ page import="de.dkfz.tbi.otp.ngsdata.Project" %>
<html>
<head>
    <title>OTP - ${projectSelection.getDisplayName()}</title>
    <meta name="layout" content="mainV2"/>
</head>
<body>
<div class="body container">
    <g:if test="${numberOfProject}">
        <div class="col-xs-12">
            <h2>${projectSelection.getDisplayName()}</h2>
            <hr>
            <p>
                <g:if test="${projectSelection instanceof Project}">
                    ${projectSelection.description}
                </g:if>
            </p>
        </div>
        <div class="col-xs-12 col-sm-12 statistics-container">
            <div class="statistics-box">
                <p class="projects"><span class="hide-on-xs">${g.message(code: "start.numbers.numberOf")}  Users</span>
                    <span class="highlight">${numberOfUsers}</span></p>
                <p class="projects"><span class="hide-on-xs">${g.message(code: "start.numbers.numberOf")} Projects</span>
                    <span class="highlight">${numberOfProject}</span></p>
                <p class="projects"><span class="hide-on-xs">${g.message(code: "start.numbers.numberOf")} ${g.message(code: "start.numbers.samples")}</span>
                    <span class="highlight">${numberOfSamples}</span></p>
                <p class="projects"><span class="hide-on-xs">${g.message(code: "start.numbers.numberOf")} ClusterJobs</span>
                    <span class="highlight">${numberOfClusterJobs}</span></p>
            </div>
            Download <g:link action="downloadDirectoriesCSV">
                directories.csv
            </g:link>
        </div>
    </g:if>
    <g:else>
        <br>
        <h3><g:message code="default.no.project"/></h3>

    </g:else>
</div>
</body>
</html>
