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

<%@ page import="de.dkfz.tbi.otp.ngsdata.Project" %>
<html>
<head>
    <title>OTP - ${projectSelection.getDisplayName()}</title>
    <meta name="layout" content="mainV2"/>
</head>
<body>
<div class="body container">
    <g:if test="${projectSelection}">
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
            <div class="optionsContainer">
                <g:form action="kpi" class="form-inline" method="get">
                    <div class="input-group">
                        <div class="input-group-addon">
                            <b>${g.message(code: "start.numbers.startDate")}</b>
                        </div>
                        <input type="date"class="form-control" id="start" name="start">
                    </div>

                    <div class="input-group">
                        <div class="input-group-addon">
                            <b>${g.message(code: "start.numbers.endDate")}</b>
                        </div>
                        <input type="date"class="form-control" id="end" name="end">
                    </div>
                    <g:actionSubmit class="btn btn-primary" value="${g.message(code: "start.numbers.btnDate")}"
                                    action="kpi"/>

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
            Download <g:link action="downloadDirectoriesCSV">
                directories.csv
            </g:link>
        </div>
    </g:if>
    <g:else>
        <g:render template="/templates/noProject"/>
    </g:else>
</div>
<script>
    var startDate = '${startDate}';
    var endDate = '${endDate}';
</script>
</body>
</html>
