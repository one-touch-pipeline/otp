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
    <title>Crash Recovery</title>
</head>

<body>
    <div class="container-fluid otp-main-container">
        <h2>Crash Recovery</h2>
        <p>Overview about the crashed jobs.</p>

        <g:if test="${workflowSystemEnabled}">
            <div class="alert alert-info">
                <i class="bi bi-info-circle"></i> The workflow system is already running. There are no problems to fix.
            </div>
        </g:if>
        <g:else>
            <g:render template="/templates/quickNavigationBar" model="[
                    linkText : 'Crash Recovery (old workflow system)',
                    link : g.createLink(controller: 'crashRecovery', action: 'index'),
                    tooltip : 'Navigate to the crash recovery page of the old workflow system'
            ]"/>

            <div class="btn-group float-right table-action-buttons" role="group" aria-label="table actions">
                <button type="button" class="btn btn-primary" onclick="restartSelectedSteps()" title="Restart the selected steps." data-toggle="tooltip" data-placement="bottom">
                    <i class="bi bi-reply"></i>
                </button>
                <button type="button" class="btn btn-primary" onclick="restartSelectedWorkflowRuns()" title="Restart the selected workflow runs." data-toggle="tooltip" data-placement="bottom">
                    <i class="bi bi-reply-all"></i>
                </button>
                <button type="button" class="btn btn-primary" onclick="markSelectedStepsAsFailed()" title="Mark the selected steps as failed." data-toggle="tooltip" data-placement="bottom">
                    <i class="bi bi-x-circle"></i>
                </button>
                <button type="button" class="btn btn-primary" onclick="markSelectedRunsAsFinalFailed()" title="Mark the selected steps as final failed." data-toggle="tooltip" data-placement="bottom">
                    <i class="bi bi-file-earmark-x"></i>
                </button>
                <button type="button" class="btn btn-primary" onclick="syncWorkflowStepData()" title="Refresh table data." data-toggle="tooltip" data-placement="bottom">
                    <i class="bi bi-arrow-repeat"></i>
                </button>
            </div>

            <table id="jobTable" class="table-sm table-hover table-striped table-bordered">
                <thead>
                <tr>
                    <th scope="col"></th>
                    <th scope="col">Workflow</th>
                    <th scope="col">Name</th>
                    <th scope="col">Step</th>
                    <th scope="col">ID</th>
                    <th scope="col">Date</th>
                    <th scope="col">Restartable</th>
                    <th scope="col">Actions</th>
                </tr>
                </thead>
                <tbody>
                </tbody>
            </table>

            <div id="noDataAlert" class="text-center" style="display: none">
                <div class="alert alert-info text-center" role="alert">
                    No crashed jobs found.
                </div>
                <button type="button" class="btn btn-primary" onclick="startWorkflowSystem()">
                    <i class="bi bi-power"></i> Start Workflow System
                </button>
            </div>

            <div id="tableLoadingSpinner" class="text-center">
                <div class="spinner-border" role="status">
                    <span class="sr-only">Loading...</span>
                </div>
            </div>
        </g:else>
    </div>
</body>
</html>