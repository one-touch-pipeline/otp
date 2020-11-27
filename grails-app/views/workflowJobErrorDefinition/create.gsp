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
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="workflowJobErrorDefinition.title.create"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="pages/workflowJobErrorDefinition/create/create.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <g:render template="tabMenu"/>

    <h1><g:message code="workflowJobErrorDefinition.title.create"/></h1>

    <g:form action="createObject" method="POST" useToken="true">
        <table class="key-value-table key-input">
            <tr>
                <td>
                    <label for="name">
                        <g:message code="workflowJobErrorDefinition.header.name"/>
                    </label>
                </td>
                <td>
                    <g:textField id="name" name="name" required="true"
                                 value="${cmd?.name}"/>
                </td>
                <td>
                    <g:message code="workflowJobErrorDefinition.header.tooltip.name"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="jobBeanName">
                        <g:message code="workflowJobErrorDefinition.header.jobBeanName"/>
                    </label>
                </td>
                <td>
                    <g:select id="jobBeanName" name="jobBeanName" class="use-select-2" required="true" from="${jobBeanNames}"
                              value="${cmd?.jobBeanName}" noSelection="${[(""): "${g.message(code: 'workflowJobErrorDefinition.noSelection.job')}"]}"/>
                </td>
                <td>
                    <g:message code="workflowJobErrorDefinition.header.tooltip.jobBeanName"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="sourceType">
                        <g:message code="workflowJobErrorDefinition.header.sourceType"/>
                    </label>
                </td>
                <td>
                    <g:select id="sourceType" name="sourceType" class="use-select-2" required="true" from="${sourceTypes}"
                              value="${cmd?.sourceType}" noSelection="${[(""): "${g.message(code: 'workflowJobErrorDefinition.noSelection.sourceType')}"]}"/>
                </td>
                <td>
                    <g:message code="workflowJobErrorDefinition.header.tooltip.sourceType"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="restartAction">
                        <g:message code="workflowJobErrorDefinition.header.action"/>
                    </label>
                </td>
                <td>
                    <g:select id="restartAction" name="restartAction" class="use-select-2" required="true" from="${actions}"
                              value="${cmd?.restartAction}" noSelection="${[(""): "${g.message(code: 'workflowJobErrorDefinition.noSelection.action')}"]}"/>
                </td>
                <td>
                    <g:message code="workflowJobErrorDefinition.header.tooltip.action"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="beanToRestart">
                        <g:message code="workflowJobErrorDefinition.header.beanToRestart"/>
                    </label>
                </td>
                <td>
                    <div>
                        <g:select id="beanToRestart" name="beanToRestart" required="true" class="use-select-2" from="${jobBeanNames}"
                                  value="${cmd?.beanToRestart}"
                                  noSelection="${[(""): "${g.message(code: 'workflowJobErrorDefinition.noSelection.restartingJob')}"]}"/>
                    </div>
                </td>
                <td>
                    <g:message code="workflowJobErrorDefinition.header.tooltip.beanToRestart"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="errorExpression">
                        <g:message code="workflowJobErrorDefinition.header.errorExpression"/>
                    </label>
                </td>
                <td>
                    <g:textField id="errorExpression" name="errorExpression" required="true"
                                 value="${cmd?.errorExpression}"/>

                </td>
                <td>
                    <g:message code="workflowJobErrorDefinition.header.tooltip.errorExpression"/>
                    <g:message code="workflowJobErrorDefinition.header.link.errorExpression"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="allowRestartingCount">
                        <g:message code="workflowJobErrorDefinition.header.allowRestartingCount"/>
                    </label>
                </td>
                <td>
                    <g:textField id="allowRestartingCount" name="allowRestartingCount" required="true" type="number"
                                 value="${cmd ? cmd.allowRestartingCount : 1}"/>
                </td>
                <td>
                    <g:message code="workflowJobErrorDefinition.header.tooltip.allowRestartingCount"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="mailText">
                        <g:message code="workflowJobErrorDefinition.header.mailText"/>
                    </label>
                </td>
                <td>
                    <g:textArea id="mailText" name="mailText" class="resize-vertical" required="true"
                                value="${cmd?.mailText}"/>
                </td>
                <td>
                    <g:message code="workflowJobErrorDefinition.header.tooltip.mailText"/>
                </td>
            </tr>
        </table>

        <div>
            <g:submitButton name="create" value="Create"/>
        </div>
    </g:form>
</div>
</body>
</html>
