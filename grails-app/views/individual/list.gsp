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
            <input type="text" class="dataTable_search" onKeyUp='$.otp.simpleSearch.search(this, "individualTable");'>
        </div>
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
