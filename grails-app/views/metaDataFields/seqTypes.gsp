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
    <title><g:message code="dataFields.title"/></title>
    <asset:javascript src="taglib/EditorSwitch.js"/>
    <asset:javascript src="common/MultiInputField.js"/>
    <asset:javascript src="pages/metaDataFields/datatable.js"/>
    <asset:javascript src="pages/metaDataFields/seqTypes/datatable.js"/>
</head>

<body>
<div class="body fixed-table-header wrapTableHeader metaDataFields">
    <g:render template="/templates/messages"/>
    <g:render template="tabMenu"/>

    <h1><g:message code="dataFields.seqType.header"/></h1>
    <otp:annotation type="info"><g:message code="dataFields.title.caseInsensitive"/></otp:annotation>
    <div class="otpDataTables">
    <table id="metadatafields-datatable">
        <thead>
        <tr>
            <th class="export_column"><g:message code="dataFields.seqType.name"/></th>
            <th class="export_column"><g:message code="dataFields.seqType.singleCell"/></th>
            <th class="export_column"><g:message code="dataFields.seqType.usesAntibodyTarget"/></th>
            <th class="export_column"><g:message code="dataFields.seqType.directory"/></th>
            <th class="export_column"><g:message code="dataFields.seqType.libraryLayouts"/></th>
            <th></th>
            <th class="export_column"><g:message code="dataFields.seqType.displayNames"/></th>
            <th class="export_column"><g:message code="dataFields.seqType.importAlias"/></th>
            <th></th>
            <th class="export_column"><g:message code="dataFields.legacy"/></th>
        </tr>
        </thead>
        <tbody>
        <g:each status="i" var="seqType" in="${seqTypes}">
            <g:if test="${seqType.legacy}">
                <tr class="text-muted">
            </g:if>
            <g:else>
                <tr>
            </g:else>
                <td>${seqType.name}</td>
                <td>
                    <asset:image src="${seqType.singleCell ? "ok.png" : "error.png"}"/>
                    <span hidden>${seqType.singleCell}</span>
                </td>
                <td>
                    <g:if test="${seqType.hasAntibodyTarget}">
                        <asset:image src="ok.png" title="${g.message(code: "dataFields.seqType.usesAntibodyTarget.true")}"/>
                    </g:if>
                    <g:else>
                        <asset:image src="error.png" title="${g.message(code: "dataFields.seqType.usesAntibodyTarget.false")}"/>
                    </g:else>
                    <span hidden>${seqType.hasAntibodyTarget}</span>
                </td>
                <td>${seqType.dirName}</td>
                <td class="keep-whitespace">${seqType.libraryLayouts}</td>
                <td>
                    <g:if test="${!(seqType.layouts.SINGLE && seqType.layouts.PAIRED && seqType.layouts.MATE_PAIR)}">
                        <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${seqType.layouts.findAll { !it.value }.collect { it.key }}"
                                checkBoxes="${seqType.layouts.findAll { !it.value }.collectEntries { [it.key.toLowerCase(), it.value] }}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createLayout', id: seqType.name, params: ["singleCell": seqType.singleCell])}"/>
                    </g:if>
                </td>
                <td>${seqType.displayName}</td>
                <td class="keep-whitespace">${seqType.importAliases}</td>
                <td>
                    <otp:editorSwitchNewValues
                            roles="ROLE_OPERATOR"
                            labels="${["Import Alias"]}"
                            textFields="${["importAlias"]}"
                            link="${g.createLink(controller: 'metaDataFields', action: 'createSeqTypeImportAlias', id: seqType.id)}"/>
                </td>
                <td>
                    <g:render template="/templates/slider" model="[
                            targetAction: 'changeSeqTypeLegacyState',
                            objectName  : 'seqType',
                            object      : seqType,
                            i           : i,
                    ]"/>
                    <span hidden>${seqType.legacy}</span>
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>
    </div>

    <h2><g:message code="dataFields.seqType.create.addSeqTypeTitle"/></h2>
    <g:form action="createSeqType" useToken="true">
        <table style="width: 50%">
            <tbody>
            <tr>
                <td><g:message code="dataFields.seqType.create.seqTypeName"/></td>
                <td><input name="seqTypeName" id="type" type="text" value="${cmd?.seqTypeName}"/></td>
            </tr>
            <tr>
                <td><g:message code="dataFields.seqType.create.aliases"/></td>
                <td class="multi-input-field">
                    <g:each in="${cmd?.aliases ?: [""]}" var="alias" status="i">
                        <div class="field">
                            <g:textField list="aliasList" name="aliases" value="${alias}" />
                            <g:if test="${i == 0}">
                                <button class="add-field">+</button>
                            </g:if>
                            <g:else>
                                <button class="remove-field">-</button>
                            </g:else>
                        </div>
                    </g:each>
                </td>
            </tr>
            <tr>
                <td><g:message code="dataFields.seqType.create.name"/></td>
                <td><input name="displayName" id="name" type="text" value="${cmd?.displayName}"/></td>
            </tr>
            <tr>
                <td><g:message code="dataFields.seqType.create.directory"/></td>
                <td><input name="dirName" id="directory" type="text" value="${cmd?.dirName}"/></td>
            </tr>
            <tr>
                <td><label for="singleCell"><g:message code="dataFields.seqType.create.singleCell"/></label></td>
                <td><g:checkBox name="singleCell" id="singleCell" value="${cmd?.singleCell ?: false}"/></td>
            </tr>
            <tr>
                <td><label for="hasAntibodyTarget"><g:message code="dataFields.seqType.create.hasAntibodyTarget"/></label></td>
                <td><g:checkBox name="hasAntibodyTarget" id="hasAntibodyTarget" value="${cmd?.hasAntibodyTarget ?: false}"/></td>
            </tr>
            <tr>
                <td><g:message code="dataFields.seqType.create.layout"/></td>
                <td>
                    <label class="vertical-align-middle">
                        <g:message code="dataFields.seqType.create.single"/>
                        <g:checkBox id="single" name="single" value="${cmd?.single ?: false}"/>
                    </label>
                    |
                    <label class="vertical-align-middle">
                        <g:message code="dataFields.seqType.create.paired"/>
                        <g:checkBox id="paired" name="paired" value="${cmd == null || cmd?.paired}"/>
                    </label>
                    |
                    <label class="vertical-align-middle">
                        <g:message code="dataFields.seqType.create.mate"/>
                        <g:checkBox id="mate_pair" name="mate_pair" value="${cmd?.mate_pair ?: false}"/>
                    </label>
                </td>
            </tr>
            </tbody>
        </table>
        <g:submitButton name="${g.message(code: "document.createType")}"/>
    </g:form>
</div>
</body>
</html>
