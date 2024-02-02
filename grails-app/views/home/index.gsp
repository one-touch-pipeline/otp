%{--
  - Copyright 2011-2024 The OTP authors
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
    <asset:javascript src="common/sharedCharts.js"/>
    <asset:javascript src="pages/home/index/index.js"/>
    <asset:javascript src="taglib/ExpandableText.js"/>
    <asset:stylesheet src="pages/home/index.less"/>
</head>
<body>
    <div class="body home">
        <h1>${g.message(code: "home.title")}</h1>

        <h2><g:message code="home.yourProjects.title"/></h2>
        <g:if test="${userProjects}">
            <g:render template="projectOverviewTable" model="[projects: userProjects, linkProjectName: true]"/>
        </g:if>
        <g:else>
            No projects
        </g:else>
        <br>
        <g:if test="${publicProjects}">
            <h2><g:message code="home.publicProjects.title"/></h2>
            <g:render template="projectOverviewTable" model="[projects: publicProjects, linkProjectName: false]"/>
        </g:if>

        <h2><g:message code="home.graph.title"/></h2>
        <form class="rounded-page-header-box" id="projectsGroupbox">
            <span><g:message code="home.graph.filter"/>:</span>
            <g:select name='projectGroup_select' class="use-select-2" style="width: 15ch;"
                      from='${projectGroups}' value='projectGroup' />
        </form>
        <div style="clear: both; text-align: center">
            <div class="sampleCountPerSequenceTypePieChart">
                <canvas id="sampleCountPerSequenceTypePie">[No canvas support]</canvas>
            </div>
            <div class="projectCountPerDateChart">
                <canvas id="projectCountPerDate">[No canvas support]</canvas>
            </div>
            <div style="display: inline-flex">
                <div>
                    <canvas id="laneCountPerDate" width="625px">[No canvas support]</canvas>
                </div>
                <div>
                    <canvas id="gigaBasesPerDay" width="625px">[No canvas support]</canvas>
                </div>
            </div>
            <div style="display: inline-flex">
                <div>
                    <canvas id="patientsCountPerSequenceType" width="625" height="400">[No canvas support]</canvas>
                </div>
                <div>
                    <canvas id="projectCountPerSequenceType" width="625" height="400">[No canvas support]</canvas>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
