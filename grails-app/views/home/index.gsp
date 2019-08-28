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
    <div class="body">
    <g:if test="${projectQuery}">
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
                    <g:each var="entry" in="${projectQuery}">
                        <tr>
                            <td><b><g:link controller="projectOverview" action="index" params="[project: entry.key]">${entry.value['displayName']}</g:link></b></td>
                            <td><b>${entry.value['seqTypes']}</b><td>
                        </tr>
                    </g:each>
                </table>
            </div>
        </div>
        <br>
        <div style="width: 10px; height: 5px;"></div>
        <h3><g:message code="home.pageTitle.graph"/></h3>
        <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message code="home.projectGroupFilter"/> :</span>
            <g:select class="criteria" id="projectGroup_select" name='projectGroup_select' from='${projectGroups}' value='projectGroup' />
        </form>
        <div class="homeGraph" style="clear: both;" >
            <div style="float: left; margin-top: 20px; border: 25px solid #E1F1FF;">
                <canvas id="sampleCountPerSequenceTypePie" width="530" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: right; margin-top: 20px; border: 25px solid #E1F1FF;">
                <canvas id="projectCountPerDate" width="530" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: left; margin-top: 20px; border: 25px solid #E1F1FF;">
                <canvas id="laneCountPerDate" width="530" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: right; margin-top: 20px; border: 25px solid #E1F1FF;">
                <canvas id="gigaBasesPerDay" width="530" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: left; margin-top: 20px; border: 25px solid #E1F1FF;" >
                <canvas id="patientsCountPerSequenceType" width="1165" height="380">[No canvas support]</canvas>
            </div>
            <div style="float: left; margin-top: 20px; border: 25px solid #E1F1FF;">
                <canvas id="projectCountPerSequenceTypeBar" width="1165" height="380">[No canvas support]</canvas>
            </div>
        </div>
        <asset:script type="text/javascript">
            $(function() {
                $.otp.projectOverviewHome.register();
                $.otp.graph.overview.init();
             });
        </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
