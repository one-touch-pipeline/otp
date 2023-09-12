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
<html>
<head>
    <title>${g.message(code: "departmentConfig.title")}</title>
    <asset:javascript src="common/UserAutoComplete.js"/>
    <asset:javascript src="pages/departmentConfiguration/index.js"/>
</head>

<body>
<div class="container-fluid otp-main-container">
    <h1>${g.message(code: "departmentConfig.departments.title")}</h1>
    <table class="table table-sm table-striped">
        <thead>
        <tr>
            <th>${g.message(code: "departmentConfig.departments.ouNumber")}</th>
            <th>${g.message(code: "departmentConfig.departments.costCenter")}</th>
        </tr>
        </thead>
        <tbody>
        <!-- Rows of current department deputies -->
        <g:each in="${departmentList}" var="department">
            <tr>
                <td>${department.ouNumber}</td>
                <td>${department.costCenter}</td>
            </tr>
        </g:each>
        </tbody>
    </table>

    <h1>${g.message(code: "departmentConfig.departmentDeputy.title")}</h1>

    <g:form controller="departmentConfiguration" method="POST" useToken="true">
    <table id="deputyTable" class="table table-sm table-striped">
        <thead>
        <tr>
            <th>${g.message(code: "departmentConfig.departmentDeputy.deputyName")}</th>
            <th>${g.message(code: "departmentConfig.departmentDeputy.dateDeputyGranted")}</th>
            <th></th>
        </tr>
        </thead>
    <tbody>
    <!-- Rows of current department deputies -->
        <g:each in="${departmentDeputyList}" var="departmentDeputy" status="index">
            <tr>
                <td>${departmentDeputy.deputyUsername}</td>
            <td>${departmentDeputy.dateDeputyGranted}</td>
            <td>
            <g:form controller="departmentConfiguration" method="POST" useToken="true">
                <input hidden value="${departmentDeputy.deputyRelationId}" name="deputyRelationToDelete.id"/>
                <button formnovalidate class="btn btn-primary deputy-remove-btn" formaction="${g.createLink(action: "removeDeputy")}"
                        title="${g.message(code: "departmentConfig.departmentDeputy.remove")}"><i class="bi bi-trash"></i></button></td>
            </g:form>
            </td>
        </tr>
        </g:each>
    <!-- Row to add new department deputy -->

            <tr>
                <td>
                    <div class="user-auto-complete">
                        <input required class="username-input input-field autocompleted form-control" name="deputyUsername"
                               id="deputyUsername" autocomplete="off"/>
                    </div>
                </td>
                <td></td>
                <td><button id="addDeputyBtn" class="btn btn-primary" formaction="${g.createLink(action: "addDeputy")}"
                            title="${g.message(code: "departmentConfig.departmentDeputy.add")}"><i class="bi bi-plus-square"></i></button></td>
            </tr>
            </tbody>
    </table>
    </g:form>
</div>
</body>
</html>