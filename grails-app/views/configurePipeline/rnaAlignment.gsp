<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="configurePipeline.rnaAlignment.title" args="[project.name, seqType.displayName]"/></title>
    <asset:javascript src="pages/configurePipeline/alignment/configureAlignment.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
<div class="body">
    <h1><g:message code="configurePipeline.rnaAlignment.title" args="[project.name, seqType.displayName]"/></h1>
    <g:if test="${hasErrors}">
        <div class="errors"> <li>${message}</li></div>
    </g:if>
    <g:elseif test="${message}">
        <div class="message">${message}</div>
    </g:elseif>
    <g:else>
        <div class="empty"><br></div>
    </g:else>
    <g:message code="configurePipeline.alignment.info"/>
    <g:form controller="configurePipeline" action="rnaAlignment" params='["project.id": project.id, "seqType.id": seqType.id]'>
        <table class="alignmentTable">
            <tr>
                <th></th>
                <th></th>
                <th><g:message code="configurePipeline.header.defaultValue"/></th>
                <th><g:message code="configurePipeline.header.info"/></th>
            </tr>
            <tr>
                <td class="myKey"><g:message code="configurePipeline.rnaAlignment.genome"/></td>
                <td><g:select class="genome" name="referenceGenome" from="${referenceGenomes}" value="${referenceGenome}"/></td>
                <td>${defaultReferenceGenome}</td>
                <td>&nbsp;</td>
            </tr>
            <g:each in="${toolNames}">
                <tr>
                    <td class="myKey">${it}:</td>
                    <td>
                        <g:select name="toolVersionValue" from="${indexToolVersion}" class="dropDown toolVersionSelect_${it}"/>
                    </td>
                    <td>&nbsp;</td>
                    <td>&nbsp;</td>
                </tr>
            </g:each>

            <tr class="geneModel">
                <td valign="top" class="myKey">
                    <g:message code="configurePipeline.rnaAlignment.geneModel"/>
                </td>
                <td valign="top" class="geneModel">
                    <g:select name="geneModelValues" from="${geneModel}" class="dropDown geneModelSelect" noSelection="[(null): '']" /><br>
                    <g:select name="geneModelValues" from="${geneModel}" class="dropDown geneModelSelect hidden" noSelection="[(null): '']" />
                </td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
            </tr>
            <tr class="newGeneModel">
                <td>&nbsp;</td>
                <td>
                    <div class="addGeneModel"><button class="buttons"><g:message code="configurePipeline.rnaAlignment.newGeneModel"/></button></div>
                </td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <td colspan="4">&nbsp;</td>
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
        <g:message code="configurePipeline.last.config"/>
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
