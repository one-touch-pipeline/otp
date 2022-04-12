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
    inputdata.seqTracks = $.otp.triggerAlignment.getDataTable().column(0).data().toArray();

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
    $.otp.triggerAlignment.getDataTable().ajax.reload();
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
        data: response.data
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
  getDataTable: () => $.fn.dataTable.tables({ api: true })
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
    scrollY: '200px',
    scrollCollapse: true,
    paging: false,
    columns: [
      { data: 'id', width: '3em' },
      { data: 'project' },
      { data: 'individual' },
      { data: 'sampleType' },
      { data: 'seqType' },
      { data: 'lane' },
      { data: 'run' },
      { data: 'ilseId' },
      { data: 'withdrawn', width: '5em' },
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
          callback(outputdata);
          if (!outputdata.data.length) {
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
});
