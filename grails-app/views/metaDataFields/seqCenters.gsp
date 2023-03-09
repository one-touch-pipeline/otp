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
    <asset:javascript src="pages/metaDataFields/datatable.js"/>
    <asset:javascript src="pages/metaDataFields/seqCenters/datatable.js"/>
</head>

<body>
<div class="body fixed-table-header wrapTableHeader metaDataFields">
    <g:render template="/templates/messages"/>
    <g:render template="tabMenu"/>

    <h1><g:message code="dataFields.seqCenter.header"/></h1>
    <otp:annotation type="info"><g:message code="dataFields.title.caseInsensitive"/></otp:annotation>
    <div class="otpDataTables">
    <table id="metadatafields-datatable">
        <thead>
        <tr>
            <th class="export_column" title="${g.message(code: "dataFields.seqCenter.name.tooltip")}"><g:message code="dataFields.seqCenter.name"/></th>
            <th class="export_column" title="${g.message(code: "dataFields.seqCenter.listSeqCenterDirName.tooltip")}"><g:message code="dataFields.seqCenter.listSeqCenterDirName"/></th>
            <th class="export_column" title="${g.message(code: "dataFields.seqCenter.autoImportDir.tooltip")}"><g:message code="dataFields.seqCenter.autoImportDir"/></th>
            <th class="export_column" title="${g.message(code: "dataFields.seqCenter.autoImportable.tooltip")}"><g:message code="dataFields.seqCenter.autoImportable"/></th>
            <th class="export_column" title="${g.message(code: "dataFields.seqCenter.legacy")}"><g:message code="dataFields.seqCenter.legacy"/></th>
        </tr>
        </thead>
        <tbody>
        <g:each status="i" var="seqCenter" in="${seqCenters}">
            <tr class="${seqCenter.legacy ? 'text-muted' : ''}">
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
                    <g:render template="/templates/slider" model="[
                            targetAction: 'changeSeqCenterLegacyState',
                            objectName  : 'seqCenter',
                            object      : seqCenter,
                            i           : i,
                    ]"/>
                    <span hidden>${seqCenter.legacy}</span>
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
