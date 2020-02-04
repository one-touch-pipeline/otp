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
    <asset:javascript src="pages/metaDataFields/seqPlatforms/datatable.js"/>
</head>

<body>
<div class="body fixed-table-header wrapTableHeader metaDataFields">
    <g:render template="linkBanner"/>

    <h1><g:message code="dataFields.seqPlatform.header"/></h1>
    <span class="annotation"><g:message code="dataFields.title.caseInsensitive"/></span>
    <div class="otpDataTables">
    <table id="metadatafields-datatable">
        <thead>
        <tr>
            <th><g:message code="dataFields.seqPlatform.name"/></th>
            <th><g:message code="dataFields.seqPlatform.listPlatformModelLabel"/></th>
            <th><g:message code="dataFields.seqPlatform.listPlatformModelLabelImportAlias"/></th>
            <th></th>
            <th><g:message code="dataFields.seqPlatform.listSequencingKitLabel"/></th>
            <th><g:message code="dataFields.seqPlaform.listSequencingKitLabelImportAlias"/></th>
            <th></th>
        </tr>
        </thead>
        <tbody>
        <g:each var="seqPlatform" in="${seqPlatforms}">
            <tr>
                <td>${seqPlatform.name}</td>
                <td>${seqPlatform.model}</td>
                <td><span class="keep-whitespace">${seqPlatform.modelImportAliases}</span></td>
                <td>
                    <g:if test="${seqPlatform.hasModel}">
                        <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${["Import Alias"]}"
                                textFields="${["importAlias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createSeqPlatformModelLabelImportAlias', id: seqPlatform.modelId)}"/>
                    </g:if>
                </td>
                <td>${seqPlatform.seqKit}</td>
                <td><span class="keep-whitespace">${seqPlatform.seqKitImportAliases}</span></td>
                <td>
                    <g:if test="${seqPlatform.hasSeqKit}">
                        <otp:editorSwitchNewValues
                                roles="ROLE_OPERATOR"
                                labels="${["Import Alias"]}"
                                textFields="${["importAlias"]}"
                                link="${g.createLink(controller: 'metaDataFields', action: 'createSequencingKitLabelImportAlias', id: seqPlatform.seqKitId)}"/>
                    </g:if>
                </td>
            </tr>
        </g:each>
        </tbody>
    </table>
    </div>
    <otp:editorSwitchNewValues
            roles="ROLE_OPERATOR"
            labels="${["Platform", "Model", "Kit"]}"
            textFields="${["platform", "model", "kit"]}"
            link="${g.createLink(controller: 'metaDataFields', action: 'createSeqPlatform')}"/>
    <br>
    <br>
</div>
</body>
</html>
