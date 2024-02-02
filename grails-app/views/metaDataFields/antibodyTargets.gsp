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
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="dataFields.title"/></title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="pages/metaDataFields/datatable.js"/>
    <asset:javascript src="pages/metaDataFields/antibodyTargets/datatable.js"/>
</head>

<body>
<div class="body fixed-table-header wrapTableHeader metaDataFields">
    <g:render template="/templates/messages"/>
    <g:render template="tabMenu"/>

    <h1><g:message code="dataFields.antibodyTarget.header"/></h1>
    <otp:annotation type="info"><g:message code="dataFields.title.caseInsensitive"/></otp:annotation>
    <div class="otpDataTables">
    <table id="metadatafields-datatable">
        <thead>
        <tr>
            <th class="export_column"><g:message code="dataFields.antibodyTarget.name"/></th>
            <th class="export_column"><g:message code="dataFields.antibodyTarget.importAlias"/></th>
            <th></th>
            <th class="export_column"><g:message code="dataFields.legacy"/></th>
        </tr>
        </thead>
        <tbody>
        <g:each status="i" var="antibodyTarget" in="${antibodyTargets}">
            <g:if test="${antibodyTarget.legacy}">
                <tr class="text-muted">
            </g:if>
            <g:else>
                <tr>
            </g:else>
                <td>${antibodyTarget.name}</td>
                <td class="keep-whitespace">${antibodyTarget.importAliases}</td>
                <td>
                    <otp:editorSwitchNewValues
                            roles="ROLE_OPERATOR"
                            labels="${["Import Alias"]}"
                            textFields="${["importAlias"]}"
                            link="${g.createLink(controller: 'metaDataFields', action: 'createAntibodyTargetImportAlias', id: antibodyTarget.id)}"/>
                </td>
                <td>
                    <g:render template="/templates/slider" model="[
                            targetAction: 'changeAntibodyTargetLegacyState',
                            objectName  : 'antibodyTarget',
                            object      : antibodyTarget,
                            i           : i,
                    ]"/>
                    <span hidden>${antibodyTarget.legacy}</span>
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>
    </div>
    <div class="new-antibody-target-container">
        <otp:editorSwitchNewValues
                roles="ROLE_OPERATOR"
                labels="${["Name"]}"
                textFields="${["name"]}"
                link="${g.createLink(controller: 'metaDataFields', action: 'createAntibodyTarget')}"/>
    </div>
    <br><br>
</div>
</body>
</html>
