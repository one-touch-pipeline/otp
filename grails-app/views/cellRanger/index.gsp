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
    <title>${g.message(code: "cellRanger.title", args: [project?.name])}</title>
    <asset:javascript src="pages/cellRanger/index/cellRanger.js"/>
</head>

<body>
    <div class="body">
        <g:render template="/templates/messages"/>
        <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]"/>

        <g:if test="${configExists}">
            <h3>${g.message(code: "cellRanger.select")}</h3>
            <g:form action="index" method="GET">
                <div class="cell-ranger-selection-grid-wrapper">
                    <div>
                        <strong>${g.message(code: "cellRanger.individual")}:</strong><br>
                        <g:select name="individual.id" from="${allIndividuals}" optionKey="id" value="${individual?.id}"
                                  noSelection="${[(""): "All Individuals"]}" onChange="submit()"/>
                    </div>
                    <div>
                        <strong>${g.message(code: "cellRanger.sampleType")}:</strong><br>
                        <g:select name="sampleType.id" from="${allSampleTypes}" optionKey="id" value="${sampleType?.id}"
                                  noSelection="${[(""): "All Sample Types"]}" onChange="submit()"/>
                    </div>
                </div>
            </g:form>
            <br>
            <g:if test="${samples}">
                <h3>${g.message(code: "cellRanger.header.create")}:</h3>
                <g:form action="create">
                    <input type="hidden" name="project.id" value="${project?.id}"/>
                    <input type="hidden" name="sampleType.id" value="${sampleType?.id}"/>
                    <input type="hidden" name="individual.id" value="${individual?.id}"/>

                    <div class="cell-ranger-creation-grid-wrapper">
                        <div class="row one">
                            <div>
                                <strong>${g.message(code: "cellRanger.create.referenceGenomeIndex")}:</strong>
                                <g:select name="referenceGenomeIndex.id" from="${referenceGenomeIndexes}" optionKey="id" noSelection="${[(""): "Select a reference"]}" value="${cmd?.referenceGenomeIndex?.id ?: referenceGenomeIndex?.id}"/>
                            </div>
                            <div>
                                <strong>${g.message(code: "cellRanger.create.seqType")}:</strong>
                                <g:select name="seqType.id" from="${seqTypes}" optionKey="id" value="${cmd?.seqType?.id}"/>
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
                                    <g:radio name="expectedOrEnforcedCells" value="neither" checked="${cmd == null || cmd.expectedOrEnforcedCells == "neither"}"/>
                                    ${g.message(code: "cellRanger.neither")}
                                </label>
                                <br>
                                <label>
                                    <g:radio name="expectedOrEnforcedCells" value="expected" checked="${cmd?.expectedOrEnforcedCells == "expected"}"/>
                                    ${g.message(code: "cellRanger.expectedCells")}
                                </label>
                                <br>
                                <label>
                                    <g:radio name="expectedOrEnforcedCells" value="enforced" checked="${cmd?.expectedOrEnforcedCells == "enforced"}"/>
                                    ${g.message(code: "cellRanger.enforcedCells")}
                                </label>
                                <br><br>
                                <label id="expectedOrEnforcedCellsValue">
                                    ${g.message(code: "cellRanger.value")}:<br>
                                    <input name="expectedOrEnforcedCellsValue" value="${cmd?.expectedOrEnforcedCellsValue}"/>
                                </label>
                            </div>
                        </div>
                        <div class="row three">
                            <g:submitButton name="Execute CellRanger"/>
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
