<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processes.processingStep.title" args="${ [step.jobDefinition.name, step.id] }"/></title>
    <asset:javascript src="modules/workflows"/>
</head>
<body>
    <div class="body_grow">
    <h1><g:message code="processes.processingStep.title" args="${ [step.jobDefinition.name, step.id] }"/></h1>
    <table>
        <thead></thead>
        <tbody>
            <tr>
                <td><g:message code="processes.processingStep.workflow.process"/></td>
                <td><g:link action="process" id="${step.process.id}"><g:message code="processes.processingStep.workflow.number"/> ${step.process.id}</g:link> <g:message code="processes.processingStep.workflow.of"/> <g:link action="plan" id="${step.process.jobExecutionPlan.id}"><g:message code="processes.processingStep.workflow"/> ${step.process.jobExecutionPlan.name}</g:link></td>
            </tr>
            <tr>
                <td><g:message code="processes.processingStep.job"/></td>
                <td>${step.jobClass}</td>
            </tr>
            <g:if test="${step.previous}">
            <tr>
                <td><g:message code="processes.processingStep.previous"/></td>
                <td><g:link action="processingStep" id="${step.previous.id}">${step.previous.jobDefinition.name}</g:link></td>
            </tr>
            </g:if>
            <g:if test="${step.next}">
            <tr>
                <td><g:message code="processes.processingStep.next"/></td>
                <td><g:link action="processingStep" id="${step.next.id}">${step.next.jobDefinition.name}</g:link></td>
            </tr>
            </g:if>
            <g:if test="${step instanceof de.dkfz.tbi.otp.job.processing.RestartedProcessingStep}">
            <tr>
                <td><g:message code="processes.processingStep.previouslyFailedStep"/></td>
                <td><g:link action="processingStep" id="${step.original.id}"># ${step.original.id}</g:link></td>
            </tr>
            </g:if>
            <tr>
                <td><g:message code="processes.processingStep.log"/></td>
                <td>
                    <g:if test="${hasLog}">
                        <g:link action="processingStepLog" id="${step.id}"><g:message code="processes.processingStep.log.showFile"/></g:link>
                    </g:if>
                    <g:else>
                        <g:message code="processes.processingStep.log.noFile"/>
                    </g:else>
                </td>
            </tr>
        </tbody>
    </table>
    <h2><g:message code="processes.processingStep.inputParameters"/></h2>
    <div class="otpDataTables" >
        <otp:dataTable codes="${[
                'otp.blank',
                'workflow.paramater.table.headers.name',
                'workflow.paramater.table.headers.description',
                'workflow.paramater.table.headers.value'
            ]}" id="inputParametersList"/>
    </div>
    <br>
    <h2><g:message code="processes.processingStep.outputParameters"/></h2>
    <div class="otpDataTables" >
        <otp:dataTable codes="${[
                'otp.blank',
                'workflow.paramater.table.headers.name',
                'workflow.paramater.table.headers.description',
                'workflow.paramater.table.headers.value',
            ]}" id="outputParametersList"/>
    </div>
    <br>
    <h2><g:message code="processes.processingStep.logOutput"/></h2>
        <table>
            <thead>
            <tr>
                <th><g:message code="otp.blank"/></th>
                <th><g:message code="workflow.paramater.table.headers.clusterJobName"/></th>
                <th><g:message code="workflow.paramater.table.headers.clusterJob"/></th>
                <th><g:message code="workflow.paramater.table.headers.clusterJobLogFile"/></th>
                <th><g:message code="workflow.paramater.table.headers.clusterJobDetails"/></th>
                <th><g:message code="workflow.paramater.table.headers.jobResult"/></th>
                <th><g:message code="workflow.paramater.table.headers.runTime"/></th>
                <th><g:message code="workflow.paramater.table.headers.node"/></th>
                <th><g:message code="otp.blank"/></th>
            </tr>
            </thead>
            <tbody>
            <g:each var="clusterJob" in="${clusterJobs}" >
                <tr>
                    <td></td>
                    <td>${clusterJob.clusterJobName}</td>
                    <td>${clusterJob.clusterJobId}</td>
                    <td>
                        <g:if test="${clusterJob.jobLog && new File(clusterJob.jobLog).exists()}">
                            <g:link action="processingStepClusterJobLog" id="${clusterJob.id}">
                                <g:message code="workflow.paramater.table.inline.log"/>
                            </g:link>
                        </g:if><g:else>
                            <g:message code="workflow.paramater.table.inline.log"/> (<g:message code="processes.processingStep.log.notAvailable"/>)
                        </g:else>
                    </td>
                    <td>
                        <g:link controller="clusterJobDetail" action="show" id="${clusterJob.id}">
                            <g:message code="workflow.paramater.table.inline.jobDetails"/>
                        </g:link>
                    </td>
                    <td>
                        <span class="clusterJobExitStatus ${clusterJob.exitStatus}">${"${clusterJob.exitStatus}: ${clusterJob.exitCode}"}</span>
                    </td>
                    <td>${clusterJob.getElapsedWalltimeAsHhMmSs()}</td>
                    <td>${clusterJob.node ?: "-"}</td>
                    <td></td>
                </tr>
            </g:each>
            </tbody>
        </table>
    <br>
    <h2><g:message code="processes.processingStep.updates"/></h2>
    <div class="otpDataTables">
        <otp:dataTable codes="${[
                'otp.blank',
                'workflow.processingstep.update.table.headers.date',
                'workflow.processingstep.update.table.headers.state',
                'workflow.processingstep.update.table.headers.error'
            ]}" id="processingStepUpdatesList"/>
    </div>
    <asset:script>
       $(document).ready(function() {
            $.otp.workflows.processingStep.register('#processingStepUpdatesList', '#inputParametersList', '#outputParametersList', ${step.id});
        });
    </asset:script>
    </div>
</body>
<asset:script>
    $(function() {
        $.otp.growBodyInit(380);
    });
</asset:script>
</html>
