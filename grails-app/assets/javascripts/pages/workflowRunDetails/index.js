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

  /**
   * Generate the child tables which can be opened by the show/hide button inside the steps table.
   *
   * @param rowData
   * @returns {string}
   */
  function generateChildTables(rowData) {
    return generateWesRunChildTable(rowData) + generateClusterJobChildTable(rowData) +
      generateWorkflowChildTable(rowData);
  }

  function generateClusterJobChildTable(rowData) {
    if (!rowData.clusterJobs || rowData.clusterJobs.length === 0) {
      return '';
    }

    let childTable = `<div class="p-3"><table class="table table-sm table-borderless p-3">
    <thead>
      <tr class="table-info">
        <th></th>
        <th>Job Name</th>
        <th>Job Id</th>
        <th></th>
        <th>Cluster Node</th>
        <th>Wall Time</th>
        <th>Exit Code</th>
      </tr>
    </thead>
    <tbody>`;

    rowData.clusterJobs.forEach((clusterJob) => {
      childTable += `<tr>
                       <td>
                         <div title="${clusterJob.state}" class="small ${statusToClassName(clusterJob.state)}"></div>
                       </td>
                       <td>` +
        `${$.otp.createLinkMarkup({
          text: `Cluster job: ${clusterJob.name}`,
          controller: 'clusterJobDetail',
          action: 'show',
          id: clusterJob.id,
          parameters: createLinkParametersForNavigation()
        })}
                       </td>
                       <td>${clusterJob.jobId}</td>`;

      if (clusterJob.hasLog) {
        childTable += '<td>' +
          `${$.otp.createLinkMarkup({
            text: 'Log',
            controller: 'clusterJobDetail',
            action: 'showLog',
            id: clusterJob.id,
            parameters: createLinkParametersForNavigation()
          })}</td>`;
      } else {
        childTable += '<td></td>';
      }

      childTable += `  <td>${clusterJob.node}</td>
                       <td>${clusterJob.wallTime}</td>
                       <td>${clusterJob.exitCode}</td>
                     </tr>`;
    });

    if (rowData.state === 'FAILED' && rowData.previousStepId) {
      childTable += `<tr>
                       <td></td>
                       <td>
                         <b><i class="bi bi-info-circle"></i> 
                           This job failed. Maybe the reason is 
                           related to the previous step (${rowData.previousStepId}).
                         </b>
                       </td>
                       <td></td>
                       <td></td>
                       <td></td>
                       <td></td>
                       <td></td>
                     </tr>`;
    }

    childTable += '</tbody>';
    childTable += '</table></div>';
    return childTable;
  }

  function generateWesRunChildTable(rowData) {
    if (!rowData.wesRuns || rowData.wesRuns.length === 0) {
      return '';
    }

    let childTable = `<div class="p-3"><table class="table table-sm table-borderless p-3">
    <thead>
      <tr class="table-info">
        <th></th>
        <th>WorkflowExecutionSystem identifier</th>
        <th>State</th>
        <th>Subpath</th>
        <th>Log name</th>
        <th>Exit code</th>
      </tr>
    </thead>
    <tbody>`;

    rowData.wesRuns.forEach((wesRun) => {
      childTable += `<tr>
                       <td>
                         <div title="${wesRun.state}" class="small ${statusToClassName(wesRun.state)}"></div>
                       </td>
                       <td>
                         ${$.otp.createLinkMarkup({
    text: `WorkflowExecutionSystem run: ${wesRun.wesIdentifier}`,
    controller: 'wesRun',
    action: 'show',
    id: wesRun.id,
    parameters: createLinkParametersForNavigation()
  })}
                       </td>
                       <td>${wesRun.state}</td>
                       <td>${wesRun.subPath}</td>
                       <td>${wesRun.logName}</td>
                       <td>${wesRun.exitCode}</td>
                     </tr>`;
    });

    childTable += '</tbody></table></div>';
    return childTable;
  }

  function generateWorkflowChildTable(rowData) {
    let childTable = `<div class="p-3"><table class="table table-sm table-borderless">
    <thead>
      <tr class="table-info">
        <th class="narrow-column">Type</th>
        <th class="wide-column"></th>
        <th class="narrow-column"></th>
      </tr>
    </thead>
    <tbody>`;

    if (rowData.hasLogs) {
      const logLink = $.otp.createLinkMarkup({
        text: 'Log',
        controller: 'workflowRunDetails',
        action: 'showLogs',
        id: rowData.id,
        parameters: createLinkParametersForNavigation()
      });
      childTable += `<tr class="log-row">
                      <td>Workflow logs:</td>
                      <td></td>
                      <td>${logLink}</td>
                    </tr>`;
    }

    if (rowData.error) {
      const errorLink = $.otp.createLinkMarkup({
        text: 'Error',
        controller: 'workflowRunDetails',
        action: 'showError',
        id: rowData.id,
        parameters: createLinkParametersForNavigation()
      });
      childTable += `<tr class="error-row"> 
                       <td>Workflow error: </td> 
                       <td class="break-text-line">${rowData.error.message}</td> 
                       <td>${errorLink}</td>
                     </tr>`;
    }
    childTable += '</tbody>';
    childTable += '</table></div>';
    return childTable;
  }

  /**
   * Render the color and tooltip of the status circle for a single workflow step row.
   *
   * @param row with workflow step data
   * @returns {string}
   */
  function renderStepStatusCircle(row) {
    const cssClass = statusToClassName(row.state);
    return `<div title="${row.state}" class="${cssClass} small"></div> ${renderStepClusterJobsStatusCircle(row)}`;
  }

  function renderStepClusterJobsStatusCircle(row) {
    if (row.clusterJobs.length > 0) {
      const state = row.cummulatedClusterJobsState;
      const cssClass = statusToClassName(state);
      return `<div title="${state} is the congregated state of the cluster/WorkflowExecutionSystem jobs" 
                   class="ml-1 ${cssClass} small"></div>`;
    }
    return '';
  }

  let lastStepFailed = false;

  const table = $('#steps').DataTable({
    columns: [
      {
        data(row, type) {
          if (type === 'sort') {
            return row.state;
          }

          return renderStepStatusCircle(row);
        }
      },
      {
        data(row, type) {
          if (type === 'sort') {
            return null;
          }
          if (!row.error && !row.clusterJobs.length && !row.wes && !row.hasLogs) {
            return '';
          }
          return '<button class=\'btn btn-xs btn-info details-control\'>' +
            '<i title=\'Show/hide details\' class=\'bi bi-chevron-bar-expand\'></i></button>';
        },
        orderable: false
      },

      {
        data: 'name',
        orderable: false
      },
      { data: 'dateCreated' },
      { data: 'lastUpdated' },
      {
        data: 'duration',
        orderable: false
      },
      { data: 'id' },
      {
        data(row, type) {
          if (type === 'sort') {
            return null;
          }

          const runState = $('#steps')
            .data('wf-run-state');
          const buttonsDisabled = (!lastStepFailed ||
          runState === 'RESTARTED' ||
          runState === 'FAILED_FINAL' ||
          row.obsolete) ? ' disabled ' : '';
          const restartStepButton = button($.otp.createLink({
            controller: 'workflowRunDetails',
            action: 'restartStep',
            parameters: { redirect: $.otp.uriWithParams }
          }), row.id, 'Restart step', 'restart-step-btn', buttonsDisabled, 'reply');
          return `<form class="restart-step-form" method="POST" class="single">
                    ${restartStepButton}
                 </form>`;
        },
        orderable: false
      }
    ],
    ordering: false,
    paging: false,
    serverSide: true,
    scrollCollapse: true,
    ajax: {
      url: $.otp.createLink({
        controller: 'workflowRunDetails',
        action: 'data',
        parameters: { 'workflowRun.id': $('#steps').data('id') }
      }),
      dataSrc(json) {
        if (json.data && json.data[0] && json.data[0].state === 'FAILED') {
          lastStepFailed = true;
        }
        return json.data;
      }
    }
  });

  function createLinkParametersForNavigation() {
    const stepsTable = $('#steps');
    const workflowRunId = stepsTable.data('wf-run-id');
    const workflowId = stepsTable.data('wf-id');
    const state = stepsTable.data('state');
    const name = stepsTable.data('name');

    return {
      'workflowRun.id': workflowRunId,
      'workflow.id': workflowId,
      state,
      name
    };
  }

  /**
   * Render the workflow configuration as JSON string.
   */
  function renderWorkflowConfiguration() {
    const configurationHolder = $('#configurationHolder');
    const configurationValue = configurationHolder.val();

    if (configurationValue) {
      configurationHolder.val(JSON.stringify(JSON.parse(configurationValue), null, 2));
    }
  }

  renderWorkflowConfiguration();

  function showChildTable(tr, row) {
    row.child(generateChildTables(row.data())).show();
    tr.addClass('shown');
    $('[title]').tooltip();
  }

  function hideChildTable(tr, row) {
    row.child.hide();
    tr.removeClass('shown');
  }

  table.on('draw', () => {
    $('[title]').tooltip();
    if (lastStepFailed) {
      $('#steps tbody tr:nth-child(2) td .details-control').trigger('click');
      $('#steps tbody tr:first-child td .details-control').trigger('click');
    }

    // expand to show the clusterJobs if exist
    $('#steps tbody tr.even, #steps tbody tr.odd').each((idx) => {
      const row = table.row(idx);
      const rowData = row.data();
      if (rowData && rowData.clusterJobs.length > 0) {
        showChildTable($(this), row);
      }
    });
  });

  $('#steps tbody').on('click', 'td .details-control', function () {
    const tr = $(this).closest('tr');
    const row = table.row(tr);

    if (row.child.isShown()) {
      hideChildTable(tr, row);
    } else {
      showChildTable(tr, row);
    }
  });
});
