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
    <title><g:message code="dataSwap.title"/></title>
    <asset:javascript src="pages/dataSwap/index.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">

    <h2><g:message code="dataSwap.title"/></h2>

    <div class="card mb-3">
        <div class="card-body">
            <g:form name="data-swap-form" useToken="true" controller="dataSwap" action="swapData" enctype="multipart/form-data">
                <div class="mb-3">
                    <label for="data-swap-file"><g:message code="dataSwap.label.file"/></label>
                    <input class="form-control drag-and-drop" type="file" multiple="true" name="dataSwapFile" id="data-swap-file">
                </div>

                <div class="mb-3">
                    <label for="delimiter"><g:message code="dataSwap.label.delimiter"/></label>
                    <g:select name="delimiter" class="mb-3 use-select-2"
                              from="${delimiters}" value="${delimiter}" optionValue="displayName"
                              noSelection="['': 'Choose a Delimiter']"/>
                </div>

                <div class="form-check mb-3" id="ignore-md5-sum-row">
                    <input class="form-check-input" checked name="dryRun" type="checkbox" id="dry-run">
                    <label class="form-check-label" for="dry-run">
                        <g:message code="dataSwap.label.dryRun"/>
                    </label>
                </div>

                <button type="submit" class="btn btn-primary" id="data-swap-btn">
                    <span id="data-swap-spinner" hidden class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                    <g:message code="dataSwap.btn.perform"/>
                </button>
            </g:form>
        </div>
    </div>

    <h2><g:message code="dataSwap.validationErrors"/></h2>
    <table id="data-swap-validation-table" class="table table-sm table-striped table-hover table-bordered">
        <thead>
        <tr>
            <th><g:message code="dataSwap.table.message"/></th>
            <th><g:message code="dataSwap.table.cell"/></th>
        </tr>
        </thead>
        <tbody></tbody>
    </table>
</div>
</body>
</html>
