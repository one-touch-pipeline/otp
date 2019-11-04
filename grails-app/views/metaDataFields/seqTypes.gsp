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
    <asset:javascript src="modules/editorSwitch"/>
</head>

<body>
<div class="body fixedTableHeader wrapTableHeader">
    <g:render template="linkBanner"/>
    <h3><g:message code="dataFields.title.caseInsensitive"/></h3>

    <h3><g:message code="dataFields.seqType.header"/></h3>
    <table>
        <thead>
        <tr>
            <th><g:message code="dataFields.seqType.name"/></th>
            <th><g:message code="dataFields.seqType.singleCell"/></th>
            <th><g:message code="dataFields.seqType.supportsAntibody"/></th>
            <th><g:message code="dataFields.seqType.directory"/></th>
            <th><g:message code="dataFields.seqType.libraryLayouts"/></th>
            <th></th>
            <th><g:message code="dataFields.seqType.displayNames"/></th>
            <th><g:message code="dataFields.seqType.importAlias"/></th>
            <th></th>
        </tr>
        </thead>
        <tbody>
        <g:each var="seqType" in="${seqTypes}">
            <tr>
                <td>${seqType.name}</td>
                <td><asset:image src="${seqType.singleCell ? "ok.png" : "error.png"}"/></td>
                <td>
                    <asset:image
                            src="${seqType.hasAntibodyTarget ? "ok.png" : "error.png"}"
                            title="${g.message(code: "dataFields.seqType.supportsAntibody.${seqType.hasAntibodyTarget}")}"/>
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
            </tr>
        </g:each>
        <td colspan="4">
            <div class="edit-switch edit-switch-new-free-text-values">
                <span class="edit-switch-editor" style="display: none">
                    <h4><g:message code="dataFields.seqType.create.addSeqTypeTitle"/></h4>
                    <input type="hidden" name="target" value="${g.createLink(controller: 'metaDataFields', action: 'createSeqType')}"/>

                    <div class="dialog">
                        <table>
                            <tbody>
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="type">
                                        <g:message code="dataFields.seqType.create.type"/>
                                    </label>
                                </td>
                                <td valign="top" class="value">
                                    <input name="type" id="type" type="text"/>
                                </td>
                                <td></td>
                            </tr>
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="directory">
                                        <g:message code="dataFields.seqType.create.directory"/>
                                    </label>
                                </td>
                                <td valign="top" class="value">
                                    <input name="dirName" id="directory" type="text"/>
                                </td>
                                <td></td>
                            </tr>
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="name">
                                        <g:message code="dataFields.seqType.create.name"/>
                                    </label>
                                </td>
                                <td valign="top" class="value">
                                    <input name="displayName" id="name" type="text"/>
                                </td>
                                <td></td>
                            </tr>
                            <tr class="prop">
                                <td colspan="1" valign="top" class="name">
                                    <label for="singleCell">
                                        <g:message code="dataFields.seqType.create.singleCell"/>
                                    </label>
                                </td>
                                <td colspan="1" valign="top" class="name">
                                    <input name="singleCell" id="singleCell" type="checkbox"/>
                                </td>
                            </tr>
                            <tr class="prop">
                                <td colspan="1" valign="top" class="name">
                                    <label for="hasAntibodyTarget">
                                        <g:message code="dataFields.seqType.create.hasAntibodyTarget"/>
                                    </label>
                                </td>
                                <td colspan="1" valign="top" class="name">
                                    <input name="hasAntibodyTarget" id="hasAntibodyTarget" type="checkbox"/>
                                </td>
                            </tr>
                            <tr class="prop">
                                <td colspan="1" valign="top" class="name">
                                    <label for="single">
                                        <g:message code="dataFields.seqType.create.layout"/>
                                    </label>
                                </td>
                                <td colspan="1" valign="top" class="name">
                                    <label for="single">
                                        <g:message code="dataFields.seqType.create.single"/>
                                    </label>
                                    <input name="single" id="single" type="checkbox"/>
                                </td>
                            </tr>
                            <tr class="prop">
                                <td colspan="1">&nbsp;</td>
                                <td colspan="1" valign="top" class="name">
                                    <label for="paired">
                                        <g:message code="dataFields.seqType.create.paired"/>
                                    </label>
                                    <input name="paired" id="paired" checked="checked" type="checkbox"/>
                                </td>
                            </tr>
                            <tr class="prop">
                                <td colspan="1">&nbsp;</td>
                                <td colspan="1" valign="top" class="value">
                                    <label for="mate_pair">
                                        <g:message code="dataFields.seqType.create.mate"/>
                                    </label>
                                    <input name="mate_pair" id="mate_pair" type="checkbox"/>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <button class="buttons save"><g:message code="default.button.save.label"/></button>
                    <button class="buttons cancel"><g:message code="default.button.cancel.label"/></button>
                </span>
                <span class="edit-switch-label" style="display: inline;">
                    <button class="add js-edit">+</button>
                </span>
            </div>
        </td>
        </tbody>
    </table>
</div>
</body>
</html>
