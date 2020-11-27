%{--
  - Copyright 2011-2020 The OTP authors
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
<%@ page import="java.time.format.DateTimeFormatter; de.dkfz.tbi.otp.config.TypeValidators; de.dkfz.tbi.otp.project.*; de.dkfz.tbi.otp.project.additionalField.*" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="workflowJobErrorDefinition.title.index"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="pages/workflowJobErrorDefinition/index/index.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <g:render template="tabMenu"/>

    <h1><g:message code="workflowJobErrorDefinition.title.index"/></h1>

    <div class="errorDefinitions">
        <table class="otpDataTables">
            <tr>
                <th>
                    <span title="${g.message(code: "workflowJobErrorDefinition.header.tooltip.name")}">
                        <g:message code="workflowJobErrorDefinition.header.name"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "workflowJobErrorDefinition.header.tooltip.jobBeanName")}">
                        <g:message code="workflowJobErrorDefinition.header.jobBeanName"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "workflowJobErrorDefinition.header.tooltip.sourceType")}">
                        <g:message code="workflowJobErrorDefinition.header.sourceType"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "workflowJobErrorDefinition.header.tooltip.action")}">
                        <g:message code="workflowJobErrorDefinition.header.action"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "workflowJobErrorDefinition.header.tooltip.beanToRestart")}">
                        <g:message code="workflowJobErrorDefinition.header.beanToRestart"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "workflowJobErrorDefinition.header.tooltip.errorExpression")}">
                        <g:message code="workflowJobErrorDefinition.header.errorExpression"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "workflowJobErrorDefinition.header.tooltip.allowRestartingCount")}">
                        <g:message code="workflowJobErrorDefinition.header.allowRestartingCount"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "workflowJobErrorDefinition.header.tooltip.mailText")}">
                        <g:message code="workflowJobErrorDefinition.header.mailText"/>
                    </span>
                </th>
                <th class="delete">
                    <span title="${g.message(code: "workflowJobErrorDefinition.header.tooltip.delete")}">
                        <g:message code="workflowJobErrorDefinition.header.delete"/>
                    </span>
                </th>
            </tr>
            <g:each in="${definitions}" var="definition">
                <tr>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'workflowJobErrorDefinition', action: 'updateField', params: [
                                        'entityId': "${definition.id}",
                                        'property': 'name',
                                ])}"
                                value="${definition.name}"/>
                    </td>
                    <td class="jobBeanName">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                values="${jobBeanNames}"
                                link="${g.createLink(controller: 'workflowJobErrorDefinition', action: 'updateField', params: [
                                        'entityId': "${definition.id}",
                                        'property': 'jobBeanName',
                                ])}"
                                value="${definition.jobBeanName}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                values="${de.dkfz.tbi.otp.workflow.restartHandler.WorkflowJobErrorDefinition.SourceType.values()}"
                                link="${g.createLink(controller: 'workflowJobErrorDefinition', action: 'updateField', params: [
                                        'entityId': "${definition.id}",
                                        'property': 'sourceType',
                                ])}"
                                value="${definition.sourceType}"/>
                    </td>
                    <td class="restartAction">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                values="${de.dkfz.tbi.otp.workflow.restartHandler.WorkflowJobErrorDefinition.Action.values()}"
                                link="${g.createLink(controller: 'workflowJobErrorDefinition', action: 'updateActionField', params: [
                                        'workflowJobErrorDefinition.id': "${definition.id}"
                                ])}"
                                sucessHandler="workflowJobErrorDefinitionActionChangeSuccessHandler"
                                value="${definition.action}"/>
                    </td>
                    <td class="jobBeanName restartJob">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                values="${jobBeanNames}"
                                noSelection="${[(""): "${g.message(code: 'workflowJobErrorDefinition.noSelection.restartingJob')}"]}"
                                link="${g.createLink(controller: 'workflowJobErrorDefinition', action: 'updateField', params: [
                                        'entityId': "${definition.id}",
                                        'property': 'beanToRestart',
                                ])}"
                                value="${definition.beanToRestart}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'workflowJobErrorDefinition', action: 'updateField', params: [
                                        'entityId': "${definition.id}",
                                        'property': 'errorExpression',
                                ])}"
                                value="${definition.errorExpression}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="integer"
                                link="${g.createLink(controller: 'workflowJobErrorDefinition', action: 'updateField', params: [
                                        'entityId': "${definition.id}",
                                        'property': 'allowRestartingCount',
                                ])}"
                                value="${definition.allowRestartingCount}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="textArea"
                                link="${g.createLink(controller: 'workflowJobErrorDefinition', action: 'updateField', params: [
                                        'entityId': "${definition.id}",
                                        'property': 'mailText',
                                ])}"
                                value="${definition.mailText}"/>
                    </td>
                    <td class="delete">
                        <g:form action="delete" method="POST" useToken="true">
                            <g:hiddenField name="workflowJobErrorDefinition.id" value="${definition.id}"/>
                            <g:submitButton name="delete" value="Delete"
                                            onClick="if (confirm('${g.message(code: 'workflowJobErrorDefinition.delete.confirm', args: [definition.name])}')) return true; else return false;"/>
                        </g:form>
                    </td>
                </tr>
            </g:each>
        </table>
    </div>
</div>
</body>
</html>
