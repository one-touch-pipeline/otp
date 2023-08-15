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
    <title>${g.message(code: "cellRanger.title", args: [selectedProject?.name])}</title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/cellRanger/index/cellRanger.js"/>
</head>
<body>
<div class="body">
    <g:set var="archived" value="${selectedProject.archived ? 'archived' : ''}"/>

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

    <h1><g:message code="cellRanger.title" args="[selectedProject.name, seqType.displayName]"/></h1>

    <g:render template="/templates/quickNavigationBar" model="[
            linkText : g.message(code: 'cellRanger.linkTo.runSelectionPage'),
            link : g.createLink(controller: 'cellRanger', action: 'finalRunSelection'),
            tooltip : g.message(code: 'cellRanger.linkTo.runSelectionPage.tooltip')
    ]"/>

    <g:if test="${archived}">
        <otp:annotation type="warning">
            <g:message code="configurePipeline.info.projectArchived.noChange" args="[selectedProject.name]"/>
        </otp:annotation>
    </g:if>

    <h2>Configure version</h2>
    <g:form action="updateVersion" params='["seqType.id": seqType.id, overviewController: controllerName]' method='POST'>
        <table class="pipelineTable">
            <tr>
                <th></th>
                <th></th>
                <th><g:message code="configurePipeline.header.defaultValue"/></th>
                <th><g:message code="configurePipeline.header.info"/></th>
            </tr>
            <tr>
                <td class="myKey"><g:message code="configurePipeline.version"/></td>
                <td><g:select name="programVersion" class="use-select-2" value="${currentVersion}" from="${availableVersions}" noSelection="${["": "Select version"]}"/></td>
                <td>${defaultVersion}</td>
                <td><g:message code="configurePipeline.cellRanger.info"/></td>
            </tr>
            <tr>
                <td colspan="4">&nbsp;</td>
            </tr>
            <tr>
                <td class="myKey"></td>
                <td><g:submitButton class="${archived}" name="submit" value="Submit"/></td>
            </tr>
        </table>
    </g:form>
    <g:if test="${currentVersion}">
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <g:form controller="configurePipeline" action="invalidateConfig" method="POST"
                    params='["seqType.id": seqType.id, "pipeline.id": pipeline.id, "originAction": actionName, overviewController: "cellRangerConfiguration"]'>
                <g:submitButton class="${archived}" name="invalidateConfig" value="Invalidate Config"/>
            </g:form>
        </sec:ifAllGranted>
    </g:if>
    <br>

    <hr>

    <h2>${g.message(code: "cellRanger.automaticRun")}</h2>
    <g:if test="${configExists}">
        <div id="updateAutomaticExecution">
            <g:form action="updateAutomaticExecution" params='["seqType.id": seqType.id, overviewController: controllerName]' method='POST'>
                <g:checkBox id="enableAutoExec" name="enableAutoExec" value="${config.autoExec}"/><label for="enableAutoExec">${g.message(code: "cellRanger.automaticRun.enable")}</label>
                <br><br>

                <div class="automaticSettings">
                    <div>
                        <strong>${g.message(code: "cellRanger.create.referenceGenomeIndex")}:</strong>
                        <g:select id="referenceGenomeIndex2" name="referenceGenomeIndex.id" class="use-select-2"
                                  from="${referenceGenomeIndexes}"
                                  value="${updateAutomaticExecutionCmd ? updateAutomaticExecutionCmd.referenceGenomeIndex.id == 'expected' : config.referenceGenomeIndex?.id}"
                                  optionKey="id" noSelection="${[(""): "Select a reference"]}"
                                  required="true"/>
                    </div>

                    <div>
                        <strong>${g.message(code: "cellRanger.create.expectedOrEnforcedCells")}:</strong><br>
                        <label>
                            <g:radio name="expectedOrEnforcedCells" value="neither"
                                     checked="${updateAutomaticExecutionCmd ? (updateAutomaticExecutionCmd.expectedCellsValue == null && updateAutomaticExecutionCmd.enforcedCellsValue == null) : (config.expectedCells == null && config.enforcedCells == null)}"/>
                            ${g.message(code: "cellRanger.default")}
                        </label>
                        <br>
                        <label>
                            <g:radio name="expectedOrEnforcedCells" value="expected"
                                     checked="${updateAutomaticExecutionCmd ? updateAutomaticExecutionCmd.expectedCellsValue : config.expectedCells}"/>
                            ${g.message(code: "cellRanger.expectedCells")}
                        </label>
                        <input name="expectedCellsValue" value="${updateAutomaticExecutionCmd?.expectedCellsValue ?: config.expectedCells}" required/>
                        <br>
                        <label>
                            <g:radio name="expectedOrEnforcedCells" value="enforced"
                                     checked="${updateAutomaticExecutionCmd ? updateAutomaticExecutionCmd.enforcedCellsValue : config.enforcedCells}"/>
                            ${g.message(code: "cellRanger.enforcedCells")}
                        </label>
                        <input name="enforcedCellsValue" value="${updateAutomaticExecutionCmd?.enforcedCellsValue ?: config.enforcedCells}" required/>
                        <br><br>
                    </div>
                </div>
                <g:submitButton id="save" name="Save"/>
            </g:form>
        </div>
    </g:if>
    <g:else>
        <otp:annotation type="info">${g.message(code: "cellRanger.noConfig")}</otp:annotation>
    </g:else>

    <hr>

    <h2>${g.message(code: "cellRanger.select")}</h2>
    <otp:annotation type="info">${g.message(code: "cellRanger.hashedSamplesInfo")}</otp:annotation>
    <otp:annotation type="info">${g.message(code: "cellRanger.xenograftSamplesInfo")}</otp:annotation>
    <g:if test="${configExists}">
        <g:form action="index" method="GET">
            <input type="hidden" name="${projectParameter}" value="${selectedProject.name}"/>
            <div class="cell-ranger-selection-grid-wrapper">
                <div>
                    <strong>${g.message(code: "cellRanger.individual")}:</strong><br>
                    <g:select name="individual.id" class="use-select-2" from="${allIndividuals}" optionKey="id" value="${individual?.id}"
                              noSelection="${[(""): "All Individuals"]}" onChange="submit()"/>
                </div>
                <div>
                    <strong>${g.message(code: "cellRanger.sampleType")}:</strong><br>
                    <g:select name="sampleType.id" class="use-select-2" from="${allSampleTypes}" optionKey="id" value="${sampleType?.id}"
                              noSelection="${[(""): "All Sample Types"]}" onChange="submit()"/>
                </div>
                <div>
                    <strong>${g.message(code: "cellRanger.seqType")}:</strong><br>
                    ${seqType}
                    <input type="hidden" name="seqType.id" value="${seqType.id}"/>
                </div>
            </div>
        </g:form>
        <br>
        <g:if test="${samples}">
            <h3>${g.message(code: "cellRanger.header.create")}:</h3>
            <g:form action="createMwp" method="POST">
                <input type="hidden" name="sampleType.id" value="${sampleType?.id}"/>
                <input type="hidden" name="individual.id" value="${individual?.id}"/>
                <input type="hidden" name="seqType.id" value="${seqType?.id}"/>

                <div class="cell-ranger-creation-grid-wrapper">
                    <div class="row one">
                        <div>
                            <strong>${g.message(code: "cellRanger.create.referenceGenomeIndex")}:</strong>
                            <g:select id="referenceGenomeIndex" name="referenceGenomeIndex.id" class="use-select-2"
                                      from="${referenceGenomeIndexes}" value="${createMwpCmd?.referenceGenomeIndex?.id ?: referenceGenomeIndex?.id}"
                                      optionKey="id" noSelection="${[(""): "Select a reference"]}"
                                      required="true"/>
                        </div>
                    </div>
                    <div class="row two">
                        <div>
                            <strong>${g.message(code: "cellRanger.create.selectedIndividuals")}:</strong>
                            <ul class="scrollable">
                                <g:each in="${selectedIndividuals}" var="individual">
                                    <li>${individual}</li>
                                </g:each>
                            </ul>
                        </div>
                        <div>
                            <strong>${g.message(code: "cellRanger.create.selectedSampleTypes")}:</strong>
                            <ul class="scrollable">
                                <g:each in="${selectedSampleTypes}" var="sampleType">
                                    <li>${sampleType}</li>
                                </g:each>
                            </ul>
                        </div>
                        <div>
                            <strong>${g.message(code: "cellRanger.create.expectedOrEnforcedCells")}:</strong><br><br>
                            <label>
                                <g:radio name="expectedOrEnforcedCells" value="neither" checked="${createMwpCmd ? (createMwpCmd.expectedCellsValue == null && createMwpCmd.enforcedCellsValue == null) : false}"/>
                                ${g.message(code: "cellRanger.default")}
                            </label>
                            <br>
                            <label>
                                <g:radio name="expectedOrEnforcedCells" value="expected" checked="${createMwpCmd ? createMwpCmd.expectedCellsValue : false}"/>
                                ${g.message(code: "cellRanger.expectedCells")}
                            </label>
                            <input name="expectedCellsValue" value="${createMwpCmd?.expectedCellsValue}" required/>
                            <br>
                            <label>
                                <g:radio name="expectedOrEnforcedCells" value="enforced" checked="${createMwpCmd ? createMwpCmd.enforcedCellsValue : false}"/>
                                ${g.message(code: "cellRanger.enforcedCells")}
                            </label>
                            <input name="enforcedCellsValue" value="${createMwpCmd?.enforcedCellsValue}" required/>
                            <br><br>
                        </div>
                    </div>
                    <div class="row three ${archived}">
                        <g:submitButton id="execute" name="Execute Cell Ranger"/>
                    </div>
                </div>
            </g:form>
            <br>
            <h3>${g.message(code: "cellRanger.header.processes")}:</h3>
            <table>
                <tr>
                    <th>${g.message(code: "cellRanger.individual")}</th>
                    <th>${g.message(code: "cellRanger.sampleType")}</th>
                    <th>${g.message(code: "cellRanger.seqType")}</th>
                    <th>${g.message(code: "cellRanger.config.programVersion")}</th>
                    <th>${g.message(code: "cellRanger.referenceGenome")}</th>
                    <th>${g.message(code: "cellRanger.referenceGenomeIndex")}</th>
                    <th>${g.message(code: "cellRanger.expectedCells")}</th>
                    <th>${g.message(code: "cellRanger.enforcedCells")}</th>
                    <th>${g.message(code: "cellRanger.processingStatus")}</th>
                </tr>
                <g:each var="mwp" in="${mwps}">
                    <tr>
                        <td>${mwp.individual}</td>
                        <td>${mwp.sampleType}</td>
                        <td>${mwp.seqType}</td>
                        <td>${mwp.config.programVersion}</td>
                        <td>${mwp.referenceGenome}</td>
                        <td>${mwp.referenceGenomeIndex.toolWithVersion}</td>
                        <td>${mwp.expectedCells}</td>
                        <td>${mwp.enforcedCells}</td>
                        <td>${mwp.bamFileInProjectFolder ? mwp.bamFileInProjectFolder.fileOperationStatus : "N/A"}</td>
                    </tr>
                </g:each>
            </table>
        </g:if>
        <g:else>
            <p>${g.message(code: "cellRanger.noSamples")}</p>
        </g:else>
    </g:if>
    <g:else>
        <otp:annotation type="info">${g.message(code: "cellRanger.noConfig")}</otp:annotation>
    </g:else>
</div>
</body>
</html>
