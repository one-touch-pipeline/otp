/*
 * Copyright 2011-2026 The OTP authors
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

  let currentGroupId = null;

  const postForm = (action, params) => {
    const body = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      (Array.isArray(value) ? value : [value]).forEach((entry) => body.append(key, entry));
    });
    return fetch($.otp.createLink({ controller: 'workflowSystemConfig', action }), {
      method: 'POST',
      body
    }).then((response) => {
      if (!response.ok) {
        // error responses are wrapped in a JSON envelope ({ timestamp, status, message, path, ... }) by
        // Spring Boot's default error handling, so the actual message has to be pulled out of it rather
        // than displaying the raw response body
        return response.json()
          .catch(() => ({}))
          .then((body) => { throw new Error(body.message || 'Unknown error occurred.'); });
      }
      return response.json();
    });
  };

  const loadVersionOptions = (workflowId, versionSelect, preselectId) => {
    versionSelect.empty();
    if (!workflowId) {
      return;
    }
    fetch($.otp.createLink({
      controller: 'workflowSystemConfig',
      action: 'getWorkflowVersions',
      parameters: { workflowId }
    }))
      .then((response) => response.json())
      .then((versions) => {
        versions.forEach((version) => {
          const option = $('<option></option>')
            .attr('value', version.id)
            .prop('selected', version.isDefault)
            .text(version.displayName);
          versionSelect.append(option);
        });
        if (preselectId) {
          versionSelect.val(preselectId);
        }
      })
      .catch(() => $.otp.toaster.showErrorToast('Error', 'Failed to load workflow versions.'));
  };

  const buildEntryRow = (entry) => {
    const row = $('<tr class="default-modal-entry-row"></tr>');
    const workflowSelect = $('<select class="form-select default-entry-workflow-select"></select>')
      .append($('#workflow-default-workflow-template').html());
    const versionSelect = $('<select class="form-select default-entry-version-select"></select>');
    const removeBtn = $('<button type="button" class="btn btn-outline-danger btn-sm">' +
      '<i class="bi bi-x-lg"></i></button>');

    removeBtn.on('click', () => {
      if ($('#default-modal-entries-body tr').length > 1) {
        row.remove();
      }
    });

    workflowSelect.on('change', () => loadVersionOptions(workflowSelect.val(), versionSelect));

    row.append($('<td></td>').append(workflowSelect));
    row.append($('<td></td>').append(versionSelect));
    row.append($('<td></td>').append(removeBtn));

    if (entry) {
      workflowSelect.val(entry.workflow.id);
      loadVersionOptions(entry.workflow.id, versionSelect, entry.workflowVersion.id);
    }

    return row;
  };

  const addEntryRow = (entry) => {
    $('#default-modal-entries-body').append(buildEntryRow(entry));
  };

  const openModal = (mode, group) => {
    const modal = $('#workflowDefaultModal');
    $('#default-modal-entries-body').empty();

    if (mode === 'edit') {
      currentGroupId = group.id;
      $('.modal-title', modal).text($('#analysisDefaultsTable').data('edit-label'));
      $('#default-modal-name').val(group.name);
      $('#default-modal-seqType').val(group.seqType.id).trigger('change');
      group.entries.forEach((entry) => addEntryRow(entry));
      $('#default-modal-delete-btn').removeClass('d-none');
    } else {
      currentGroupId = null;
      $('.modal-title', modal).text($('#add-workflow-default-btn').text().trim());
      $('#default-modal-name').val('');
      $('#default-modal-seqType').val('').trigger('change');
      addEntryRow();
      $('#default-modal-delete-btn').addClass('d-none');
    }

    modal.modal('show');
  };

  const saveGroup = () => {
    const name = $('#default-modal-name').val();
    const seqTypeId = $('#default-modal-seqType').val();
    const rows = $('#default-modal-entries-body tr').toArray().map((row) => ({
      workflowId: $('.default-entry-workflow-select', row).val(),
      versionId: $('.default-entry-version-select', row).val()
    }));

    // a row with no workflow selected is just a blank spacer and gets dropped below; only a row where a
    // workflow was chosen but no version yet counts as invalid
    if (rows.some((row) => row.workflowId && !row.versionId)) {
      $.otp.toaster.showErrorToast('Error', 'Please select a workflow version for every row.');
      return;
    }

    const versionIds = rows.filter((row) => row.versionId).map((row) => row.versionId);

    const params = { name, 'seqType.id': seqTypeId, workflowVersion: versionIds };
    if (currentGroupId) {
      params['group.id'] = currentGroupId;
    }

    postForm('saveWorkflowDefaultGroup', params).then(() => {
      loadDefaultsTable();
      $.otp.toaster.showSuccessToast('Success', 'Workflow default saved.');
    }).catch((error) => $.otp.toaster.showErrorToast('Error', `Failed to save default. ${error.message}`));
  };

  const deleteGroup = () => {
    if (!currentGroupId) {
      return;
    }
    if (!window.confirm($('#default-modal-delete-btn').data('confirm-message'))) {
      return;
    }
    postForm('deleteWorkflowDefaultGroup', { 'group.id': currentGroupId }).then(() => {
      $('#workflowDefaultModal').modal('hide');
      loadDefaultsTable();
      $.otp.toaster.showSuccessToast('Success', 'Workflow default deleted.');
    }).catch((error) => $.otp.toaster.showErrorToast('Error', `Failed to delete default. ${error.message}`));
  };

  /**
   * Merges the name and edit-button cells into a rowspan across each group's rows, removing the now-redundant
   * duplicate cells. Requires rows of the same group to stay contiguous, which is why ordering/paging are off.
   */
  const mergeGroupCells = (api) => {
    const rowNodes = api.rows({ order: 'current' }).nodes().toArray();
    const rowData = api.rows({ order: 'current' }).data().toArray();

    let anchorNameCell = null;
    let anchorEditCell = null;
    let anchorGroupId = null;
    let span = 0;

    const flushSpan = () => {
      if (span > 1) {
        anchorNameCell.setAttribute('rowspan', span);
        anchorEditCell.setAttribute('rowspan', span);
      }
    };

    rowData.forEach((entry, index) => {
      const cells = rowNodes[index].cells;
      if (entry.groupId === anchorGroupId) {
        cells[0].remove();
        cells[cells.length - 1].remove();
        span += 1;
      } else {
        flushSpan();
        anchorGroupId = entry.groupId;
        [anchorNameCell] = cells;
        anchorEditCell = cells[cells.length - 1];
        span = 1;
      }
    });
    flushSpan();
  };

  const renderDefaultsTable = (rows) => {
    const table = $('#analysisDefaultsTable');
    if ($.fn.DataTable.isDataTable(table)) {
      table.DataTable().clear().rows.add(rows).draw();
      return;
    }
    table.DataTable({
      dom: 'it',
      paging: false,
      ordering: false,
      data: rows,
      language: { emptyTable: table.data('empty-message') },
      columns: [
        { data: 'name', className: 'align-middle', render: $.fn.dataTable.render.text() },
        { data: 'workflow.displayName', render: $.fn.dataTable.render.text() },
        { data: 'seqType.displayName', render: $.fn.dataTable.render.text() },
        { data: 'workflowVersion.displayName', render: $.fn.dataTable.render.text() },
        {
          data: null,
          className: 'align-middle',
          render: () => `<button type="button" class="btn btn-outline-primary btn-sm workflow-default-edit-btn">
            ${table.data('edit-label')}</button>`
        }
      ],
      drawCallback() {
        mergeGroupCells(this.api());
      }
    });
  };

  $(document).on('click', '.workflow-default-edit-btn', function () {
    const api = $('#analysisDefaultsTable').DataTable();
    const rowData = api.row($(this).closest('tr')).data();
    const entries = api.rows().data().toArray()
      .filter((row) => row.groupId === rowData.groupId)
      .map((row) => ({ workflow: row.workflow, workflowVersion: row.workflowVersion }));
    openModal('edit', { id: rowData.groupId, name: rowData.name, seqType: rowData.seqType, entries });
  });

  const loadDefaultsTable = () => {
    fetch($.otp.createLink({ controller: 'workflowSystemConfig', action: 'getWorkflowDefaultGroups' }))
      .then((response) => response.json())
      .then((rows) => renderDefaultsTable(rows))
      .catch(() => $.otp.toaster.showErrorToast('Error', 'Failed to load workflow defaults.'));
  };

  $(document).ready(() => {
    const modal = $('#workflowDefaultModal');
    modal.find('#confirmModal').on('click', () => saveGroup());
    modal.find('.closeModal').on('click', () => modal.modal('hide'));

    // select2's dropdown is appended to <body> by default, which conflicts with Bootstrap's modal focus
    // trap (clicks into the search box lose focus immediately). Anchoring it to the modal fixes that.
    $('#default-modal-seqType').select2({
      theme: 'bootstrap4',
      minimumResultsForSearch: 7,
      dropdownParent: modal
    });

    $('#add-workflow-default-btn').on('click', () => openModal('add'));
    $('#default-modal-add-row-btn').on('click', () => addEntryRow());
    $('#default-modal-delete-btn').on('click', () => deleteGroup());

    loadDefaultsTable();
  });
}());
