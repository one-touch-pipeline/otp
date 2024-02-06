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

$(() => {
  'use strict';

  $('input:checkbox#enableAutoExec').on('change', (e) => {
    if (e.target.checked) {
      $('.automaticSettings').show();
    } else {
      $('.automaticSettings').hide();
    }
  }).trigger('change');

  $('input:radio[name=expectedOrEnforcedCells]').on('change', (e) => {
    const target = $(e.target);
    const container = target.closest('div').parent();
    const value = container.find('input:radio[name=expectedOrEnforcedCells]:checked').val();
    const expected = container.find('input[name=expectedCellsValue]');
    const enforced = container.find('input[name=enforcedCellsValue]');

    expected.prop('disabled', true);
    enforced.prop('disabled', true);
    if (value === 'expected') {
      expected.prop('disabled', false);
    } else if (value === 'enforced') {
      enforced.prop('disabled', false);
    }
  }).trigger('change');

  let backendCall;
  // Flag to control if all samples are selected
  let selectAllRows = true;
  initialNewBackendCall();

  // Initializing Data Tables
  const sampleTable = $('table#sampleTable').DataTable({
    columns: [
      {
        data() {
          // all checkboxes are checked initially, which means all samples are selected
          return selectAllRows;
        },
        orderable: false
      },
      {
        data: 'individual',
        orderable: true
      },
      {
        data: 'sampleType',
        orderable: true
      },
      {
        data: 'seqType',
        orderable: true
      }
    ],
    columnDefs: [
      {
        target: 0,
        render: (data, type, row) => {
          if (type === 'sort') {
            return null;
          }
          const isChecked = data ? 'checked' : '';
          return `<input class="tableCheckbox" type="checkbox" name="samples" value="${row.sampleId}" ${isChecked}>`;
        }
      }
    ],
    processing: true,
    ordering: true,
    paging: true,
    pageLength: 50,
    scrollCollapse: true,
    ajax: (data, callback) => {
      $.when(backendCall).done((sampleAndMwpData) => {
        callback({ data: sampleAndMwpData.samples });
      });
    }
  });

  const mwpTable = $('table#mwpTable').DataTable({
    columns: [
      {
        data: 'individual',
        orderable: true
      },
      {
        data: 'sampleType',
        orderable: true
      },
      {
        data: 'seqType',
        orderable: true
      },
      {
        data: 'config',
        orderable: true
      },
      {
        data: 'referenceGenome',
        orderable: true
      },
      {
        data: 'referenceGenomeIndex',
        orderable: true
      },
      {
        data: 'expectedCells',
        orderable: true
      },
      {
        data: 'enforcedCells',
        orderable: true
      },
      {
        data: 'bamFileStatus',
        orderable: true
      }
    ],
    processing: true,
    ordering: true,
    paging: true,
    pageLength: 50,
    scrollCollapse: true,
    ajax: (data, callback) => {
      $.when(backendCall).done((sampleAndMwpData) => {
        callback({ data: sampleAndMwpData.mwps });
      });
    }
  });

  function initialNewBackendCall() {
    if (backendCall && !backendCall.status) {
      backendCall.abort();
    }
    backendCall = getSamplesAndMwpFromBackend();
  }

  function getSamplesAndMwpFromBackend() {
    return $.ajax({
      url: $.otp.createLink({
        controller: 'cellRangerConfiguration',
        action: 'getIndividualsAndSampleTypesBySeqType'
      }),
      type: 'POST',
      data: new FormData($('#sampleForm')[0]),
      success(json) {
        return json;
      },
      processData: false,
      contentType: false
    });
  }

  function setQueryParam(key, value) {
    // update browser link with params
    const url = new URL(window.location.href);

    url.searchParams.set(key, value);
    const updatedUrl = url.href;
    window.history.pushState({ path: updatedUrl }, '', updatedUrl);
  }

  function removeQueryParam(key) {
    const url = new URL(window.location.href);

    url.searchParams.delete(key);
    const updatedUrl = url.href;
    window.history.pushState({ path: updatedUrl }, '', updatedUrl);
  }

  $('#selectAll').on('click', (e) => {
    const { checked } = e.target;

    // rerender the whole table
    selectAllRows = checked;
    sampleTable.rows().invalidate();
  });

  $('#executeButton').on('click', (e) => {
    const formData = new FormData($('#sampleForm')[0]);
    sampleTable.$('input').serializeArray().forEach((tableEntry) => formData.append(tableEntry.name, tableEntry.value));

    $('#executeSpinner').removeAttr('hidden');
    $('#executeButton').attr('disabled', 'true');

    $.ajax({
      url: $.otp.createLink({
        controller: 'CellRangerConfiguration',
        action: 'createMwp'
      }),
      type: 'POST',
      data: formData,
      success(json) {
        $.otp.toaster.showSuccessToast(
          'Update successful',
          'Cell Ranger Merging Work Package successfully created.'
        );
        initialNewBackendCall();

        mwpTable.ajax.reload();
        return json;
      },
      error(error) {
        const errorMessage = error.responseJSON.message;
        $.otp.toaster.showErrorToast('Cell Ranger Merging Work Package creation failed', errorMessage);
      },
      complete() {
        $('#executeSpinner').attr('hidden', 'true');
        $('#executeButton').removeAttr('disabled');
      },
      processData: false,
      contentType: false
    });

    e.preventDefault();
  });

  $('#individualSelect').on('change', (e) => {
    const data = $(e.target).select2('data').map((entry) => entry.id);
    setQueryParam('individual.id', data);
  });

  $('#sampleTypeSelect').on('change', (e) => {
    const data = $(e.target).select2('data').map((entry) => entry.id);
    setQueryParam('sampleType.id', data);
  });

  $('#referenceGenomeSelect').on('change', (e) => {
    const data = $(e.target).select2('data').map((entry) => entry.id);
    setQueryParam('reference.id', data);
  });

  function initializeSelect2WithQueryParams() {
    const queryParams = new URL(window.location.href).searchParams;
    const sampleTypeData = queryParams.getAll('sampleTypes')
      .concat(queryParams.has('sampleType.id') ? queryParams.get('sampleType.id').split(',') : []);
    const individualData = queryParams.getAll('individuals')
      .concat(queryParams.has('individual.id') ? queryParams.get('individual.id').split(',') : []);
    setQueryParam('sampleType.id', sampleTypeData);
    setQueryParam('individual.id', individualData);
    removeQueryParam('sampleTypes');
    removeQueryParam('individuals');

    $('#sampleTypeSelect').val(sampleTypeData).trigger('change');
    $('#individualSelect').val(individualData).trigger('change');
    reloadTables();
  }

  initializeSelect2WithQueryParams();

  function reloadTables() {
    initialNewBackendCall();

    sampleTable.ajax.reload();
    mwpTable.ajax.reload();
  }

  $('#individualSelect, #sampleTypeSelect').on('change', () => {
    reloadTables();
  });
});
