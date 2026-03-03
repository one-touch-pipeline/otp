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

$(() => {
  'use strict';

  $.otp = $.otp || {};

  $.otp.triggerWorkflows = {
    /*
     * Controllers to provide the endpoint for fetching data
     */
    CONTROLLER: 'triggerWorkflows',
    TRIGGER_ACTION: 'triggerWorkflows',

    SEARCH_CONTROLLER: 'searchSeqTrack',
    SEARCH_ACTION: {
      'project-tab': 'searchSeqTrackByProjectSeqType',
      'pid-tab': 'searchSeqTrackByPidSeqType',
      'seqtrack-id-tab': 'searchSeqTrackBySeqTrackId',
      'ilse-tab': 'searchSeqTrackByIlseNumber',
      'bam-tab': 'searchSeqTrackByBamId',
      'multi-input-tab': 'searchSeqTrackByMultiInput'
    },

    TOAST_TITLE: {
      TRIGGER_SUCCESS: 'Workflows triggered',
      TRIGGER_WARNING: 'No workflows triggered',
      TRIGGER_FAILED: 'Failed triggering workflows',
      TRIGGER_CANNOT: 'Cannot trigger workflow',

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
        seqTracks: [],
        bamFiles: [],
        deciderActions: []
      };
      inputdata.ignoreSeqPlatformGroup = $('#ignoreSeqPlatformGroup').prop('checked');
      inputdata.withdrawBamFiles = $('input[name="withdrawBamFiles"]:checked').val();
      inputdata.seqTracks = $.otp.triggerWorkflows.getSeqTrackTable().column(0).data().toArray();
      inputdata.bamFiles = $.otp.triggerWorkflows.getBamTable().column(0).data().toArray();

      // prepare the selected parameters of deciders for the backend call
      $('#deciderActionSelection select.form-control').each((idx, elm) => {
        inputdata.deciderActions.push(JSON.stringify({
          name: $(elm).attr('name'),
          createAction: $($(elm).select2('data')).attr('id')
        }));
      });

      if (inputdata.seqTracks.length || inputdata.bamFiles.length) {
        $('#triggerWorkflowsButton').prop('disabled', true);
        $.ajax({
          url: $.otp.createLink({
            controller: $.otp.triggerWorkflows.CONTROLLER,
            action: $.otp.triggerWorkflows.TRIGGER_ACTION
          }),
          type: 'POST',
          data: inputdata,
          success: (response) => {
            $('#triggerWorkflowsButton').prop('disabled', false);
            // clear content
            const resultInfo = $('#resultInfo');
            const resultWarning = $('#resultWarning');
            const resultWorkPackageList = $('#resultWorkPackageList');
            resultInfo.empty();
            resultWarning.empty();
            resultWorkPackageList.empty();
            if (response) {
              response.infos.forEach((info) => {
                resultInfo.append(`<li>${info}</li>`);
              });
              response.warnings.forEach((warning) => {
                resultWarning.append(`<li>${warning}</li>`);
              });
              if (response.warnings.length === 0) {
                resultWarning.append('<li>none</li>');
              }
              response.newWorkPackages.forEach((newWorkPackage) => {
                resultWorkPackageList.append(`<li>${newWorkPackage}</li>`);
              });
              if (response.newWorkPackages.length) {
                $.otp.toaster.showSuccessToast(
                  $.otp.triggerWorkflows.TOAST_TITLE.TRIGGER_SUCCESS,
                  `${response.newWorkPackages.length} Workflows have been started successfully. ` +
                  'Refer to result section for details.'
                );
              } else {
                resultWorkPackageList.append('<li>none</li>');
                $.otp.toaster.showWarningToast(
                  $.otp.triggerWorkflows.TOAST_TITLE.TRIGGER_WARNING,
                  'No workflows were started. Refer to result section for details.'
                );
              }
            } else {
              $.otp.toaster.showWarningToast(
                $.otp.triggerWorkflows.TOAST_TITLE.TRIGGER_WARNING,
                'Something went wrong during triggering.'
              );
            }
          },
          error: (err) => {
            $('#triggerWorkflowsButton').prop('disabled', false);
            if (err && err.status && err.responseJSON) {
              $.otp.toaster.showErrorToast(`${err.responseJSON.error}`, err.responseJSON.message);
            } else {
              $.otp.toaster.showErrorToast(
                $.otp.triggerWorkflows.TOAST_TITLE.TRIGGER_FAILED,
                'Internal error occurred in the backend.'
              );
            }
          }
        });
      } else {
        $.otp.toaster.showErrorToast(
          $.otp.triggerWorkflows.TOAST_TITLE.TRIGGER_CANNOT,
          'No SeqTracks are available. Search for SeqTracks before triggering workflows.'
        );
      }
    },

    /**
     * Start to search for seqTracks by reload the Datatables
     * Search query must be updated first
     */
    search: () => {
      // clear first the query for backend call input
      $.otp.triggerWorkflows.searchQuery = {};
      // fetch the active search tab
      $.otp.triggerWorkflows.searchQuery.type = $('#myTab .nav-link.active').attr('id');
      $.otp.triggerWorkflows.searchQuery.redirect = {
        controller: 'TriggerWorkflows',
        action: 'generateWarnings'
      };

      // eslint-disable-next-line default-case
      switch ($.otp.triggerWorkflows.searchQuery.type) {
        case 'project-tab':
          $.otp.triggerWorkflows.searchQuery.seqTypes = $('#seqTypeProject').select2('data')
            .map((option) => option.id);
          if (!$.otp.triggerWorkflows.searchQuery.seqTypes.length) {
            $.otp.toaster.showErrorToast(
              $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_ERROR,
              'At least one SeqType must be selected'
            );
            return;
          }
          break;
        case 'pid-tab':
          $.otp.triggerWorkflows.searchQuery.pids = $.otp.parseDelimitedStringToArray($('#pid-selection').val());
          if (!$.otp.triggerWorkflows.searchQuery.pids.length) {
            $.otp.toaster.showErrorToast(
              $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_ERROR,
              'At least one PID must be supplied'
            );
            return;
          }
          $.otp.triggerWorkflows.searchQuery.seqTypes = $('#seqTypePid').select2('data')
            .map((option) => option.id);
          if (!$.otp.triggerWorkflows.searchQuery.seqTypes.length) {
            $.otp.toaster.showErrorToast(
              $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_ERROR,
              'At least one SeqType must be selected'
            );
            return;
          }
          break;
        case 'seqtrack-id-tab':
          $.otp.triggerWorkflows.searchQuery.seqtrackIds =
            $.otp.parseDelimitedStringToArray($('#seqTrackId-selection').val());
          if (!$.otp.triggerWorkflows.searchQuery.seqtrackIds.length) {
            $.otp.toaster.showErrorToast(
              $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_ERROR,
              'At least one seqTrack ID must be supplied'
            );
            return;
          }
          break;
        case 'ilse-tab':
          $.otp.triggerWorkflows.searchQuery.ilseNumbers = $.otp.parseDelimitedStringToArray($('#ilse-selection')
            .val());
          if (!$.otp.triggerWorkflows.searchQuery.ilseNumbers.length) {
            $.otp.toaster.showErrorToast(
              $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_ERROR,
              'At least one ilse Number must be supplied'
            );
            return;
          }
          break;
        case 'bam-tab':
          $.otp.triggerWorkflows.searchQuery.bamIds = $.otp.parseDelimitedStringToArray($('#bam-selection').val());
          if (!$.otp.triggerWorkflows.searchQuery.bamIds.length) {
            $.otp.toaster.showErrorToast(
              $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_ERROR,
              'At least one BAM ID must be supplied'
            );
            return;
          }
          break;
        case 'multi-input-tab': {
          const multiInputList = $.otp.parseDelimitedMultiLineStringToArrays($('#multi-input-selection').val());
          if (!multiInputList.length) {
            $.otp.toaster.showErrorToast(
              $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_ERROR,
              'At least one multi input must be supplied'
            );
            return;
          }
          $.otp.triggerWorkflows.searchQuery.pids = multiInputList.map((multiInput) => multiInput[0] || null);
          $.otp.triggerWorkflows.searchQuery.sampleTypes = multiInputList.map((multiInput) => multiInput[1] || null);
          $.otp.triggerWorkflows.searchQuery.seqTypes = multiInputList.map((multiInput) => multiInput[2] || null);
          $.otp.triggerWorkflows.searchQuery.readTypes = multiInputList.map((multiInput) => multiInput[3] || null);
          $.otp.triggerWorkflows.searchQuery.singleCells = multiInputList.map((multiInput) => multiInput[4] || null);
          if (!($.otp.triggerWorkflows.searchQuery.singleCells
            .every((singleCell) => ['true', 'false'].includes(singleCell)))) {
            $.otp.toaster.showErrorToast(
              $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_ERROR,
              'The single cell argument must be a boolean'
            );
            return;
          }
          break;
        }
      }
      // reload the datatable by fetching data from backend
      $.otp.triggerWorkflows.getSeqTrackTable().ajax.reload();
    },

    /**
     * Fetch table data from backend by calling the corresponding method of the search controller
     */
    fetchData: (inputdata) => new Promise((resolve) => {
      $.ajax({
        url: $.otp.createLink({
          controller: $.otp.triggerWorkflows.SEARCH_CONTROLLER,
          action: $.otp.triggerWorkflows.SEARCH_ACTION[inputdata.type]
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
              $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_ERROR,
              err.responseJSON.message
            );
          } else {
            $.otp.toaster.showErrorToast(
              $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_ERROR,
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

    getMissingWorkflowConfigsWarningsTable: () => $('#missingWorkflowConfigsWarnings').DataTable(),

    getReferenceGenomeWarningsTable: () => $('#missingReferenceGenomeWarnings').DataTable(),

    getMissingSeqPlatformGroupsTable: () => $('#missingSeqPlatformGroupsTable').DataTable(),

    getSeqPlatformWarningsTable: () => $('#seqPlatformWarnings').DataTable(),

    getLibPrepKitWarningsTable: () => $('#libraryPrepKitWarnings').DataTable(),

    getWarningsForMissingSampleTypePerProjectTable: () => $('#warningsForMissingSampleTypePerProject').DataTable(),

    getSeqTrackTable: () => $('#seqTrackTable').DataTable(),

    getBamTable: () => $('#bamTable').DataTable(),

    getWorkflowTable: () => $('#workflowTable').DataTable(),

    getMissingLibraryPrepKitWarningsTable: () => $('#missingLibraryPrepKitWarnings').DataTable()
  };

  $(document).ready(() => {
    /**
     * Initialization of a DataTable for SeqTrack table in TriggerWokflows page
     */
    $('#seqTrackTable').DataTable({
      deferRender: true,
      processing: true,
      dom: 'B<"toolbar"><"top"i>frt',
      scrollY: '500px',
      scrollCollapse: true,
      paging: false,
      buttons: $.otp.getDownloadButton(),
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
        { data: 'libPrepkit' },
        { data: 'seqPlatform' },
        { data: 'seqPlatformGroup' },
        { data: 'species' },
        { data: 'mixedInSpecies' },
        { data: 'bamIds' }
      ],
      columnDefs: [{
        defaultContent: '',
        targets: '_all'
      }],
      ajax: (inputdata, callback) => {
        if ($.otp.triggerWorkflows.tableInitialized) {
          // eslint-disable-next-line no-param-reassign
          inputdata = $.otp.triggerWorkflows.searchQuery;
          $.otp.triggerWorkflows.fetchData(inputdata).then((outputdata) => {
            callback(outputdata.data);

            const { warnings } = outputdata.data;

            if (outputdata.data.bamData && outputdata.data.bamData.length) {
              $.otp.triggerWorkflows.getBamTable().clear().rows.add(outputdata.data.bamData.map((o) => [
                o.id,
                o.project,
                o.individual,
                o.sampleType,
                o.withdrawn,
                o.libPrepKit,
                o.species,
                o.mixedInSpecies,
                o.referenceGenome,
                o.seqPlatformGroup
              ])).draw();
            } else {
              $.otp.triggerWorkflows.getBamTable().clear().draw();
            }

            if (outputdata.data.info && outputdata.data.info.workflows && outputdata.data.info.workflows.length) {
              $.otp.triggerWorkflows.getWorkflowTable().clear().rows.add(outputdata.data.info.workflows.map((o) => [
                o.project,
                o.seqType,
                o.workflow,
                o.version,
                o.referenceGenome.length > 0 ?
                  `
                      <table class="table table-bordered">
                      <thead>
                        <tr>
                            <th>Species</th>
                            <th>Reference genome</th>
                        </tr>
                      </thead>
                      <tbody>
                      ${o.referenceGenome.map((subTableElement) => [`
                          <tr>
                              <td>${subTableElement.species}</td>
                              <td>${subTableElement.referenceGenome}</td>
                          </tr>
                        `]).join('')}
                      </tbody>
                    </table>
                ` : '-'
              ])).draw();
            } else {
              $.otp.triggerWorkflows.getWorkflowTable().clear().draw();
            }

            // withdrawnData
            if (warnings.withdrawnSeqTracks && warnings.withdrawnSeqTracks.length) {
              $('#withdrawnSeqTracksWarningsCard').removeClass('d-none');
              $.otp.triggerWorkflows.getWithdrawnWarningsTable().clear().rows.add(
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
              $.otp.triggerWorkflows.getWithdrawnWarningsTable().clear().draw();
            }

            // alignment config missing
            if (warnings.missingWorkflowConfigs && warnings.missingWorkflowConfigs.length) {
              $('#missingWorkflowConfigWarningsCard').removeClass('d-none');
              $.otp.triggerWorkflows.getMissingWorkflowConfigsWarningsTable().clear().rows.add(
                warnings.missingWorkflowConfigs.map((o) => [
                  o.workflow,
                  o.project,
                  o.seqType,
                  o.count
                ])
              ).draw();
            } else {
              $('#missingWorkflowConfigWarningsCard').addClass('d-none');
              $.otp.triggerWorkflows.getMissingWorkflowConfigsWarningsTable().clear().draw();
            }

            // reference genome config missing
            if (warnings.missingReferenceGenomes && warnings.missingReferenceGenomes.length) {
              $('#missingReferenceGenomeWarningsCard').removeClass('d-none');
              $.otp.triggerWorkflows.getReferenceGenomeWarningsTable().clear().rows.add(
                warnings.missingReferenceGenomes.map((o) => [
                  o.project,
                  o.seqType,
                  o.species,
                  o.count
                ])
              ).draw();
            } else {
              $('#missingReferenceGenomeWarningsCard').addClass('d-none');
              $.otp.triggerWorkflows.getReferenceGenomeWarningsTable().clear().draw();
            }

            if (warnings.missingSeqPlatformGroups && warnings.missingSeqPlatformGroups.length) {
              $('#missingSeqPlatformGroupsCard').removeClass('d-none');
              $.otp.triggerWorkflows.getMissingSeqPlatformGroupsTable().clear().rows.add(
                warnings.missingSeqPlatformGroups.map((o) => [
                  o.project,
                  o.individual,
                  o.seqType,
                  o.sampleType,
                  o.seqPlatforms,
                  `<a href="${o.link.path}">${o.link.name}</a></a>`
                ])
              ).draw();
            } else {
              $('#missingSeqPlatformGroupsCard').addClass('d-none');
              $.otp.triggerWorkflows.getMissingSeqPlatformGroupsTable().clear().draw();
            }

            if (warnings.missingLibPrepKits && warnings.missingLibPrepKits.length) {
              $('#missingLibraryPrepKitWarningsCard').removeClass('d-none');
              $.otp.triggerWorkflows.getMissingLibraryPrepKitWarningsTable().clear().rows.add(
                warnings.missingLibPrepKits.map((o) => [
                  o.project,
                  o.individual,
                  o.seqType,
                  o.sampleType,
                  o.lane,
                  o.run
                ])
              ).draw();
            } else {
              $('#missingLibraryPrepKitWarningsCard').addClass('d-none');
              $.otp.triggerWorkflows.getMissingLibraryPrepKitWarningsTable().clear().draw();
            }

            // seqPlatformGroup missmatch
            if (warnings.seqPlatformGroups && warnings.seqPlatformGroups.length) {
              $('#seqPlatformWarningsCard').removeClass('d-none');
              $.otp.triggerWorkflows.getSeqPlatformWarningsTable().clear().rows.add(
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
              $.otp.triggerWorkflows.getSeqPlatformWarningsTable().clear().draw();
            }

            // library preparation kit missmatch
            if (warnings.libraryPreparationKits && warnings.libraryPreparationKits.length) {
              $('#libraryPrepKitWarningsCard').removeClass('d-none');
              $.otp.triggerWorkflows.getLibPrepKitWarningsTable().clear().rows.add(
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
              $.otp.triggerWorkflows.getLibPrepKitWarningsTable().clear().draw();
            }

            // missing SampleTypePerProject
            if (warnings.missingSampleTypePerProject && warnings.missingSampleTypePerProject.length) {
              $('#warningsForMissingSampleTypePerProjectCard').removeClass('d-none');
              $.otp.triggerWorkflows.getWarningsForMissingSampleTypePerProjectTable().clear().rows.add(
                warnings.missingSampleTypePerProject.map((o) => [
                  o.project,
                  o.sampleType
                ])
              ).draw();
            } else {
              $('#warningsForMissingSampleTypePerProjectCard').addClass('d-none');
              $.otp.triggerWorkflows.getWarningsForMissingSampleTypePerProjectTable().clear().draw();
            }

            // message
            // eslint-disable-next-line no-extra-boolean-cast
            if (!!outputdata.data.message) {
              $.otp.toaster.showErrorToast(
                $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_INFO,
                outputdata.data.message
              );
            }

            if ((!outputdata.data.data && !outputdata.data.bamData) ||
              (!outputdata.data.data.length && !outputdata.data.bamData.length)) {
              $.otp.toaster.showWarningToast(
                $.otp.triggerWorkflows.TOAST_TITLE.SEARCH_WARNING,
                'No SeqTracks can be found. Make sure the search inputs are correct'
              );
            }
          });
        } else {
          callback({ data: [] });
          $.otp.triggerWorkflows.tableInitialized = true;
        }
      }
    }).on('draw', () => {
      $('#searchSeqTrackButton').prop('disabled', false);
    }).on('preXhr.dt', () => {
      $('#searchSeqTrackButton').prop('disabled', true);
    });

    /** Move description text to toolbar */
    $('div.toolbar').html($('#seqTrackTableText'));

    $('#bamTable').DataTable({
      dom: 'B<"toolbar"><"top"i>frtp',
      buttons: $.otp.getDownloadButton(),
      scrollCollapse: true,
      paging: false,
      scrollY: '500px'
    });

    $('#workflowTable').DataTable({
      dom: 'B<"toolbar"><"top"i>frtp',
      buttons: $.otp.getDownloadButton(),
      scrollCollapse: true,
      paging: false
    });

    $('#withdrawnWarnings').DataTable({
      dom: 'B<"toolbar"><"top"i>frtp',
      buttons: ['csv'],
      scrollCollapse: true,
      paging: false
    });

    $('#missingWorkflowConfigWarnings').DataTable({
      dom: 'B<"toolbar"><"top"i>frtp',
      buttons: ['csv'],
      scrollCollapse: true,
      paging: false
    });

    $('#missingReferenceGenomeWarnings').DataTable({
      dom: 'B<"toolbar"><"top"i>frtp',
      buttons: ['csv'],
      scrollCollapse: true,
      paging: false
    });

    $('#seqPlatformWarnings').DataTable({
      dom: 'B<"toolbar"><"top"i>frtp',
      buttons: ['csv'],
      scrollCollapse: true,
      paging: false
    });

    $('#libraryPrepKitWarnings').DataTable({
      dom: 'B<"toolbar"><"top"i>frtp',
      buttons: ['csv'],
      scrollCollapse: true,
      paging: false
    });

    $('#missingLibraryPrepKitWarnings').DataTable({
      dom: 'B<"toolbar"><"top"i>frtp',
      buttons: ['csv'],
      scrollCollapse: true,
      paging: false
    });

    $('#warningsForMissingSampleTypePerProject').DataTable({
      dom: 'B<"toolbar"><"top"i>frtp',
      buttons: ['csv'],
      scrollCollapse: true,
      paging: false
    });
  });
});
