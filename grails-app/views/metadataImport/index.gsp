<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.Problems" %>
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.Level" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<asset:stylesheet src="metadataImport/metadataImport.css"/>
<title>
    <g:message code="metadataImport.title"/>
    <sec:ifAllGranted roles="ROLE_OPERATOR">
        <g:message code="metadataImport.titleOperator"/>
    </sec:ifAllGranted>
</title>
</head>
<body>
    <g:if test="${cmd}">
        <div id= "border" class="borderMetadataImport${Level.normalize(context.getMaximumProblemLevel()).name}">
            <g:if test="${context.problems.isEmpty()}">
                No problems found :)
            </g:if>
            <g:else>
                <ul>
                <g:each var="problem" in="${context.problems}" >
                    <li class="${Level.normalize(problem.level).name}">${problem.getLevelAndMessage().encodeAsHTML().replace('\n', '<br/>')}
                        <g:each var="cell" in="${problem.affectedCells}" >
                            <a href="#${cell.cellAddress}">${cell.cellAddress}</a>
                        </g:each>
                    </li>
                </g:each>
                </ul>
            </g:else>
            <p class="table">
            <g:if test="${context.spreadsheet}">
                <table>
                    <thead>
                        <tr>
                            <g:each var="cell" in="${context.spreadsheet.header.cells}" >
                                <g:set var="cellProblems" value ="${context.getProblems(cell)}"/>
                                <th
                                    id="${cell.cellAddress}"
                                    class="${Level.normalize(Problems.getMaximumProblemLevel(cellProblems)).name}"
                                    title="${cellProblems*.getLevelAndMessage().join('\n\n').encodeAsHTML()}"
                                >
                                    ${cell.text.encodeAsHTML()}
                                </th>
                            </g:each>
                        </tr>
                    </thead>
                    <tbody>
                        <g:each var="row" in="${context.spreadsheet.dataRows}" >
                            <tr>
                                <g:each var="cell" in="${row.cells}" >
                                    <g:set var="cellProblems" value ="${context.getProblems(cell)}"/>
                                    <td
                                        id="${cell.cellAddress}"
                                        class="${Level.normalize(Problems.getMaximumProblemLevel(cellProblems)).name}"
                                        title="${cellProblems*.getLevelAndMessage().join('\n\n').encodeAsHTML()}"
                                    >
                                        ${cell.text.encodeAsHTML()}
                                    </td>
                                </g:each>
                            </tr>
                        </g:each>
                    </tbody>
                </table>
            </g:if>
        </div>
    </g:if>
    <div>
    <g:form controller="MetadataImport" action="index">
        <table>
            <tr>
                <td><g:message code="metadataImport.path"/></td>
                <td><g:textField name="path" size="130" value="${cmd?.path}"/></td>
            </tr>
            <tr>
                <td><g:message code="metadataImport.directory"/></td>
                <td>
                <g:if test="${cmd}">
                    <g:set var="active" value ="${cmd?.directory}"/>
                </g:if>
                <g:else>
                    <g:set var="active" value ="${directoryStructures.keySet().first()}"/>
                </g:else>
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
                <td><g:checkBox name="align" checked="${cmd == null || cmd?.align}"/></td>
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
        <g:if test="${cmd}">
            <g:hiddenField name="md5" value="${context.metadataFileMd5sum}"/>
        </g:if>
    </g:form>
    <h3><g:message code="metadataImport.implementedValidations"/></h3>
        <ul>
            <g:each var="implementedValidation" in="${implementedValidations}" >
                <li>${implementedValidation.encodeAsHTML()}</li>
            </g:each>
        </ul>
    </div>
</body>
</html>
