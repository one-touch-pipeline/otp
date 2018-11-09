<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processingOption.title"/></title>
    <asset:javascript src="modules/editorSwitch"/>
</head>
<body>
    <div class="body">
        <g:render template="/templates/messages"/>

        <h1><g:message code="processingOption.title"/></h1>

        <div class="otpDataTables">
            <table id="optionTable">
                <thead>
                <tr>
                    <th><g:message code="processingOption.list.headers.name"/></th>
                    <th><g:message code="processingOption.list.headers.type"/></th>
                    <th><g:message code="processingOption.list.headers.value"/></th>
                    <th></th>
                    <th><g:message code="processingOption.list.headers.dateCreated"/></th>
                    <th><g:message code="processingOption.list.headers.project"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each in="${options}" var="option">
                    <otp:editTable>
                        <g:form action="update">
                            <td><otp:tableCell cell="${option.name}"/></td>
                            <input type="hidden" name="optionName" value="${option.name.value}">
                            <td><otp:tableCell cell="${option.type}"/></td>
                            <input type="hidden" name="type" value="${option.type.value}">
                            <td>
                                <span class="edit-fields" style="display: none;">
                                    <g:if test="${option.allowedValues}">
                                        <g:select name="value" from="${option.allowedValues}" value="${option.value.value}"
                                                  title="${g.message(code: "processingOption.list.headers.value")}"/>
                                    </g:if>
                                    <g:else>
                                        <input class="" name="value" value="${option.value.tooltip}"
                                               title="${g.message(code: "processingOption.list.headers.value")}">
                                    </g:else>
                                </span>
                                <span class="show-fields">
                                    <otp:tableCell cell="${option.value}"/>
                                </span>
                            </td>
                            <td><otp:editTableButtons/></td>
                            <td>${option.dateCreated}</td>
                            <td>${option.project}</td>
                        </g:form>

                    </otp:editTable>
                </g:each>
                </tbody>
                <tfoot>
                <otp:editTable>
                    <th><g:message code="processingOption.list.headers.name"/></th>
                    <th><g:message code="processingOption.list.headers.type"/></th>
                    <th><g:message code="processingOption.list.headers.value"/></th>
                    <th></th>
                    <th><g:message code="processingOption.list.headers.dateCreated"/></th>
                    <th><g:message code="processingOption.list.headers.project"/></th>
                </otp:editTable>
                </tfoot>
            </table>
        </div>
    </div>
</body>
</html>
