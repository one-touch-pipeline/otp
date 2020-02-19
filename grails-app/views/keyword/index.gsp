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

    <h1><g:message code="keyword.index.header" args="[project.name]"/></h1>

    <h2><g:message code="keyword.index.addKeywords" args="[project.name]"/></h2>
    <g:form controller="keyword" action="save">
        <input type="hidden" value="${project.id}" name="project">
        <g:textField list="keywordList" name="value" size="50" autofocus="true" required="true" autocomplete="off"/>
        <datalist id="keywordList">
            <g:each in="${keywords}" var="keyword">
                <option value="${keyword.name}">${keyword.name}</option>
            </g:each>
        </datalist>
        <g:submitButton name="Add"/>
    </g:form>

    <h2><g:message code="keyword.index.keywordsOfProject" args="[project.name]"/></h2>
    <div class="scrollable-keyword-list">
        <ul>
            <g:if test="${projectKeywords}">
                <g:each var="keyword" in="${projectKeywords}">
                    <li>
                        <div class="no-wrap-list-item">
                            <g:form controller="keyword" action="remove">
                                <span class="keyword-in-list">${keyword.name}</span>
                                <g:submitButton name="Remove"/>
                                <input type="hidden" value="${project.id}" name="project.id">
                                <input type="hidden" value="${keyword.id}" name="keyword.id">
                            </g:form>
                        </div>
                    </li>
                </g:each>
            </g:if>
            <g:else>
                <li><g:message code="keyword.index.keywordsOfProject.none"/></li>
            </g:else>
        </ul>
    </div>
    <hr>
    <h2><g:message code="keyword.index.overviewKeywords" args="[keywords.size()]"/></h2>
    <div class="scrollable-keyword-list">
        <ul>
            <g:each in="${keywords}" var="keyword">
                <li><span class="keyword-in-list">${keyword.name}</span></li>
            </g:each>
        </ul>
    </div>
    <br>
    <br>
</div>
</body>
</html>
