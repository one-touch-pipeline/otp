<%@ page import="de.dkfz.tbi.otp.ngsdata.Project" %>
<html>
<head>
    <title>OTP - ${projectSelection.getDisplayName()}</title>
    <meta name="layout" content="mainV2"/>
</head>
<body>
<div class="body container">
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
</div>
</body>
</html>
