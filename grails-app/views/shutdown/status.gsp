%{--
  - Copyright 2011-2019 The OTP authors
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
    <asset:stylesheet src="pages/shutdown/status/styles.less"/>
    <title><g:message code="serverShutdown.title"/></title>
</head>
<body>
    <div class="container-fluid otp-main-container">
        <g:render template="/templates/messages"/>
        <div class="card">
            <div class="card-header">
                <i class="bi bi-info-circle align-text-bottom"></i> <g:message code="serverShutdown.shutdownInformation"/><br>
                <small class="text-muted"><g:message code="serverShutdown.shutdownInProcess"/></small>
            </div>
            <div class="card-body">
                <ul class="list-group">
                    <li class="list-group-item"><b><g:message code="serverShutdown.reasonLabel"/>:</b> ${shutdown.reason}</li>
                    <li class="list-group-item"><b><g:message code="serverShutdown.initiatedAtLabel"/>:</b> ${shutdownInititated}</li>
                    <li class="list-group-item"><b><g:message code="serverShutdown.initiatedByLabel"/>:</b> ${shutdown.initiatedBy.username}</li>
                </ul>
            </div>
        </div>
        <div class="empty-divider"></div>
        <h5><g:message code="serverShutdown.old.headline"/></h5>
        <p><g:message code="serverShutdown.old.descriptionRunning"/></p>
        <div class="card">
            <div class="card-header">
                <span class="badge badge-secondary"><g:message code="serverShutdown.old.badgeOld"/></span> <g:message code="serverShutdown.runningJobsNotResumable"/><br>
                <small class="text-muted"><g:message code="serverShutdown.notResumableDescription"/></small>
            </div>
            <div class="card-body">
                <table class="table table-sm">
                    <thead>
                    <tr>
                        <th><g:message code="serverShutdown.old.cellNameWorkflow"/></th>
                        <th><g:message code="serverShutdown.old.cellNameId"/></th>
                        <th><g:message code="serverShutdown.old.cellNameStep"/></th>
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
            </div>
        </div>
        <div class="empty-divider"></div>
        <div class="card">
            <div class="card-header">
                <span class="badge badge-secondary"><g:message code="serverShutdown.old.badgeOld"/></span> <g:message code="serverShutdown.runningJobsResumable"/><br>
                <small class="text-muted"><g:message code="serverShutdown.resumableDescription"/></small>
            </div>
            <div class="card-body">
                <table class="table table-sm">
                    <thead>
                    <tr>
                        <th><g:message code="serverShutdown.old.cellNameWorkflow"/></th>
                        <th><g:message code="serverShutdown.old.cellNameId"/></th>
                        <th><g:message code="serverShutdown.old.cellNameStep"/></th>
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
            </div>
        </div>
        <div class="empty-divider"></div>
        <h5><g:message code="serverShutdown.new.headline"/></h5>
        <p><g:message code="serverShutdown.new.description"/></p>
        <div class="card">
            <div class="card-header">
                <span class="badge badge-primary"><g:message code="serverShutdown.new.badge"/></span> <g:message code="serverShutdown.runningJobsNotResumable"/><br>
                <small class="text-muted"><g:message code="serverShutdown.notResumableDescription"/></small>
            </div>
            <div class="card-body">
                <table class="table table-sm">
                    <thead>
                    <tr>
                        <th><g:message code="serverShutdown.new.cellWorkflow"/></th>
                        <th><g:message code="serverShutdown.new.cellName"/></th>
                        <th><g:message code="serverShutdown.new.cellStep"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each var="step" in="${notRestartableRunningWorkflowSteps}">
                        <tr>
                            <td><g:link controller="workflowRunList" action="index" params="['workflow.id': step.workflowRun.workflow.id]">${step.workflowRun.workflow.displayName}</g:link></td>
                            <td><g:link controller="workflowRunDetails" action="index" id="${step.workflowRun.id}" params="['workflow.id': step.workflowRun.workflow.id]">${step.workflowRun.displayName}</g:link></td>
                            <td>${step.beanName}</td>
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="empty-divider"></div>
        <div class="card">
            <div class="card-header">
                <span class="badge badge-primary"><g:message code="serverShutdown.new.badge"/></span> <g:message code="serverShutdown.runningJobsResumable"/><br>
                <small class="text-muted"><g:message code="serverShutdown.resumableDescription"/></small>
            </div>
            <div class="card-body">
                <table class="table table-sm">
                    <thead>
                    <tr>
                        <th><g:message code="serverShutdown.new.cellWorkflow"/></th>
                        <th><g:message code="serverShutdown.new.cellName"/></th>
                        <th><g:message code="serverShutdown.new.cellStep"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <g:each var="step" in="${restartableRunningWorkflowSteps}">
                        <tr>
                            <td><g:link controller="workflowRunList" action="index" params="['workflow.id': step.workflowRun.workflow.id]">${step.workflowRun.workflow.displayName}</g:link></td>
                            <td><g:link controller="workflowRunDetails" action="index" id="${step.workflowRun.id}" params="['workflow.id': step.workflowRun.workflow.id]">${step.workflowRun.displayName}</g:link></td>
                            <td>${step.beanName}</td>
                        </tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
        </div>
        <div class="empty-divider"></div>
        <div class="float-right">
            <g:form action="cancelShutdown" style="display: inline">
                <button type="submit" class="btn btn-light"><i class="bi bi-arrow-left align-text-bottom"></i> <g:message code="serverShutdown.button.cancel"/></button>
            </g:form>
            <g:form action="closeApplication" style="display: inline">
                <button type="submit" class="btn btn-danger"><i class="bi bi-exclamation-octagon align-text-bottom"></i> <g:message code="serverShutdown.button.shutdown"/></button>
            </g:form>
        </div>
        <div class="empty-divider"></div>
    </div>
</body>
</html>
