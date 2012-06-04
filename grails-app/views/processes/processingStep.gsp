<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Processing Step ${step.jobDefinition.name} (# ${step.id})</title>
<jqDT:resources/>
<g:javascript library="jquery.dataTables" />
<g:javascript src="jquery.timeago.js"/>
</head>
<body>
  <div class="body">
    <h1>Processing Step ${step.jobDefinition.name} (# ${step.id})</h1>
    <table>
        <thead></thead>
        <tbody>
            <tr>
                <td>Process:</td>
                <td><g:link action="process" id="${step.process.id}"># ${step.process.id}</g:link> of <g:link action="plan" id="${step.process.jobExecutionPlan.id}">Workflow ${step.process.jobExecutionPlan.name}</g:link></td>
            </tr>
            <tr>
                <td>Job:</td>
                <td>${step.jobClass}</td>
            </tr>
            <tr>
                <td>Version:</td>
                <td>${step.jobVersion}</td>
            </tr>
            <g:if test="${step.previous}">
            <tr>
                <td>Previous:</td>
                <td><g:link action="processingStep" id="${step.previous.id}">${step.previous.jobDefinition.name}</g:link></td>
            </tr>
            </g:if>
            <g:if test="${step.next}">
            <tr>
                <td>Next:</td>
                <td><g:link action="processingStep" id="${step.next.id}">${step.next.jobDefinition.name}</g:link></td>
            </tr>
            </g:if>
        </tbody>
    </table>
    <h2>Input Parameters</h2>
    <div>
    <table id="inputParametersList">
        <thead>
            <tr>
                <th>#</th>
                <th>Name</th>
                <th>Description</th>
                <th>Value</th>
            </tr>
        </thead>
        <tbody>
        </tbody>
    </table>
    <g:javascript>
        $(document).ready(function() {
            $.otp.createParameterListView('#inputParametersList', ${step.id}, true);
        });
    </g:javascript>
    </div>
    <h2>Output Parameters</h2>
    <div>
    <table id="outputParametersList">
        <thead>
            <tr>
                <th>#</th>
                <th>Name</th>
                <th>Description</th>
                <th>Value</th>
            </tr>
        </thead>
        <tbody>
        </tbody>
    </table>
    <g:javascript>
        $(document).ready(function() {
            $.otp.createParameterListView('#outputParametersList', ${step.id}, false);
        });
    </g:javascript>
    </div>
    <h2>Updates</h2>
    <div>
    <table id="processingStepUpdatesList">
        <thead>
            <tr>
                <th>#</th>
                <th>Date</th>
                <th>State</th>
                <th>Error Message</th>
            </tr>
        </thead>
        <tbody>
        </tbody>
    </table>
    </div>
    <g:javascript>
       $(document).ready(function() {
            $.otp.createProcessingStepUpdatesListView('#processingStepUpdatesList', ${step.id});
        });
    </g:javascript>
  </div>
</body>
</html>
