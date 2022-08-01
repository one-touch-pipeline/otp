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
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="configurePipeline.alignment.title" args="[selectedProject.name, seqType.displayName]"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/configurePipeline/alignment/configureAlignment.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>
<body>
    <div class="body">
        <g:set var="archived" value="${selectedProject.archived ? 'archived' : ''}"/>

        <g:render template="/templates/messages"/>

        <h1><g:message code="configurePipeline.alignment.title" args="[selectedProject.name, seqType.displayName]"/></h1>

        <g:if test="${projects}">
            <g:form class="rounded-page-header-box" controller="configurePipeline" action="copyAlignment" method="POST"
                    params='["seqType.id": seqType.id]'>
                <g:message code="configurePipeline.alignment.copy"/>

                <g:select class="use-select-2" id="project_select" name='basedProject.id'
                          from='${projects}' optionKey='id' optionValue='name'/>
                <g:submitButton name="copy" value="Copy"/>
            </g:form>
        </g:if>

        <otp:annotation type="info"><g:message code="configurePipeline.info.defaultValues.exceptIndexes"/></otp:annotation>
        <g:if test="${archived}">
            <otp:annotation type="warning">
                <g:message code="configurePipeline.info.projectArchived.noChange" args="[selectedProject.name]"/>
            </otp:annotation>
        </g:if>


        <g:form controller="configurePipeline" action="saveAlignment" params='["seqType.id": seqType.id]' method="POST">
            <table class="alignmentTable">
                <tr>
                    <th></th>
                    <th></th>
                    <th><g:message code="configurePipeline.header.defaultValue"/></th>
                    <th><g:message code="configurePipeline.header.info"/></th>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.alignment.genome"/></td>
                    <td><g:select class="genome use-select-2" name="referenceGenome" from="${referenceGenomes}" value="${referenceGenome}"/></td>
                    <td>${defaultReferenceGenome}</td>
                    <td><g:message code="configurePipeline.alignment.genome.info"/></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.alignment.statSizeFileName"/></td>
                    <td><select class="use-select-2" id="statSizeFileNames" name="statSizeFileName" value=""${statSizeFileName}/></td>
                    <td>&nbsp;</td>
                    <td><g:message code="configurePipeline.alignment.statSizeFileName.info"/></td>
                </tr>
                <g:if test="${!isWgbs}">
                    <tr>
                        <td class="myKey"><g:message code="configurePipeline.alignment.bwaVersion"/></td>
                        <td><g:select class="use-select-2" name="bwaMemVersion" from="${bwaMemVersions}" value="${bwaMemVersion}"/></td>
                        <td>${defaultBwaMemVersion}</td>
                        <td>&nbsp;</td>
                    </tr>
                </g:if>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.alignment.mergeTool"/></td>
                    <td><g:select class="use-select-2" name="mergeTool" from="${mergeTools}" value="${mergeTool}"/></td>
                    <td>${defaultMergeTool}</td>
                    <td><g:message code="configurePipeline.alignment.mergeTool.info"/></td>
                </tr>
                <tr>
                    <td class="myKey"><g:message code="configurePipeline.alignment.sambambaVersion"/></td>
                    <td><g:select class="use-select-2" name="sambambaVersion" from="${sambambaVersions}" value="${sambambaVersion}"/></td>
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
                    <td><g:textField name="programVersion" value="${programVersion}"/></td>
                    <td>${defaultProgramVersion}</td>
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
                    <td><g:textField name="config" value="${nextConfigVersion}"/></td>
                    <td>-</td>
                    <td><g:message code="configurePipeline.config.info"/></td>
                </tr>
                <g:if test="${!isWgbs && !isChipSeq}">
                    <tr>
                        <td class="myKey"><g:message code="configurePipeline.adapterTrimmingNeeded"/></td>
                        <td><g:checkBox name="adapterTrimmingNeeded" checked="false" value="true"/></td>
                        <td>-</td>
                        <td>&nbsp;</td>
                    </tr>
                </g:if>
                <tr>
                    <td colspan="4">&nbsp;</td>
                </tr>
                <tr>
                    <td class="myKey"></td>
                    <td>
                        <g:submitButton class="${archived}" name="submit" value="Submit"/>
                        <g:link controller="alignmentConfigurationOverview" class="btn">${g.message(code: "default.button.cancel.label")}</g:link>
                    </td>
                </tr>
            </table>
        </g:form>
        <g:if test="${configState.content}">
            <h2><g:message code="configurePipeline.current.config"/></h2>
            <g:form controller="configurePipeline" action="invalidateConfig" method="POST"
                    params='["seqType.id": seqType.id, "pipeline.id": pipeline.id, "originAction": actionName, overviewController: "alignmentConfigurationOverview"]'>
                <g:submitButton class="${archived}" name="invalidateConfig" value="Invalidate Config"/>
            </g:form>
            <g:if test="${configState.changed}">
                <otp:annotation type="warning"><g:message code="configurePipeline.current.config.changed"/></otp:annotation>
            </g:if>
            <code style="white-space: pre-wrap">
                ${configState.content}
            </code>
        </g:if>
    </div>
</body>
</html>
