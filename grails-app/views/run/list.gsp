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
        <h1><g:message code="run.list.title"/></h1>
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
    <r:script>
$(function() {
    $.otp.runList();
});
    </r:script>
</body>
</html>
