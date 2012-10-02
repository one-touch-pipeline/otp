<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="processingOption.title"/></title>
<r:require module="jqueryDatatables"/>
</head>
<body>
    <div class="body">
        <h1><g:message code="processingOption.title"/></h1>
        <table id="optionTable">
            <thead>
                <tr>
                    <th><g:message code="processingOption.list.headers.name"/></th>
                    <th><g:message code="processingOption.list.headers.type"/></th>
                    <th><g:message code="processingOption.list.headers.value"/></th>
                    <th><g:message code="processingOption.list.headers.project"/></th>
                    <th><g:message code="processingOption.list.headers.dateCreated"/></th>
                    <th><g:message code="processingOption.list.headers.dateObsoleted"/></th>
                    <th><g:message code="processingOption.list.headers.comment"/></th>
                </tr>
            </thead>
            <tbody></tbody>
            <tfoot>
                <tr>
                    <th><g:message code="processingOption.list.headers.name"/></th>
                    <th><g:message code="processingOption.list.headers.type"/></th>
                    <th><g:message code="processingOption.list.headers.value"/></th>
                    <th><g:message code="processingOption.list.headers.project"/></th>
                    <th><g:message code="processingOption.list.headers.dateCreated"/></th>
                    <th><g:message code="processingOption.list.headers.dateObsoleted"/></th>
                    <th><g:message code="processingOption.list.headers.comment"/></th>
                </tr>
            </tfoot>
        </table>
    </div>
    <r:script>
$(function() {
    $.otp.option.register();
});
    </r:script>
</body>
</html>
