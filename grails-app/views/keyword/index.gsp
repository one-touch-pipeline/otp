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
    <asset:javascript src="taglib/EditorSwitch.js"/>
</head>
<body>
<div class="body">
    <g:render template="/templates/messages"/>
    <g:render template="/templates/projectSelection"/>

    <div class="keyword-header">
        <div class="item">
            <h1><g:message code="keyword.index.header" args="[selectedProject.name]"/></h1>
        </div>
        <div class="item">
            <g:form controller="projectConfig" view="index">
                <g:submitButton name="back" value="Back to Overview"/>
            </g:form>
        </div>
    </div>

    <div class="keyword-container">
        <div class="item">
            <h2><g:message code="keyword.index.keywordsOfProject" args="[selectedProject.name, projectKeywords.size()]"/></h2>
            <div class="scrollable-keyword-list">
                <ul>
                    <li>
                        <g:form controller="keyword" action="createOrAdd">
                            <g:textField list="keywordList" name="value" size="30" autofocus="true" required="true" autocomplete="off"/>
                            <datalist id="keywordList">
                                <g:each in="${availableKeywords}" var="keyword">
                                    <option value="${keyword.name}">${keyword.name}</option>
                                </g:each>
                            </datalist>
                            <g:submitButton name="Add"/>
                        </g:form>
                    </li>
                    <g:if test="${projectKeywords}">
                        <g:each var="keyword" in="${projectKeywords}">
                            <li><g:render template="keywordListItem" model="[keyword: keyword, selectedProject: selectedProject, action: 'remove']"/></li>
                        </g:each>
                    </g:if>
                    <g:else>
                        <li><g:message code="keyword.index.keywordsOfProject.none"/></li>
                    </g:else>
                </ul>
            </div>
        </div>
        <div class="item">
            <h2><g:message code="keyword.index.otherAvailableKeywords" args="[availableKeywords.size()]"/></h2>
            <div class="scrollable-keyword-list">
                <ul>
                    <g:each in="${availableKeywords}" var="keyword">
                        <li><g:render template="keywordListItem" model="[keyword: keyword, selectedProject: selectedProject, action: 'add']"/></li>
                    </g:each>
                </ul>
            </div>
        </div>
    </div>
    <br>
    <br>
</div>
</body>
</html>
