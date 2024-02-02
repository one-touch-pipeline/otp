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
    <title><g:message code="individual.list.title"/></title>
    <asset:javascript src="common/DataTableFilter.js"/>
    <asset:javascript src="pages/individual/list/datatable.js"/>
</head>

<body>
    <div class="body">
        <div class="dataTable_searchContainer">
            <div id="searchbox" class="rounded-page-header-box">
                <g:message code="search.simple.header"/>:
                <input type="text" class="dataTable_search" onKeyUp='$.otp.simpleSearch.search(this, "individualTable");' placeholder="min. 3 characters">
            </div>
        </div>

        <g:render template="/templates/dataTableFilter" model="[filterTree: filterTree]"/>

        <div class="otpDataTables">
            <otp:dataTable codes="${tableHeader}" id="individualTable"/>
        </div>
    </div>
</body>
</html>
