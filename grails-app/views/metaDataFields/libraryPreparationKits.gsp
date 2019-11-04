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

    <h3><g:message code="dataFields.libPrepKit.header"/></h3>
    <table>
        <thead>
        <tr>
            <th><g:message code="dataFields.libPrepKit.name"/></th>
            <th><g:message code="dataFields.libPrepKit.shortDisplayName"/></th>
            <th><g:message code="dataFields.libPrepKit.importAlias"/></th>
            <th></th>
            <th><g:message code="dataFields.libPrepKit.adapterFile"/></th>
            <th><g:message code="dataFields.libPrepKit.reverseComplementAdapterSequenceShort"/></th>
            <th><g:message code="dataFields.libPrepKit.genomes"/></th>
        </tr>
        </thead>
        <tbody>
        <g:each var="libraryPreparationKit" in="${libraryPreparationKits}">
            <tr>
                <td>${libraryPreparationKit.name}</td>
                <td>${libraryPreparationKit.shortDisplayName}</td>
                <td class="keep-whitespace">${libraryPreparationKit.importAliases}</td>
                <td align="right">
                    <otp:editorSwitchNewValues
                            roles="ROLE_OPERATOR"
                            labels="${[g.message(code: "dataFields.libPrepKit.importAlias")]}"
                            textFields="${["importAlias"]}"
                            link="${g.createLink(controller: 'metaDataFields', action: 'createLibraryPreparationKitImportAlias', id: libraryPreparationKit.id)}"/>
                </td>
                <td>
                    <g:if test="${libraryPreparationKit.adapterFile}">
                        <span title="${libraryPreparationKit.adapterFile}">${new File(libraryPreparationKit.adapterFile).name}</span>
                    </g:if>
                    <g:else>
                        <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${[g.message(code: "dataFields.libPrepKit.adapterFile")]}"
                                textFields="${["adapterFile"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'addAdapterFileToLibraryPreparationKit', params: ["libraryPreparationKit.id": libraryPreparationKit.id])}"/>
                    </g:else>
                </td>
                <td>
                    <g:if test="${libraryPreparationKit.reverseComplementAdapterSequence}">
                        <asset:image src="ok.png" title="${libraryPreparationKit.reverseComplementAdapterSequence}"/>
                    </g:if>
                    <g:else>
                        <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${[g.message(code: "dataFields.libPrepKit.reverseComplementAdapterSequenceShort")]}"
                                textFields="${["reverseComplementAdapterSequence"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'addAdapterSequenceToLibraryPreparationKit', params: ["libraryPreparationKit.id": libraryPreparationKit.id])}"/>
                    </g:else>
                </td>
                <td class="keep-whitespace">${libraryPreparationKit.referenceGenomesWithBedFiles}</td>
            </tr>
        </g:each>
        <td colspan="3">
            <otp:editorSwitchNewValues
                    roles="ROLE_OPERATOR"
                    labels="${[
                            g.message(code: "dataFields.libPrepKit.name"),
                            g.message(code: "dataFields.libPrepKit.shortDisplayName"),
                            g.message(code: "dataFields.libPrepKit.adapterFile"),
                            g.message(code: "dataFields.libPrepKit.reverseComplementAdapterSequenceShort"),
                    ]}"
                    textFields="${["name", "shortDisplayName", "adapterFile", "reverseComplementAdapterSequence"]}"
                    link="${g.createLink(controller: 'metaDataFields', action: 'createLibraryPreparationKit')}"/>
        </td>
        </tbody>
    </table>
</div>
</body>
</html>
