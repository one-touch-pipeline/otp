<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="run.list.title"/></title>
    </head>
<body>
    <div class="body">
        <div class="dataTable_searchContainer">
            <div id="searchbox">
                <span class="blue_label"><g:message code="simple.quick.search"/> :</span>
                <input type="text" class="dataTable_search :" onKeyUp='$.otp.simpleSearch.search(this, "runTable");' placeholder="min. 3 characters">
            </div>
        </div>
        <div  class= "searchCriteriaTableSequences">
            <table id="searchCriteriaTable2">
                    <tr>
                    <td>
                         <span class="blue_label"><g:message code="extended.search"/> :</span>
                    </td>
                   <td>
                <table id="searchCriteriaTable">
                    <tr>
                        <td>
                            <select class="criteria" name="criteria">
                                <option value="none"><g:message code="sequence.search.none"/></option>
                                <option value="seqCenterSelection"><g:message code="run.search.seqCenter"/></option>
                                <option value="runSearch"><g:message code="run.search.run"/></option>
                                <option value="storageRealmSelection"><g:message code="run.search.storageRealm"/></option>
                                <option value="dateCreatedSelection"><g:message code="run.search.dateCreated"/></option>
                                <option value="dateExecutedSelection"><g:message code="run.search.dateExecuted"/></option>
                            </select>
                        </td>
                        <td>
                                <g:select class="criteria" name="seqCenterSelection" from="${seqCenters}" optionValue="name" optionKey="id" style="display: none"/>
                                <input class="criteria" type="text" name="runSearch" style="display: none"/>
                                <g:select class="criteria" name="storageRealmSelection" from="${storageRealm}" style="display: none"/>
                                <span id="dateCreatedSelection" class="dateSelection" style="display: none">
                                    <g:message code="search.from.date"/>:<g:datePicker name="dateCreatedSelection_start" value="${dateCreated}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
                                    <g:message code="search.to.date"/>:<g:datePicker name="dateCreatedSelection_end" value="${dateCreated}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
                                </span>
                                <span id="dateExecutedSelection" class="dateSelection" style="display: none">
                                    <g:message code="search.from.date"/>:<g:datePicker name="dateExecutedSelection_start" value="${dateExecuted}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
                                    <g:message code="search.to.date"/>:<g:datePicker name="dateExecutedSelection_end" value="${dateExecuted}" precision="day" years="${2010..Calendar.getInstance().get(Calendar.YEAR)}"/>
                                </span>
                        </td>
                        <td>
                            <input id="button" class="blue_labelForPlus" type="button" value="+" style="display: none"/>
                        </td>
                    </tr>
                </table></td></tr>
            </table>
        </div>
        <div class="dataTables_container" id="runTable_containe">
            <otp:dataTable codes="${['run.list.name',
                'run.list.seqCenter',
                'run.list.storageRealm',
                'run.list.dateCreated',
                'run.list.dateExecuted',
                'run.list.blacklisted',
                'run.list.multipleSource',
                'run.list.fastqcState'
                ] }" id="runTable"/>
        </div>
    </div>
    <r:script>
        $(function() {
            $.otp.runList();
        });
    </r:script>
</body>
</html>
