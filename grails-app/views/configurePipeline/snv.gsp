<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="configurePipeline.snv.title" args="[project.name, seqType.displayName]"/></title>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>

        <h1 style="display: inline"><g:message code="configurePipeline.snv.title" args="[project.name, seqType.displayName]"/></h1>
        <g:form controller="projectConfig" style="display: inline; float: right">
            <g:submitButton name="back" value="Back to Overview"/>
        </g:form>
        <g:if test="${hasErrors == true}">
            <div class="errors"> <li>${message}</li></div>
        </g:if>
        <g:elseif test="${message}">
            <div class="message">${message}</div>
        </g:elseif>
        <g:else>
            <div class="empty"><br></div>
        </g:else>
        <g:message code="configurePipeline.info"/>
        <div><g:message code="configurePipeline.note.human"/></div>
        <g:form controller="configurePipeline" action="snv" params='["project.id": project.id, "seqType.id": seqType.id]'>
            <table class="pipelineTable">
                <tr>
                    <th></th>
                    <th></th>
                    <th><g:message code="configurePipeline.header.defaultValue"/></th>
                    <th><g:message code="configurePipeline.header.info"/></th>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.plugin.name"/></td>
                    <td><g:textField name="pluginName" value="${pluginName}"/></td>
                    <td>${defaultPluginName}</td>
                    <td>&nbsp;</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.plugin.version"/></td>
                    <td><g:textField name="pluginVersion" value="${pluginVersion}"/></td>
                    <td>${defaultPluginVersion}</td>
                    <td>&nbsp;</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.base.project"/></td>
                    <td><g:textField name="baseProjectConfig" value="${baseProjectConfig}"/></td>
                    <td>${defaultBaseProjectConfig}</td>
                    <td>&nbsp;</td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.config"/></td>
                    <td><g:textField name="config" value="${config}"/></td>
                    <td>-</td>
                    <td><g:message code="configurePipeline.config.info"/></td>
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
        <g:if test="${lastRoddyConfig}">
            <h2><g:message code="configurePipeline.last.config"/></h2>
            <g:form controller="configurePipeline" action="invalidateConfig"
                    params='["project.id": project.id, "seqType.id": seqType.id, "pipeline.id": pipeline.id, "originAction": actionName]'>
                <g:submitButton name="invalidateConfig" value="Invalidate Config"/>
            </g:form>
            <code style="white-space: pre-wrap">
                ${lastRoddyConfig}
            </code>
        </g:if>
    </div>
</body>
</html>
