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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="projectOverview.title" args="[project?.name]"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/projectConfig/index/functions.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <g:if test="${projects}">
        <div class="project-selection-header-container">
            <div class="grid-element">
                <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]"/>
            </div>
            <div class="grid-element comment-box">
                <g:render template="/templates/commentBox" model="[
                        commentable     : project,
                        targetController: 'projectConfig',
                        targetAction    : 'saveProjectComment',
                ]"/>
            </div>
        </div>
        <div>
            <g:render template="linkBanner" model="[project: project, active: 'alignment' ]"/>
        </div>
        <br>

        <h1>${g.message(code: 'projectOverview.alignmentInformation.title')}</h1>

        <div>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <h2><g:message code="projectOverview.alignmentInformation.configureRoddy"/></h2>

                <div class="show_button">
                    <ul>
                        <g:each in="${roddySeqTypes}" var="seqType">
                            <li>
                                <g:if test="${seqType.isRna()}">
                                    <g:link controller='configurePipeline' action='rnaAlignment' params='["project.id": project.id, "seqType.id": seqType.id]'
                                            class="configure">
                                        ${seqType.displayNameWithLibraryLayout}
                                    </g:link>
                                </g:if>
                                <g:else>
                                    <g:link controller='configurePipeline' action='alignment' params='["project.id": project.id, "seqType.id": seqType.id]'
                                            class="configure">
                                        ${seqType.displayNameWithLibraryLayout}
                                    </g:link>
                                </g:else>
                            </li>
                        </g:each>
                    </ul>
                </div>
            </sec:ifAllGranted>
            <div id="alignment_info">
                <table style="visibility: hidden" id="alignment_info_table">
                    <tr>
                        <th>${g.message(code: 'projectOverview.alignmentInformation.tool')}</th>
                        <th>${g.message(code: 'projectOverview.alignmentInformation.version')}</th>
                        <th>${g.message(code: 'projectOverview.alignmentInformation.arguments')}</th>
                    </tr>

                </table>
            </div>
        </div>

        <div>
            <h2>${g.message(code: 'projectOverview.mergingCriteria')}</h2>
            <table>
                <tr>
                    <th>${g.message(code: 'projectOverview.mergingCriteria.seqType')}</th>
                    <th>${g.message(code: 'projectOverview.mergingCriteria.libPrepKit')}</th>
                    <th>${g.message(code: 'projectOverview.mergingCriteria.seqPlatformGroup')}</th>
                </tr>
                <g:each in="${seqTypeMergingCriteria}" var="m">
                    <tr>
                        <td>
                            <g:link controller="mergingCriteria" action="projectAndSeqTypeSpecific"
                                    params='["project.id": project.id, "seqType.id": m.key.id]'>
                                ${m.key}
                            </g:link>
                        </td>
                        <td>
                            ${m.value?.useLibPrepKit != null ? m.value.useLibPrepKit : "Not configured"}
                        </td>
                        <td>
                            ${m.value?.useSeqPlatformGroup ?: "Not configured"}
                        </td>
                    </tr>
                </g:each>
            </table>
        </div>

        <div>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <h2><g:message code="projectOverview.alignmentInformation.configureCellRanger"/></h2>

                <div class="show_button">
                    <ul>
                        <g:each in="${cellRangerSeqTypes}" var="seqType">
                            <li>
                                <g:link controller='configureCellRangerPipeline' action='index' params='["project.id": project.id, "seqType.id": seqType.id]'
                                        class="configure">
                                    ${seqType.displayNameWithLibraryLayout}
                                </g:link>
                            </li>
                        </g:each>
                    </ul>
                </div>
            </sec:ifAllGranted>
            <table>
                <tr>
                    <th>${g.message(code: 'projectOverview.alignmentInformation.cellRanger.seqType')}</th>
                    <th>${g.message(code: 'projectOverview.alignmentInformation.cellRanger.version')}</th>
                </tr>
                <g:each in="${cellRangerOverview}" var="m">
                    <tr>
                        <td>
                            ${m.seqType?.getDisplayNameWithLibraryLayout()}
                        </td>
                        <td>
                            ${m.config?.programVersion ?: "Not configured"}
                        </td>
                    </tr>
                </g:each>
            </table>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:link controller="cellRanger">${g.message(code: 'projectOverview.alignmentInformation.cellRanger.link')}</g:link>
            </sec:ifAllGranted>
        </div>
        <br>

        <div class="otpDataTables">
            <h2>${g.message(code: 'projectOverview.listReferenceGenome.title')}</h2>
            <otp:dataTable
                    codes="${[
                            'projectOverview.index.referenceGenome.sequenceTypeName',
                            'projectOverview.index.referenceGenome.sampleTypeName',
                            'projectOverview.index.referenceGenome',
                            'projectOverview.index.statSizeFile',
                            'projectOverview.index.adapterTrimming',
                    ]}"
                    id="listReferenceGenome"/>
        </div>
        <asset:script type="text/javascript">
            $(function() {
                $.otp.projectConfig.referenceGenome();
                $.otp.projectConfig.asynchronousCallAlignmentInfo();
            });
        </asset:script>
    </g:if>
    <g:else>
        <g:render template="/templates/noProject"/>
    </g:else>
</div>
</body>
</html>
