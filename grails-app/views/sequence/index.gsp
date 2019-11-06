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
    <title><g:message code="sequence.title"/></title>
    <asset:javascript src="common/DataTableFilter.js"/>
    <asset:javascript src="pages/sequence/index/datatable.js"/>
</head>
<body>
    <div class="body">
    <g:if test="${projects}">
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
                            <option value="none"><g:message code="sequence.search.none"/></option>
                            <option value="projectSelection"><g:message code="sequence.search.project"/></option>
                            <option value="individualSearch"><g:message code="sequence.search.individual"/></option>
                            <option value="sampleTypeSelection"><g:message code="sequence.search.sample"/></option>
                            <option value="seqTypeSelection"><g:message code="sequence.search.seqType"/></option>
                            <option value="ilseIdSearch"><g:message code="sequence.search.ilse"/></option>
                            <option value="libraryLayoutSelection"><g:message code="sequence.search.libLayout"/></option>
                            <option value="singleCell"><g:message code="sequence.search.singleCell"/></option>
                            <option value="libraryPreparationKitSelection"><g:message code="sequence.search.libPrepKit"/></option>
                            <option value="antibodyTargetSelection"><g:message code="sequence.search.antibodyTarget"/></option>
                            <option value="seqCenterSelection"><g:message code="sequence.search.seqCenter"/></option>
                            <option value="runSearch"><g:message code="sequence.search.run"/></option>
                        </select>
                    </td>
                    <td class="value">
                        <g:select class="criteria" name="projectSelection" from="${projects}" optionValue="displayName" optionKey="id" style="display: none"/>
                        <input class="criteria" type="text" name="individualSearch" style="display: none" placeholder="min. 3 characters"/>
                        <g:select class="criteria" name="sampleTypeSelection" from="${sampleTypes}" optionValue="name" optionKey="id" style="display: none"/>
                        <g:select class="criteria" name="seqTypeSelection" from="${seqTypes}" style="display: none"/>
                        <input class="criteria" type="text" name="ilseIdSearch" style="display: none"/>
                        <g:select class="criteria" name="libraryLayoutSelection" from="${libraryLayouts}" style="display: none"/>
                        <g:select class="criteria" name="singleCell" from="[true, false]" style="display: none"/>
                        <g:select class="criteria" name="libraryPreparationKitSelection" from="${libraryPreparationKits}" style="display: none"/>
                        <g:select class="criteria" name="antibodyTargetSelection" from="${antibodyTargets}" optionValue="name" optionKey="name" style="display: none"/>
                        <g:select class="criteria" name="seqCenterSelection" from="${seqCenters}" optionValue="name" optionKey="id" style="display: none"/>
                        <input class="criteria" type="text" name="runSearch" style="display: none" placeholder="min. 3 characters"/>
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
             <p id="withdrawn_description">
                 <g:message code="sequence.information.withdrawn"/>
            </p>
        </div>
        <div class="otpDataTables">
        <otp:dataTable codes="${tableHeader}" id="sequenceTable"/>
        </div>
    <asset:script type="text/javascript">
        $(function() {
            $.otp.sequence.register();
        });
    </asset:script>
    </g:if>
    <g:else>
        <g:render template="/templates/noProject"/>
    </g:else>
    </div>
</body>
</html>
