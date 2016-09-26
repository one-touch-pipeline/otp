<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.Problems" %>
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.Level" %>
<html>
<head>
    <meta name="layout" content="metadataLayout" />
    <title>
        <g:message code="metadataImport.title"/>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <g:message code="metadataImport.titleOperator"/>
        </sec:ifAllGranted>
    </title>
</head>
<body>
    <g:if test="${context}">
        <div id= "border" class="borderMetadataImport${Level.normalize(context.getMaximumProblemLevel()).name}">
            <g:if test="${context.problems.isEmpty()}">
                No problems found :)
            </g:if>
            <g:else>
                <ul>
                    <g:each var="problem" in="${context.problems}" >
                        <li class="${Level.normalize(problem.level).name}"><span style="white-space: pre-line "> ${problem.getLevelAndMessage()}</span>
                            <g:each var="cell" in="${problem.affectedCells}" >
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
                                <g:each var="cell" in="${context.spreadsheet.header.cells}" >
                                    <th>
                                        ${cell.columnAddress}
                                    </th>
                                </g:each>
                            </tr>
                            <tr>
                                <th>
                                    ${context.spreadsheet.header.cells.first().rowAddress}
                                </th>
                                <g:each var="cell" in="${context.spreadsheet.header.cells}" >
                                    <g:set var="cellProblems" value ="${context.getProblems(cell)}"/>
                                    <th
                                        class="${Level.normalize(Problems.getMaximumProblemLevel(cellProblems)).name}"
                                        title="${cellProblems*.getLevelAndMessage().join('\n\n')}"
                                    >
                                        <span class="anchor" id="${cell.cellAddress}"></span>
                                        ${cell.text}
                                    </th>
                                </g:each>
                            </tr>
                        </thead>
                        <tbody>
                            <g:each var="row" in="${context.spreadsheet.dataRows}" >
                                <tr>
                                    <th>
                                        ${row.cells.first().rowAddress}
                                    </th>
                                    <g:each var="cell" in="${row.cells}" >
                                        <g:set var="cellProblems" value ="${context.getProblems(cell)}"/>
                                        <td
                                            class="${Level.normalize(Problems.getMaximumProblemLevel(cellProblems)).name}"
                                            title="${cellProblems*.getLevelAndMessage().join('\n\n')}"
                                        >
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
    <g:if test="${errorMessage}">
        <div class="errorMessage"><g:message code="${errorMessage}"/></div>
    </g:if>
    <div>
    <g:form controller="MetadataImport" action="index">
        <table class="options">
            <sec:ifAllGranted roles="ROLE_OPERATOR">
                <tr>
                    <td><g:message code="metadataImport.otrs"/></td>
                    <td>
                        <g:textField name="ticketNumber" size="30" value="${cmd.ticketNumber}"/>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="metadataImport.otrs.seqcenter.comment"/></td>
                    <td>
                        <g:textArea name="seqCenterComment" rows="5" style="width: 1000px" value="${cmd.seqCenterComment}"/>
                    </td>
                </tr>
            </sec:ifAllGranted>
            <tr>
                <td><g:message code="metadataImport.path"/></td>
                <td><g:textField name="path" style="width: 1000px" value="${cmd.path}"/></td>
            </tr>
            <tr>
                <td><g:message code="metadataImport.directory"/></td>
                <td>
                    <g:set var="active" value="${cmd.directory ?: directoryStructures.keySet().first()}"/>
                    <g:radioGroup name="directory" labels="${directoryStructures.values()}" values="${directoryStructures.keySet()}" value="${active}">
                        <label>
                            ${it.radio}
                            <g:message code="${it.label}"/>
                         </label>
                    </g:radioGroup>
                </td>
            </tr>
            <tr>
                <td><label><g:message code="runSubmit.align"/></label></td>
                <td><g:checkBox name="align" checked="${cmd.align}"/></td>
            </tr>
        </table>
        <g:submitButton name="submit" value="Validate"/>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <g:submitButton name="submit" value="Import"/>
            <g:if test="${context?.getMaximumProblemLevel() == Level.WARNING}">
                <label>
                    <g:checkBox name="ignoreWarnings"/>
                    Ignore Warnings
                </label>
            </g:if>
        </sec:ifAllGranted>
        <g:hiddenField name="md5" value="${context?.metadataFileMd5sum}"/>
    </g:form>
    <h3><g:message code="metadataImport.implementedValidations"/></h3>
        <ul>
            <g:each var="implementedValidation" in="${implementedValidations}" >
                <li>${implementedValidation}</li>
            </g:each>
        </ul>
    </div>
</body>
</html>
