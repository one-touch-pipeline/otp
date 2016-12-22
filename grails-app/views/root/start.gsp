<%@ page import="de.dkfz.tbi.otp.ngsdata.Project" %>
<html>
<head>
    <title>OTP - ${projectSelection.getDisplayName()}</title>
    <meta name="layout" content="mainV2"/>
</head>
<body>
<div class="container">
    <div class="row">
        <div class="col-xs-12">
            <h2>${projectSelection.getDisplayName()}</h2>
            <hr>
            <p>
                <g:if test="${projectSelection instanceof Project}">
                    ${projectSelection.description}
                </g:if>
            </p>
        </div>
    </div>
    <div class="row">
        <div class="col-xs-12 col-sm-6 info-box-container">
            <h2>${g.message(code: "otp.menu.sample")}</h2>
            <hr>
            <div class="info-box clearfix">
                <img src="${assetPath(src: 'v2/start/sample.png')}" alt="">
                <h3>${g.message(code: "start.box.sample.title")}</h3>
                <p>
                    ${g.message(code: "start.box.sample.text")}
                </p>
            </div>
        </div>
        <div class="col-xs-12 col-sm-6 info-box-container">
            <h2>${g.message(code: "otp.menu.run")}</h2>
            <hr>
            <div class="info-box clearfix">
                <img src="${assetPath(src: 'v2/start/run.png')}" alt="">
                <h3>${g.message(code: "start.box.run.title")}</h3>
                <p>
                    ${g.message(code: "start.box.run.text")}
                </p>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-xs-12 col-sm-6 info-box-container">
            <h2 title="${g.message(code: "projectOverview.mouseOver.lane")}">${g.message(code: "otp.menu.lane")}</h2>
            <hr>
            <div class="info-box clearfix">
                <img src="${assetPath(src: 'v2/start/lane.png')}" alt="">
                <h3>${g.message(code: "start.box.lane.title")}</h3>
                <p>
                    ${g.message(code: "start.box.lane.text")}
                </p>
            </div>
        </div>
        <div class="col-xs-12 col-sm-6 info-box-container">
            <h2>${g.message(code: "otp.menu.statistics")}</h2>
            <hr>
            <div class="info-box clearfix">
                <img src="${assetPath(src: 'v2/start/statistics.png')}" alt="">
                <h3>${g.message(code: "start.box.statistics.title")}</h3>
                <p>
                    ${g.message(code: "start.box.statistics.text")}
                </p>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-xs-12">
            <h2>${g.message(code: "start.numbers.title", args: [projectSelection.getDisplayName()])}</h2>
            <hr>
        </div>
        <div class="col-xs-12 col-sm-6 statistics-container">
            <div class="statistics-box">
                <p class="projects"><span class="hide-on-xs">${g.message(code: "start.numbers.numberOf")} </span>${g.message(code: "start.numbers.individuals")}
                    <span class="highlight">${numberIndividuals}</span></p>
                <p class="projects"><span class="hide-on-xs">${g.message(code: "start.numbers.numberOf")} </span>${g.message(code: "start.numbers.samples")}
                    <span class="highlight">${numberSamples}</span></p>
                <p class="projects"><span class="hide-on-xs">${g.message(code: "start.numbers.numberOf")} </span>${g.message(code: "start.numbers.lanes")}
                    <span class="highlight">${numberSeqTracks}</span></p>
            </div>
        </div>
        <div class="col-xs-12 col-sm-6 statistics-container">
            <div class="statistics-box">
                <p class="projects"><span class="hide-on-xs">${g.message(code: "start.numbers.numberOf")} </span>${g.message(code: "start.numbers.newIndividuals")}
                    <span class="highlight">${numberNewIndividuals}</span></p>
                <p class="projects"><span class="hide-on-xs">${g.message(code: "start.numbers.numberOf")} </span>${g.message(code: "start.numbers.newSamples")}
                    <span class="highlight">${numberNewSamples}</span></p>
                <p class="projects"><span class="hide-on-xs">${g.message(code: "start.numbers.numberOf")} </span>${g.message(code: "start.numbers.newLanes")}
                    <span class="highlight">${numberNewSeqTracks}</span></p>

            </div>
            <span class="statistics-explanation">${g.message(code: "start.numbers.whatDoesNewMean")}</span>
        </div>
    </div>
</div>
</body>
</html>
