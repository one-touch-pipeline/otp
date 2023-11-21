/*
 * Copyright 2011-2023 The OTP authors
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
// eslint-disable-next-line strict
$(() => {
  addEventHandlerToTable();
});

/**
 * Loading workflow versions for a specified data table row in a child table
 * @param correspondingRow is the data table row containing the workflow
 * @returns {*|jQuery} the initial loading jQuery object to render
 */
const loadWorkflowVersionData = (correspondingRow) => {
  'use strict';

  const workflowId = correspondingRow.data().id;

  return fetch($.otp.createLink({
    controller: 'workflowSystemConfig',
    action: 'getWorkflowVersions',
    parameters: { workflowId }
  }))
    .then((response) => response.json())
    .then((data) => {
      correspondingRow.child(renderWorkflowVersionTable(data, workflowId), 'px-2 py-0').show();
      $('div.slider', correspondingRow.child()).slideDown();
    }).catch((error) => {
      correspondingRow.child.hide();
      $.otp.toaster.showErrorToast('Error', `Failed to load workflow versions. Please try again. ${error}`);
    });
};

/**
 * Assign opening and loading of the data child table for the workflow versions when clicking on workflow table row
 * and handling the corresponding loading spinner.
 */
const addEventHandlerToTable = () => {
  'use strict';

  $('#workflowOverview tbody').on('click', 'td.details-control button', function () {
    const table = $('#workflowOverview').DataTable();
    const tr = $(this).closest('tr');
    const spinner = $(this).find('#versions-spinner');
    const icon = $(this).find('#versions-icon');
    const row = table.row(tr);

    if (row.child.isShown()) {
      $('div.slider', row.child()).slideUp(() => {
        row.child.hide();
        tr.removeClass('shown');
      });
    } else {
      spinner.show();
      icon.hide();
      loadWorkflowVersionData(row).finally(() => {
        spinner.hide();
        icon.show();
        tr.addClass('shown');
      });
    }
  });
};

/**
 * Render the data table for the workflow versions with given data.
 * @param workflowVersionData: data set for the data table
 * @param workflowId: the corresponding id of the workflow
 * @returns {*|jQuery} the rendered dataTable
 */
const renderWorkflowVersionTable = (workflowVersionData, workflowId) => {
  'use strict';

  const workflowVersionsDataTable = $('#workflowVersionsTemplate').clone()
    .attr('id', `workflowVersions${workflowId}`);
  workflowVersionsDataTable.removeAttr('hidden');

  workflowVersionsDataTable.DataTable({
    data: workflowVersionData,
    order: [0, 'asc'],
    columns: [
      { data: 'name' },
      { data: 'comment' },
      {
        data: 'commentData',
        render: ({
          author,
          date
        }) => {
          let deprecationText = '';
          if (date && date !== 'N/A') {
            deprecationText += `${author} (${date})`;
          }
          return deprecationText;
        }
      },
      {
        data: 'deprecateDate',
        render: (deprecateDate) => {
          let deprecationText = '';
          if (deprecateDate && deprecateDate !== 'N/A') {
            deprecationText += `Yes (since ${deprecateDate})`;
          }
          return deprecationText;
        }
      },
      {
        render: (column, type, row) => `<button class="btn btn-sm btn-primary float-right"
          id="modify-btn-${row.id}" onclick="openAndRenderModificationModal(
          JSON.parse('${JSON.stringify(row).replace(/'/g, '&apos;').replace(/"/g, '&quot;')}'))"
          data-original-title="Change deprecation state">
             <i class="bi bi-archive"></i>
           </button>`
      }
    ]
  });

  workflowVersionsDataTable.find('[title], [data-original-title]').tooltip({
    placement: 'top'
  });
  // An empty element is needed, that two rows are added and the alternating coloring doesn't change.
  return ['', $('<div/>').addClass('slider').append($('<div/>').addClass('p-1').append(workflowVersionsDataTable))];
};

/**
 * Open and render the modal to update a workflow version.
 * @param workflowVersion: contains current data about the workflowVersion
 */
// eslint-disable-next-line no-unused-vars
const openAndRenderModificationModal = (workflowVersion) => {
  'use strict';

  const modal = $('#updateWorkflowVersionModal');
  const confirmButton = modal.find('#confirmModal');
  const cancelButtons = modal.find('.closeModal');

  confirmButton.unbind('click');
  confirmButton.on('click', () => {
    modal.modal('hide');
    updateDeprecationStateWorkflowVersion(workflowVersion, modal);
  });

  cancelButtons.unbind('click');
  cancelButtons.on('click', () => {
    modal.modal('hide');
  });

  updateModalWithCurrentWorkflowVersion(workflowVersion, modal);

  modal.modal('toggle').show();
};

/**
 * Updates the content of the workflowVersion update modal
 * @param workflowVersion: contains data about the current workflow version
 * @param modal: contains the jQuery modal element
 */
const updateModalWithCurrentWorkflowVersion = (workflowVersion, modal) => {
  'use strict';

  $('.modal-title', modal).html(`Modify Workflow Version ${workflowVersion.name}`);
  $('#comment', modal).val(workflowVersion.comment);
  $('#deprecate-state', modal).prop('checked', workflowVersion.deprecateDate !== 'N/A');
};

/**
 * Updates the state of deprecation of the workflow version via ajax
 * @param workflowVersion: containing the current workflowVersion before changing
 * @param modal: the modal containing the elements with the changed data
 */
const updateDeprecationStateWorkflowVersion = (workflowVersion, modal) => {
  'use strict';

  const workflowVersionToModify = {
    workflowVersionId: workflowVersion.id,
    comment: $('#comment', modal).val(),
    deprecate: $('#deprecate-state', modal).prop('checked')
  };

  fetch($.otp.createLink({
    controller: 'workflowSystemConfig',
    action: 'updateWorkflowVersion'
  }), {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(workflowVersionToModify)
  }).then((response) => {
    if (!response.ok) {
      return response.json().then((res) => {
        throw Error(res.message);
      });
    }
    return response.json();
  }).then((response) => {
    const wvTable = $(`#workflowVersions${workflowVersion.workflowId}`);
    const rowToUpdate = wvTable.dataTable().api().cells(`:contains(${response.name})`).nodes()[0];

    wvTable.dataTable().fnUpdate(response, rowToUpdate, undefined, true);

    $.otp.toaster.showSuccessToast(
      'Update successful',
      `Workflow version <b>${workflowVersion.name}</b> has been updated.`
    );
  }).catch((error) => {
    $.otp.toaster.showErrorToast('Error', `Failed to update workflow version. ${error}`);
  });
};
