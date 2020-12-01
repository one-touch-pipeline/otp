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
    <title>${g.message(code: "start.numbers.title")}</title>
    <asset:javascript src="pages/statistics/kpi.js"/>
</head>
<body>
    <div class="body container">
        <g:render template="/templates/projectSelection"/>
        <div class="col-xs-12">
            <h2>${g.message(code: "start.numbers.title")}</h2>
            <hr>
        </div>

        <div class="col-xs-12 col-sm-12 statistics-container">
            <div class="optionsContainer">
                <g:form action="kpi" class="form-inline" method="get">
                    <input type="hidden" name="${projectParameter}" value="${selectedProject.name}"/>
                    <div class="input-group">
                        <div class="input-group-prepend">
                            <span class="input-group-text"><label for="projectGroup">${g.message(code: "start.numbers.group")}</label></span>
                        </div>
                        <g:select class="form-control" id="projectGroup" name="projectGroup" value="${projectGroup}" from="${availableProjectGroups*.name}"
                                  noSelection='["": g.message(code: "start.numbers.allProjects")]'/>

                        <div class="input-group-prepend input-group-append">
                            <span class="input-group-text"><label for="start">${g.message(code: "start.numbers.startDate")}</label></span>
                        </div>
                        <input type="date" class="form-control" id="start" name="start" value="${startDate}" max="${maxDate}">

                        <div class="input-group-prepend input-group-append">
                            <span class="input-group-text"><label for="end">${g.message(code: "start.numbers.endDate")}</label></span>
                        </div>
                        <input type="date" class="form-control" id="end" name="end" value="${endDate}" max="${maxDate}">

                        <div class="input-group-append">
                            <button class="btn btn-primary" type="submit" id="button-addon2">${g.message(code: "start.numbers.btnDate")}</button>
                        </div>
                    </div>
                </g:form>
            </div>
            <br>
            <g:if test="${startDate && endDate}">
                <br>
                <div class="alert alert-info" role="alert">
                    <span class="glyphicon glyphicon-info-sign" aria-hidden="true"></span>
                    <span class="sr-only">${g.message(code: "start.numbers.info.sign")}</span>
                    ${g.message(code: "start.numbers.info.users")}</div>
            </g:if>
            <div class="statistics-box">
                <h2>${projectGroup ?: g.message(code: "start.numbers.allProjects")}</h2>
                <hr>
                <p class="projects"><span
                        class="hide-on-xs">${g.message(code: "start.numbers.numberOf")}</span> ${g.message(code: "start.numbers.users")}
                    <span class="highlight">${numberOfUsers} <g:if test="${numberOfCreatedUsers}">(${numberOfCreatedUsers})</g:if></span></p>

                <p class="projects"><span
                        class="hide-on-xs">${g.message(code: "start.numbers.numberOf")}</span> ${g.message(code: "start.numbers.projects")}
                    <span class="highlight">${numberOfProject}</span></p>

                <p class="projects"><span
                        class="hide-on-xs">${g.message(code: "start.numbers.numberOf")}</span> ${g.message(code: "start.numbers.samples")}
                    <span class="highlight">${numberOfSamples}</span></p>

                <p class="projects"><span
                        class="hide-on-xs">${g.message(code: "start.numbers.numberOf")}</span> ${g.message(code: "start.numbers.clusterJobs")}
                    <span class="highlight">${numberOfClusterJobs}</span></p>
            </div>

            <div class="statistics-box">
                <h2>${selectedProject.displayName}</h2>
                <hr>
                <p class="projects"><span
                        class="hide-on-xs">${g.message(code: "start.numbers.numberOf")}</span> ${g.message(code: "start.numbers.users")}
                    <span class="highlight">${numberOfUsersProject} <g:if test="${numberOfCreatedUsersProject}">(${numberOfCreatedUsersProject})</g:if></span></p>

                <p class="projects"><span
                        class="hide-on-xs">${g.message(code: "start.numbers.numberOf")}</span> ${g.message(code: "start.numbers.samples")}
                    <span class="highlight">${numberOfSamplesProject}</span></p>

                <p class="projects"><span
                        class="hide-on-xs">${g.message(code: "start.numbers.numberOf")}</span> ${g.message(code: "start.numbers.clusterJobs")}
                    <span class="highlight">${numberOfClusterJobsProject}</span></p>
            </div>

            <div class="statistics-box">
                <h2>${g.message(code: "start.numbers.download")}</h2>
                <hr>
                <g:link action="downloadDirectoriesCSV">directories.csv</g:link>
            </div>
        </div>
    </div>
</body>
</html>
