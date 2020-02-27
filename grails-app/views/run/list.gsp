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
        <title><g:message code="run.list.title"/></title>
        <asset:javascript src="common/DataTableFilter.js"/>
        <asset:javascript src="pages/run/list/datatable.js"/>
    </head>
<body>
    <div class="body">
        <div class="dataTable_searchContainer">
            <div id="searchbox" class="rounded-page-header-box">
                <span class="blue_label"><g:message code="simple.quick.search"/> :</span>
                <input type="text" class="dataTable_search :" onKeyUp='$.otp.simpleSearch.search(this, "runTable");' placeholder="min. 3 characters">
            </div>
        </div>
        <div class="searchCriteriaTableSequences">
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
                                <option value="none"><g:message code="sequence.search.none"/></option>
                                <option value="seqCenterSelection"><g:message code="run.search.seqCenter"/></option>
                                <option value="runSearch"><g:message code="run.search.run"/></option>
                                <option value="dateCreatedSelection"><g:message code="run.search.dateCreated"/></option>
                                <option value="dateExecutedSelection"><g:message code="run.search.dateExecuted"/></option>
                            </select>
                        </td>
                        <td class="value">
                                <g:select class="criteria" name="seqCenterSelection" from="${seqCenters}" optionValue="name" optionKey="id" style="display: none"/>
                                <input class="criteria" type="text" name="runSearch" style="display: none"/>
                                <span id="dateCreatedSelection" class="dateSelection" style="display: none">
                                    <g:message code="search.from.date"/>:<g:datePicker name="dateCreatedSelection_start" value="${dateCreated}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
                                    <g:message code="search.to.date"/>:<g:datePicker name="dateCreatedSelection_end" value="${dateCreated}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
                                </span>
                                <span id="dateExecutedSelection" class="dateSelection" style="display: none">
                                    <g:message code="search.from.date"/>:<g:datePicker name="dateExecutedSelection_start" value="${dateExecuted}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
                                    <g:message code="search.to.date"/>:<g:datePicker name="dateExecutedSelection_end" value="${dateExecuted}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
                                </span>
                        </td>
                        <td class="remove">
                            <input class="blue_labelForPlus" type="button" value="${g.message(code: "otp.filter.remove")}" style="display: none"/>
                        </td>
                        <td class="add">
                            <input class="blue_labelForPlus" type="button" value="${g.message(code: "otp.filter.add")}" style="display: none"/>
                        </td>
                    </tr>
                </table></td></tr>
            </table>
        </div>
        <div class="otpDataTables">
            <otp:dataTable codes="${tableHeader}" id="runTable"/>
        </div>
    <asset:script>
        $(function() {
            $.otp.runList();
        });
    </asset:script>
    </div>
</body>
</html>
