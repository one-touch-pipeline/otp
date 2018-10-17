<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="configurePipeline.cellRanger.title" args="[project.name, seqType.displayName]"/></title>
</head>
<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <br><g:link controller="projectConfig">Back to Overview</g:link><br><br>

    <h1 style="display: inline"><g:message code="configurePipeline.cellRanger.title" args="[project.name, seqType.displayName]"/></h1>
    <g:form action="update" params='["project.id": project.id, "seqType.id": seqType.id]'>
        <table class="pipelineTable">
            <tr>
                <th></th>
                <th></th>
                <th><g:message code="configurePipeline.header.defaultValue"/></th>
                <th><g:message code="configurePipeline.header.info"/></th>
            </tr>
            <tr>
                <td class="myKey"><g:message code="configurePipeline.version"/></td>
                <td><g:select name="programVersion" value="${defaultVersion}" from="${availableVersions}" noSelection="${["": "Select version"]}"/></td>
                <td>${defaultVersion}</td>
                <td><g:message code="configurePipeline.cellRanger.info"/></td>

            </tr>
            <tr>
                <td class="myKey"><g:message code="configurePipeline.referenceGenomeIndex"/></td>
                <td><g:select name="referenceGenomeIndex.id" value="${referenceGenomeIndex?.id}" from="${referenceGenomeIndexes}" optionKey='id' noSelection="${["": "Select Reference Genome Index"]}"/></td>
                <td></td>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <td colspan="4">&nbsp;</td>
            </tr>
            <tr>
                <td class="myKey"></td>
                <td><g:submitButton name="submit" value="Submit"/></td>
            </tr>
        </table>
    </g:form>
    <g:if test="${currentVersion}">
        <g:form controller="configurePipeline" action="invalidateConfig"
                params='["project.id": project.id, "seqType.id": seqType.id, "pipeline.id": pipeline.id, "originAction": actionName]'>
            <g:submitButton name="invalidateConfig" value="Invalidate Config"/>
        </g:form>
    </g:if>
</div>
</body>
</html>
