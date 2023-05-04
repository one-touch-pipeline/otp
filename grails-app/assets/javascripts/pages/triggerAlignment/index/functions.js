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

$.otp.triggerAlignment = {
  /*
   * Controllers to provide the endpoint for fetching data
   */
  CONTROLLER: 'triggerAlignment',
  TRIGGER_ACTION: 'triggerAlignment',

  SEARCH_CONTROLLER: 'searchSeqTrack',
  SEARCH_ACTION: {
    'project-tab': 'searchSeqTrackByProjectSeqType',
    'pid-tab': 'searchSeqTrackByPidSeqType',
    'lane-tab': 'searchSeqTrackByLaneId',
    'ilse-tab': 'searchSeqTrackByIlseNumber',
    'multi-input-tab': 'searchSeqTrackByMultiInput'
  },

  TOAST_TITLE: {
    TRIGGER_SUCCESS: 'Alignment workflows triggered',
    TRIGGER_WARNING: 'No alignment workflows triggered',
    TRIGGER_FAILED: 'Failed triggering alignment workflows',
    TRIGGER_CANNOT: 'Cannot trigger alignment workflow',

    SEARCH_INFO: 'Could not find any SeqTracks for the following input',
    SEARCH_WARNING: 'No SeqTracks found',
    SEARCH_ERROR: 'Failed to search for SeqTrack'
  },

  tableInitialized: false,
  searchQuery: {},

  /**
   * Trigger the alignment workflow
   */
  trigger: () => {
    const inputdata = {
      withdrawBamFiles: false,
      seqTracks: []
    };
    inputdata.ignoreSeqPlatformGroup = $('#ignoreSeqPlatformGroup').prop('checked');
    inputdata.withdrawBamFiles = $('input[name="withdrawBamFiles"]:checked').val();
    inputdata.seqTracks = $.otp.triggerAlignment.getSeqTrackTable().column(0).data().toArray();

    if (inputdata.seqTracks.length) {
      $('#triggerAlignmentButton').prop('disabled', true);
      $.ajax({
        url: $.otp.createLink({
          controller: $.otp.triggerAlignment.CONTROLLER,
          action: $.otp.triggerAlignment.TRIGGER_ACTION
        }),
        type: 'POST',
        data: inputdata,
        success: (response) => {
          $('#triggerAlignmentButton').prop('disabled', false);
          // clear content
          const workPackageList = $('#workPackageList');
          workPackageList.empty();
          if (response && Array.isArray(response) && response.length) {
            response.forEach((workPackage) => {
              workPackageList.append(`<li>${workPackage}</li>`);
            });
            $.otp.toaster.showSuccessToast(
              $.otp.triggerAlignment.TOAST_TITLE.TRIGGER_SUCCESS,
              `${response.length} Alignment workflows have been started successfully. ` +
              'Refer to result section for details.'
            );
          } else {
            $.otp.toaster.showWarningToast(
              $.otp.triggerAlignment.TOAST_TITLE.TRIGGER_WARNING,
              'No alignment workflow has been started. Make sure the workflows are configured correctly.'
            );
          }
        },
        error: (err) => {
          $('#triggerAlignmentButton').prop('disabled', false);
          if (err && err.status && err.responseJSON) {
            $.otp.toaster.showErrorToast(`${err.responseJSON.error}`, err.responseJSON.message);
          } else {
            $.otp.toaster.showErrorToast(
              $.otp.triggerAlignment.TOAST_TITLE.TRIGGER_FAILED,
              'Internal error occurred in the backend.'
            );
          }
        }
      });
    } else {
      $.otp.toaster.showErrorToast(
        $.otp.triggerAlignment.TOAST_TITLE.TRIGGER_CANNOT,
        'No SeqTracks are available. Search for SeqTracks before triggering alignment workflows.'
      );
    }
  },

  /**
   * Start to search for seqTracks by reload the Datatables
   * Search query must be updated first
   */
  search: () => {
    // clear first the query for backend call input
    $.otp.triggerAlignment.searchQuery = {};
    // fetch the active search tab
    $.otp.triggerAlignment.searchQuery.type = $('#myTab .nav-link.active').attr('id');
    $.otp.triggerAlignment.searchQuery.redirect = {
      controller: 'TriggerAlignment',
      action: 'generateWarnings'
    };

    // eslint-disable-next-line default-case
    switch ($.otp.triggerAlignment.searchQuery.type) {
      case 'project-tab':
        $.otp.triggerAlignment.searchQuery.seqTypes = $('#seqTypeProject').select2('data')
          .map((option) => option.id);
        if (!$.otp.triggerAlignment.searchQuery.seqTypes.length) {
          $.otp.toaster.showErrorToast(
            $.otp.triggerAlignment.TOAST_TITLE.SEARCH_ERROR,
            'At least one SeqType must be selected'
          );
          return;
        }
        break;
      case 'pid-tab':
        $.otp.triggerAlignment.searchQuery.pids = $.otp.parseDelimitedStringToArray($('#pid-selection').val());
        if (!$.otp.triggerAlignment.searchQuery.pids.length) {
          $.otp.toaster.showErrorToast(
            $.otp.triggerAlignment.TOAST_TITLE.SEARCH_ERROR,
            'At least one PID must be supplied'
          );
          return;
        }
        $.otp.triggerAlignment.searchQuery.seqTypes = $('#seqTypePid').select2('data')
          .map((option) => option.id);
        if (!$.otp.triggerAlignment.searchQuery.seqTypes.length) {
          $.otp.toaster.showErrorToast(
            $.otp.triggerAlignment.TOAST_TITLE.SEARCH_ERROR,
            'At least one SeqType must be selected'
          );
          return;
        }
        break;
      case 'lane-tab':
        $.otp.triggerAlignment.searchQuery.lanes = $.otp.parseDelimitedStringToArray($('#lane-selection').val());
        if (!$.otp.triggerAlignment.searchQuery.lanes.length) {
          $.otp.toaster.showErrorToast(
            $.otp.triggerAlignment.TOAST_TITLE.SEARCH_ERROR,
            'At least one Land ID must be supplied'
          );
          return;
        }
        break;
      case 'ilse-tab':
        $.otp.triggerAlignment.searchQuery.ilseNumbers = $.otp.parseDelimitedStringToArray($('#ilse-selection').val());
        if (!$.otp.triggerAlignment.searchQuery.ilseNumbers.length) {
          $.otp.toaster.showErrorToast(
            $.otp.triggerAlignment.TOAST_TITLE.SEARCH_ERROR,
            'At least one ilse Number must be supplied'
          );
          return;
        }
        break;
      case 'multi-input-tab': {
        const multiInputList = $.otp.parseDelimitedMultiLineStringToArrays($('#multi-input-selection').val());
        if (!multiInputList.length) {
          $.otp.toaster.showErrorToast(
            $.otp.triggerAlignment.TOAST_TITLE.SEARCH_ERROR,
            'At least one multi input must be supplied'
          );
          return;
        }
        $.otp.triggerAlignment.searchQuery.pids = multiInputList.map((multiInput) => multiInput[0] || null);
        $.otp.triggerAlignment.searchQuery.sampleTypes = multiInputList.map((multiInput) => multiInput[1] || null);
        $.otp.triggerAlignment.searchQuery.seqTypes = multiInputList.map((multiInput) => multiInput[2] || null);
        $.otp.triggerAlignment.searchQuery.readTypes = multiInputList.map((multiInput) => multiInput[3] || null);
        $.otp.triggerAlignment.searchQuery.singleCells = multiInputList.map((multiInput) => multiInput[4] || null);
        if (!($.otp.triggerAlignment.searchQuery.singleCells
          .every((singleCell) => ['true', 'false'].includes(singleCell)))) {
          $.otp.toaster.showErrorToast(
            $.otp.triggerAlignment.TOAST_TITLE.SEARCH_ERROR,
            'The single cell argument must be a boolean'
          );
          return;
        }
        break;
      }
    }
    // reload the datatable by fetching data from backend
    $.otp.triggerAlignment.getSeqTrackTable().ajax.reload();
  },

  /**
   * Fetch table data from backend by calling the corresponding method of the search controller
   */
  fetchData: (inputdata) => new Promise((resolve) => {
    $.ajax({
      url: $.otp.createLink({
        controller: $.otp.triggerAlignment.SEARCH_CONTROLLER,
        action: $.otp.triggerAlignment.SEARCH_ACTION[inputdata.type]
      }),
      method: 'GET',
      data: inputdata,
      success: (response) => (resolve({
        data: response
      })),
      error: (err) => {
        $('#searchSeqTrackButton').prop('disabled', false);
        if (err && err.status && err.responseJSON) {
          $.otp.toaster.showErrorToast(
            $.otp.triggerAlignment.TOAST_TITLE.SEARCH_ERROR,
            err.responseJSON.message
          );
        } else {
          $.otp.toaster.showErrorToast(
            $.otp.triggerAlignment.TOAST_TITLE.SEARCH_ERROR,
            'Internal error occurred in the backend.'
          );
        }
      }
    });
  }),

  /**
   * Helper function to get the datatable
   */
  getWithdrawnWarningsTable: () => $('#withdrawnWarnings').DataTable(),

  getMissingAlignmentConfigsWarningsTable: () => $('#missingAlignmentConfigsWarnings').DataTable(),

  getReferenceGenomeWarningsTable: () => $('#missingReferenceGenomeWarnings').DataTable(),

  getSeqPlatformWarningsTable: () => $('#seqPlatformWarnings').DataTable(),

  getLibPrepKitWarningsTable: () => $('#libraryPrepKitWarnings').DataTable(),

  getSeqTrackTable: () => $('#seqTrackTable').DataTable()
};

$(document).ready(() => {
  'use strict';

  /**
   * Initialization of a DataTable for SeqTrack table in TriggerAlignment page
   */
  $('#seqTrackTable').DataTable({
    deferRender: true,
    processing: true,
    dom: 'B<"toolbar">frtip',
    scrollY: '500px',
    scrollCollapse: true,
    paging: false,
    buttons: ['csv'],
    columns: [
      {
        data: 'id',
        width: '3em'
      },
      { data: 'project' },
      { data: 'individual' },
      { data: 'sampleType' },
      { data: 'seqType' },
      { data: 'lane' },
      { data: 'run' },
      { data: 'ilseId' },
      {
        data: 'withdrawn',
        width: '5em'
      },
      { data: 'libPrepKit' },
      { data: 'seqPlatform' },
      { data: 'seqPlatformGroup' }
    ],
    columnDefs: [{
      defaultContent: '',
      targets: '_all'
    }],
    ajax: (inputdata, callback) => {
      if ($.otp.triggerAlignment.tableInitialized) {
        // eslint-disable-next-line no-param-reassign
        inputdata = $.otp.triggerAlignment.searchQuery;
        $.otp.triggerAlignment.fetchData(inputdata).then((outputdata) => {
          callback(outputdata.data);

          const { warnings } = outputdata.data;

          // withdrawnData
          if (warnings.withdrawnSeqTracks && warnings.withdrawnSeqTracks.length) {
            $('#withdrawnSeqTracksWarningsCard').removeClass('d-none');
            $.otp.triggerAlignment.getWithdrawnWarningsTable().clear().rows.add(
              warnings.withdrawnSeqTracks.map((o) => [
                o.project,
                o.individual,
                o.seqType,
                o.sampleType,
                o.count
              ])
            ).draw();
          } else {
            $('#withdrawnSeqTracksWarningsCard').addClass('d-none');
            $.otp.triggerAlignment.getWithdrawnWarningsTable().clear().draw();
          }

          // alignment config missing
          if (warnings.missingAlignmentConfigs && warnings.missingAlignmentConfigs.length) {
            $('#missingAlignmentConfigWarningsCard').removeClass('d-none');
            $.otp.triggerAlignment.getMissingAlignmentConfigsWarningsTable().clear().rows.add(
              warnings.missingAlignmentConfigs.map((o) => [o.project, o.seqType, o.count])
            ).draw();
          } else {
            $('#missingAlignmentConfigWarningsCard').addClass('d-none');
            $.otp.triggerAlignment.getMissingAlignmentConfigsWarningsTable().clear().draw();
          }

          // reference genome config missing
          if (warnings.missingReferenceGenomes && warnings.missingReferenceGenomes.length) {
            $('#missingReferenceGenomeWarningsCard').removeClass('d-none');
            $.otp.triggerAlignment.getReferenceGenomeWarningsTable().clear().rows.add(
              warnings.missingReferenceGenomes.map((o) => [
                o.project,
                o.seqType,
                o.species,
                o.count
              ])
            ).draw();
          } else {
            $('#missingReferenceGenomeWarningsCard').addClass('d-none');
            $.otp.triggerAlignment.getReferenceGenomeWarningsTable().clear().draw();
          }

          // seqPlatformGroup missmatch
          if (warnings.seqPlatformGroups && warnings.seqPlatformGroups.length) {
            $('#seqPlatformWarningsCard').removeClass('d-none');
            $.otp.triggerAlignment.getSeqPlatformWarningsTable().clear().rows.add(
              warnings.seqPlatformGroups.map((o) => [
                o.project,
                o.individual,
                o.seqType,
                o.sampleType,
                `
                  <table class="table table-bordered">
                    <thead>
                      <tr>
                          <th>Seq. Platform Group Id</th>
                          <th>Count</th>
                          <th>Containing Seq. Platforms</th>
                      </tr>
                    </thead>
                    <tbody>
                    ${o.seqPlatformGroupTable.map((subTableElement) => [`
                        <tr>
                            <td>${subTableElement.seqPlatformGroupId}</td>
                            <td>${subTableElement.count}</td>
                            <td>
                              <ul style="list-style-type: disc; padding-left: 20px;">
                                ${subTableElement.seqPlatforms.map((listElement) => [`
                                  <li>${listElement}</li>
                                `]).join('')}
                              </ul>
                          </td>
                        </tr>
                      `]).join('')}
                    </tbody>
                  </table>
                `
              ])
            ).draw();
          } else {
            $('#seqPlatformWarningsCard').addClass('d-none');
            $.otp.triggerAlignment.getSeqPlatformWarningsTable().clear().draw();
          }

          // library preparation kit missmatch
          if (warnings.libraryPreparationKits && warnings.libraryPreparationKits.length) {
            $('#libraryPrepKitWarningsCard').removeClass('d-none');
            $.otp.triggerAlignment.getLibPrepKitWarningsTable().clear().rows.add(
              warnings.libraryPreparationKits.map((o) => [
                o.project,
                o.individual,
                o.seqType,
                o.sampleType,
                `
                  <table class="table table-bordered">
                    <thead>
                      <tr>
                          <th>Library Preperation Kit</th>
                          <th>Count</th>
                      </tr>
                    </thead>
                    <tbody>
                      ${o.libraryPreparationKitTable.map((subTableElement) => [`
                        <tr>
                          <td>${subTableElement.libraryPreparationKit}</td>
                          <td>${subTableElement.count}</td>
                        </tr>
                      `]).join('')}
                    </tbody>
                  </table>
                `
              ])
            ).draw();
          } else {
            $('#libraryPrepKitWarningsCard').addClass('d-none');
            $.otp.triggerAlignment.getLibPrepKitWarningsTable().clear().draw();
          }

          // message
          // eslint-disable-next-line no-extra-boolean-cast
          if (!!outputdata.data.message) {
            $.otp.toaster.showErrorToast(
              $.otp.triggerAlignment.TOAST_TITLE.SEARCH_INFO,
              outputdata.data.message
            );
          }

          if (!outputdata.data.data || !outputdata.data.data.length) {
            $.otp.toaster.showWarningToast(
              $.otp.triggerAlignment.TOAST_TITLE.SEARCH_WARNING,
              'No SeqTracks can be found. Make sure the search inputs are correct'
            );
          }
        });
      } else {
        callback({ data: [] });
        $.otp.triggerAlignment.tableInitialized = true;
      }
    }
  }).on('draw', () => {
    $('#searchSeqTrackButton').prop('disabled', false);
  }).on('preXhr.dt', () => {
    $('#searchSeqTrackButton').prop('disabled', true);
  });

  /** Move description text to toolbar */
  $('div.toolbar').html($('#seqTrackTableText'));

  $('#withdrawnWarnings').DataTable({
    dom: 'B<"toolbar">frtip',
    buttons: ['csv'],
    scrollCollapse: true,
    paging: false
  });

  $('#missingAlignmentConfigsWarnings').DataTable({
    dom: 'B<"toolbar">frtip',
    buttons: ['csv'],
    scrollCollapse: true,
    paging: false
  });

  $('#missingReferenceGenomeWarnings').DataTable({
    dom: 'B<"toolbar">frtip',
    buttons: ['csv'],
    scrollCollapse: true,
    paging: false
  });

  $('#seqPlatformWarnings').DataTable({
    dom: 'B<"toolbar">frtip',
    buttons: ['csv'],
    scrollCollapse: true,
    paging: false
  });

  $('#libraryPrepKitWarnings').DataTable({
    dom: 'B<"toolbar">frtip',
    buttons: ['csv'],
    scrollCollapse: true,
    paging: false
  });
});
