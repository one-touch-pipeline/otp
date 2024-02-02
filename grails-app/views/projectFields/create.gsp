%{--
  - Copyright 2011-2024 The OTP authors
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
    <title><g:message code="projectFields.create.title"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <g:render template="tabMenu"/>

    <h1><g:message code="projectFields.create.title"/></h1>

    <g:form action="${cmd.projectFieldType.createActionName()}" method="POST" useToken="true">
        <table class="key-value-table key-input">
            <tr>
                <td>
                    <label for="name">
                        <g:message code="projectFields.header.name"/>
                    </label>
                </td>
                <td>
                    <g:textField id="name" name="name" required="true"
                                 value="${cmd.name}"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.name"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="projectFieldType">
                        <g:message code="projectFields.header.projectFieldType"/>
                    </label>
                </td>
                <td>
                    <g:select id="projectFieldType" name="projectFieldType" class="use-select-2" required="true" from="${ProjectFieldType.supportedValues}"
                              value="${cmd.projectFieldType}" onChange="submit();"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.projectFieldType"/>
                    <b><g:message code="projectFields.note.fixed"/></b>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="cardinalityType">
                        <g:message code="projectFields.header.cardinalityType"/>
                    </label>
                </td>
                <td>
                    <g:select id="cardinalityType" name="cardinalityType" class="use-select-2" required="true" from="${ProjectCardinalityType.values()}"
                              value="${cmd.cardinalityType}"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.cardinalityType"/>
                    <b><g:message code="projectFields.note.fixed"/></b>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="descriptionConfig">
                        <g:message code="projectFields.header.descriptionConfig"/>
                    </label>
                </td>
                <td>
                    <g:textArea id="descriptionConfig" name="descriptionConfig" class="resize-vertical" required="true"
                                value="${cmd.descriptionConfig}"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.descriptionConfig"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="descriptionRequest">
                        <g:message code="projectFields.header.descriptionRequest"/>
                    </label>
                </td>
                <td>
                    <g:textArea id="descriptionRequest" name="descriptionRequest" class="resize-vertical" required="true"
                                value="${cmd.descriptionRequest}"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.descriptionRequest"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="fieldUseForSequencingProjects">
                        <g:message code="projectFields.header.fieldUseForSequencingProjects"/>
                    </label>
                </td>
                <td>
                    <g:select id="fieldUseForSequencingProjects" name="fieldUseForSequencingProjects" class="use-select-2" required="true"
                              from="${FieldExistenceType.values()}"
                              value="${cmd.fieldUseForSequencingProjects}"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.fieldUseForSequencingProjects"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="fieldUseForDataManagementProjects">
                        <g:message code="projectFields.header.fieldUseForDataManagementProjects"/>
                    </label>
                </td>
                <td>
                    <g:select id="fieldUseForDataManagementProjects" name="fieldUseForDataManagementProjects" class="use-select-2" required="true"
                              from="${FieldExistenceType.values()}"
                              value="${cmd.fieldUseForDataManagementProjects}"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.fieldUseForDataManagementProjects"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="sourceOfData">
                        <g:message code="projectFields.header.sourceOfData"/>
                    </label>
                </td>
                <td>
                    <g:select id="sourceOfData" name="sourceOfData" class="use-select-2" required="true" from="${ProjectSourceOfData.values()}"
                              value="${cmd.sourceOfData}"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.sourceOfData"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="projectDisplayOnConfigPage">
                        <g:message code="projectFields.header.projectDisplayOnConfigPage"/>
                    </label>
                </td>
                <td>
                    <g:select id="projectDisplayOnConfigPage" name="projectDisplayOnConfigPage" class="use-select-2" required="true"
                              from="${ProjectDisplayOnConfigPage.values()}"
                              value="${cmd.projectDisplayOnConfigPage}"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.projectDisplayOnConfigPage"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="sortNumber">
                        <g:message code="projectFields.header.sortNumber"/>
                    </label>
                </td>
                <td>
                    <g:textField id="sortNumber" name="sortNumber" required="true" type="number"
                                 value="${cmd.sortNumber}"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.sortNumber"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="changeOnlyByOperator">
                        <g:message code="projectFields.header.changeOnlyByOperator"/>
                    </label>
                </td>
                <td>
                    <g:checkBox id="changeOnlyByOperator" name="changeOnlyByOperator" checked="${cmd.changeOnlyByOperator}"
                                value="true"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.changeOnlyByOperator"/>
                </td>
            </tr>
            <tr>
                <td>
                    <label for="usedExternally">
                        <g:message code="projectFields.header.usedExternally"/>
                    </label>
                </td>
                <td>
                    <g:checkBox id="usedExternally" name="usedExternally" checked="${cmd.usedExternally}"
                                value="true"/>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.usedExternally"/>
                </td>
            </tr>
            <g:if test="${cmd.projectFieldType == ProjectFieldType.DOMAIN_REFERENCE}">
                <tr>
                    <td>
                        <label for="domainClassName">
                            <g:message code="projectFields.header.domainReference"/>
                        </label>
                    </td>
                    <td>
                        <g:select id="domainClassName" name="domainClassName" class="use-select-2" required="true" from="${referenceAbleDomains}"
                                  value="${cmd.domainClassName}"/>
                    </td>
                    <td>
                        <g:message code="projectFields.header.tooltip.domainReference"/>
                        <b><g:message code="projectFields.note.fixed"/></b>
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="allowCustomValue">
                            <g:message code="projectFields.header.allowCustomValue"/>
                        </label>
                    </td>
                    <td>
                        <g:checkBox id="allowCustomValue" name="allowCustomValue" checked="${cmd.allowCustomValue}"
                                    value="true"/>
                    </td>
                    <td>
                        <g:message code="projectFields.header.tooltip.allowCustomValue"/>
                    </td>
                </tr>
            </g:if>
            <tr>
                <td>
                    <label for="defaultValue">
                        <g:message code="projectFields.header.defaultValue"/>
                    </label>
                </td>
                <td>
                    <g:if test="${cmd.projectFieldType == ProjectFieldType.TEXT}">
                        <g:textArea id="defaultValue" name="defaultValue"
                                    value="${cmd.defaultValue}"/>
                    </g:if>
                    <g:if test="${cmd.projectFieldType == ProjectFieldType.FLAG}">
                        <g:select id="defaultValue" name="defaultValue"
                                  noSelection="${[(""): "no default value"]}"
                                  from="[Boolean.TRUE, Boolean.FALSE]"
                                  value="${cmd.defaultValue}"
                                  class="use-select-2"/>
                    </g:if>
                    <g:if test="${cmd.projectFieldType == ProjectFieldType.INTEGER}">
                        <g:textField id="defaultValue" name="defaultValue" type="number"
                                     value="${cmd.defaultValue}"/>
                    </g:if>
                    <g:if test="${cmd.projectFieldType == ProjectFieldType.DECIMAL_NUMBER}">
                        <g:textField id="defaultValue" name="defaultValue"
                                     value="${cmd.defaultValue}"/>

                    </g:if>
                    <g:if test="${cmd.projectFieldType == ProjectFieldType.DATE}">
                        <input type="date" id="defaultValue" name="defaultValue"
                               value="${cmd.defaultValue?.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)}"/>
                    </g:if>
                    <g:if test="${cmd.projectFieldType == ProjectFieldType.DOMAIN_REFERENCE}">
                        <g:message code="projectFields.info.defaultValue.definition"/>
                    </g:if>
                </td>
                <td>
                    <g:message code="projectFields.header.tooltip.defaultValue"/>
                </td>
            </tr>
            <g:if test="${cmd.projectFieldType.propertyAllowedValue}">
                <tr>
                    <td>
                        <g:message code="projectFields.header.allowedValues"/>
                    </td>
                    <td>
                        <g:message code="projectFields.info.allowedValues.definition"/>
                    </td>
                    <td>
                        <g:message code="projectFields.header.tooltip.allowedValues"/>
                    </td>
                </tr>
            </g:if>
            <g:if test="${cmd.projectFieldType == ProjectFieldType.TEXT}">
                <tr>
                    <td>
                        <label for="typeValidator">
                            <g:message code="projectFields.header.validator"/>
                        </label>
                    </td>
                    <td>
                        <g:select id="typeValidator" name="typeValidator" class="use-select-2" from="${validators}"
                                  noSelection="${[(""): "No Filter"]}"
                                  value="${cmd instanceof ProjectFieldsCreateTextCommand ? ((ProjectFieldsCreateTextCommand) cmd).typeValidator : ""}"/>
                    </td>
                    <td>
                        <g:message code="projectFields.header.tooltip.validator"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="regularExpression">
                            <g:message code="projectFields.header.regularExpression"/>
                        </label>
                    </td>
                    <td>
                        <g:textField id="regularExpression" name="regularExpression"
                                     value="${cmd instanceof ProjectFieldsCreateTextCommand ? ((ProjectFieldsCreateTextCommand) cmd).regularExpression : ""}"/>
                    </td>
                    <td>
                        <g:message code="projectFields.header.regularExpression"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="regularExpressionError">
                            <g:message code="projectFields.header.regularExpressionError"/>
                        </label>
                    </td>
                    <td>
                        <g:textField id="regularExpressionError" name="regularExpressionError"
                                     value="${cmd instanceof ProjectFieldsCreateTextCommand ? ((ProjectFieldsCreateTextCommand) cmd).regularExpressionError : ""}"/>
                    </td>
                    <td>
                        <g:message code="projectFields.header.regularExpressionError"/>
                    </td>
                </tr>
            </g:if>
        </table>
        <div>
            <g:submitButton name="create" value="Create"/>
        </div>
    </g:form>
</div>
</body>
</html>
