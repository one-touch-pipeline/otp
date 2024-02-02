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
    <title>${g.message(code: 'projectOverview.analysis.title', args: [selectedProject?.name])}</title>
    <asset:javascript src="common/CommentBox.js"/>
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

        <h1>${g.message(code: 'projectOverview.analysis.title', args: [selectedProject?.name])}</h1>

        <div>
            <h2>${g.message(code: 'projectOverview.snv.title')}</h2>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:message code="projectOverview.snv.configure"/>
                <ul>
                    <g:each in="${snvSeqTypes}" var="seqType">
                        <li>
                            <g:link controller='ConfigureSnvPipeline' params='["seqType.id": seqType.id]' class="configure">
                                ${seqType.displayNameWithLibraryLayout}
                            </g:link>
                        </li>
                    </g:each>
                </ul>
                <br>
            </sec:ifAllGranted>
            <table>
                <g:each var="row" in="${snvConfigTable}" status="i">
                    <tr>
                        <g:each var="cell" in="${row}">
                            <g:if test="${i == 0}">
                                <th>${cell}</th>
                            </g:if>
                            <g:else>
                                <td class="tableEntry">${cell}</td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:each>
            </table>
            <br>
        </div>

        <div>
            <h2>${g.message(code: 'projectOverview.indel.title')}</h2>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:message code="projectOverview.indel.configure"/>
                <ul>
                    <g:each in="${indelSeqTypes}" var="seqType">
                        <li>
                            <g:link controller='ConfigureIndelPipeline' params='["seqType.id": seqType.id]' class="configure">
                                ${seqType.displayNameWithLibraryLayout}
                            </g:link>
                        </li>
                    </g:each>
                </ul>
            </sec:ifAllGranted>
            <table>
                <g:each var="row" in="${indelConfigTable}" status="i">
                    <tr>
                        <g:each var="cell" in="${row}">
                            <g:if test="${i == 0}">
                                <th>${cell}</th>
                            </g:if>
                            <g:else>
                                <td class="tableEntry">${cell}</td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:each>
            </table>
            <br>
        </div>

        <div>
            <h2>${g.message(code: 'projectOverview.sophia.title')}</h2>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:message code="projectOverview.sophia.configure"/>
                <ul>
                    <g:each in="${sophiaSeqTypes}" var="seqType">
                        <li>
                            <g:if test="${!checkSophiaReferenceGenome[seqType]}">
                                <g:link controller='ConfigureSophiaPipeline' params='["seqType.id": seqType.id]' class="configure">
                                    ${seqType.displayNameWithLibraryLayout}
                                </g:link>
                            </g:if>
                            <g:else>
                                ${seqType.displayNameWithLibraryLayout}: ${checkSophiaReferenceGenome[seqType]}
                            </g:else>
                        </li>
                    </g:each>
                </ul>
            </sec:ifAllGranted>
            <table>
                <g:each var="row" in="${sophiaConfigTable}" status="i">
                    <tr>
                        <g:each var="cell" in="${row}">
                            <g:if test="${i == 0}">
                                <th>${cell}</th>
                            </g:if>
                            <g:else>
                                <td class="tableEntry">${cell}</td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:each>
            </table>
            <br>
        </div>

        <div>
            <h2>${g.message(code: 'projectOverview.aceseq.title')}</h2>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:message code="projectOverview.aceseq.configure"/>
                <ul>
                    <g:each in="${aceseqSeqTypes}" var="seqType">
                        <li>
                            <g:if test="${!checkAceseqReferenceGenome[seqType]}">
                                <g:link controller='ConfigureAceseqPipeline' params='["seqType.id": seqType.id]' class="configure">
                                    ${seqType.displayNameWithLibraryLayout}
                                </g:link>
                            </g:if>
                            <g:else>
                                ${seqType.displayNameWithLibraryLayout}: ${checkAceseqReferenceGenome[seqType]}
                            </g:else>
                        </li>
                    </g:each>
                </ul>
            </sec:ifAllGranted>
            <table>
                <g:each var="row" in="${aceseqConfigTable}" status="i">
                    <tr>
                        <g:each var="cell" in="${row}">
                            <g:if test="${i == 0}">
                                <th>${cell}</th>
                            </g:if>
                            <g:else>
                                <td class="tableEntry">${cell}</td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:each>
            </table>
            <br>
        </div>

        <div>
            <h2>${g.message(code: 'projectOverview.runYapsa.title')}</h2>
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <g:message code="projectOverview.runYapsa.configure"/>
                <ul>
                    <g:each in="${runYapsaSeqTypes}" var="seqType">
                        <li>
                            <g:link controller='configureRunYapsaPipeline' action='index' params='["seqType.id": seqType.id]'
                                    class="configure">
                                ${seqType.displayNameWithLibraryLayout}
                            </g:link>
                        </li>
                    </g:each>
                </ul>
            </sec:ifAllGranted>
            <table>
                <g:each var="row" in="${runYapsaConfigTable}" status="i">
                    <tr>
                        <g:each var="cell" in="${row}">
                            <g:if test="${i == 0}">
                                <th>${cell}</th>
                            </g:if>
                            <g:else>
                                <td class="tableEntry">${cell}</td>
                            </g:else>
                        </g:each>
                    </tr>
                </g:each>
            </table>
            <br>
        </div>
    </div>
</body>
</html>
