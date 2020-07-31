%{--
  - Copyright 2011-2019 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

<html>
<head>
    <title>OTP - ${selectedProject.getDisplayName()}</title>
    <meta name="layout" content="mainV2"/>
</head>
<body>
<div class="body container">
    <div class="row">
        <div class="col-xs-12">
            <h2>${selectedProject.getDisplayName()}s</h2>
            <hr>
            <p>
                ${selectedProject.description}
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
            <h2 title="${g.message(code: "sampleOverview.registeredLanes.tooltip")}">${g.message(code: "otp.menu.lane")}</h2>
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
            <h2>${g.message(code: "start.numbers.title", args: [selectedProject.getDisplayName()])}</h2>
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
