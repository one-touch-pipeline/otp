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
    <title>Crash Recovery</title>
    <asset:javascript src="pages/crashRecovery/index/crashRecovery.js"/>
</head>
<body>
    <div class="body">
        <g:if test="${crashRecovery}">
        <div class="modal" id="dialog-select-job">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="dialog-select-job-label">Select a Job</h5>
                        <button type="button" class="close" data-bs-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <p>Please select a Job from the List to perform the operation.</p>
                        <p>In case the List is empty, you can restart the Scheduler with the "Start Scheduler" button.</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary" data-bs-dismiss="modal">Ok</button>
                    </div>
                </div>
            </div>
        </div>
        <div class="modal" id="dialog-error-message-job">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="dialog-error-message-job-label">Enter Error Message for Job</h5>
                        <button type="button" class="close" data-bs-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                        <p>Please provide an error message why the Job has been set to failed.</p>
                        <input type="text"/>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="button" class="btn btn-primary" id="markAsFailed">Mark Job as Failed</button>
                    </div>
                </div>
            </div>
        </div>
        <div class="modal" id="dialog-parameters-of-job">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="dialog-parameters-of-job-label">Output parameters</h5>
                        <button type="button" class="close" data-bs-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                    </div>
                    <div class="modal-body">
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                        <button type="button" class="btn btn-primary" id="parameters-ok">OK</button>
                    </div>
                </div>
            </div>
        </div>

        <h1><g:message code="crashRecovery.title"/></h1>

        <g:render template="/templates/quickNavigationBar" model="[
                linkText : 'Crash Recovery (new workflow system)',
                link : g.createLink(controller: 'crashRepair', action: 'index'),
                tooltip : 'Navigate to the crash recovery page of the new workflow system'
        ]"/>

        <g:if test="${!processingOptionsValid}">
            <otp:annotation type="danger">
                There are invalid processing options: <g:link controller="processingOption">view and correct them here</g:link>
            </otp:annotation>
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
            <button class="btn btn-primary" id="markFinished">Mark as Finished</button>
            <button class="btn btn-primary" id="markSucceeded">Mark as Succeeded</button>
            <button class="btn btn-primary" id="markFailed">Mark as Failed</button>
            <button class="btn btn-primary" id="restart">Restart</button>
            <button class="btn btn-primary" id="startScheduler">Start Scheduler</button>
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
