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

<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="otp.welcome.title"/></title>
    <asset:javascript src="modules/graph"/>
    <asset:javascript src="pages/home/index/projectOverview.js"/>
</head>
<body>
    <div class="body home">
        <h1>${g.message(code: "home.title")}</h1>

        <h2><g:message code="home.yourProjects.title"/></h2>
        <g:if test="${userProjects}">
            <div class="table-with-fixed-header">
                <table>
                     <thead>
                        <tr>
                            <th><g:message code="home.project"/></th>
                            <th><g:message code="home.pis"/></th>
                            <th><g:message code="home.desc"/></th>
                            <th><g:message code="home.seqType"/></th>
                        </tr>
                    </thead>
                    <g:each in="${userProjects}" var="project">
                        <tr>
                            <td><g:link controller="projectOverview" action="index" params="[(projectParameter): project.name]">${project.displayName}</g:link></td>
                            <td>${project.pis?.join(", ") ?: "-"}</td>
                            <td title="${project.description}">${project.shortDescription ?: "-"}</td>
                            <td>${project.st.collect { "${it.seqType} (${it.numberOfSamples})" }.join(", ")}</td>
                        </tr>
                    </g:each>
                </table>
            </div>
        </g:if>
        <g:else>
            No projects
        </g:else>
        <br>
        <g:if test="${publicProjects}">
            <h2><g:message code="home.publicProjects.title"/></h2>
            <div class="table-with-fixed-header">
                <table>
                    <thead>
                    <tr>
                        <th><g:message code="home.project"/></th>
                        <th><g:message code="home.pis"/></th>
                        <th><g:message code="home.desc"/></th>
                        <th><g:message code="home.seqType"/></th>
                    </tr>
                    </thead>
                    <g:each in="${publicProjects}" var="project">
                        <tr>
                            <td>${project.displayName}</td>
                            <td>${project.pis?.join(", ") ?: "-"}</td>
                            <td title="${project.description}">${project.shortDescription ?: "-"}</td>
                            <td>${project.st.collect { "${it.seqType} (${it.numberOfSamples})" }.join(", ")}</td>
                        </tr>
                    </g:each>
                </table>
            </div>
        </g:if>

        <h2><g:message code="home.graph.title"/></h2>
        <form class="rounded-page-header-box" id="projectsGroupbox">
            <span><g:message code="home.graph.filter"/>:</span>
            <g:select name='projectGroup_select' class="use-select-2" style="width: 15ch;"
                      from='${projectGroups}' value='projectGroup' />
        </form>
        <div style="clear: both; text-align: center">
            <div>
                <canvas id="sampleCountPerSequenceTypePie" width="1250" height="400">[No canvas support]</canvas>
            </div>
            <div>
                <canvas id="projectCountPerDate" width="1250" height="400">[No canvas support]</canvas>
            </div>
            <div>
                <canvas id="laneCountPerDate" width="625" height="400">[No canvas support]</canvas>
                <canvas id="gigaBasesPerDay" width="625" height="400">[No canvas support]</canvas>
            </div>
            <div>
                <canvas id="patientsCountPerSequenceType" width="625" height="400">[No canvas support]</canvas>
                <canvas id="projectCountPerSequenceTypeBar" width="625" height="400">[No canvas support]</canvas>
            </div>
        </div>
    </div>
</body>
</html>
