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
    <asset:javascript src="modules/editorSwitch.js"/>
    <asset:javascript src="pages/metaDataFields/datatable.js"/>
    <asset:javascript src="pages/metaDataFields/seqCenters/datatable.js"/>
</head>

<body>
<div class="body fixed-table-header wrapTableHeader metaDataFields">
    <g:render template="/templates/messages"/>
    <g:render template="linkBanner" model="[ active: 'seqCenters']"/>

    <h1><g:message code="dataFields.seqCenter.header"/></h1>
    <span class="annotation"><g:message code="dataFields.title.caseInsensitive"/></span>
    <div class="otpDataTables">
    <table id="metadatafields-datatable">
        <thead>
        <tr>
            <th title="${g.message(code: "dataFields.seqCenter.name.tooltip")}"><g:message code="dataFields.seqCenter.name"/></th>
            <th title="${g.message(code: "dataFields.seqCenter.listSeqCenterDirName.tooltip")}"><g:message code="dataFields.seqCenter.listSeqCenterDirName"/></th>
            <th title="${g.message(code: "dataFields.seqCenter.autoImportDir.tooltip")}"><g:message code="dataFields.seqCenter.autoImportDir"/></th>
            <th title="${g.message(code: "dataFields.seqCenter.autoImportable.tooltip")}"><g:message code="dataFields.seqCenter.autoImportable"/></th>
            <th title="${g.message(code: "dataFields.seqCenter.importDirsAllowLinking.tooltip")}"><g:message code="dataFields.seqCenter.importDirsAllowLinking"/></th>
            <th hidden><g:message code="dataFields.seqCenter.importDirsAllowLinking"/></th>
            <th title="${g.message(code: "dataFields.seqCenter.copyMetadataFile.tooltip")}"><g:message code="dataFields.seqCenter.copyMetadataFile"/></th>
        </tr>
        </thead>
        <tbody>
        <g:each var="seqCenter" in="${seqCenters}">
            <tr>
                <td>${seqCenter.name}</td>
                <td>${seqCenter.dirName}</td>
                <td>
                    <otp:editorSwitch
                            roles="DISABLED"
                            link="${g.createLink(controller: 'metaDataFields', action: 'updateAutoImportDirectory', params: ['seqCenter.id': seqCenter.id])}"
                            value="${seqCenter.autoImportDir}"/>
                </td>
                <td>
                    <otp:editorSwitch
                        roles="DISABLED"
                        template="dropDown"
                        link="${g.createLink(controller: 'metaDataFields', action: 'updateAutoImportable', params: ['seqCenter.id': seqCenter.id])}"
                        values="${["true", "false"]}"
                        value="${seqCenter.autoImportable}"/>
                </td>
                <td>
                    <g:each var="importDirs" in="${seqCenter.importDirsAllowLinking}">
                        <otp:editorSwitch
                            roles="ROLE_OPERATOR"
                            link="${g.createLink(controller: 'metaDataFields', action: 'updateImportDirsAllowLinking', params: ['seqCenter.id': seqCenter.id, 'oldAbsolutePath': importDirs])}"
                            value="${importDirs}"/>
                    </g:each>
                    <otp:editorSwitchNewValues
                            roles="ROLE_OPERATOR"
                            labels="${["Path"]}"
                            textFields="${["absolutePath"]}"
                            link="${g.createLink(controller: 'metaDataFields', action: 'createImportDirsAllowLinking', params: ['seqCenter.id': seqCenter.id])}"/>
                </td>
                <td hidden><span class="keep-whitespace">${seqCenter.importDirsAllowLinking.join(";\n")}</span></td>
                <td>
                    <otp:editorSwitch
                        roles="DISABLED"
                        template="dropDown"
                        link="${g.createLink(controller: 'metaDataFields', action: 'updateCopyMetadataFile', params: ['seqCenter.id': seqCenter.id])}"
                        values="${["true", "false"]}"
                        value="${seqCenter.copyMetadataFile}"/>
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>
    </div>
    <otp:editorSwitchNewValues
            roles="ROLE_OPERATOR"
            labels="${["Name", "Directory"]}"
            textFields="${["name", "dirName"]}"
            link="${g.createLink(controller: 'metaDataFields', action: 'createSeqCenter')}"/>
    <br>
    <br>
</div>
</body>
</html>
