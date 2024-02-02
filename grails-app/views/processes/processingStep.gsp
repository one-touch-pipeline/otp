%{--
  - Copyright 2011-2024 The OTP authors
  -
  - Permission is hereby granted, free of charge, to any person obtaining a copy
  - of this software and associated documentation files (the "Software"), to deal
  - in the Software without restriction, including without limitation the rights
  - to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  - copies of the Software, and to permit persons to whom the Software is
  - furnished to do so, subject to the following conditions:
  -
  - The above copyright notice and this permission notice shall be included in all
  - copies or substantial portions of the Software.
  -
  - THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  - IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  - FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  - AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  - LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  - OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  - SOFTWARE.
  --}%

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="main"/>
    <title><g:message code="processes.processingStep.title" args="${ [step.jobDefinition.name, step.id] }"/></title>
    <asset:javascript src="pages/processes/common.js"/>
    <asset:javascript src="pages/processes/processingStep/processingStep.js"/>
</head>
<body>
    <div class="body">
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
                <th>
                    <span onclick="$.otp.workflows.processingStep.promptClusterJobIds('${clusterJobsWrapper*.clusterJobId.join(" ")}')">
                        <u><g:message code="workflow.paramater.table.headers.clusterJob"/></u>
                    </span>
                </th>
                <th><g:message code="workflow.paramater.table.headers.clusterJobLogFile"/></th>
                <th><g:message code="workflow.paramater.table.headers.clusterJobDetails"/></th>
                <th><g:message code="workflow.paramater.table.headers.jobResult"/></th>
                <th><g:message code="workflow.paramater.table.headers.runTime"/></th>
                <th><g:message code="workflow.paramater.table.headers.node"/></th>
                <th><g:message code="otp.blank"/></th>
            </tr>
            </thead>
            <tbody>
            <g:each var="wrappedJob" in="${clusterJobsWrapper}" >
                <tr>
                    <td></td>
                    <td>${wrappedJob.job.clusterJobName}</td>
                    <td>${wrappedJob.job.clusterJobId}</td>
                    <td>
                        <g:if test="${wrappedJob.job.jobLog && new File(wrappedJob.job.jobLog).exists()}">
                            <g:link controller="clusterJobDetail" action="showLog" id="${wrappedJob.job.id}">
                                <g:message code="workflow.paramater.table.inline.log"/>
                            </g:link>
                        </g:if><g:else>
                            <g:message code="workflow.paramater.table.inline.log"/> (<g:message code="processes.processingStep.log.notAvailable"/>)
                        </g:else>
                    </td>
                    <td>
                        <g:link controller="clusterJobDetail" action="show" id="${wrappedJob.job.id}">
                            <g:message code="workflow.paramater.table.inline.jobDetails"/>
                        </g:link>
                    </td>
                    <td>
                        <span class="clusterJobExitStatus ${wrappedJob.job.exitStatus}">${"${wrappedJob.job.exitStatus}: ${wrappedJob.job.exitCode}"}</span>
                    </td>
                    <td>${wrappedJob.elapsedWalltimeAsHhMmSs}</td>
                    <td>${wrappedJob.job.node ?: "-"}</td>
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
    <asset:script  type="text/javascript">
        $(document).ready(function() {
            $.otp.workflows.processingStep.register('#processingStepUpdatesList', '#inputParametersList', '#outputParametersList', ${step.id});
        });
    </asset:script>
    </div>
</body>
</html>
