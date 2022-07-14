/*
 * Copyright 2011-2022 The OTP authors
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

$.otp = $.otp || {};

$.otp.workflowConfig = {

  /**
   * Controller to provide the data
   */
  CONTROLLER: 'workflowConfig',

  /**
   * Search query fields
   */
  QUERY_FIELDS: [
    'workflows',
    'workflowVersions',
    'projects',
    'seqTypes',
    'referenceGenomes',
    'libraryPreparationKits'
  ],

  /**
   * Operations on the selector
   */
  OPERATION: {
    CREATE: 'create',
    UPDATE: 'update',
    VIEW: 'view',
    DEPRECATE: 'deprecate'
  },

  /**
   * Search matching types
   */
  MATCHING_TYPE: {
    EXACT: {
      tooltip: 'Exact Match: matching all attributes',
      icon: 'bullseye'
    },
    RELATED: {
      tooltip: 'Related Match: matching any of the given attributes',
      icon: 'circle'
    }
  },

  /**
   * Tooltips
   */
  TOOLTIPS: {
    createButton: 'Create a new selector based on the current selector',
    editButton: 'Edit the current selector',
    deprecateButton: 'Delete the current selector with its fragment being deprecated'
  },

  /**
   * Client side validation
   */
  validatePriority: (input) => {
    'use strict';

    const priority = input.value;
    input.setCustomValidity((Number.isNaN(priority) || parseInt(priority, 10) <= 0) ?
      `"${priority}" must be a positive integer.` : '');
  },

  validateName: (input) => {
    'use strict';

    const selectorList = $.map($('div a.dropdown-item'), (n) => n.text);
    input.setCustomValidity(selectorList.includes(input.value) ? 'Selector name must be unique' : '');
  },

  /**
   * Clear the query fields
   */
  clear: () => {
    'use strict';

    $.otp.workflowConfig.findAllQuerySelections().each((idx, queryField) => $(queryField).val(null).trigger('change'));
  },

  /**
   * Called after the OK button in the modal dialog is pressed,
   * which collects the modifications and send to the backend to either
   * create a new selector, update or deprecate the current selector
   */
  save: () => {
    'use strict';

    const form = $('#workflowConfigModalForm');
    const operation = form.find('input#pp-operation').val();

    if (operation === 'Create') {
      // remove the fragment.id and selector.id
      form.find('#pp-fragments').val('');
      form.find('#pp-id').val('');
    }

    // convert to string
    const data = form.serialize();

    // send modifying ajax operation (create, update or deprecate) to backend controller
    $.ajax({
      url: $.otp.createLink({
        controller: $.otp.workflowConfig.CONTROLLER,
        action: operation.toLowerCase()
      }),
      type: 'POST',
      data,
      success(response) {
        let newSelector = response;
        let op = operation.toLowerCase();
        const row = $.otp.workflowConfig.getDataTable().row(`#${response.id}`);
        response.exactMatch = false;
        if (op === $.otp.workflowConfig.OPERATION.UPDATE) {
          response.exactMatch = row.data().exactMatch;
          newSelector = JSON.parse(JSON.stringify(response)
            .replace(/"workflowVersion"/g, '"name"'));
          row.data(newSelector)
            .draw();
        } else if (op === $.otp.workflowConfig.OPERATION.DEPRECATE) {
          row.remove().draw();
        } else if (op === $.otp.workflowConfig.OPERATION.CREATE) {
          $.otp.workflowConfig.getDataTable().row.add(response).draw();
        }

        op = `${op}d`;
        $.otp.toaster.showSuccessToast('Workflow Config', `Selector ${newSelector.name} has been ${op} successfully`);

        // close the dialog
        $.otp.workflowConfig.getDialog().modal('hide');
      },
      error(err) {
        if (err && err.status && err.responseJSON) {
          $.otp.toaster.showErrorToast(
            `Failed with HTTP ${err.status}: ${err.responseJSON.error}`,
            err.responseJSON.message
          );
        } else {
          $.otp.toaster.showErrorToast(
            'Failed with operation on Workflow Config Selector',
            'Failed to create/update/deprecate a selector.'
          );
        }
      }
    }).always(() => {
    });
  },

  /**
   * By selecting a selector the search query fields can be prefilled.
   */
  prefill: (id) => {
    'use strict';

    $.otp.workflowConfig.findQuerySelection('type').val(null).trigger('change');
    // get all workflows from the selector id
    $.ajax({
      url: $.otp.createLink({
        controller: $.otp.workflowConfig.CONTROLLER,
        action: 'selector',
        id
      }),
      type: 'GET',
      success(response) {
        $.otp.workflowConfig.QUERY_FIELDS.forEach((f) => {
          const selectedOptions = response[f].map((option) => option.id);
          $.otp.workflowConfig.findQuerySelection(f).val(selectedOptions).trigger('change');
        });
      },
      error(err) {
        const message = err && err.responseJSON ? err.responseJSON.message : `Workflow selector ${id} cannot be found.`;
        $.otp.toaster.showErrorToast('Failed fetching Selector', message);
      }
    }).always(() => {
    });
  },

  /**
   * Global store for the selected options of search query.
   * E.g.: Workflows, SeqTypes etc.
   * which is used to fetch the selectors from backend
   */
  selectedOptions: {},

  tableInitialized: false,

  /**
   * Trigger the search by reload the Datatables
   * Search query must be updated first
   */
  search: () => {
    'use strict';

    // clear first
    $.otp.workflowConfig.selectedOptions = {};
    $.otp.workflowConfig.findAllQuerySelections().each((idx, queryField) => {
      $.otp.workflowConfig.selectedOptions[queryField.name] =
        $(queryField).select2('data').map((option) => option.id).join(',');
    });

    // reload the datatable by fetching data from backend
    $.otp.workflowConfig.getDataTable().ajax.reload();
  },

  initDialogWithData: (rowData, operation) => {
    'use strict';

    const thisDialog = $.otp.workflowConfig.getDialog();

    const capitalizedOperation = operation.replace(/^\w/, (c) => c.toUpperCase());
    thisDialog.find('.modal-title#workflowConfigModalLabel').text(`${capitalizedOperation} Workflow Configuration`);

    thisDialog.find('.modal-content #pp-operation').val(capitalizedOperation);
    thisDialog.find('.modal-content #pp-operation').attr('name', `_action_${operation}`);

    thisDialog.find('.modal-content #workflowConfigModalForm').removeClass('d-none');
    thisDialog.find('.modal-content #confirmContent').addClass('d-none');

    thisDialog.find('.modal-dialog').addClass('modal-xl');
    if (operation === $.otp.workflowConfig.OPERATION.DEPRECATE) {
      thisDialog.find('.modal-content #workflowConfigModalForm').addClass('d-none');
      thisDialog.find('.modal-content #confirmContent').removeClass('d-none');
      thisDialog.find('.modal-dialog').removeClass('modal-xl');
    } else if (operation === $.otp.workflowConfig.OPERATION.VIEW) {
      thisDialog.find('.modal-footer #save-button').addClass('d-none');
    }

    // set fields to be readonly in VIEW mode
    // selectorType == DEFAULT_VALUES are not editable
    thisDialog.find(':input:not(:button):not(select#pp-fragments)').attr(
      'disabled',
      !operation || operation === $.otp.workflowConfig.OPERATION.VIEW ||
      (rowData.selectorType === 'DEFAULT_VALUES' && operation !== $.otp.workflowConfig.OPERATION.CREATE)
    );
    // Priority is readonly field
    thisDialog.find(':input[name="priority"]').attr('disabled', true);

    // assign fields in the dialog page
    thisDialog.find('.modal-body input[name="selector.id"]').val(operation === $.otp.workflowConfig.OPERATION.CREATE ?
      -1 : rowData.id);
    thisDialog.find('.modal-body input[name="selectorName"]').val(rowData.name);
    thisDialog.find('.modal-body select[name="type"]').val(rowData.selectorType).trigger('change');
    // Priority is assigned to the value only in view mode
    thisDialog.find('.modal-body input[name="priority"]').val(
      !operation || operation === $.otp.workflowConfig.OPERATION.VIEW ? rowData.priority : null
    );

    // confirmation dialog of deprecation
    thisDialog.find('.modal-body #confirmContent .fragmentName').text(rowData.name);

    // assign selections to query fields
    $.otp.workflowConfig.QUERY_FIELDS.forEach((f) => {
      $(`.modal-body select[name="${f}"]`).val(rowData[f].map((obj) => obj.id)).trigger('change');
    });
  },

  /**
   * Fetch table data from backend by calling the data() method of controller
   */
  fetchData: (inputdata) => {
    'use strict';

    return new Promise((resolve) => {
      $.ajax({
        url: $.otp.createLink({
          controller: $.otp.workflowConfig.CONTROLLER,
          action: 'data'
        }),
        data: inputdata,
        success: (response) => (resolve({
          data: response.data
        }))
      });
    });
  },

  /**
   * Helper function to get the datatable
   */
  getDataTable: () => {
    'use strict';

    return $.fn.dataTable.tables({ api: true });
  },

  /**
   * Helper function to get the dialog
   */
  getDialog: () => {
    'use strict';

    return $('#workflowConfigModal');
  },

  /**
   * Helper function to return the query selection, which is
   * a select2 multiselect.
   *
   * @param name is the name attribute value of the select2 field
   */
  findQuerySelection: (name) => {
    'use strict';

    return $(`.search-query select.custom-select[name="${name}"]`);
  },

  /**
   * Helper function to return the correct query selections
   */
  findAllQuerySelections: () => {
    'use strict';

    return $('.search-query select.custom-select');
  },

  /**
   * Helper function to convert localDate to ISO string
   */
  localDateToString: (localDate) => {
    'use strict';

    return `${localDate.year}-${localDate.monthValue.toString()
      .padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}`;
  }
};

$(document).ready(() => {
  'use strict';

  /**
   * Datatables shown a list of selectors that match the search query.
   * Selectors contain exact matches and related matches. The exact matches are marked
   */
  const workflowConfigTable = $('#workflowConfigResult').DataTable({
    deferRender: true,
    processing: true,
    dom: '<"toolbar">frtip',
    columns: [
      { data: 'id' },
      { data: 'fid' },
      { data: 'exactMatch' },
      { data: 'name' },
      { data: 'selectorType' },
      { data: 'priority' },
      { data: 'workflows' },
      { data: 'workflowVersions' },
      { data: 'projects' },
      { data: 'seqTypes' },
      { data: 'referenceGenomes' },
      { data: 'libraryPreparationKits' },
      { data: '' }
    ],
    rowId: 'id',
    order: [[2, 'desc']],
    columnDefs: [
      {
        targets: [0, 1],
        visible: false
      },
      {
        // Matching type
        targets: [2],
        orderData: [2],
        render: (data, type) => {
          if (type === 'sort') {
            return data;
          }
          const matchingType = data ? $.otp.workflowConfig.MATCHING_TYPE.EXACT :
            $.otp.workflowConfig.MATCHING_TYPE.RELATED;
          return `<i class="bi bi-${matchingType.icon}" data-toogle="tooltip" title="${matchingType.tooltip}"></i>`;
        }
      },
      {
        // selector name
        targets: [3],
        orderData: [2, 3, 4],
        render: (data, type, row, meta) => `${row.exactMatch ? '<strong>' : ''}
            <a href="#" data-toggle="modal" data-target="#workflowConfigModal" data-rowidx="${meta.row}">${data}</a>
            ${row.exactMatch ? '</strong>' : ''}`
      },
      {
        targets: [4],
        // eslint-disable-next-line no-nested-ternary
        render: (data) => (typeof (data) === 'string' ? data : data ? data.name : '')
      },
      {
        targets: [6, 7, 8, 9, 10, 11],
        render: (data) => {
          if (!data) {
            return '';
          }
          return typeof (data) === 'string' ? data : data.map((item) => item.name).join(', ');
        }
      },
      {
        targets: [12],
        className: 'text-nowrap',
        data: null,
        orderable: false,
        render: (data, type, row, meta) => {
          const disabled = row.selectorType === 'DEFAULT_VALUES' ? 'disabled' : '';
          return `<button id="new-row" class="btn btn-sm btn-primary" type="button" data-toggle="modal" 
            data-target="#workflowConfigModal" data-rowidx="${meta.row}" data-toggle="tooltip" 
            title="${$.otp.workflowConfig.TOOLTIPS.createButton}" data-operation="create">
            <i class="bi bi-plus-square"></i></button>
            <button id="edit-row" class="btn btn-sm btn-primary" type="button" data-toggle="modal" 
            data-target="#workflowConfigModal" data-rowidx="${meta.row}" data-toggle="tooltip" 
            title="${$.otp.workflowConfig.TOOLTIPS.editButton}" data-operation="update" ${disabled}>
            <i class="bi bi-pencil"></i></button>
            <button id="deprecate-row" class="btn btn-sm btn-danger" type="button" data-toggle="modal"
            data-target="#workflowConfigModal" data-rowidx="${meta.row}" data-toggle="tooltip" 
            title="${$.otp.workflowConfig.TOOLTIPS.deprecateButton}" data-operation="deprecate" ${disabled}>
            <i class="bi bi-journal-code"></i></button>`;
        }
      }
    ],
    rowCallback: (row, data) => {
      if (data.exactMatch) {
        $(row).css('background-color', '#A5D3FF;');
      }
    },
    ajax: (inputdata, callback) => {
      // eslint-disable-next-line no-param-reassign
      inputdata.query = $.otp.workflowConfig.selectedOptions;
      if ($.otp.workflowConfig.tableInitialized) {
        $.otp.workflowConfig.fetchData(inputdata).then((outputdata) => {
          callback(outputdata);
        });
      } else {
        callback({
          data: []
        });
        $.otp.workflowConfig.tableInitialized = true;
      }
    }
  }).on('draw', () => {
    $('#search-button').prop('disabled', false);
    $('#search-button span').addClass('d-none');
  }).on('preXhr.dt', () => {
    $('#search-button').prop('disabled', true);
    $('#search-button span').removeClass('d-none');
  });

  /**
   * Initialize the dialog
   */
  $.otp.workflowConfig.getDialog().on('show.bs.modal', function (event) {
    // set the correct title operation: create|update|deprecate|view
    let operation = $(event.relatedTarget).data('operation');
    if (operation) {
      $(this).find('.modal-footer #save-button').removeClass('d-none');
    }
    operation = operation || $.otp.workflowConfig.OPERATION.VIEW;

    const rowIdx = $(event.relatedTarget).data('rowidx');
    const rowData = workflowConfigTable.row(rowIdx).data() || {};
    if (rowIdx === undefined) { // create a select without reference
      $.otp.workflowConfig.QUERY_FIELDS.forEach((f) => {
        rowData[f] = $.otp.workflowConfig.findQuerySelection(f).val().map((id) => ({ id }));
      });
      rowData.selectorType = null;

      rowData.id = -1;
      rowData.name = null;
      rowData.fid = null;
      rowData.priority = null;

      $('.modal-body textarea').val('');
      const oFragment = $('select#pp-fragments');
      oFragment.empty();
      oFragment.val(null).trigger('change');
    }

    $.otp.workflowConfig.initDialogWithData(rowData, operation);

    // don't fetch the fragment by creating selector
    if (rowData.id < 0) {
      return;
    }

    // load the fragment associated with the current selector
    // only the selector already exists
    $.ajax({
      url: $.otp.createLink({
        controller: 'workflowConfig',
        action: 'fragments',
        id: rowData.id
      }),
      type: 'GET',
      success(response) {
        const oFragment = $('select#pp-fragments');
        oFragment.empty();
        oFragment.val(null).trigger('change');
        response.forEach((item) => {
          // convert datetime ISOString to otp format
          const dateCreated = item.dateCreated.replace(/T.*$/, '');
          const optionVal = item.deprecationDate ?
            `deprecated (${dateCreated}...${$.otp.workflowConfig.localDateToString(
              item.deprecationDate
            )})` : `current (since ${dateCreated})`;
          const newOption = new Option(optionVal, item.id, false, false);
          // attach the extra data (configValues) to the option
          newOption.configValues = item.configValues;
          $('select#pp-fragments').append(newOption).trigger('change');
        });
        oFragment.val(response[0].id).trigger('change');
        oFragment.on('select2:select', (e) => {
          // eslint-disable-next-line prefer-destructuring
          const configValues = e.params.data.element.configValues;
          // configValues
          const formattedValue = JSON.stringify(JSON.parse(configValues), null, 2);
          $('.modal-body textarea').val(formattedValue);
        });

        // workaround since the manual event triggering of select2 is not working
        const formattedValue = JSON.stringify(JSON.parse(response[0].configValues), null, 2);
        $('.modal-body textarea').val(formattedValue);
      },
      error(err) {
        const message = (err && err.responseJSON) ? err.responseJSON.message : 'Failed fetching Selector\'s fragments.';
        $.otp.toaster.showErrorToast('Failed fetching Selector\'s fragments', message);
      }
    });
  });

  /**
   * Format the JSON code in config value field
   */
  $('button.format').on('click', (e) => {
    e.preventDefault();
    const value = $('.modal-body textarea');
    try {
      value.val(JSON.stringify(JSON.parse(value.val()), null, 2));
    } catch (err) {
      $.otp.toaster.showErrorToast('Syntax Error in JSON', err);
    }
  });
});
