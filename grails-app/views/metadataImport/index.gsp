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
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.LogLevel; de.dkfz.tbi.util.spreadsheet.validation.Problems" %>
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.LogLevel" %>
<html>
<head>
    <meta name="layout" content="metadataLayout"/>
    <meta name="contextPath" content="${request.contextPath}">
    <title>
    <g:message code="metadataImport.title"/>
    <sec:ifAllGranted roles="ROLE_OPERATOR">
        <g:message code="metadataImport.titleOperator"/>
    </sec:ifAllGranted>
    </title>
    <asset:javascript src="modules/defaultPageDependencies.js"/>
    <asset:javascript src="pages/metadataImport/index/metadataImportDataTable.js"/>
    <asset:javascript src="common/MultiInputField.js"/>
    <asset:javascript src="common/DisableOnSubmit.js"/>
</head>

<body>
<g:if test="${contexts}">
    <g:each var="context" in="${contexts}">
        <div id="border" class="borderMetadataImport${de.dkfz.tbi.util.spreadsheet.validation.LogLevel.normalize(context.maximumProblemLevel).name}">
            <g:if test="${context.problems.isEmpty()}">
                No problems found :)
            </g:if>
            <g:else>
                <ul>
                    <g:each var="problem" in="${context.problems}">
                        <li class="${LogLevel.normalize(problem.level).name}"><span style="white-space: pre-wrap">${problem.levelAndMessage}</span>
                            <g:each var="cell" in="${problem.affectedCells}">
                                <a href="#${cell.cellAddress}">${cell.cellAddress}</a>
                            </g:each>
                        </li>
                    </g:each>
                </ul>
                <h4><g:message code="metadataImport.summary"/></h4>
                <ul>
                    <g:each var="problemType" in="${context.summary}">
                        <li><span style="white-space: pre">${problemType}</span></li>
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
                                <th class="${LogLevel.normalize(Problems.getMaximumProblemLevel(cellProblems)).name}"
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
                                    <td class="${LogLevel.normalize(Problems.getMaximumProblemLevel(cellProblems)).name}"
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
    </g:each>
</g:if>
<g:render template="/templates/messages"/>
<div>
    <g:form useToken="true" controller="metadataImport" action="validateOrImport">
        <table class="options">
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <tr>
                    <td><g:message code="metadataImport.otrs"/></td>
                    <td>
                        <g:textField name="ticketNumber" size="30" required="true" value="${cmd.ticketNumber}"/>&nbsp;&nbsp;&nbsp;
                        <g:checkBox name="automaticNotification" checked="${cmd.automaticNotification}" value="true"/>
                        <g:message code="metadataImport.otrs.automaticNotificationFlag"/></td>
                </tr>
                <tr>
                    <td><g:message code="metadataImport.otrs.seqCenter.comment"/></td>
                    <td>
                        <g:textArea name="seqCenterComment" rows="5" style="width: 1000px" value="${cmd.seqCenterComment}"/>
                    </td>
                </tr>
            </sec:ifAllGranted>
            <tr>
                <td><g:message code="metadataImport.path"/></td>
                <td class="multi-input-field">
                    <g:each in="${paths}" var="path" status="i">
                        <div class="field">
                            <g:textField name="paths" style="width: 1000px" value="${path}"/>
                            <g:if test="${i == 0}">
                                <button class="add-field">+</button>
                            </g:if>
                            <g:else>
                                <button class="remove-field">-</button>
                            </g:else>
                        </div>
                    </g:each>
                </td>
            </tr>
            <tr>
                <td><g:message code="metadataImport.directoryStructure"/></td>
                <td>
                    <g:set var="active" value="${cmd.directoryStructure ?: directoryStructures.first().name()}"/>
                    <g:radioGroup name="directoryStructure" labels="${directoryStructures*.displayName}" values="${directoryStructures*.name()}"
                                  value="${active}">
                        <label>
                            ${it.radio}
                            <g:message code="${it.label}"/>
                        </label>
                    </g:radioGroup>
                </td>
            </tr>
            <tr>
                <td><label><g:message code="runSubmit.align"/></label></td>
                <td><g:checkBox name="align" checked="${cmd.align}" value="true"/></td>
            </tr>
            <g:if test="${cmd.paths}">
                <tr>
                    <td><label><g:message code="metadataImport.ignoreMd5sumError.label"/></label></td>
                    <td>
                        <g:checkBox name="ignoreMd5sumError" checked="${cmd.ignoreMd5sumError}" value="true"/>
                    </td>
                </tr>
            </g:if>
        </table>
        <g:submitButton id="validate" name="submit" value="Validate"/>
        <g:each var="context" in="${contexts}">
            <g:hiddenField name="md5" value="${context?.metadataFileMd5sum}"/>
        </g:each>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <g:if test="${isValidated && !(problems > LogLevel.WARNING.intValue())}">
                <g:submitButton id="import" name="submit" value="Import"/>
            </g:if>
            <g:if test="${problems == LogLevel.WARNING.intValue()}">
                <label>
                    <g:checkBox name="ignoreWarnings" checked="false" value="true"/>
                    <g:message code="metadataImport.ignore"/>
                </label>
            </g:if>
        </sec:ifAllGranted>
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
