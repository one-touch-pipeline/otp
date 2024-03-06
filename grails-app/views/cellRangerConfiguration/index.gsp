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

<%@ page import="de.dkfz.tbi.otp.project.Project" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${g.message(code: "cellRanger.title", args: [selectedProject?.name])}</title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/cellRanger/index/cellRanger.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <g:set var="archived" value="${selectedProject.state == Project.State.ARCHIVED ? 'archived' : ''}"/>
    <g:set var="deleted" value="${selectedProject.state == Project.State.DELETED ? 'deleted' : ''}"/>

    <div class="project-selection-header-container">
        <div class="grid-element">
            <g:render template="/templates/bootstrap/projectSelection"/>
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
    <br>

    <h1><g:message code="cellRanger.title" args="[selectedProject.name, seqType.displayName]"/></h1>

    <g:render template="/templates/quickNavigationBar" model="[
            linkText: g.message(code: 'cellRanger.linkTo.runSelectionPage'),
            link    : g.createLink(controller: 'cellRanger', action: 'finalRunSelection'),
            tooltip : g.message(code: 'cellRanger.linkTo.runSelectionPage.tooltip')
    ]"/>

    <g:render template="/templates/bootstrap/noChange" model="[project: selectedProject]"/>

    <h2><g:message code="configurePipeline.configureVersion.title"/></h2>
    <g:message code="configurePipeline.cellRanger.info"/>

    <g:form action="updateVersion" params='["seqType.id": seqType.id, overviewController: controllerName]' method='POST'>
        <div class="mb-3 row">
            <label for="versionSelect" class="col-sm-2 col-form-label">
                <g:message code="configurePipeline.version"/>
                (default: ${defaultVersion})
            </label>
            <g:select name="programVersion" id="versionSelect" class="use-select-2 col-sm-4" value="${currentVersion}" from="${availableVersions}"
                      noSelection="${["": "Select version"]}"/>
            <div class="col-sm-5">
                <g:submitButton class="${archived} ${deleted} btn btn-primary" name="submit" value="Submit"/>
            </div>
        </div>
    </g:form>
    <g:if test="${currentVersion}">
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <g:form controller="configurePipeline" action="invalidateConfig" method="POST"
                    params='["seqType.id": seqType.id, "pipeline.id": pipeline.id, "originAction": actionName, overviewController: "cellRangerConfiguration"]'>
                <g:submitButton class="${archived} ${deleted} btn btn-primary" name="invalidateConfig" value="Invalidate Config"/>
            </g:form>
        </sec:ifAllGranted>
    </g:if>

    <hr>

    <h2>${g.message(code: "cellRanger.automaticRun")}</h2>
    <g:if test="${configExists}">
        <div id="updateAutomaticExecution">
            <g:form name="" action="updateAutomaticExecution" params='["seqType.id": seqType.id, overviewController: controllerName]' method='POST'>
                <g:set var="checked" value="${config.autoExec ? 'checked' : ''}"/>

                <div class="custom-control custom-checkbox pb-3">
                    <g:checkBox id="enableAutoExec" name="enableAutoExec" value="${config.autoExec}" class="custom-control-input"/>
                    <label class="custom-control-label" for="enableAutoExec">${g.message(code: "cellRanger.automaticRun.enable")}</label>
                </div>

                <div class="automaticSettings">
                    <div class="form-group col-md-6">
                        <label for="referenceGenomeIndex2">
                            ${g.message(code: "cellRanger.create.referenceGenomeIndex")}

                        </label>
                        <g:select id="referenceGenomeIndex2" name="referenceGenomeIndex.id" class="use-select-2 form-control"
                                  from="${referenceGenomeIndexes}"
                                  value="${updateAutomaticExecutionCmd ? updateAutomaticExecutionCmd.referenceGenomeIndex.id == 'expected' : config.referenceGenomeIndex?.id}"
                                  optionKey="id" noSelection="${[(""): "Select a reference"]}"
                                  required="true"/>
                    </div>

                    <div class="form-group col-md-6">
                        <label for="referenceGenomeIndex2">
                            ${g.message(code: "cellRanger.create.expectedOrEnforcedCells")}
                        </label>

                        <div class="custom-control custom-radio">
                            <g:radio name="expectedOrEnforcedCells" value="neither" id="defaultExpectedCells" class="custom-control-input"
                                     checked="${updateAutomaticExecutionCmd ? (updateAutomaticExecutionCmd.expectedCellsValue == null && updateAutomaticExecutionCmd.enforcedCellsValue == null) : (config.expectedCells == null && config.enforcedCells == null)}"/>
                            <label class="custom-control-label" for="defaultExpectedCells">${g.message(code: "cellRanger.default")}</label>
                        </div>

                        <div class="custom-control custom-radio">
                            <g:radio name="expectedOrEnforcedCells" value="expected" class="custom-control-input" id="expectedCellsRadio"
                                     checked="${updateAutomaticExecutionCmd ? updateAutomaticExecutionCmd.expectedCellsValue : config.expectedCells}"/>

                            <label class="custom-control-label" for="expectedCellsRadio">${g.message(code: "cellRanger.expectedCells")}</label>
                            <input name="expectedCellsValue" class="form-control"
                                   value="${updateAutomaticExecutionCmd?.expectedCellsValue ?: config.expectedCells}" required/>
                        </div>

                        <div class="custom-control custom-radio">
                            <g:radio name="expectedOrEnforcedCells" value="enforced" class="custom-control-input" id="enforcedCellsRadio"
                                     checked="${updateAutomaticExecutionCmd ? updateAutomaticExecutionCmd.enforcedCellsValue : config.enforcedCells}"/>
                            <label class="custom-control-label" for="enforcedCellsRadio">${g.message(code: "cellRanger.enforcedCells")}</label>
                            <input name="enforcedCellsValue" class="form-control"
                                   value="${updateAutomaticExecutionCmd?.enforcedCellsValue ?: config.enforcedCells}" required/>
                        </label>
                        </div>
                    </div>
                </div>
                <g:submitButton id="save" name="Save" class="btn btn-primary"/>
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
        <g:form name="sampleForm" action="createMwp" method="POST">
            <input type="hidden" name="selectedProject.id" value="${selectedProject.id}"/>

            <div class="row">
                <div class="col-sm">
                    <table class="table-sm table-striped table-hover key-value-table key-input pb-3">
                        <tr>
                            <td><label for="individualSelect">${g.message(code: "cellRanger.individual")}</label></td>
                            <td>
                                <g:select multiple="multiple" id="individualSelect" name="individuals" class="use-select-2" from="${allIndividuals}"
                                          value="${params?["individual.id"]?.split(',')}"
                                          optionKey="id" noSelection="${[(""): "All Individuals"]}"/>
                            </td>
                        </tr>
                        <tr>
                            <td><label for="sampleTypeSelect">${g.message(code: "cellRanger.sampleType")}</label></td>
                            <td>
                                <g:select multiple="multiple" id="sampleTypeSelect" name="sampleTypes" class="use-select-2" from="${allSampleTypes}"
                                          value="${params?["sampleType.id"]?.split(',')}"
                                          optionKey="id" noSelection="${[(""): "All Sample Types"]}"/>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <label for="referenceGenomeSelect">${g.message(code: "cellRanger.create.referenceGenomeIndex")}</label>
                            </td>
                            <td>
                                <g:select id="referenceGenomeSelect" name="referenceGenomeIndex.id" class="use-select-2"
                                          from="${referenceGenomeIndexes}"
                                          value="${createMwpCmd?.referenceGenomeIndex?.id ?: params?["reference.id"] ?: params?["referenceGenomeIndex.id"]}"
                                          optionKey="id" noSelection="${[(""): "Select a reference"]}" required="true"/>
                            </td>
                        </tr>
                        <tr>
                            <td><label for="seqTypeSelect">${g.message(code: "cellRanger.seqType")}</label></td>
                            <td>
                                <g:select id="seqTypeSelect" name="seqType.id" class="use-select-2"
                                          from="${seqTypes}" value="${createMwpCmd?.seqType?.id ?: params?["seqType.id"] ?: seqType.id}"
                                          optionKey="id" noSelection="${[(""): "Select a seq type"]}" required="true"/>
                            </td>
                        </tr>
                    </table>
                </div>

                <div class="col-sm">
                    <div class="form-group col-md-6">
                        <label for="referenceGenomeIndex2">
                            ${g.message(code: "cellRanger.create.expectedOrEnforcedCells")}
                        </label>

                        <div class="custom-control custom-radio">
                            <g:radio name="expectedOrEnforcedCells" value="neither" id="defaultExpectedCells2" class="custom-control-input"
                                     checked="${!createMwpCmd || (createMwpCmd.expectedCellsValue == null && createMwpCmd.enforcedCellsValue == null)}"/>
                            <label class="custom-control-label" for="defaultExpectedCells2">${g.message(code: "cellRanger.default")}</label>
                        </div>

                        <div class="custom-control custom-radio">
                            <g:radio name="expectedOrEnforcedCells" value="expected" class="custom-control-input" id="expectedCellsRadio2"
                                     checked="${createMwpCmd && createMwpCmd.expectedCellsValue != null && createMwpCmd.enforcedCellsValue == null}"/>

                            <label class="custom-control-label" for="expectedCellsRadio2">${g.message(code: "cellRanger.expectedCells")}</label>
                            <input name="expectedCellsValue" class="form-control" value="${createMwpCmd?.expectedCellsValue}" required/>
                        </div>

                        <div class="custom-control custom-radio">
                            <g:radio name="expectedOrEnforcedCells" value="enforced" class="custom-control-input" id="enforcedCellsRadio2"
                                     checked="${createMwpCmd && createMwpCmd.expectedCellsValue == null && createMwpCmd.enforcedCellsValue != null}"/>
                            <label class="custom-control-label" for="enforcedCellsRadio2">${g.message(code: "cellRanger.enforcedCells")}</label>
                            <input name="enforcedCellsValue" class="form-control" value="${createMwpCmd?.enforcedCellsValue}" required/>
                        </label>
                        </div>
                    </div>
                </div>
            </div>
        </g:form>

        <h3>${g.message(code: "cellRanger.header.create")}:</h3>
        <table id="sampleTable" class="table table-sm table-striped table-hover table-bordered w-100 fixed-table-header">
            <thead><tr>
                <th><input type='checkbox' name='selectAll' id="selectAll" value="${g.message(code: "cellRanger.selectAll")}" checked></th>
                <th>${g.message(code: "cellRanger.create.selectedIndividuals")}</th>
                <th>${g.message(code: "cellRanger.create.selectedSampleTypes")}</th>
                <th>${g.message(code: "cellRanger.create.selectedSeqType")}</th>
            </tr></thead>
            <tbody>
            </tbody>
            <tfoot>
            </tfoot>
        </table>
        <button id="executeButton" class="btn btn-primary" type="button">
            ${g.message(code: "cellRanger.execute")}
            <span id="executeSpinner" class="spinner-border spinner-border-sm" hidden role="status" aria-hidden="true">
                <span class="visually-hidden">Loading...</span>
            </span>
        </button>

        <h3>${g.message(code: "cellRanger.header.processes")}:</h3>
        <table id="mwpTable" class="table table-sm table-striped table-hover table-bordered w-100 fixed-table-header">
            <thead>
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
            </thead>
            <tbody>
            </tbody>
        </table>

    </g:if>
    <g:else>
        <otp:annotation type="info">${g.message(code: "cellRanger.noConfig")}</otp:annotation>
    </g:else>
</div>
</body>
</html>
