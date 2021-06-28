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
    <title><g:message code="projectFields.overview.title"/></title>
    <asset:javascript src="common/CommentBox.js"/>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="common/MultiInputField.js"/>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <g:render template="tabMenu"/>

    <h1><g:message code="projectFields.overview.title"/></h1>

    <div class="additionalFields">
        <table class="otpDataTables">
            <tr>
                <th class="name">
                    <span title="${g.message(code: "projectFields.header.tooltip.name")}">
                        <g:message code="projectFields.header.name"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.projectFieldType")}">
                        <g:message code="projectFields.header.projectFieldType"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.cardinalityType")}">
                        <g:message code="projectFields.header.cardinalityType"/>
                    </span>
                </th>
                <th class="description">
                    <span title="${g.message(code: "projectFields.header.tooltip.descriptionConfig")}">
                        <g:message code="projectFields.header.descriptionConfig"/>
                    </span>
                </th>
                <th class="description">
                    <span title="${g.message(code: "projectFields.header.tooltip.descriptionRequest")}">
                        <g:message code="projectFields.header.descriptionRequest"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.fieldUseForSequencingProjects")}">
                        <g:message code="projectFields.header.fieldUseForSequencingProjects"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.fieldUseForDataManagementProjects")}">
                        <g:message code="projectFields.header.fieldUseForDataManagementProjects"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.sourceOfData")}">
                        <g:message code="projectFields.header.sourceOfData"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.projectDisplayOnConfigPage")}">
                        <g:message code="projectFields.header.projectDisplayOnConfigPage"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.sortNumber")}">
                        <g:message code="projectFields.header.sortNumber"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.changeOnlyByOperator")}">
                        <g:message code="projectFields.header.changeOnlyByOperator"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.usedExternally")}">
                        <g:message code="projectFields.header.usedExternally"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.defaultValue")}">
                        <g:message code="projectFields.header.defaultValue"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.allowedValues")}">
                        <g:message code="projectFields.header.allowedValues"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.validator")}">
                        <g:message code="projectFields.header.validator"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.regularExpression")}">
                        <g:message code="projectFields.header.regularExpression"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.regularExpressionError")}">
                        <g:message code="projectFields.header.regularExpressionError"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.domainReference")}">
                        <g:message code="projectFields.header.domainReference"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.allowCustomValue")}">
                        <g:message code="projectFields.header.allowCustomValue"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.legacy")}">
                        <g:message code="projectFields.header.legacy"/>
                    </span>
                </th>
                <th>
                    <span title="${g.message(code: "projectFields.header.tooltip.trash")}">
                        <g:message code="projectFields.header.trash"/>
                    </span>
                </th>
            </tr>
            <g:each in="${fieldDefinitions}" var="fieldDefinition">
                <tr>
                    <td class="name">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'name',
                                ])}"
                                value="${fieldDefinition.name}"/>
                    </td>
                    <td>
                        ${fieldDefinition.projectFieldType}
                    </td>
                    <td>
                        ${fieldDefinition.cardinalityType}
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="textArea"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'descriptionConfig',
                                ])}"
                                value="${fieldDefinition.descriptionConfig}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="textArea"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'descriptionRequest',
                                ])}"
                                value="${fieldDefinition.descriptionRequest}"/>
                    </td>
                    <td class="fieldExistenceType">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                values="${de.dkfz.tbi.otp.project.additionalField.FieldExistenceType.values()}"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'fieldUseForSequencingProjects',
                                ])}"
                                value="${fieldDefinition.fieldUseForSequencingProjects}"/>
                    </td>
                    <td class="fieldExistenceType">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                values="${de.dkfz.tbi.otp.project.additionalField.FieldExistenceType.values()}"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'fieldUseForDataManagementProjects',
                                ])}"
                                value="${fieldDefinition.fieldUseForDataManagementProjects}"/>
                    </td>
                    <td class="sourceOfData">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                values="${de.dkfz.tbi.otp.project.additionalField.ProjectSourceOfData.values()}"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'sourceOfData',
                                ])}"
                                value="${fieldDefinition.sourceOfData}"/>
                    </td>
                    <td class="projectDisplayOnConfigPage">
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="dropDown"
                                values="${de.dkfz.tbi.otp.project.additionalField.ProjectDisplayOnConfigPage.values()}"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'projectDisplayOnConfigPage',
                                ])}"
                                value="${fieldDefinition.projectDisplayOnConfigPage}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="integer"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'sortNumber',
                                ])}"
                                value="${fieldDefinition.sortNumber}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="toggle"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'changeOnlyByOperator',
                                ])}"
                                value="${fieldDefinition.changeOnlyByOperator}"/>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="toggle"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'usedExternally',
                                ])}"
                                value="${fieldDefinition.usedExternally}"/>
                    </td>
                    <td class="defaultValue">
                        <g:if test="${fieldDefinition.projectFieldType.templateDefaultValue}">
                            <g:if test="${fieldDefinition.projectFieldType == ProjectFieldType.DOMAIN_REFERENCE}">
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="dropDown"
                                        values="${data[((DomainReferenceFieldDefinition) fieldDefinition).domainClassName]}"
                                        link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                                'entityId': "${fieldDefinition.id}",
                                                'property': "${fieldDefinition.projectFieldType.propertyDefaultValue}",
                                        ])}"
                                        optionKey="id"
                                        optionValue="stringForProjectFieldDomainReference"
                                        noSelection="${[(""): "no default value"]}"
                                        value="${fieldDefinition.defaultValue}"/>
                            </g:if>
                            <g:else>
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="${fieldDefinition.projectFieldType.templateDefaultValue}"
                                        link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                                'entityId': "${fieldDefinition.id}",
                                                'property': "${fieldDefinition.projectFieldType.propertyDefaultValue}",
                                        ])}"
                                        value="${fieldDefinition.defaultValue ?: ''}"/>
                            </g:else>
                        </g:if>
                        <g:else>
                            NA
                        </g:else>
                    </td>
                    <td>
                        <g:if test="${fieldDefinition.projectFieldType.templateAllowedValue}">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="${fieldDefinition.projectFieldType.templateAllowedValue}"
                                    link="${g.createLink(controller: 'projectFields', action: 'updateMultiField', params: [
                                            'entityId': "${fieldDefinition.id}",
                                            'property': "${fieldDefinition.projectFieldType.propertyAllowedValue}",
                                    ])}"
                                    value="${fieldDefinition.valueList ?: ['']}"/>
                        </g:if>
                        <g:else>
                            NA
                        </g:else>
                    </td>
                    <td>
                        <g:if test="${fieldDefinition.projectFieldType == ProjectFieldType.TEXT}">
                            <div class="typeValidator">
                                <otp:editorSwitch
                                        roles="ROLE_OPERATOR"
                                        template="dropDown"
                                        values="${validators}"
                                        link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                                'entityId': "${fieldDefinition.id}",
                                                'property': 'typeValidator',
                                        ])}"
                                        noSelection="${[(""): "No Filter"]}"
                                        value="${((TextFieldDefinition) fieldDefinition).typeValidator}"/>
                            </div>
                        </g:if>
                    </td>
                    <td>
                        <g:if test="${fieldDefinition.projectFieldType == ProjectFieldType.TEXT}">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                            'entityId': "${fieldDefinition.id}",
                                            'property': 'regularExpression',
                                    ])}"
                                    value="${((TextFieldDefinition) fieldDefinition).regularExpression}"/>
                        </g:if>
                    </td>
                    <td>
                        <g:if test="${fieldDefinition.projectFieldType == ProjectFieldType.TEXT}">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                            'entityId': "${fieldDefinition.id}",
                                            'property': 'regularExpressionError',
                                    ])}"
                                    value="${((TextFieldDefinition) fieldDefinition).regularExpressionError}"/>
                        </g:if>
                    </td>
                    <td>
                        <g:if test="${fieldDefinition.projectFieldType == ProjectFieldType.DOMAIN_REFERENCE}">
                            <span title="${((DomainReferenceFieldDefinition) fieldDefinition).domainClassName}">
                                ${((DomainReferenceFieldDefinition) fieldDefinition).shortClassName()}
                            </span>
                        </g:if>
                    </td>
                    <td>
                        <g:if test="${fieldDefinition.projectFieldType == ProjectFieldType.DOMAIN_REFERENCE}">
                            <otp:editorSwitch
                                    roles="ROLE_OPERATOR"
                                    template="toggle"
                                    link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                            'entityId': "${fieldDefinition.id}",
                                            'property': 'allowCustomValue',
                                    ])}"
                                    value="${((DomainReferenceFieldDefinition) fieldDefinition).allowCustomValue}"/>
                        </g:if>
                    </td>
                    <td>
                        <otp:editorSwitch
                                roles="ROLE_OPERATOR"
                                template="toggle"
                                link="${g.createLink(controller: 'projectFields', action: 'updateField', params: [
                                        'entityId': "${fieldDefinition.id}",
                                        'property': 'legacy',
                                ])}"
                                value="${fieldDefinition.legacy}"/>
                    </td>
                    <td>
                        <g:if test="${!usedFieldDefinitionMap[fieldDefinition]}">
                            <g:form action="deleteFieldDefinition" method="POST" useToken="true">
                                <g:hiddenField name="fieldDefinition.id" value="${fieldDefinition.id}"/>
                                <g:submitButton name="deleteFieldDefinition" value="Delete"
                                                onClick="if (confirm('${g.message(code: 'projectFields.confirm.trash', args: [fieldDefinition.name])}')) return true; else return false;"/>
                            </g:form>
                        </g:if>
                    </td>
                </tr>
            </g:each>
        </table>
    </div>
</div>
</body>
</html>
