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
    <title><g:message code="configurePipeline.rnaAlignment.title" args="[selectedProject.name, seqType.displayName]"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="pages/configurePipeline/alignment/configureAlignment.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>
<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <h1><g:message code="configurePipeline.rnaAlignment.title" args="[selectedProject.name, seqType.displayName]"/></h1>

    <h2><g:message code="configurePipeline.alignment.configFile"/></h2>

    <otp:annotation type="info"><g:message code="configurePipeline.info.defaultValues"/></otp:annotation>

    <g:form controller="configurePipeline" action="rnaAlignmentConfig" method="POST"
            params='["seqType.id": seqType.id]'>
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
            <tr>
                <td colspan="4">&nbsp;</td>
            </tr>
            <tr>
                <td class="myKey"></td>
                <td>
                    <g:submitButton name="submit" value="Submit"/>
                    <g:link controller="alignmentConfigurationOverview" class="btn">${g.message(code: "default.button.cancel.label")}</g:link>
                </td>
            </tr>
        </table>
    </g:form>

    <h2><g:message code="configurePipeline.alignment.referenceGenome"/></h2>

    <otp:annotation type="info"><g:message code="configurePipeline.info.defaultValues.exceptIndexes"/></otp:annotation>

    <g:form controller="configurePipeline" action="rnaAlignmentReferenceGenome" method="POST"
            params='["seqType.id": seqType.id]'>
        <table class="alignmentTable">
            <tr>
                <th></th>
                <th></th>
                <th><g:message code="configurePipeline.header.defaultValue"/></th>
                <th><g:message code="configurePipeline.header.info"/></th>
            </tr>
            <tr>
                <td valign="top" class="myKey"><g:message code="configurePipeline.mouse.data"/></td>
                <td><g:checkBox name="mouseData" checked="false" value="true"/></td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <td class="myKey"><g:message code="configurePipeline.rnaAlignment.genome"/></td>
                <td><g:select class="genome use-select-2" name="referenceGenome" from="${referenceGenomes}"
                              value="${referenceGenome}"/></td>
                <td>${defaultReferenceGenome}</td>
                <td>&nbsp;</td>
            </tr>

            <tr>
                <td class="myKey"><g:message code="configurePipeline.rnaAlignment.deprecateConfigurations"/></td>
                <td><g:checkBox name="deprecateConfigurations" checked="true" value="true"/></td>
                <td>&nbsp;</td>
                <td><g:message code="configurePipeline.rnaAlignment.deprecateConfigurations.info"/></td>
            </tr>

            <g:each in="${toolNames}">
                <tr>
                    <td class="myKey">${it}:</td>
                    <td>
                        <g:select id="toolVersionSelect_${it}" name="toolVersionValue" from="${indexToolVersion}"
                                  class="toolVersionSelect use-select-2"/>
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
                    <g:select name="geneModel.id" from="${geneModel}" class="geneModelSelect use-select-2"
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
                        <div class="scrollable" style="max-height: 300px">
                            <ul>
                                <g:each in="${entry.values}" var="sampleType">
                                    <li><g:checkBox name="sampleTypeIds" value="${sampleType.id}" checked="false"/>${sampleType.name}</li>
                                </g:each>
                            </ul>
                        </div>
                    <td>&nbsp;</td>
                    <td>
                        <g:message code="${entry.info}"/><br><br>
                        <g:message code="configurePipeline.sample.type.info.general"/>
                    </td>
                </tr>
            </g:each>

            <tr>
                <td colspan="4">&nbsp;</td>
            </tr>

            <tr>
                <td class="myKey"></td>
                <td>
                    <g:submitButton name="submit" value="Submit"/>
                    <g:link controller="alignmentConfigurationOverview" class="btn">${g.message(code: "default.button.cancel.label")}</g:link>
                </td>
                <td>&nbsp;</td>
                <td>&nbsp;</td>
            </tr>
        </table>
    </g:form>
    <g:if test="${configState.content}">
        <h2><g:message code="configurePipeline.current.config"/></h2>
        <g:form controller="configurePipeline" action="invalidateConfig" method="POST"
                params='["seqType.id": seqType.id, "pipeline.id": pipeline.id, "originAction": actionName, overviewController: "alignmentConfigurationOverview"]'>
            <g:submitButton name="invalidateConfig" value="Invalidate Config"/>
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
