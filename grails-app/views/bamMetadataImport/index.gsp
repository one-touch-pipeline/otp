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
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.Problems" %>
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.LogLevel" %>
<html>
<head>
    <meta name="layout" content="metadataLayout"/>
    <meta name="contextPath" content="${request.contextPath}">
    <title>
        <g:message code="bamMetadataImport.title"/>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <g:message code="bamMetadataImport.titleOperator"/>
        </sec:ifAllGranted>
    </title>
    <asset:javascript src="modules/defaultPageDependencies.js"/>
    <asset:javascript src="pages/metadataImport/index/metadataImportDataTable.js"/>
    <asset:javascript src="common/MultiInputField.js"/>
    <asset:javascript src="common/DisableOnSubmit.js"/>
</head>

<body>
<g:if test="${context}">
    <div id="border" class="borderMetadataImport${LogLevel.normalize(context.maximumProblemLevel).name}">
        <g:if test="${context.problems.isEmpty()}">
            No problems found :)
        </g:if>
        <g:else>
            <ul>
                <g:each var="problem" in="${context.problems}">
                    <li class="${LogLevel.normalize(problem.level).name}"><span style="white-space: pre-line ">${problem.levelAndMessage}</span>
                        <g:each var="cell" in="${problem.affectedCells}">
                            <a href="#${cell.cellAddress}">${cell.cellAddress}</a>
                        </g:each>
                    </li>
                </g:each>
            </ul>
        </g:else>
        <div class="fixed-scrollbar-container">
            <g:if test="${context.spreadsheet}">
                <table>
                    <thead>
                    <tr>
                        <th></th>
                        <g:each var="cell" in="${context.spreadsheet.header.cells}">
                            <th>
                                ${cell.columnAddress}
                            </th>
                        </g:each>
                    </tr>
                    <tr>
                        <th>
                            ${context.spreadsheet.header.cells.first().rowAddress}
                        </th>
                        <g:each var="cell" in="${context.spreadsheet.header.cells}">
                            <g:set var="cellProblems" value="${context.getProblems(cell)}"/>
                            <th
                                    class="${LogLevel.normalize(Problems.getMaximumProblemLevel(cellProblems)).name}"
                                    title="${cellProblems*.levelAndMessage.join('\n\n')}">
                                <span class="anchor" id="${cell.cellAddress}"></span>
                                ${cell.text}
                            </th>
                        </g:each>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each var="row" in="${context.spreadsheet.dataRows}">
                        <tr>
                            <th>
                                ${row.cells.first().rowAddress}
                            </th>
                            <g:each var="cell" in="${row.cells}">
                                <g:set var="cellProblems" value="${context.getProblems(cell)}"/>
                                <td
                                        class="${LogLevel.normalize(Problems.getMaximumProblemLevel(cellProblems)).name}"
                                        title="${cellProblems*.levelAndMessage.join('\n\n')}">
                                    <span class="anchor" id="${cell.cellAddress}"></span>
                                    ${cell.text}
                                </td>
                            </g:each>
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </g:if>
        </div>
    </div>
</g:if>
<g:render template="/templates/messages"/>
<div>
    <g:form useToken="true" controller="bamMetadataImport" action="validateOrImport">
        <table class="options">
            <tr>
                <td><g:message code="bamMetadataImport.path"/></td>
                <td><g:textField name="path" style="width: 1000px" value="${cmd.path}"/></td>
            </tr>
            <tr>
                <td><label><g:message code="bamMetadataImport.linkOperation"/></label></td>
                <td>
                    <label title="${g.message(code: 'bamMetadataImport.linkOperation.copyAndKeep.title')}">
                        <g:radio name="linkOperation"
                                 checked="${cmd?.linkOperation == null || cmd?.linkOperation == de.dkfz.tbi.otp.dataprocessing.ImportProcess.LinkOperation.COPY_AND_KEEP}"
                                 value="${de.dkfz.tbi.otp.dataprocessing.ImportProcess.LinkOperation.COPY_AND_KEEP}"/>
                        <g:message code="bamMetadataImport.linkOperation.copyAndKeep"/>
                    </label>
                    <br/>
                    <label title="${g.message(code: 'bamMetadataImport.linkOperation.copyAndLink.title')}">
                        <g:radio name="linkOperation"
                                 checked="${cmd?.linkOperation == de.dkfz.tbi.otp.dataprocessing.ImportProcess.LinkOperation.COPY_AND_LINK}"
                                 value="${de.dkfz.tbi.otp.dataprocessing.ImportProcess.LinkOperation.COPY_AND_LINK}"/>
                        <g:message code="bamMetadataImport.linkOperation.copyAndLink"/>
                    </label>
                    <br/>
                    <label title="${g.message(code: 'bamMetadataImport.linkOperation.linkSource.title')}">
                        <g:radio name="linkOperation"
                                 checked="${cmd?.linkOperation == de.dkfz.tbi.otp.dataprocessing.ImportProcess.LinkOperation.LINK_SOURCE}"
                                 value="${de.dkfz.tbi.otp.dataprocessing.ImportProcess.LinkOperation.LINK_SOURCE}"/>
                        <g:message code="bamMetadataImport.linkOperation.linkSource"/>
                    </label>
                </td>
            </tr>
            <tr>
                <td><label><g:message code="bamMetadataImport.furtherFile"/></label></td>
                <td class="multi-input-field">
                    <g:each in="${furtherFiles}" var="file" status="i">
                        <div class="field">
                            <g:textField name="furtherFilePaths" style="width: 1000px" value="${file}"/>
                            <g:if test="${i == 0}">
                                <button class="add-field">+</button>
                                <label style="color: red"><g:message code="bamMetadataImport.furtherFile.info"/></label>
                            </g:if>
                            <g:else>
                                <button class="remove-field">-</button>
                            </g:else>
                        </div>
                    </g:each>
                </td>
            </tr>
            <tr>
                <td>
                    <label><g:message code="bamMetadataImport.triggerAnalysis"/></label>
                </td>
                <td>
                    <g:checkBox name="triggerAnalysis" checked="${cmd.triggerAnalysis}" value="true"/>
                    <i><g:message code="bamMetadataImport.triggerAnalysis.info"/></i>
                </td>
            </tr>
        </table>
        <br>
        <g:submitButton id="validate" name="submit" value="Validate"/>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <g:submitButton id="import" name="submit" value="Import"/>
            <g:if test="${context?.maximumProblemLevel == LogLevel.WARNING}">
                <label>
                    <g:checkBox name="ignoreWarnings" checked="false" value="true"/>
                    <g:message code="bamMetadataImport.ignore.warning"/>
                </label>
            </g:if>
        </sec:ifAllGranted>
        <g:hiddenField name="md5" value="${context?.metadataFileMd5sum}"/>
    </g:form>
    <h3><g:message code="metadataImport.implementedValidations"/></h3>
    <ul>
        <g:each var="implementedValidation" in="${implementedValidations}">
            <li>${implementedValidation}</li>
        </g:each>
    </ul>
</div>
</body>
</html>
