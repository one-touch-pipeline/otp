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
    <title><g:message code="individual.list.title"/></title>
    <asset:javascript src="pages/individual/list/datatable.js"/>
</head>
<body>
    <div class="body">
    <g:if test="${projects}">
        <div class="dataTable_searchContainer">
            <div id="searchbox">
                <span class="blue_label"><g:message code="simple.quick.search"/> :</span>
                <input type="text" class="dataTable_search" onKeyUp='$.otp.simpleSearch.search(this, "individualTable");' placeholder="min. 3 characters">
            </div>
        </div>
           <div class= "searchCriteriaTableSequences">
        <table id="searchCriteriaTable2">
            <tr>
            <td>
                <span class="blue_label"><g:message code="extended.search"/> :</span>
            </td>
           <td>
         <table id="searchCriteriaTable">
            <tr>
                <td class="attribute">
                    <select class="criteria" name="criteria">
                        <option value="none"><g:message code="individual.search.none"/></option>
                        <option value="projectSelection"><g:message code="individual.search.project"/></option>
                        <option value="pidSearch"><g:message code="individual.search.pid"/></option>
                        <option value="mockFullNameSearch"><g:message code="individual.search.mockFullName"/></option>
                        <option value="mockPidSearch"><g:message code="individual.search.mockPid"/></option>
                        <option value="typeSelection"><g:message code="individual.search.type"/></option>
                    </select>
                </td>
                <td class="value">
                    <g:select class="criteria" name="projectSelection" from="${projects}" optionValue="name" optionKey="id" style="display: none"/>
                    <input class="criteria" type="text" name="pidSearch" style="display: none" placeholder="min. 3 characters"/>
                    <input class="criteria" type="text" name="mockFullNameSearch" style="display: none" placeholder="min. 3 characters"/>
                    <input class="criteria" type="text" name="mockPidSearch" style="display: none" placeholder="min. 3 characters"/>
                    <g:select class="criteria" name="typeSelection" from="${individualTypes}" style="display: none"/>
                </td>
                <td class="remove">
                    <input class="blue_labelForPlus" type="button" value="${g.message(code: "otp.filter.remove")}" style="display: none"/>
                </td>
                <td class="add">
                    <input class="blue_labelForPlus" type="button" value="${g.message(code: "otp.filter.add")}" style="display: none"/>
                 </td>
            </tr>
        </table>
        </td>
        </tr>
        </table>
        </div>
        <div class="otpDataTables">
        <otp:dataTable codes="${[
                    'individual.list.pid',
                    'individual.list.mockName',
                    'individual.list.mockPid',
                    'individual.list.project',
                    'individual.list.type'
        ]}" id="individualTable"/>
        </div>
    <asset:script type="text/javascript">
        $(function() {
            $.otp.individualList();
        });
    </asset:script>
    </g:if>
    <g:else>
        <h3><g:message code="default.no.project"/></h3>
    </g:else>
    </div>
</body>
</html>
