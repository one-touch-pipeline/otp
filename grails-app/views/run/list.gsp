<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="run.list.title"/></title>
        <r:require module="jqueryDatatables"/>
    </head>
<body>
    <div class="body">
        <div class="dataTable_searchContainer">
            <g:message code="run.list.search"/>: <input type="text" class="dataTable_search" onKeyUp='$.otp.simpleSearch.search(this, "runTable");'>
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
