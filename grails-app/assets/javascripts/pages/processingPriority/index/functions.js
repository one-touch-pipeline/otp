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

$.otp.processingPriority = {

  /*
     * Field names shown in the data table (id and version are hidden)
     */
  mapFieldIndex: {
    id: 0,
    version: 1,
    name: 2,
    priority: 3,
    allowedParallelWorkflowRuns: 4,
    queue: 5,
    roddyConfigSuffix: 6,
    errorMailPrefix: 7
  },

  /*
     * Last column of data table contains only two buttons (delete, edit)
     */
  /* eslint-disable no-template-curly-in-string */
  ppActionColumn:
        '<button id="delete-row" class="btn btn-sm btn-outline-danger"' +
        ' type="button" title="${g.message(code: "processingPriority.tooltip.delete")}">\n' +
        '         <i class="bi bi-trash"></i>\n' +
        '</button>\n' +
        '<button id="edit-row" class="btn btn-sm btn-outline-secondary"' +
        ' type="button" title="${g.message(code: "processingPriority.tooltip.edit")}">\n' +
        '        <i class="bi bi-pencil"></i>\n' +
        '</button>',
  /* eslint-enable no-template-curly-in-string */

  /*
     * Delete the priority from backend, if successful remove the row also from the data table
     */
  remove() {
    'use strict';

    const oTable = this.getDataTable();
    const rowIndex = $('#pp-row').val();
    const data = oTable.row(rowIndex).data();
    const entityId = data[this.mapFieldIndex.id];
    const entityName = data[this.mapFieldIndex.name];
    $.ajax({
      url: $.otp.createLink({
        controller: 'ProcessingPriority',
        action: 'delete',
        id: entityId
      }),
      type: 'DELETE',
      success() {
        oTable.row(rowIndex).remove().draw();
        $.otp.toaster.showSuccessToast(
          'Delete Processing Priority',
          `Processing Priority ${entityName} has been deleted.`
        );
      },
      error() {
        $.otp.toaster.showErrorToast(
          'Failed Deleting Processing Priority',
          `Processing Priority ${entityName} cannot be deleted.`
        );
      }
    }).always(() => {
      $.otp.processingPriority.closeDialog();
    });
  },

  validateName(input) {
    'use strict';

    const rowIndex = $('#pp-row').val();

    const sValue = $('#pp-name').val();
    const colIndex = this.mapFieldIndex.name;
    const vFeedback = $('#vf-name');

    const namesAvailable = this.getDataTable().columns(colIndex).data().flatten()
      .map((s) => s.toUpperCase());

    if (rowIndex > -1) {
      const oriVal = this.getDataTable().row(rowIndex).data()[colIndex].toUpperCase();
      const idx = namesAvailable.indexOf(oriVal);
      namesAvailable.splice(idx, 1);
    }

    if (namesAvailable.indexOf(sValue.toUpperCase()) > -1) {
      vFeedback.removeClass('d-none');
      input.setCustomValidity(`"${input.value}" is not valid.`);
    } else {
      vFeedback.addClass('d-none');
      input.setCustomValidity('');
    }

    this.toggleSaveButton();
  },

  validatePriority(input) {
    'use strict';

    const rowIndex = $('#pp-row').val();

    const sValue = $('#pp-priority').val();
    const colIndex = this.mapFieldIndex.priority;
    const vFeedback = $('#vf-priority');

    const namesAvailable = this.getDataTable().columns(colIndex).data().flatten()
      .map((s) => (typeof (s) === 'number' ? s.toString() : s));

    if (rowIndex > -1) {
      const oriVal = this.getDataTable().row(rowIndex).data()[colIndex].toString();
      const idx = namesAvailable.indexOf(oriVal);
      namesAvailable.splice(idx, 1);
    }

    if (namesAvailable.indexOf(sValue) > -1) {
      vFeedback.removeClass('d-none');
      input.setCustomValidity(`"${input.value}" is not valid.`);
    } else {
      vFeedback.addClass('d-none');
      input.setCustomValidity('');
    }

    this.toggleSaveButton();
  },

  validateAllowedParallelWorkflowRuns(input) {
    'use strict';

    const sValue = $('#pp-allowedParallelWorkflowRuns').val();
    const vFeedback = $('#vf-allowedParallelWorkflowRuns');

    const val = parseInt(sValue, 10);
    if (val <= 0) {
      vFeedback.removeClass('d-none');
      input.setCustomValidity(`"${input.value}" is not valid.`);
    } else {
      vFeedback.addClass('d-none');
      input.setCustomValidity('');
    }

    this.toggleSaveButton();
  },

  validateString() {
    'use strict';

    // nothing to do for custom validation
    this.toggleSaveButton();
  },

  /*
     * Activate the save button only if all fields are valid
     */
  toggleSaveButton() {
    'use strict';

    $('#processingPriorityForm')[0].checkValidity();

    $('#processingPriorityForm').addClass('was-validated');

    const bNotValid = Object.keys(this.mapFieldIndex).some((field) => !($(`#pp-${field}`)[0].validity.valid));
    $('.modal-footer button#bt-save').prop('disabled', bNotValid);
  },

  /*
     * Save the processing priority if a new object is created or existing one to be modified
     */
  save() {
    'use strict';

    const entityId = $('#pp-id').val();
    const rowIndex = $('#pp-row').val();
    const version = $('#pp-version').val();
    const form = $('#processingPriorityForm');

    const type = 'POST';
    let url;
    if (entityId > 0) {
      url = $.otp.createLink({
        controller: 'ProcessingPriority',
        action: 'update',
        id: entityId
      });
      $('form input#pp-id').prop('disabled', false);
    } else {
      url = $.otp.createLink({
        controller: 'ProcessingPriority',
        action: 'save'
      });
      $('form input#pp-id').prop('disabled', true);
    }
    const data = form.serialize();

    $.ajax({
      type,
      url,
      data,
      success(response) {
        const oTable = $.otp.processingPriority.getDataTable();
        const oData = [];
        Object.keys($.otp.processingPriority.mapFieldIndex).forEach((field) => {
          oData[$.otp.processingPriority.mapFieldIndex[field]] = response[field];
        });
        oData.push($.otp.processingPriority.ppActionColumn);

        if (entityId > 0) {
          oData[1] = parseInt(version, 10) + 1;
          oTable.row(rowIndex).data(oData).draw();
        } else {
          oData[1] = 0;
          oTable.row.add(oData).draw();
        }
        $.otp.toaster.showSuccessToast(
          'Save Processing Priority',
          `Processing Priority ${oData[$.otp.processingPriority.mapFieldIndex.name]} has been saved.`
        );
      },
      error() {
        $.otp.toaster.showErrorToast(
          'Failed Saving Processing Priority',
          'Make sure that both name and priority are globally unique.'
        );
      }
    }).always(() => {
      $.otp.processingPriority.closeDialog();
    });
  },

  /*
     * Helper functions
     */
  getDataTable() {
    'use strict';

    return $.fn.dataTable.tables({ api: true });
  },

  getDialogObj() {
    'use strict';

    return $('#processingPriorityModal');
  },

  openDialog() {
    'use strict';

    return $('#processingPriorityModal').modal();
  },

  closeDialog() {
    'use strict';

    $('#processingPriorityModal').modal('hide');
  }
};

$(document).ready(() => {
  'use strict';

  /*
     * Initialization of a DataTable for Processing Priority List
     * ordered by priority
     * rows id and version are hidden
     */
  const processingPriorityTable = $('#processingPriority').DataTable({
    dom: '<"toolbar">frtip',
    order: [[3, 'asc']],
    columnDefs: [
      {
        targets: [0, 1],
        visible: false,
        searchable: false
      },
      {
        targets: [8],
        orderable: false
      },
      {
        type: 'num',
        targets: [0, 1, 3, 4]
      }
    ]
  });

  $('div.toolbar').html(`<button type="button" class="btn btn-primary"
               data-bs-toggle="modal" data-bs-target="#processingPriorityModal">Create</button>`);

  /*
     * Event handler triggered by Delete icon in the table
     * it launches the modal dialog window for confirmation before deletion
     * it checks if the priority has been used somewhere, if yes, stop the deletion
     */
  $('#processingPriority').on('click', 'tbody tr td button#delete-row', function () {
    const rowIndex = processingPriorityTable.row($(this).parents('tr')).index();
    const aData = processingPriorityTable.row(rowIndex).data();

    // call backend to fetch the referring objects if exist
    $.ajax({
      type: 'get',
      url: $.otp.createLink({
        controller: 'ProcessingPriority',
        action: 'refer',
        id: aData[0]
      }),
      success(response) {
        if ($.isEmptyObject(response)) {
          const oDialog = $.otp.processingPriority.openDialog();
          oDialog.find('.modal-footer button#bt-delete').removeClass('d-none');
          oDialog.find('.modal-footer button#bt-save').addClass('d-none');

          oDialog.find('.modal-body div#edit-processingPriority').addClass('d-none');
          oDialog.find('.modal-body div#confirm-delete').removeClass('d-none');
          oDialog.find('.modal-body div#confirm-delete').prop('disabled', false);
          oDialog.find('.modal-body input#pp-row').val(rowIndex);
          oDialog.find('.modal-body em#em-name').text(aData[$.otp.processingPriority.mapFieldIndex.name]);
        } else {
          $.otp.toaster.showErrorToast(
            'Delete Processing Priority',
            `${aData[$.otp.processingPriority.mapFieldIndex.name]}
               cannot be deleted due to references: ${JSON.stringify(response)}`
          );
        }
      },
      error() {
        $.otp.toaster.showErrorToast(
          'Check Processing Priority References',
          'Check OTP server logs for detailed information'
        );
      }
    });
  });

  /*
     * Event handler triggered by Edit icon in the table
     * it launches the modal dialog window for editing the attributes of selected priority
     * data is transferred from selected row to the dialog
     */
  $('#processingPriority').on('click', 'tbody td button#edit-row', function () {
    const aData = processingPriorityTable.row($(this).parents('tr')).data();

    const oDialog = $.otp.processingPriority.openDialog();

    oDialog.find('.modal-footer button#bt-delete').addClass('d-none');
    oDialog.find('.modal-footer button#bt-save').removeClass('d-none');
    oDialog.find('.modal-footer button#bt-save').prop('disabled', true);

    oDialog.find('.modal-body input#pp-row').val(processingPriorityTable.row($(this).parents('tr')).index());

    // init dialog input fields
    Object.keys($.otp.processingPriority.mapFieldIndex).forEach((field, idx) => {
      oDialog.find(`.modal-body input#pp-${field}`).val(aData[idx]);
    });

    $.otp.processingPriority.toggleSaveButton();
  });

  /*
     * Event handler to cleanup after closing the dialog window
     */
  $('#processingPriorityModal').on('hidden.bs.modal', () => {
    const oDialog = $.otp.processingPriority.getDialogObj();

    oDialog.find('.modal-body div#edit-processingPriority').removeClass('d-none');
    oDialog.find('.modal-body div#confirm-delete').addClass('d-none');

    oDialog.find('.modal-footer button#bt-delete').addClass('d-none');
    oDialog.find('.modal-footer button#bt-save').removeClass('d-none');
    oDialog.find('.modal-footer button#bt-save').prop('disabled', true);

    oDialog.find('.modal-body input#pp-row').val(-1);

    // reset all fields to empty
    Object.keys($.otp.processingPriority.mapFieldIndex).forEach((field) => {
      oDialog.find(`.modal-body input#pp-${field}`).val('');
      oDialog.find(`.modal-body input#pp-${field}`)[0].setCustomValidity('');
    });
  });
});
