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
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="keyword.index.title"/></title>
    <asset:javascript src="pages/projectConfig/index/functions.js"/>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <g:render template="/templates/projectSelection" model="['project': project, 'projects': projects]"/>
    <table>
            <tr>
                <td>
                    <ul>
                    <strong><g:message code="keyword.index.editedKeywords"/> ${project}</strong>
                    <g:each in="${project.keywords}" var="projectKeyword">
                        <li>
                            <div class="grid-element">
                        <g:form controller="keyword" action="remove">
                            ${projectKeyword.name}
                            <g:submitButton name="Delete"/>
                            <input type="hidden" value="${project.id}" name="project.id">
                            <input type="hidden" value="${projectKeyword.id}" name="keyword.id">
                        </g:form>
                            </div>
                        </li>
                    </g:each>
                    </ul>
                </td>
    <g:form controller="keyword" action="save">
                <td><input type="hidden" value="${project.id}" name="project"></td>
                <td class="myKey"><g:message code="keyword.index.addKeywords"/> ${project}</td>
                <td class="multi-input-field">
                        <div class="field">
                            <g:textField list="keywordList" name="value" size="80" autofocus="true" required="true"/><g:submitButton name="Update"/>
                        </div>
                    <datalist id="keywordList">
                        <g:each in="${keywords}" var="keyword">
                            <option value="${keyword.name}">${keyword.name}</option>
                        </g:each>
                    </datalist>
                </td>
            </tr>
        </table>
    </g:form>
    <br>
    <hr>
    <table>
        <tr>
            <td><h3><g:message code="keyword.index.overviewKeywords"/></h3></td>
        </tr>
    </table>
        <div style="overflow: auto; max-height: 60em;">
            <g:each in="${keywords}" var="keyword">
                <table>
                    <tr>
                        <td>${keyword.name}</td>
                    </tr>
                </table>
            </g:each>
        </div>
</div>
</body>
</html>
