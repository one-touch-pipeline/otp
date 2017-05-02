<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="sequence.title"/></title>
    <asset:javascript src="pages/sequence/index/datatable.js"/>
</head>
<body>
    <div class="body">
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
                            <option value="seqCenterSelection"><g:message code="sequence.search.seqCenter"/></option>
                            <option value="runSearch"><g:message code="sequence.search.run"/></option>
                        </select>
                    </td>
                    <td class="value">
                        <g:select class="criteria" name="projectSelection" from="${projects}" optionValue="name" optionKey="id" style="display: none"/>
                        <input class="criteria" type="text" name="individualSearch" style="display: none" placeholder="min. 3 characters"/>
                        <g:select class="criteria" name="sampleTypeSelection" from="${sampleTypes}" optionValue="name" optionKey="id" style="display: none"/>
                        <g:select class="criteria" name="seqTypeSelection" from="${seqTypes}" style="display: none"/>
                        <input class="criteria" type="text" name="ilseIdSearch" style="display: none"/>
                        <g:select class="criteria" name="libraryLayoutSelection" from="${libraryLayouts}" style="display: none"/>
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
        <otp:dataTable codes="${[
                    'sequence.list.headers.project',
                    'sequence.list.headers.individual',
                    'sequence.list.headers.sampleType',
                    'sequence.list.headers.seqType',
                    'sequence.list.headers.libLayout',
                    'sequence.list.headers.seqCenter',
                    'sequence.list.headers.run',
                    'sequence.list.headers.lane',
                    'sequence.list.headers.library',
                    'sequence.list.headers.fastqc',
                    'sequence.list.headers.ilseId',
                    'sequence.list.headers.warning',
                    'sequence.list.headers.date'
            ]}" id="sequenceTable"/>
        </div>
    </div>
    <asset:script>
        $(function() {
            $.otp.sequence.register();
        });
    </asset:script>
</body>
</html>
