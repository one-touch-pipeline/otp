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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title>${g.message(code: 'projectOverview.alignmentInformation.title', args: [selectedProject?.name])}</title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/alignmentConfigurationOverview/index/functions.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>
        <div class="project-selection-header-container">
            <div class="grid-element">
                <g:render template="/templates/projectSelection"/>
            </div>
            <div class="grid-element comment-box">
                <g:render template="/templates/commentBox" model="[
                        commentable     : selectedProject,
                        targetController: 'projectConfig',
                        targetAction    : 'saveProjectComment',
                ]"/>
            </div>
        </div>
        <div>
            <g:render template="/projectConfig/tabMenu"/>
        </div>

        <h1>${g.message(code: 'projectOverview.alignmentInformation.title', args: [selectedProject?.name])}</h1>
        <g:if test="${errorMessage}">
            <otp:annotation type="danger">
                ${g.message(code: 'projectOverview.alignmentInformation.error', args: [errorMessage])}
            </otp:annotation>
        </g:if>
        <div>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <h2><g:message code="projectOverview.alignmentInformation.configureRoddy"/></h2>
                <otp:annotation type="info">The configuration for the new system moved to the <g:link controller="workflowSelection">workflow selection page</g:link>.</otp:annotation>

                <div class="show_button">
                    <ul>
                        <g:each in="${roddySeqTypes}" var="seqType">
                            <li>
                                <g:if test="${seqType.isRna()}">
                                    <g:link controller='configurePipeline' action='rnaAlignment' params='["seqType.id": seqType.id]'
                                            class="configure">
                                        ${seqType.displayNameWithLibraryLayout}
                                    </g:link>
                                </g:if>
                                <g:else>
                                    <g:link controller='configurePipeline' action='alignment' params='["seqType.id": seqType.id]'
                                            class="configure">
                                        ${seqType.displayNameWithLibraryLayout}
                                    </g:link>
                                </g:else>
                            </li>
                        </g:each>
                    </ul>
                </div>
            </sec:ifAllGranted>

            <g:if test="${alignmentInfo}">
                <h2>${g.message(code: 'projectOverview.alignmentInformation.tool.configuration')}</h2>
                <div class="fixed-table-header">
                    <table>
                        <tr>
                            <th>${g.message(code: 'projectOverview.alignmentInformation.tool')}</th>
                            <th>${g.message(code: 'projectOverview.alignmentInformation.version')}</th>
                            <th>${g.message(code: 'projectOverview.alignmentInformation.arguments')}</th>
                        </tr>
                        <g:each in="${alignmentInfo}" var="info">
                            <tr><td colspan="3"><strong>${info.key}</strong></td></tr>
                            <tr><td style="padding: 5px; white-space: nowrap;">${g.message(code: "projectOverview.alignmentInformation.tool.aligning")}<td>${info.value.alignmentProgram}</td><td>${info.value.alignmentParameter}</td></tr>
                            <tr><td style="padding: 5px; white-space: nowrap;">${g.message(code: "projectOverview.alignmentInformation.tool.merging")}</td><td>${info.value.mergeCommand}</td><td>${info.value.mergeOptions}</td></tr>
                            <tr><td style="padding: 5px; white-space: nowrap;">${g.message(code: "projectOverview.alignmentInformation.tool.samtools")}</td><td>${info.value.samToolsCommand}</td><td></td></tr>
                            <tr><td style="padding: 5px; white-space: nowrap;">${g.message(code: "projectOverview.alignmentInformation.tool.roddy")}</td><td>${info.value.programVersion}</td><td></td></tr>
                        </g:each>
                    </table>
                </div>
            </g:if>

            <h2>${g.message(code: 'projectOverview.alignmentInformation.tool.configuration.old')}</h2>
            <div id="alignment_info" class="fixed-table-header">
                <table style="visibility: hidden" id="alignment_info_table">
                    <tr>
                        <th>${g.message(code: 'projectOverview.alignmentInformation.tool')}</th>
                        <th>${g.message(code: 'projectOverview.alignmentInformation.version')}</th>
                        <th>${g.message(code: 'projectOverview.alignmentInformation.arguments')}</th>
                    </tr>

                </table>
            </div>
        </div>

        <div class="fixed-table-header">
            <h2>${g.message(code: 'projectOverview.mergingCriteria')}</h2>
            <otp:annotation type="info">The configuration for the new system moved to the <g:link controller="workflowSelection">workflow selection page</g:link>.</otp:annotation>
            <table class="merging-criteria-table">
                <tr>
                    <th>${g.message(code: 'mergingCriteria.seqType')}</th>
                    <th>${g.message(code: 'mergingCriteria.libPrepKit')}</th>
                    <th>${g.message(code: 'mergingCriteria.seqPlatformGroup')}</th>
                </tr>
                <g:each in="${seqTypeMergingCriteria}" var="m">
                    <tr>
                        <td>
                            <g:link controller="projectSeqPlatformGroup" action="index"
                                    params='["seqType.id": m.key.id]'>
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

        <div class="otpDataTables fixed-table-header">
            <h2>${g.message(code: 'projectOverview.listReferenceGenome.title')}</h2>
            <otp:annotation type="info">The configuration for the new system moved to the <g:link controller="workflowSelection">workflow selection page</g:link>.</otp:annotation>
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
        <br>
    </div>
</body>
</html>
