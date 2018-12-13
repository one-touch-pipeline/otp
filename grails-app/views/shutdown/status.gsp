<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Server Shutdown</title>
</head>
<body>
    <div class="body">
    <!-- TODO: global info messages -->
        <g:render template="/templates/messages"/>
        <h2>A Shutdown is currently in process.</h2>
        <table>
            <tbody>
                <tr>
                    <th>Initiated by:</th>
                    <td>${shutdown.initiatedBy.username}</td>
                </tr>
                <tr>
                    <th>Initiated at:</th>
                    <td>${shutdown.initiated}</td>
                </tr>
                <tr>
                    <th>Reason for Shutdown:</th>
                    <td>${shutdown.reason}</td>
                </tr>
            </tbody>
        </table>
        <h2>List of running Jobs which cannot be resumed</h2>
        <table>
            <thead>
                <tr>
                    <th>Workflow</th>
                    <th>Process Id</th>
                    <th>Processing Step</th>
                    <th>Job</th>
                </tr>
            </thead>
            <tbody>
                <g:each var="step" in="${notResumableJobs}">
                    <tr>
                        <td><g:link controller="processes" action="plan" id="${step.process.jobExecutionPlan.id}">${step.process.jobExecutionPlan.name}</g:link></td>
                        <td><g:link controller="processes" action="process" id="${step.process.id}">${step.process.id}</g:link></td>
                        <td><g:link controller="processes" action="processingStep" id="${step.id}">${step.jobDefinition.name}</g:link></td>
                    </tr>
                </g:each>
            </tbody>
        </table>
        <h2>List of running Jobs which can be resumed</h2>
        <table>
            <thead>
                <tr>
                    <th>Workflow</th>
                    <th>Process Id</th>
                    <th>Processing Step</th>
                    <th>Job</th>
                    </tr>
            </thead>
            <tbody>
                <g:each var="step" in="${resumableJobs}">
                    <tr>
                        <td><g:link controller="processes" action="plan" id="${step.process.jobExecutionPlan.id}">${step.process.jobExecutionPlan.name}</g:link></td>
                        <td><g:link controller="processes" action="process" id="${step.process.id}">${step.process.id}</g:link></td>
                        <td><g:link controller="processes" action="processingStep" id="${step.id}">${step.jobDefinition.name}</g:link></td>
                    </tr>
                </g:each>
            </tbody>
        </table>
        <hr/>
        <div>
            <g:form action="cancelShutdown" style="display: inline">
                <g:submitButton name="Cancel Shutdown"/>
            </g:form>
            <g:form action="closeApplication" style="display: inline">
                <g:submitButton name="Stop Application Context"/>
            </g:form>
        </div>
    </div>
</body>
</html>
