%{--
  - Copyright 2011-2021 The OTP authors
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
    <asset:javascript src="pages/crashRepair/index/crashRepair.js"/>
    <asset:stylesheet src="pages/crashRepair/styles.less"/>
    <title><g:message code="crashRepair.title"/></title>
</head>

<body>
    <div class="container-fluid otp-main-container">
        <h2><g:message code="crashRepair.title"/></h2>
        <p><g:message code="crashRepair.description"/></p>

        <g:if test="${workflowSystemEnabled}">
            <div class="alert alert-info">
                <i class="bi bi-info-circle"></i> <g:message code="crashRepair.noProblemInfo"/>
            </div>
        </g:if>
        <g:else>
            <g:render template="/templates/quickNavigationBar" model="[
                    linkText: g.message(code: 'crashRepair.navigate.linkText'),
                    link    : g.createLink(controller: 'crashRecovery', action: 'index'),
                    tooltip : g.message(code: 'crashRepair.navigate.tooltip'),
            ]"/>

            <g:if test="${!processingOptionsValid}">
                <otp:annotation type="danger">
                    <g:message code="crashRepair.invalidProcessingOptions.text"/> <g:link controller="processingOption"><g:message code="crashRepair.invalidProcessingOptions.link"/></g:link>
                </otp:annotation>
            </g:if>

            <div class="btn-group float-right table-action-buttons" role="group" aria-label="table actions">
                <button type="button" class="btn btn-primary" onclick="restartSelectedSteps()" title="${g.message(code: 'crashRepair.button.restartSteps.tooltip')}" data-bs-toggle="tooltip" data-placement="bottom">
                    <i class="bi bi-reply"></i>
                </button>
                <button type="button" class="btn btn-primary" onclick="restartSelectedWorkflowRuns()" title="${g.message(code: 'crashRepair.button.restartRuns.tooltip')}" data-bs-toggle="tooltip" data-placement="bottom">
                    <i class="bi bi-reply-all"></i>
                </button>
                <button type="button" class="btn btn-primary" onclick="markSelectedStepsAsFailed()" title="${g.message(code: 'crashRepair.button.stepsFailed.tooltip')}" data-bs-toggle="tooltip" data-placement="bottom">
                    <i class="bi bi-x-circle"></i>
                </button>
                <button type="button" class="btn btn-primary" onclick="markSelectedRunsAsFinalFailed()" title="${g.message(code: 'crashRepair.button.stepsFinalFailed.tooltip')}" data-bs-toggle="tooltip" data-placement="bottom">
                    <i class="bi bi-file-earmark-x"></i>
                </button>
                <button type="button" class="btn btn-primary" onclick="syncWorkflowStepData()" title="${g.message(code: 'crashRepair.button.refreshData.tooltip')}" data-bs-toggle="tooltip" data-placement="bottom">
                    <i class="bi bi-arrow-repeat"></i>
                </button>
            </div>

            <table id="jobTable" class="table table-sm table-hover table-striped table-bordered">
                <thead>
                <tr>
                    <th scope="col"></th>
                    <th scope="col"><g:message code="crashRepair.table.workflow"/></th>
                    <th scope="col"><g:message code="crashRepair.table.name"/></th>
                    <th scope="col"><g:message code="crashRepair.table.step"/></th>
                    <th scope="col"><g:message code="crashRepair.table.id"/></th>
                    <th scope="col"><g:message code="crashRepair.table.date"/></th>
                    <th scope="col"><g:message code="crashRepair.table.restartable"/></th>
                    <th scope="col"><g:message code="crashRepair.table.actions"/></th>
                </tr>
                </thead>
                <tbody>
                </tbody>
            </table>

            <div id="noDataAlert" class="text-center" style="display: none">
                <div class="alert alert-info text-center" role="alert">
                    <g:message code="crashRepair.noCrashedJobs"/>
                </div>
                <button type="button" class="btn btn-primary" onclick="startWorkflowSystem()" ${processingOptionsValid ?: "disabled"}>
                    <i class="bi bi-power"></i> <g:message code="crashRepair.button.startWorkflowSystem"/>
                </button>
            </div>

            <div id="tableLoadingSpinner" class="text-center">
                <div class="spinner-border" role="status">
                    <span class="sr-only"><g:message code="crashRepair.loadingText"/></span>
                </div>
            </div>
        </g:else>
    </div>
</body>
</html>
