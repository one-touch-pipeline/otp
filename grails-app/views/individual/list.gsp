<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="individual.list.title"/></title>
        <jqDT:resources/>
    </head>
<body>
    <div class="body">
        <h1><g:message code="individual.list.title"/></h1>
        <table id="individualTable">
            <thead>
            <tr>
                <th><g:message code="individual.list.pid"/></th>
                <th><g:message code="individual.list.mockName"/></th>
                <th><g:message code="individual.list.mockPid"/></th>
                <th><g:message code="individual.list.project"/></th>
                <th><g:message code="individual.list.type"/></th>
            </tr>
            </thead>
            <tbody></tbody>
            <tfoot>
            <tr>
                <th><g:message code="individual.list.pid"/></th>
                <th><g:message code="individual.list.mockName"/></th>
                <th><g:message code="individual.list.mockPid"/></th>
                <th><g:message code="individual.list.project"/></th>
                <th><g:message code="individual.list.type"/></th>
            </tr>
            </tfoot>
        </table>
    </div>
    <r:script>
$(function() {
    $.otp.individualList();
});
    </r:script>
</body>
</html>
