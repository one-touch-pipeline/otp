/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

function syncWorkflowStepData() {
  'use strict';

  const table = $('#jobTable');
  const noDataAlert = $('#noDataAlert');
  const loadingSpinner = $('#tableLoadingSpinner');

  noDataAlert.hide();
  loadingSpinner.show();
  table.DataTable().clear().draw();

  $.ajax({
    url: $.otp.createLink({
      controller: 'crashRepair',
      action: 'runningWorkflowSteps'
    }),
    dataType: 'json',
    type: 'GET',
    success(result) {
      loadingSpinner.hide();

      if (!result || Object.keys(result).length === 0) {
        noDataAlert.show();
      } else {
        renderStepDataTable(result);
      }
    },
    error() {
      $.otp.toaster.showErrorToast(
        'Data Loading',
        'An error occurred while trying to load the jobs data. Please try again.'
      );
      loadingSpinner.hide();
    }
  });
}

function renderStepDataTable(steps) {
  'use strict';

  const table = $('#jobTable');
  const dataTable = table.DataTable();

  steps.forEach((step) => {
    dataTable.row.add([
      `<input type="checkbox" name="stepId" value="${step.id}" class="tableCheckbox">`,
      `<a href="${$.otp.createLink({
        controller: 'workflowRunList',
        action: 'index',
        parameters: { 'workflow.id': step.workflowId }
      })}">${step.workflowName}</a>`,
      `<a href="${$.otp.createLink({
        controller: 'workflowRunDetails',
        action: 'index',
        parameters: { id: step.workflowRunId, 'workflow.id': step.workflowId }
      })}" 
          title="${step.workflowRunName.replaceAll('\n', '<br>')}" 
          data-bs-toggle="tooltip" 
          data-bs-placement="bottom">
         ${step.workflowRunShortName}
       </a>`,
      step.beanName,
      step.id,
      step.lastUpdated,
      step.workflowRunJobCanBeRestarted ? '<i class="bi bi-check"></i>' : '<i class="bi bi-x"></i>',
      /* eslint-disable max-len */
      `<div class="btn-group float-right row-action-buttons" role="group" aria-label="table actions">
           <button class="btn btn-primary" 
                   onclick="restartStep(${step.id})"${(!step.workflowRunJobCanBeRestarted ? 'disabled' : '')}
                   data-bs-toggle="tooltip" 
                   data-placement="top" 
                   title="Restart step #${step.id}">
               <i class="bi bi-reply"></i>
           </button>
           <button class="btn btn-primary" 
                   onclick="restartWorkflowRun(${step.id})"
                   data-bs-toggle="tooltip" 
                   data-placement="top" 
                   title="Restart workflow run #${step.workflowRunId}">
               <i class="bi bi-reply-all"></i>
           </button>
           <button class="btn btn-primary" 
                   onclick="setWorkflowStepAsFailed(${step.id})"
                   data-bs-toggle="tooltip" 
                   data-placement="top" 
                   title="Mark #${step.id} as failed">
               <i class="bi bi-x-circle"></i>
           </button>
           <button class="btn btn-primary" 
                   onclick="setWorkflowRunAsFinalFailed(${step.id})"
                   data-bs-toggle="tooltip" 
                   data-placement="top" 
                   title="Mark #${step.workflowRunId} as final failed">
               <i class="bi bi-file-earmark-x"></i>
           </button>
       </div>`
      /* eslint-enable max-len */
    ]).draw();

    $('a').tooltip({ html: true });
  });

  // enable the new tooltips
  table.tooltip({
    selector: 'button'
  });
}

/**
 * Restarts a workflow step by sending a request to the backend.
 * @param stepId which should be restarted
 */
function restartStep(stepId) {
  'use strict';

  $.ajax({
    url: $.otp.createLink({
      controller: 'crashRepair',
      action: 'restartWorkflowStep'
    }),
    dataType: 'json',
    type: 'POST',
    data: {
      workflowStep: stepId
    },
    success() {
      $.otp.toaster.showSuccessToast(
        `Step #${stepId} restarted`,
        `Restart of the step #${stepId} was successful.`
      );
      syncWorkflowStepData();
    },
    error(error) {
      if (error && error.responseJSON && error.responseJSON.message) {
        $.otp.toaster.showErrorToast('Restart Failed', error.responseJSON.message);
      } else {
        $.otp.toaster.showErrorToast('Restart Failed', 'Unknown error during job restart.');
      }
    }
  });
}

/**
 * Restarts a workflow run by sending a request to the backend.
 * @param stepId referring to the workflow run
 */
function restartWorkflowRun(stepId) {
  'use strict';

  $.ajax({
    url: $.otp.createLink({
      controller: 'crashRepair',
      action: 'restartWorkflowRun'
    }),
    dataType: 'json',
    type: 'POST',
    data: {
      workflowStep: stepId
    },
    success() {
      $.otp.toaster.showSuccessToast(
        `Workflow run of step #${stepId} restarted`,
        `Restart of the step #${stepId} was successful.`
      );
      syncWorkflowStepData();
    },
    error(error) {
      if (error && error.responseJSON && error.responseJSON.message) {
        $.otp.toaster.showErrorToast('Restart Failed', error.responseJSON.message);
      } else {
        $.otp.toaster.showErrorToast('Restart Failed', 'Unknown error during workflow restart.');
      }
    }
  });
}

/**
 * Set a workflow step as failed. This includes also the workflow run state to be failed.
 * @param stepId
 */
function setWorkflowStepAsFailed(stepId) {
  'use strict';

  $.ajax({
    url: $.otp.createLink({
      controller: 'crashRepair',
      action: 'markWorkflowStepAsFailed'
    }),
    dataType: 'json',
    type: 'POST',
    data: {
      workflowStep: stepId
    },
    success() {
      $.otp.toaster.showSuccessToast('Failed set', `Workflow step #${stepId} has successful been set as failed.`);
      syncWorkflowStepData();
    },
    error(error) {
      if (error && error.responseJSON && error.responseJSON.message) {
        $.otp.toaster.showErrorToast('Mark as failed', error.responseJSON.message);
      } else {
        $.otp.toaster.showErrorToast('Mark as failed', 'Unknown error during mark as failed.');
      }
    }
  });
}

/**
 * Mark a workflow run of a given workflow step as final failed.
 * @param stepId which is part of the final failed workflow run
 */
function setWorkflowRunAsFinalFailed(stepId) {
  'use strict';

  $.ajax({
    url: $.otp.createLink({
      controller: 'crashRepair',
      action: 'markWorkflowRunAsFinalFailed'
    }),
    dataType: 'json',
    type: 'POST',
    data: {
      workflowStep: stepId
    },
    success() {
      $.otp.toaster.showSuccessToast(
        'Final failed set',
        `Workflow run of the step #${stepId} has successful been set as final failed.`
      );
      syncWorkflowStepData();
    },
    error(error) {
      if (error && error.responseJSON && error.responseJSON.message) {
        $.otp.toaster.showErrorToast('Mark as final failed', error.responseJSON.message);
      } else {
        $.otp.toaster.showErrorToast('Mark as final failed', 'Unknown error during mark as final failed.');
      }
    }
  });
}

/* eslint-disable no-unused-vars */
function startWorkflowSystem() {
  'use strict';

  $.ajax({
    url: $.otp.createLink({
      controller: 'crashRepair',
      action: 'startWorkflowSystem'
    }),
    dataType: 'json',
    type: 'POST',
    success() {
      $.otp.toaster.showSuccessToast('Workflow System', 'Workflow system has been started.');
      window.setTimeout(() => {
        window.location.reload(true);
      }, 3000);
    },
    error(error) {
      if (error && error.responseJSON && error.responseJSON.message) {
        $.otp.toaster.showErrorToast('Workflow System', error.responseJSON.message);
      } else {
        $.otp.toaster.showErrorToast('Workflow System', 'Workflow system start failed.');
      }
    }
  });
}

function restartSelectedSteps() {
  'use strict';

  getSelectedSteps((steps) => {
    steps.each((index, step) => {
      restartStep(step);
    });
  });
}

function restartSelectedWorkflowRuns() {
  'use strict';

  getSelectedSteps((steps) => {
    steps.each((index, step) => {
      restartWorkflowRun(step);
    });
  });
}

function markSelectedStepsAsFailed() {
  'use strict';

  getSelectedSteps((steps) => {
    steps.each((index, step) => {
      setWorkflowStepAsFailed(step);
    });
  });
}

function markSelectedRunsAsFinalFailed() {
  'use strict';

  getSelectedSteps((steps) => {
    steps.each((index, step) => {
      setWorkflowRunAsFinalFailed(step);
    });
  });
}
/* eslint-enable no-unused-vars */

function getSelectedSteps(callback) {
  'use strict';

  const selectedSteps = $('.tableCheckbox:checked');
  const selectedStepIds = selectedSteps.map((step) => selectedSteps[step].value);
  callback(selectedStepIds);
}

function initializeDataTable() {
  'use strict';

  const table = $('#jobTable');
  table.DataTable({
    paging: false
  });
}

$(document).ready(() => {
  'use strict';

  initializeDataTable();
  syncWorkflowStepData();
});
