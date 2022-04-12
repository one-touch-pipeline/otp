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
    'ilse-tab': 'searchSeqTrackByIlseNumber'
  },

  TOAST_TITLE: {
    TRIGGER_SUCCESS: 'Alignment workflows triggered',
    TRIGGER_WARNING: 'No alignment workflows triggered',
    TRIGGER_FAILED: 'Failed triggering alignment workflows',
    TRIGGER_CANNOT: 'Cannot trigger alignment workflow',

    SEARCH_INFO: 'Some inputs seem to be wrong',
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
      withdraw: false,
      seqTracks: []
    };
    inputdata.withdraw = $('#withdrawBamFiles').prop('checked');
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
    dom: '<"toolbar">frtip',
    scrollY: '500px',
    scrollCollapse: true,
    paging: false,
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

          // withdrawnData
          if (outputdata.data.warnings.withdrawnSeqTracks.length) {
            $('#withdrawnSeqTracksWarningsCard').removeClass('d-none');
            $.otp.triggerAlignment.getWithdrawnWarningsTable().clear().rows.add(
              outputdata.data.warnings.withdrawnSeqTracks.map((o) => [
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
          if (outputdata.data.warnings.missingAlignmentConfigs.length) {
            $('#missingAlignmentConfigWarningsCard').removeClass('d-none');
            $.otp.triggerAlignment.getMissingAlignmentConfigsWarningsTable().clear().rows.add(
              outputdata.data.warnings.missingAlignmentConfigs.map((o) => [o.project, o.seqType, o.count])
            ).draw();
          } else {
            $('#missingAlignmentConfigWarningsCard').addClass('d-none');
            $.otp.triggerAlignment.getMissingAlignmentConfigsWarningsTable().clear().draw();
          }

          // reference genome config missing
          if (outputdata.data.warnings.missingReferenceGenomes.length) {
            $('#missingReferenceGenomeWarningsCard').removeClass('d-none');
            $.otp.triggerAlignment.getReferenceGenomeWarningsTable().clear().rows.add(
              outputdata.data.warnings.missingReferenceGenomes.map((o) => [
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
          if (outputdata.data.warnings.seqPlatformGroups.length) {
            $('#seqPlatformWarningsCard').removeClass('d-none');
            $.otp.triggerAlignment.getSeqPlatformWarningsTable().clear().rows.add(
              outputdata.data.warnings.seqPlatformGroups.map((o) => [
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
          if (outputdata.data.warnings.libraryPreparationKits.length) {
            $('#libraryPrepKitWarningsCard').removeClass('d-none');
            $.otp.triggerAlignment.getLibPrepKitWarningsTable().clear().rows.add(
              outputdata.data.warnings.libraryPreparationKits.map((o) => [
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
          if (!!outputdata.data.warnings.message) {
            $.otp.toaster.showErrorToast(
              $.otp.triggerAlignment.TOAST_TITLE.SEARCH_INFO,
              outputdata.data.warnings.message
            );
          }

          if (!outputdata.data.data.length) {
            $.otp.toaster.showErrorToast(
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

  $('#withdrawnWarnings').DataTable({
    dom: '<"toolbar">frtip',
    scrollCollapse: true,
    paging: false
  });

  $('#missingAlignmentConfigsWarnings').DataTable({
    dom: '<"toolbar">frtip',
    scrollCollapse: true,
    paging: false
  });

  $('#missingReferenceGenomeWarnings').DataTable({
    dom: '<"toolbar">frtip',
    scrollCollapse: true,
    paging: false
  });

  $('#seqPlatformWarnings').DataTable({
    dom: '<"toolbar">frtip',
    scrollCollapse: true,
    paging: false
  });

  $('#libraryPrepKitWarnings').DataTable({
    dom: '<"toolbar">frtip',
    scrollCollapse: true,
    paging: false
  });
});
