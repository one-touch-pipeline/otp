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
    <title><g:message code="dataFields.adapterFile.title"/></title>
</head>

<body>
<div class="body">
    <g:render template="/templates/messages"/>

    <h1><g:message code="dataFields.adapterFile.header" args="[libraryPreparationKit]"/></h1>

    <table class="key-value-table key-input">
        <tr>
            <td><g:message code="dataFields.libPrepKit.name"/></td>
            <td>${libraryPreparationKit?.name}</td>
        </tr>
        <tr>
            <td><g:message code="dataFields.libPrepKit.reverseComplementAdapterSequenceShort"/></td>
            <td>${libraryPreparationKit?.reverseComplementAdapterSequence}</td>
        </tr>
        <tr>
            <td><g:message code="dataFields.libPrepKit.importAlias"/></td>
            <td>
                <g:each var="alias" in="${libraryPreparationKit?.importAlias}">
                    ${alias}<br>
                </g:each>
            </td>
        </tr>
    </table>

    <h2><g:message code="dataFields.adapterFile.path"/></h2>
    <code>${libraryPreparationKit?.adapterFile}</code>

    <h2><g:message code="dataFields.adapterFile.content"/></h2>
    <pre>${adapterFileContent}</pre>
</div>
</body>
</html>
