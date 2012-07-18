<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="run.list.title"/></title>
        <jqDT:resources type="js"/>
    </head>
<body>
    <div class="body">
        <h1><g:message code="run.list.title"/></h1>
        <table id="runTable">
            <thead>
            <tr>
                <th><g:message code="run.list.name"/></th>
                <th><g:message code="run.list.seqCenter"/></th>
                <th><g:message code="run.list.storageRealm"/></th>
                <th><g:message code="run.list.dateCreated"/></th>
                <th><g:message code="run.list.dateExecuted"/></th>
                <th><g:message code="run.list.blacklisted"/></th>
                <th><g:message code="run.list.multipleSource"/></th>
            </tr>
            </thead>
            <tbody></tbody>
            <tfoot>
            <tr>
                <th><g:message code="run.list.name"/></th>
                <th><g:message code="run.list.seqCenter"/></th>
                <th><g:message code="run.list.storageRealm"/></th>
                <th><g:message code="run.list.dateCreated"/></th>
                <th><g:message code="run.list.dateExecuted"/></th>
                <th><g:message code="run.list.blacklisted"/></th>
                <th><g:message code="run.list.multipleSource"/></th>
            </tr>
            </tfoot>
        </table>
    </div>
    <r:script>
$(function() {
    $.otp.runList();
});
    </r:script>
</body>
</html>
