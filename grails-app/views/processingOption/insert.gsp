<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title><g:message code="processingOption.insert.title"/></title>
</head>
<body>
    <div class="body">
        <h1><g:message code="processingOption.insert.title"/></h1>
        <form id="add-processingOption-form" method="POST">
            <div class="dialog">
                <table>
                    <tbody>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="name"><g:message code="processingOption.insert.name"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:processingOption,field:'name','errors')}">
                                <input type="text" id="name" name="name" />
                            </td>
                        </tr>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="type"><g:message code="processingOption.insert.type"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:processingOption,field:'type','errors')}">
                                <input type="text" id="type" name="type" />
                            </td>
                        </tr>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="value"><g:message code="processingOption.insert.value"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:processingOption,field:'value','errors')}">
                                <input type="text" id="value" name="value" />
                            </td>
                        </tr>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="project"><g:message code="processingOption.insert.project"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:processingOption,field:'project','errors')}">
                                <g:select name="project" from="${projects}" id="project" />
                            </td>
                        </tr>
                        <tr class="prop">
                            <td valign="top" class="name">
                                <label for="comment"><g:message code="processingOption.insert.comment"/></label>
                            </td>
                            <td valign="top" class="value ${hasErrors(bean:processingOption,field:'comment','errors')}">
                                <input type="text" id="comment" name="comment" />
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <input type="submit" value="${g.message(code: 'processingOption.insert.save')}"/>
            </div>
        </form>
    </div>
    <r:script>
        $(function() {
            $.otp.addProcessingOption.register();
        });
    </r:script>
</body>
</html>
