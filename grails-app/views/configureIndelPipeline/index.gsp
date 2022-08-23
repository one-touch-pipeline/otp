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
    <title><g:message code="configurePipeline.indel.title" args="[selectedProject.name, seqType.displayName]"/></title>
    <asset:javascript src="common/CommentBox.js"/>
</head>
<body>
    <div class="body">
        <g:set var="archived" value="${selectedProject.archived ? 'archived' : ''}"/>

        <g:render template="/templates/messages"/>

        <h1><g:message code="configurePipeline.indel.title" args="[selectedProject.name, seqType.displayName]"/></h1>

        <otp:annotation type="info">
            <g:message code="configurePipeline.info.defaultValues"/>
        </otp:annotation>
        <otp:annotation type="warning">
            <g:message code="configurePipeline.info.humanOnly"/>
        </otp:annotation>

        <g:if test="${archived}">
            <otp:annotation type="warning">
                <g:message code="configurePipeline.info.projectArchived.noChange" args="[selectedProject.name]"/>
            </otp:annotation>
        </g:if>

        <g:form action="save" params='["seqType.id": seqType.id]' method="POST">
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
                        <g:submitButton class="${archived}" name="submit" value="Submit"/>
                        <g:link controller="analysisConfigurationOverview" class="btn">${g.message(code: "default.button.cancel.label")}</g:link>
                    </td>
                </tr>
            </table>
        </g:form>
        <g:if test="${configState.content}">
            <h2><g:message code="configurePipeline.current.config"/></h2>
            <g:form controller="configurePipeline" action="invalidateConfig" method="POST"
                    params='["seqType.id": seqType.id, "pipeline.id": pipeline.id, "originAction": actionName, overviewController: "analysisConfigurationOverview"]'>
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
