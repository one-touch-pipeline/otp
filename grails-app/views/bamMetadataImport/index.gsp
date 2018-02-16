<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.Problems" %>
<%@ page import="de.dkfz.tbi.util.spreadsheet.validation.Level" %>
<html>
<head>
    <meta name="layout" content="metadataLayout" />
    <title>
        <g:message code="bamMetadataImport.title"/>
        <sec:ifAllGranted roles="ROLE_OPERATOR">
            <g:message code="bamMetadataImport.titleOperator"/>
        </sec:ifAllGranted>
    </title>
    <asset:javascript src="modules/defaultPageDependencies.js"/>
    <asset:javascript src="pages/bamMetadataImport/index/addFurtherFile.js"/>
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
    <g:form controller="BamMetadataImport" action="index">
        <table class="options">
            <tr>
                <td><g:message code="bamMetadataImport.path"/></td>
                <td><g:textField name="path" style="width: 1000px" value="${cmd.path}"/></td>
            </tr>
            <tr>
                <td><label><g:message code="bamMetadataImport.replaceWithLink"/></label></td>
                <td><g:checkBox name="replaceWithLink" checked="${cmd.replaceWithLink}"/></td>
            </tr>
            <tr>
                <td><label><g:message code="bamMetadataImport.furtherFile"/></label></td>
                <td class="input-fields-wrap">
                    <input type="text" style="width:600px" name="furtherFilePaths"><button class="add-field-button">+</button>
                    <label style="color: red"><g:message code="bamMetadataImport.furtherFile.info"/></label>
                </td>
            </tr>
            <tr>
                <td><label><g:message code="bamMetadataImport.triggerAnalysis"/></label></td>
                <td><g:checkBox name="triggerAnalysis" checked="${cmd.triggerAnalysis}"/><i><g:message code="bamMetadataImport.triggerAnalysis.info"/></i></td>
            </tr>
        </table>
        <br>
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
<asset:script>
    $(function() {
        $.otp.bamMetadataImport.addValues();
        $.otp.bamMetadataImport.returnValues([${raw(cmd.furtherFilePaths.collect{"'$it'"}.join(', '))}]);
    });
</asset:script>
</body>
</html>
