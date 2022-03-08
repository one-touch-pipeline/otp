/*
 * Copyright 2011-2021 The OTP authors
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

/**
 * Load workflows from backend.
 */
const loadWorkflowOverviewData = () => {
  'use strict';

  fetch($.otp.createLink({ controller: 'workflowSystemConfig', action: 'getWorkflows' }))
    .then((response) => response.json())
    .then((data) => renderWorkflowOverviewTable(data))
    .catch(() => {
      $.otp.toaster.showErrorToast('Error', 'Failed to load workflows. Please try again.');
    });
};

/**
 * Fill the workflow overview table with data and render it.
 *
 * @param workflowData data set for the table rows
 */
const renderWorkflowOverviewTable = (workflowData) => {
  'use strict';

  $('#workflowOverview').DataTable({
    data: workflowData,
    pageLength: 100,
    order: [1, 'asc'],
    columns: [
      { data: 'id', visible: false },
      { data: 'name' },
      { data: 'priority' },
      {
        data: 'enabled',
        render: (enabled) => {
          if (enabled) {
            return '<i class="bi bi-check-circle text-success"></i>';
          }
          return '<i class="bi bi-x-circle text-danger"></i>';
        }
      },
      { data: 'maxParallelWorkflows' },
      {
        data: 'supportedSeqTypes',
        render: (seqTypes) => {
          if (seqTypes) {
            return seqTypes.map((seqType) => {
              if (seqType.singleCell) {
                return `${seqType.displayName} ${seqType.libraryLayout} single cell`;
              }
              return `${seqType.displayName} ${seqType.libraryLayout} bulk`;
            }).join(', ');
          }
          return '';
        }
      },
      {
        data: 'allowedRefGenomes',
        render: (refGenomes) => (refGenomes ? refGenomes.map((refGenome) => refGenome.name).join(', ') : '')
      },
      {
        data: 'deprecationDate',
        render: (date) => (date !== 'na' ? date : '')
      },
      {
        data: '',
        render: (column, type, row) => `<button class="btn btn-sm btn-primary"
          onclick="openAndRenderEditModal(
            JSON.parse('${JSON.stringify(row).replace(/'/g, '&apos;').replace(/"/g, '&quot;')}'))">
            <i class="bi bi-pencil"></i>
          </button>`
      }
    ]
  });
};

/**
 * Open and render modal dialog for editing workflows with the given workflow data.
 *
 * @param workflow to show and edit
 */
// eslint-disable-next-line no-unused-vars
const openAndRenderEditModal = (workflow) => {
  'use strict';

  const modal = $('#editWorkflowModal');
  const confirmButton = modal.find('#confirmModal');
  const cancelButtons = modal.find('.closeModal');

  confirmButton.unbind('click');
  confirmButton.on('click', () => {
    modal.hide();
    updateWorkflow(workflow, modal);
  });

  cancelButtons.unbind('click');
  cancelButtons.on('click', () => {
    modal.hide();
  });

  updateModalWithCurrentWorkflow(workflow, modal);

  modal.modal('toggle').show();
};

/**
 * Update dialog data with currently selected workflow data.
 *
 * @param workflow
 * @param modal
 */
const updateModalWithCurrentWorkflow = (workflow, modal) => {
  'use strict';

  $('.modal-title', modal).html(workflow.name);
  $('#modal-priority', modal).val(workflow.priority);
  $('#modal-max-runs', modal).val(workflow.maxParallelWorkflows);
  $('#modal-enabled', modal).prop('checked', workflow.enabled);
  $('#modal-deprecated', modal).prop('checked', workflow.deprecationDate !== 'na');
  $('#modal-seqTypes', modal).trigger('change');
  $('#modal-refGenomes', modal).trigger('change');

  if (workflow.supportedSeqTypes) {
    $('#modal-seqTypes', modal).val(workflow.supportedSeqTypes.map((s) => s.id));
  } else {
    $('#modal-seqTypes', modal).val();
  }

  if (workflow.allowedRefGenomes) {
    $('#modal-refGenomes', modal).val(workflow.allowedRefGenomes.map((rg) => rg.id));
  } else {
    $('#modal-refGenomes', modal).val();
  }
};

/**
 * Update workflow from modal. Send update to the backend and update the workflow in the overview table.
 *
 * @param workflow
 * @param modal
 */
const updateWorkflow = (workflow, modal) => {
  'use strict';

  const workflowToUpdate = {
    id: workflow.id,
    priority: $('#modal-priority', modal).val(),
    enabled: $('#modal-enabled', modal).prop('checked'),
    maxParallelWorkflows: $('#modal-max-runs', modal).val(),
    deprecated: $('#modal-deprecated', modal).prop('checked'),
    supportedSeqTypes: $('#modal-seqTypes', modal).select2('data').map((s) => s.id),
    allowedRefGenomes: $('#modal-refGenomes', modal).select2('data').map((rg) => rg.id)
  };

  fetch($.otp.createLink({ controller: 'workflowSystemConfig', action: 'updateWorkflow' }), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(workflowToUpdate)
  })
    .then((response) => {
      if (!response.ok) {
        return response.json().then((res) => {
          throw Error(res.message);
        });
      }

      return response.json();
    })
    .then((response) => {
      const wfTable = $('#workflowOverview');
      const rowToUpdate = wfTable.dataTable().api().cells(`:contains(${response.name})`).nodes()[0];

      wfTable.dataTable().fnUpdate(response, rowToUpdate, undefined, false);

      $.otp.toaster.showSuccessToast(
        'Update successful',
        `<b>${workflow.name}</b> configuration has been updated.`
      );
    })
    .catch((error) => {
      $.otp.toaster.showErrorToast('Error', `Failed to load workflows. ${error}`);
    });
};

$(document).ready(() => {
  'use strict';

  loadWorkflowOverviewData();
  $('[title]').tooltip({
    placement: 'bottom'
  });
});
