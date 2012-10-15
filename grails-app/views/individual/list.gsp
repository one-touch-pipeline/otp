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
        <h1><g:message code="individual.list.title"/></h1>
        <otp:dataTable codes="${[
                'individual.list.pid',
                'individual.list.mockName',
                'individual.list.mockPid',
                'individual.list.project',
                'individual.list.type'
            ] }" id="individualTable"/>
    </div>
    <r:script>
$(function() {
    $.otp.individualList();
});
    </r:script>
</body>
</html>
