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

%{--
To be used in conjunction with: common/DataTableFilter.js
--}%

<div id="data-table-filter-container" class="rounded-page-header-box" style="display: table">
    <h3 style="display: table-cell; vertical-align: middle"><g:message code="search.extended.header"/>:</h3>
    <table style="display: table-cell" id="searchCriteriaTable">
        <tr class="dtf_row">
            <td class="attribute">
                <select class="dtf_criterium" autocomplete="off">
                    <option value="none"><g:message code="search.extended.noCriteria"/></option>
                    <g:each in="${filterTree}" var="column">
                        <option value="${column.name}"><g:message code="${column.msgcode}"/></option>
                    </g:each>
                </select>
            </td>
            <td class="value">
                <g:each in="${filterTree}" var="column">
                    <span id="dtf_${column.name}" class="dtf_value_span" style="display: none">
                    <g:if test="${column.type == "LIST"}">
                        <g:if test="${column.value && column.key}">
                            <g:select name="${column.name}" from="${column.from}"
                                      optionValue="${column.value}" optionKey="${column.key}" autocomplete="off" />
                        </g:if>
                        <g:else>
                            <g:select name="${column.name}" from="${column.from}" autocomplete="off" />
                        </g:else>

                    </g:if>
                    <g:elseif test="${column.type == "DATE"}">
                        <span id="${column.name}" class="dateSelection">
                            <g:message code="search.from.date"/>:
                            <input type="date" name="${column.name}_start" autocomplete="off"
                                   value="${new Date().minus(7).format('yyyy-MM-dd')}"/>
                            <g:message code="search.to.date"/>:
                            <input type="date" name="${column.name}_end" autocomplete="off"
                                   value="${new Date().format('yyyy-MM-dd')}"/>
                        </span>
                    </g:elseif>
                    <g:elseif test="${column.type == "NUMBER"}">
                        <input type="number" name="${column.name}" autocomplete="off" />
                    </g:elseif>
                    <g:else>
                        <input type="text" name="${column.name}" autocomplete="off"/>
                    </g:else>
                    </span>
                </g:each>
            </td>
            <td class="remove" style="display: none">
                <input type="button" value="- ${g.message(code: "otp.filter.remove")}"/>
            </td>
            <td class="add" style="display: none">
                <input type="button" value="+ ${g.message(code: "otp.filter.add")}"/>
            </td>
        </tr>
    </table>
</div>
