%{--
  - Copyright 2011-2020 The OTP authors
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

<%@ page import="de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage.Status" %>
<html>
<head>
    <title>${g.message(code: "otp.menu.cellRanger.finalRunSelection")}</title>
    <asset:javascript src="pages/cellRanger/finalRunSelection/finalRunSelection.js"/>
</head>

<body>
<div class="body">
    <g:set var="archived" value="${selectedProject.archived ? 'archived' : ''}"/>
    <g:render template="/templates/bootstrap/projectSelection"/>

    <g:render template="/templates/quickNavigationBar" model="[
            linkText: g.message(code: 'cellRanger.linkTo.configurationPage'),
            link    : g.createLink(controller: 'cellRangerConfiguration', action: 'index'),
            tooltip : g.message(code: 'cellRanger.linkTo.configurationPage.tooltip')
    ]"/>

    <h5><strong>${g.message(code: "otp.menu.cellRanger.finalRunSelection")}</strong></h5>

    <otp:annotation type="info">${g.message(code: "cellRanger.selection.info")}</otp:annotation>

    <g:if test="${selectedProject.archived}">
        <otp:annotation type="warning">
            <g:message code="configurePipeline.info.projectArchived.noPlot" args="[selectedProject.name]"/>
        </otp:annotation>
    </g:if>

    <g:set var="labelId" value="${0}"/>

    <table class="table table-sm table-striped table-hover">
        <tr>
            <th>${g.message(code: "cellRanger.selection.header.individual")}</th>
            <th>${g.message(code: "cellRanger.selection.header.sampleType")}</th>
            <th>${g.message(code: "cellRanger.selection.header.seqType")}</th>
            <th>${g.message(code: "cellRanger.selection.header.version")}</th>
            <th>${g.message(code: "cellRanger.selection.header.reference")}</th>
            <th>${g.message(code: "cellRanger.selection.header.runs")}</th>
            <th></th>
            <th></th>
        </tr>
        <g:each in="${groupedMwps}" var="mwps">
            <tr>
                <td>${mwps.sample.individual.displayName}</td>
                <td>${mwps.sample.sampleType.displayName}</td>
                <td>${mwps.seqType.nameWithLibraryLayout}</td>
                <td>${mwps.programVersion}</td>
                <td>${mwps.reference}</td>
                <td>
                    <g:form action="saveFinalRunSelection" class="inline-element js-confirm">
                        <input type="hidden" name="sample.id" value="${mwps.sample.id}"/>
                        <input type="hidden" name="seqType.id" value="${mwps.seqType.id}"/>
                        <input type="hidden" name="programVersion" value="${mwps.programVersion}"/>
                        <input type="hidden" name="reference.id" value="${mwps.reference.id}"/>
                        <g:each in="${mwps.mwps}" var="mwp">
                            <g:if test="${mwps.anyUnsetAndNoneFinal && mwp.status == Status.UNSET}">
                                <input type="radio" name="mwp" value="${mwp.id}" id="input-${++labelId}">
                                <label for="input-${labelId}"><g:render template="mwp" model="[mwp: mwp]"/></label>
                            </g:if>
                            <g:else>
                                &nbsp;● <g:render template="mwp" model="[mwp: mwp]"/>
                            </g:else>
                            <g:if test="${mwp.bamFileInProjectFolder && mwp.status != Status.DELETED && !archived}">
                                <g:link controller="alignmentQualityOverview" action="viewCellRangerSummary"
                                        params="['singleCellBamFile.id': mwp.bamFileInProjectFolder.id]"
                                        target="_blank">${g.message(code: "cellRanger.selection.plot")}</g:link>
                            </g:if>
                            <g:elseif test="${mwp.status == Status.UNSET}">
                                ${g.message(code: "cellRanger.selection.noPlot")}
                            </g:elseif>
                            <br>
                        </g:each>
                        <g:if test="${mwps.anyUnsetAndNoneFinal}">
                            <input type="radio" name="mwp" value="delete" id="input-${++labelId}">
                            <label for="input-${labelId}">${g.message(code: "cellRanger.selection.delete")}</label><br>
                        </g:if>
                        <g:if test="${mwps.anyUnsetAndNoneFinal}">
                            <g:submitButton class="btn btn-primary" name="${g.message(code: "cellRanger.selection.save")}"
                                            disabled="${mwps.atLeastOneInProgress}"/>
                            <g:if test="${mwps.atLeastOneInProgress}">
                                ${g.message(code: "cellRanger.selection.wait")}
                            </g:if>
                        </g:if>
                        <g:elseif test="${mwps.anyFinal}">
                            <button type="button" class="btn btn-outline-danger"
                                    data-bs-toggle="modal"
                                    data-bs-target="#confirmDeleteModal"
                                    data-sample="${mwps.sample.id}"
                                    data-seq-type="${mwps.seqType.id}"
                                    data-program-version="${mwps.programVersion}"
                                    data-reference="${mwps.reference.id}"
                                    data-mwps="${mwps.mwps*.id}">
                                <i class="bi bi-trash"></i>
                            </button>
                        </g:elseif>
                    </g:form>
                </td>
                <td class="${archived}">
                    <g:link controller="cellRangerConfiguration"
                            params="${["individual.id": mwps.sample.individual.id, "sampleType.id": mwps.sample.sampleType.id, "seqType.id": mwps.seqType.id, "reference.id": mwps.reference.id]}">
                        ${g.message(code: "cellRanger.selection.rerun")}
                    </g:link>
                </td>
                <td>
                    <g:link controller="alignmentQualityOverview" action="index" params="${[seqType: mwps.seqType.id, sample: mwps.sample.id]}">
                        ${g.message(code: "cellRanger.selection.showDetails")}
                    </g:link>
                </td>
            </tr>
        </g:each>
        <tr>
            <td colspan="10">
                <g:if test="${!groupedMwps}">
                    ${g.message(code: "cellRanger.selection.noRuns")}
                </g:if>
            </td>
        </tr>
    </table>
    <otp:otpModal modalId="confirmDeleteModal" title="${g.message(code: "cellRanger.dialog.confirmDeleteTitle")}" type="dialog" closeText="Cancel"
                  confirmText="Confirm" closable="false" submit="true">
        ${g.message(code: "cellRanger.dialog.confirmDeleteBody")}
    </otp:otpModal>
</body>
</html>
