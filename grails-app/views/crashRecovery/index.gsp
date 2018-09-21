<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title>Crash Recovery</title>
    <asset:javascript src="modules/jqueryDatatables"/>
    <asset:stylesheet src="modules/jqueryDatatables"/>
    <asset:javascript src="modules/crashRecovery"/>
</head>
<body>
    <div class="body">
        <g:if test="${crashRecovery}">
        <div id="dialog-select-job" title="Select a Job" style="display: none">
            <p>Please select a Job from the List to perform the operation.</p>
            <p>In case the List is empty, you can restart the Scheduler with the "Start Scheduler" button.</p>
        </div>
        <div id="dialog-error-message-job" title="Enter Error Message for Job" style="display: none">
            <p>Please provide an error message why the Job has ben set to failed.</p>
            <input type="text"/>
        </div>
        <h1><g:message code="crashRecovery.title"/></h1>
            <g:if test="${!processingOptionsValid}">
                <div id="infoBox"><div class="errors">
                    <ul><li>There are invalid processing options: <g:link controller="processingOption">view and correct them here</g:link></li></ul>
                </div></div>
            </g:if>
        <div>
            <p>Select one of the Crashed Jobs and click one of the actions underneath the table. Jobs that are at least sometimes resumable are preselected.</p>
            <ul>
                <li><strong>Mark as Finished:</strong> The Job has finished and a later Job will decide whether the Job succeeded or failed. Next Job will be scheduled.</li>
                <li><strong>Mark as Succeeded:</strong> The Job succeeded. Next Job will be scheduled.</li>
                <li><strong>Mark as Failed:</strong> The Job failed. No job of the same Process will be scheduled.</li>
                <li><strong>Restart:</strong> The Job failed and is scheduled for another execution.</li>
                 <li><strong>Start Scheduler:</strong> Tries to restart the Scheduler. Only use after all Jobs have been triaged.</li>
            </ul>
        </div>
        <table id="crashRecoveryTable">
            <thead>
                <tr>
                    <th></th>
                    <th>Workflow</th>
                    <th>Process Id</th>
                    <th>Processing Step</th>
                    <th>Job</th>
                </tr>
            </thead>
        </table>
        <div>
            <button id="markFinished">Mark as Finished</button>
            <button id="markSucceeded">Mark as Succeeded</button>
            <button id="markFailed">Mark as Failed</button>
            <button id="restart">Restart</button>
            <button id="startScheduler">Start Scheduler</button>
        </div>
        </g:if>
        <g:else>
            <h2>No Crash Recovery in place</h2>
        </g:else>
    </div>
    <asset:script>
        $(function () {
            $.otp.crashRecovery.setupView();
        });
    </asset:script>
</body>
</html>
