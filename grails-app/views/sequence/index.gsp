<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="sequence.title"/></title>
<r:require module="jqueryDatatables"/>
</head>
<body>
    <div class="body">
        <table id="searchCriteriaTable">
            <tr>
                <td>
                    <select name="criteria">
                        <option value="none"><g:message code="sequence.search.none"/></option>
                        <option value="projectSelection"><g:message code="sequence.search.project"/></option>
                        <option value="individualSearch"><g:message code="sequence.search.individual"/></option>
                        <option value="sampleTypeSelection"><g:message code="sequence.search.sample"/></option>
                        <option value="seqTypeSelection"><g:message code="sequence.search.seqType"/></option>
                        <option value="libraryLayoutSelection"><g:message code="sequence.search.libLayout"/></option>
                        <option value="seqCenterSelection"><g:message code="sequence.search.seqCenter"/></option>
                        <option value="runSearch"><g:message code="sequence.search.run"/></option>
                    </select>
                </td>
                <td>
                    <g:select name="projectSelection" from="${projects}" optionValue="name" optionKey="id" style="display: none"/>
                    <input type="text" name="individualSearch" style="display: none"/>
                    <g:select name="sampleTypeSelection" from="${sampleTypes}" optionValue="name" optionKey="id" style="display: none"/>
                    <g:select name="seqTypeSelection" from="${seqTypes}" style="display: none"/>
                    <g:select name="libraryLayoutSelection" from="${libraryLayouts}" style="display: none"/>
                    <g:select name="seqCenterSelection" from="${seqCenters}" optionValue="name" optionKey="id" style="display: none"/>
                    <input type="text" name="runSearch" style="display: none"/>
                </td>
                <td>
                    <input type="button" value="+" style="display: none"/>
                </td>
            </tr>
        </table>
        <table id="sequenceTable">
            <thead>
                <tr>
                    <th><g:message code="sequence.list.headers.project"/></th>
                    <th><g:message code="sequence.list.headers.individual"/></th>
                    <th><g:message code="sequence.list.headers.sampleType"/></th>
                    <th><g:message code="sequence.list.headers.seqType"/></th>
                    <th><g:message code="sequence.list.headers.libLayout"/></th>
                    <th><g:message code="sequence.list.headers.seqCenter"/></th>
                    <th><g:message code="sequence.list.headers.run"/></th>
                    <th><g:message code="sequence.list.headers.lane"/></th>
                    <th><g:message code="sequence.list.headers.fastqc"/></th>
                    <th><g:message code="sequence.list.headers.otpAlignment"/></th>
                    <th><g:message code="sequence.list.headers.origAlignment"/></th>
                    <th><g:message code="sequence.list.headers.date"/></th>
                </tr>
            </thead>
            <tbody></tbody>
            <tfoot>
                <tr>
                    <th><g:message code="sequence.list.headers.project"/></th>
                    <th><g:message code="sequence.list.headers.individual"/></th>
                    <th><g:message code="sequence.list.headers.sampleType"/></th>
                    <th><g:message code="sequence.list.headers.seqType"/></th>
                    <th><g:message code="sequence.list.headers.libLayout"/></th>
                    <th><g:message code="sequence.list.headers.seqCenter"/></th>
                    <th><g:message code="sequence.list.headers.run"/></th>
                    <th><g:message code="sequence.list.headers.lane"/></th>
                    <th><g:message code="sequence.list.headers.fastqc"/></th>
                    <th><g:message code="sequence.list.headers.otpAlignment"/></th>
                    <th><g:message code="sequence.list.headers.origAlignment"/></th>
                    <th><g:message code="sequence.list.headers.date"/></th>
                </tr>
            </tfoot>
        </table>
    </div>
    <div class="buttons" style="clear: both">
        <a href="exportCsv" id="export-csv"><g:message code="sequence.list.export.csv"/></a>
    </div>
    <r:script>
$(function() {
    $.otp.sequence.register();
});
    </r:script>
</body>
</html>
