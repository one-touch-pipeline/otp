/*
 * Copyright 2011-2025 The OTP authors
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
(function () {
  'use strict';

  const loadWorkflowOverviewData = () => {
    fetch($.otp.createLink({ controller: 'workflowSystemConfig', action: 'getWorkflows' }))
      .then((r) => r.json())
      .then((data) => renderWorkflowOverviewTable(data))
      .catch(() => $.otp.toaster.showErrorToast('Error', 'Failed to load workflows. Please try again.'));
  };

  const renderWorkflowOverviewTable = (workflowData) => {
    $('#workflowOverview').DataTable({
      data: workflowData,
      pageLength: 100,
      order: [2, 'asc'],
      columns: [
        { data: 'id', visible: false },
        {
          className: 'details-control',
          ordering: false,
          // eslint-disable-next-line max-len
          render: () => '<button class="btn btn-xs btn-info spinner-button" title="Show/hide workflow versions"><i id="versions-icon" class="bi bi-chevron-bar-expand versions-icon"></i><span class="spinner-border spinner-border-sm d-none" style="display: none" aria-hidden="true"></span></button>'
        },
        { data: 'name' },
        { data: 'priority' },
        {
          data: 'enabled',
          render: (enabled) => (enabled ?
            '<i class="bi bi-check-circle text-success"></i>' :
            '<i class="bi bi-x-circle text-danger"></i>')
        },
        { data: 'maxParallelWorkflows' },
        {
          data: 'defaultVersion',
          render: (dv) => (dv ? dv.workflowVersion : '')
        },
        {
          data: 'supportedSeqTypes',
          render: (seqTypes) => (seqTypes ? seqTypes.map((s) => s.displayName).join(', ') : '')
        },
        {
          data: 'allowedRefGenomes',
          render: (refGenomes) => (refGenomes ? refGenomes.map((g) => g.name).join(', ') : '')
        },
        {
          data: 'deprecationDate',
          render: (d) => (d !== 'N/A' ? `Yes (since ${d})` : '')
        },
        {
          data: '',
          // eslint-disable-next-line max-len
          render: () => '<button class="btn btn-sm btn-primary float-right workflow-edit-btn" aria-label="Edit workflow"><i class="bi bi-pencil"></i></button>'
        }
      ]
    });
    $('[data-original-title], [title]').tooltip({ placement: 'bottom' });
  };
  $(document).on('click', '.workflow-edit-btn', function () {
    const api = $('#workflowOverview').DataTable();
    const rowData = api.row($(this).closest('tr')).data();
    WorkflowEdit.openModal(rowData, {
      onSuccess: (updated) => {
        const idx = api.rows().indexes().toArray().find((i) => api.row(i).data().id === updated.id);
        if (idx !== undefined) {
          api.row(idx).data(updated).draw(false);
        }
      }
    });
  });

  $(document).ready(() => { loadWorkflowOverviewData(); });
}());
