<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main"/>
        <title><g:message code="notification.administration.title"/></title>
        <r:require module="jqueryDatatables"/>
        <r:require module="jqueryUI"/>
    </head>
<body>
    <div class="body">
        <div id="template-dialog" title="${g.message(code: 'notification.administration.template.title') }" style="display: none;">
            <input type="hidden" name="templateId"/>
            <p><g:message code="notification.administration.template.help"/></p>
            <textarea cols="50" rows="50"></textarea>
        </div>
        <div id="trigger-dialog" title="${g.message(code: 'notification.administration.trigger.title')}" style="display: none;">
            <input type="hidden" name="triggerId"/>
            <p><g:message code="notification.administration.trigger.workflow"/></p>
            <div>
                <select name="jobExecutionPlan">
                <g:each in="${workflows}">
                    <option value="${it.id}">${it.name}</option>
                </g:each>
                </select>
            </div>
            <div>
                <p><input type="checkbox" name="definition"><g:message code="notification.administration.trigger.processingStep"/></p>
                <select name="jobDefinition" style="display: none;"></select>
            </div>
        </div>
        <select id="typeSelection-Prototype" style="display: none;">
            <option value="PROCESS_STARTED"><g:message code="notification.administration.type.processStarted"/></option>
            <option value="PROCESS_SUCCEEDED"><g:message code="notification.administration.type.processSucceeded"/></option>
            <option value="PROCESS_FAILED"><g:message code="notification.administration.type.processFailed"/></option>
            <option value="PROCESS_STEP_STARTED"><g:message code="notification.administration.type.jobStarted"/></option>
            <option value="PROCESS_STEP_FINISHED"><g:message code="notification.administration.type.jobEnded"/></option>
            <option value="PROCESS_STEP_FAILED"><g:message code="notification.administration.type.jobFailed"/></option>
        </select>
        <select id="mediumSelection-Protoype" style="display: none;">
            <option value="EMAIL"><g:message code="notification.administration.medium.email"/></option>
            <option value="JABBER"><g:message code="notification.administration.medium.jabber"/></option>
        </select>
        <h1><g:message code="notification.administration.list.header"/></h1>
        <otp:dataTable codes="${[
                'notification.administration.list.headers.enabled',
                'notification.administration.list.headers.type',
                'notification.administration.list.headers.medium',
                'notification.administration.list.headers.trigger',
                'notification.administration.list.headers.subject',
                'notification.administration.list.headers.message'
            ]}" id="notificationsTable"/>
    </div>
<r:script>
$(function() {
    $.otp.notificationAdministration.setup();
});
</r:script>
</body>
</html>