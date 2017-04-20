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
    <g:if test="${hasErrors == 'true'}">
        <div class="errors"><li>${message}</li></div>
    </g:if>
    <g:elseif test="${message}">
        <div class="message">${message}</div>
    </g:elseif>
    <g:else>
        <div class="empty"><br></div>
    </g:else>
    <g:if test="${lastRoddyConfig}">
        <h2><g:message code="configurePipeline.alignment.configFile.invalid"/></h2>
        <g:form controller="configurePipeline" action="rnaAlignmentConfigInvalid"
                params='["project.id": project.id, "seqType.id": seqType.id]'>
            <g:submitButton name="invalidConfig" value="Invalid Config"/>
        </g:form>
    </g:if>
    <h2><g:message code="configurePipeline.alignment.configFile"/></h2>
    <g:message code="configurePipeline.alignment.info.config.rna"/>
    <g:form controller="configurePipeline" action="rnaAlignmentConfig"
            params='["project.id": project.id, "seqType.id": seqType.id]'>
        <table class="alignmentTable">
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

    <h2><g:message code="configurePipeline.alignment.referenceGenome"/></h2>
    <g:message code="configurePipeline.alignment.info"/>
    <g:form controller="configurePipeline" action="rnaAlignmentReferenceGenome"
            params='["project.id": project.id, "seqType.id": seqType.id]'>
        <table class="alignmentTable">
            <tr>
                <th></th>
                <th></th>
                <th><g:message code="configurePipeline.header.defaultValue"/></th>
                <th><g:message code="configurePipeline.header.info"/></th>
            </tr>
            <tr>
                <td valign="top" class="myKey"><g:message code="configurePipeline.mouse.data"/></td>
                <td><g:checkBox name="mouseData"/></td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="configurePipeline.rnaAlignment.genome"/></td>
                <td><g:select class="genome" name="referenceGenome" from="${referenceGenomes}"
                              value="${referenceGenome}"/></td>
                <td>${defaultReferenceGenome}</td>
                <td>&nbsp;</td>
            </tr>
            <g:each in="${toolNames}">
                <tr>
                    <td class="myKey">${it}:</td>
                    <td>
                        <g:select name="toolVersionValue" from="${indexToolVersion}"
                                  class="dropDown toolVersionSelect_${it}"/>
                    </td>
                    <g:if test="${it == "GENOME_STAR_INDEX"}">
                        <td>${defaultGenomeStarIndex}</td>
                    </g:if><g:else>
                    <td>&nbsp;</td>
                </g:else>
                    <td>&nbsp;</td>
                </tr>
            </g:each>

            <tr class="geneModel">
                <td valign="top" class="myKey">
                    <g:message code="configurePipeline.rnaAlignment.geneModel"/>
                </td>
                <td valign="top" class="geneModel">
                    <g:select name="geneModel.id" from="${geneModel}" class="dropDown geneModelSelect"
                              noSelection="[(null): '']"/><br>
                </td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <td colspan="4">&nbsp;</td>
            </tr>
            <g:each in="${possibleSampleTypes}" var="entry">
                <tr>
                    <td class="myKey"><g:message code="${entry.name}"/></td>
                    <td>
                        <ul>
                            <g:each in="${entry.values}" var="sampleType">
                                <li>
                                    <g:checkBox name="sampleTypeIds" value="${sampleType.id}"
                                                checked="false"/>${sampleType.name}
                                </li>
                            </g:each>
                        </ul>
                    <td>&nbsp;</td>
                    <td><g:message code="${entry.info}"/></td>
                </tr>
            </g:each>
            <tr>
                <td colspan="4">&nbsp;</td>
            </tr>
            <tr>
                <td class="myKey"></td>
                <td><g:submitButton name="submit" value="Submit"/></td>
                <td>&nbsp;</td>
                <td><g:message code="configurePipeline.sample.type.info"/></td>
            </tr>
        </table>
    </g:form>

    <g:if test="${lastRoddyConfig}">
        <h2><g:message code="configurePipeline.last.config"/></h2>
        <code style="white-space: pre-wrap">
            ${lastRoddyConfig}
        </code>
    </g:if>

    <br>
    <g:form controller="projectOverview" action="specificOverview" params='[project: project.name]'>
        <g:submitButton name="back" value="Back to Overview"/>
    </g:form>
</div>
<asset:script>
    $(function() {
        $.otp.configureAlignment.register();
    });
</asset:script>
</body>
</html>
