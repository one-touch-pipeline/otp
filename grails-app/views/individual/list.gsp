<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="individual.list.title"/></title>
    <r:require module="jqueryDatatables"/>
</head>
<body>
    <div class="body">
        <div class="dataTable_searchContainer">
            <g:message code="individual.list.search"/>:
            <input type="text" class="dataTable_search" onKeyUp='$.otp.simpleSearch.search(this, "individualTable");' placeholder="min. 3 characters">
        </div>
        <div class="searchCriteriaTable_label"><g:message code="sequence.list.search"/>:</div>
        <table id="searchCriteriaTable">
            <tr>
                <td>
                    <select name="criteria">
                        <option value="none"><g:message code="individual.search.none"/></option>
                        <option value="projectSelection"><g:message code="individual.search.project"/></option>
                        <option value="pidSearch"><g:message code="individual.search.pid"/></option>
                        <option value="mockFullNameSearch"><g:message code="individual.search.mockFullName"/></option>
                        <option value="mockPidSearch"><g:message code="individual.search.mockPid"/></option>
                        <option value="typeSelection"><g:message code="individual.search.type"/></option>
                    </select>
                </td>
                <td>
                    <g:select name="projectSelection" from="${projects}" optionValue="name" optionKey="id" style="display: none"/>
                    <input type="text" name="pidSearch" style="display: none" placeholder="min. 3 characters"/>
                    <input type="text" name="mockFullNameSearch" style="display: none" placeholder="min. 3 characters"/>
                    <input type="text" name="mockPidSearch" style="display: none" placeholder="min. 3 characters"/>
                    <g:select name="typeSelection" from="${individualTypes}" style="display: none"/>
                </td>
                <td>
                    <input type="button" value="+" style="display: none"/>
                </td>
            </tr>
        </table>
        <div class="dataTables_container" id="individualTable_container">
        <otp:dataTable codes="${[
                    'individual.list.pid',
                    'individual.list.mockName',
                    'individual.list.mockPid',
                    'individual.list.project',
                    'individual.list.type'
        ]}" id="individualTable"/>
        </div>
    </div>
    <r:script>
        $(function() {
            $.otp.individualList();
        });
    </r:script>
</body>
</html>
