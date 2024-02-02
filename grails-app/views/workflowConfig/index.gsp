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
    <asset:javascript src="pages/workflowConfig/index/functions.js"/>
    <asset:stylesheet src="pages/workflowConfig/selector/styles.less"/>
    <g:set var="entityName" value="${message(code: 'workflowConfig.config', default: 'Pipeline Config')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>
</head>

<body>
<div class="container-fluid otp-main-container">
    <h3><g:message code="workflowConfig.config"/></h3>
    <p><g:message code="workflowConfig.description"/></p>

    <!-- search query -->
    <div class="no-gutters search-query">
        <g:render template="search"/>
        <div class="row">
            <div class="col-sm">
                <button id="search-button" type="button" class="btn btn-primary" title="${g.message(code: "workflowConfig.button.search.title")}"
                        data-bs-toggle="tooltip" onclick="$.otp.workflowConfig.search()">
                    <g:message code="workflowConfig.button.search"/>
                </button>
                <button id="clear-button" type="button" class="btn btn-primary" title="${g.message(code: "workflowConfig.button.clear.title")}"
                        data-bs-toggle="tooltip" onclick="$.otp.workflowConfig.clear()">
                    <g:message code="workflowConfig.button.clear"/>
                </button>
                <button id="create-button" type="button" class="btn btn-primary" data-bs-toggle="modal" title="${g.message(code: "workflowConfig.button.create.title")}"
                        data-bs-toggle="tooltip" data-bs-target="#workflowConfigModal" data-operation="create">
                    <g:message code="workflowConfig.button.create"/>
                </button>
            </div>
            <div class="col-sm mb-3">
                <div class="input-group">
                    <div class="input-group-prepend">
                        <label class="input-group-text"><g:message code="workflowConfig.selector.type"/></label>
                    </div>
                    <select name="type" class="custom-select use-select-2" multiple>
                        <g:each in="${selectorTypes}" var="selectorType">
                            <option value="${selectorType}">${selectorType}</option>
                        </g:each>
                    </select>
                </div>
            </div>
        </div>
        <div class="row">
            <div class="col">
                <g:select id="workflowSelector"
                          name="workflowSelector"
                          from="${allSelectors}"
                          class="use-select-2 btn-primary"
                          noSelection="${['': '']}"
                          optionKey="id"
                          optionValue="name"
                          data-placeholder="${g.message(code: "workflowConfig.button.searchBySelector")}"
                          title="${g.message(code: "workflowConfig.button.searchBySelector.title")}"/>
            </div>
        </div>
    </div>

    <table id="workflowConfigResult" class="table table-sm table-striped table-hover table-bordered w-100" data-page-length='50'>
        <thead>
        <tr>
            <g:each in="${columns}" var="column" status="i">
                <th title="${g.message(code: column.message)}">${g.message(code: column.message)}</th>
            </g:each>
        </tr>
        </thead>
        <tbody>
        </tbody>
        <tfoot>
        </tfoot>
    </table>
</div>

<g:render template="edit"/>

</body>
</html>
