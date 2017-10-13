<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="configurePipeline.alignment.title" args="[project.name, seqType.displayName]"/></title>
    <asset:javascript src="pages/configurePipeline/alignment/configureAlignment.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
    <div class="body">
        <h1 style="display: inline"><g:message code="configurePipeline.alignment.title" args="[project.name, seqType.displayName]"/></h1>
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
        <g:if test="${projects}">
            <g:form controller="configurePipeline" action="alignment"
                    params='["project.id": project.id, "seqType.id": seqType.id]'>
                <span class="blue_label"><g:message code="configurePipeline.alignment.copy"/></span>

                <g:select class="criteria" id="project_select" name='basedProject.id'
                          from='${projects}' optionKey='id' optionValue='name'/>
                <g:submitButton name="copy" value="Copy"/>
            </g:form>
            <br>
        </g:if>
        <g:message code="configurePipeline.alignment.info"/>
        <g:form controller="configurePipeline" action="alignment" params='["project.id": project.id, "seqType.id": seqType.id]'>
            <table class="alignmentTable">
                <tr>
                    <th></th>
                    <th></th>
                    <th><g:message code="configurePipeline.header.defaultValue"/></th>
                    <th><g:message code="configurePipeline.header.info"/></th>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.alignment.genome"/></td>
                    <td><g:select class="genome" name="referenceGenome" from="${referenceGenomes}" value="${referenceGenome}"/></td>
                    <td>${defaultReferenceGenome}</td>
                    <td><g:message code="configurePipeline.alignment.genome.info"/></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.alignment.statSizeFileName"/></td>
                    <td><select id="statSizeFileNames" name="statSizeFileName" value=""${statSizeFileName}/></td>
                    <td>&nbsp;</td>
                    <td><g:message code="configurePipeline.alignment.statSizeFileName.info"/></td>
                </tr>
                <g:if test="${!isWgbs}">
                    <tr>
                        <td class="myKey"><g:message code="configurePipeline.alignment.bwaVersion"/></td>
                        <td><g:select name="bwaMemVersion" from="${bwaMemVersions}" value="${bwaMemVersion}"/></td>
                        <td>${defaultBwaMemVersion}</td>
                        <td>&nbsp;</td>
                    </tr>
                </g:if>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.alignment.mergeTool"/></td>
                    <td><g:select name="mergeTool" from="${mergeTools}" value="${mergeTool}"/></td>
                    <td>${defaultMergeTool}</td>
                    <td><g:message code="configurePipeline.alignment.mergeTool.info"/></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.alignment.sambambaVersion"/></td>
                    <td><g:select name="sambambaVersion" from="${sambambaVersions}" value="${sambambaVersion}"/></td>
                    <td>${defaultSambambaVersion}</td>
                    <td><g:message code="configurePipeline.alignment.sambambaVersion.info"/></td>
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
                <g:if test="${!isWgbs && !isChipSeq}">
                    <tr>
                        <td class="myKey"><g:message code="configurePipeline.adapterTrimmingNeeded"/></td>
                        <td><g:checkBox name="adapterTrimmingNeeded" checked="false"/></td>
                        <td>-</td>
                        <td>&nbsp;</td>
                    </tr>
                </g:if>
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
    <asset:script>
        $(function() {
            $.otp.configureAlignment.register();
        });
    </asset:script>
</body>
</html>
