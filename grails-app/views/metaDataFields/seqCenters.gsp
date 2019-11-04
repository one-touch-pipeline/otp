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

    <h3><g:message code="dataFields.seqCenter.header"/></h3>
    <table>
        <thead>
        <tr>
            <th><g:message code="dataFields.seqCenter.name"/></th>
            <th><g:message code="dataFields.seqCenter.listSeqCenterDirName"/></th>
        </tr>
        </thead>
        <tbody>
        <g:each var="seqCenter" in="${seqCenters}">
            <tr>
                <td>${seqCenter.name}</td>
                <td>${seqCenter.dirName}</td>
            </tr>
        </g:each>
        <td colspan="2">
            <otp:editorSwitchNewValues
                    roles="ROLE_OPERATOR"
                    labels="${["Name", "Directory"]}"
                    textFields="${["name", "dirName"]}"
                    link="${g.createLink(controller: 'metaDataFields', action: 'createSeqCenter')}"/>
        </td>
        </tbody>
    </table>
</div>
</body>
</html>
