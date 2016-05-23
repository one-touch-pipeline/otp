<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
<title><g:message code="configureAlignment.title" args="[projectName, seqType.displayName]"/></title>
<asset:javascript src="pages/configureAlignment/index/configureAlignment.js"/>
</head>
<body>
    <div class="body">
        <h1><g:message code="configureAlignment.title" args="[projectName, seqType.displayName]"/></h1>
        <g:if test="${hasErrors}">
            <div class="errors"> <li>${message}</li></div>
        </g:if>
        <g:elseif test="${message}">
            <div class="message">${message}</div>
        </g:elseif>
        <g:else>
            <div class="empty"><br></div>
        </g:else>
        <g:message code="configureAlignment.info"/>
        <g:form controller="ConfigureAlignment" action="index" params='[projectName: projectName, seqTypeName: seqType.name, libraryLayout: seqType.libraryLayout]'>
            <table class="alignmentTable">
                <tr>
                    <th></th>
                    <th></th>
                    <th><g:message code="configureAlignment.header.defaultValue"/></th>
                    <th><g:message code="configureAlignment.header.info"/></th>
                </tr>
                <tr class="default">
                    <td class="myKey"><g:message code="configureAlignment.genome"/></td>
                    <td><g:select class="genome" name="referenceGenome" from="${referenceGenomes}" value="${referenceGenome}"/></td>
                    <td>${defaultReferenceGenome}</td>
                    <td><g:message code="configureAlignment.genome.info"/></td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.statSizeFileName"/></td>
                    <td><select id="statSizeFileNames" name="statSizeFileName" value=""${statSizeFileName}/></td>
                    <td>&nbsp;</td>
                    <td><g:message code="configureAlignment.statSizeFileName.info"/></td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.mergeTool"/></td>
                    <td><g:select name="mergeTool" from="${mergeTools}" value="${mergeTool}"/></td>
                    <td>${defaultMergeTool}</td>
                    <td><g:message code="configureAlignment.mergeTool.info"/></td>
                </tr>
                <tr class="pancan">
                    <td colspan="4">&nbsp;</td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.plugin.name"/></td>
                    <td><g:textField name="pluginName" value="${pluginName}"/></td>
                    <td>${defaultPluginName}</td>
                    <td>&nbsp;</td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.plugin.version"/></td>
                    <td><g:textField name="pluginVersion" value="${pluginVersion}"/></td>
                    <td>${defaultPluginVersion}</td>
                    <td>&nbsp;</td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.base.project"/></td>
                    <td><g:textField name="baseProjectConfig" value="${baseProjectConfig}"/></td>
                    <td>${defaultBaseProjectConfig}</td>
                    <td>&nbsp;</td>
                </tr>
                <tr class="pancan">
                    <td class="myKey"><g:message code="configureAlignment.config"/></td>
                    <td><g:textField name="config" value="${config}"/></td>
                    <td>-</td>
                    <td><g:message code="configureAlignment.config.info"/></td>
                </tr>
                <tr class="pancan">
                    <td colspan="4">&nbsp;</td>
                </tr>
                <tr>
                    <td class="myKey"></td>
                    <td><g:submitButton name="submit" value="Submit"/></td>
                </tr>
            </table>
        </g:form>
        <g:if test="${lastRoddyConfig}">
            <g:message code="configureAlignment.last.config"/>
            <code style="white-space: pre-wrap">
                ${lastRoddyConfig}
            </code>
        </g:if>
    </div>
    <asset:script>
        $(function() {
            $.otp.configureAlignment.register();
        });
    </asset:script>
</body>
</html>
