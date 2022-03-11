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
$(() => {
  const runsTable = $('#runs');
  const table = runsTable.DataTable({
    columns: [
      {
        data(row, type) {
          if (type === 'sort') {
            return null;
          }
          const buttonsDisabled = (row.state !== 'FAILED' || row.stepId === null) ? ' disabled ' : '';
          return `<input type="checkbox" name="step" value="${row.stepId}"
                            form="bulk" class="tableCheckbox" ${buttonsDisabled}>`;
        },
        orderable: false
      },
      {
        data(row, type) {
          if (type === 'sort') {
            return row.state;
          }
          return `<div title="${row.state}: ${row.stateDesc}"
                        class="${statusToClassName(row.state)} small"></div>`;
        }
      },
      {
        data(row, type) {
          if (type === 'sort') {
            return row.comment;
          }
          if (row.comment) {
            return `<i class="bi-info-circle" title="${row.comment}"></i>`;
          }
          return '';
        }
      },
      { data: 'workflow', orderable: false },
      {
        data(row, type) {
          if (type === 'sort') {
            return row.shortName;
          }
          if (row.shortName) {
            const workflowRunDetailsPageLink = $.otp.createLink({
              controller: 'workflowRunDetails',
              action: 'index',
              id: row.id,
              parameters: {
                'workflow.id': $('#workflow').val(),
                state: $('#state').val(),
                name: $('#name').val()
              }
            });
            return `<a title="${row.displayName.replaceAll('\n', '<br>')}" 
                       data-toggle="tooltip" data-placement="bottom" href="${workflowRunDetailsPageLink}">
                      ${row.shortName}
                    </a>`;
          }
          return '';
        }
      },
      { data: 'step', orderable: false },
      { data: 'dateCreated' },
      { data: 'lastUpdated' },
      { data: 'duration', orderable: false },
      { data: 'id' },
      {
        data(row, type) {
          if (type === 'sort') {
            return null;
          }

          const buttonsDisabled = (row.state !== 'FAILED' || row.stepId === null) ? ' disabled ' : '';
          const stepsDisabled = (row.steps.length === 0) ? ' disabled ' : '';

          const failedFinalButton = button($.otp.createLink({
            controller: 'workflowRunList', action: 'setFailedFinal'
          }),
          row.stepId,
          'Set failed final',
          buttonsDisabled,
          'file-earmark-x');
          const restartStepButton = button($.otp.createLink({
            controller: 'workflowRunList', action: 'restartStep'
          }),
          row.stepId,
          `Restart "${row.step}" step`,
          buttonsDisabled,
          'reply');
          const restartRunButton = button($.otp.createLink({
            controller: 'workflowRunList', action: 'restartRun'
          }),
          row.stepId,
          'Restart run',
          buttonsDisabled,
          'reply-all');
          const workflowRunListPageLink = $.otp.createLink({
            controller: 'workflowRunList',
            action: 'restartStep'
          });

          let result = `<form method="POST" class="single">
                          <div class="btn-group float-right">
                            ${failedFinalButton}
                            ${restartStepButton} 
                            <div class="btn-group" role="group">
                              <button class="btn btn-xs btn-primary dropdown-toggle dropdown-toggle-split"
                                      type="button" data-toggle="dropdown" 
                                      aria-haspopup="true" aria-expanded="false" 
                                      title="Restart previous steps" ${buttonsDisabled} ${stepsDisabled}>
                                <span class="sr-only">Toggle Dropdown</span>
                              </button>
                              <div class="dropdown-menu dropdown-menu-right">`;

          for (let i = 0; i < row.steps.length; i++) {
            result += `<button class="dropdown-item" formaction="${workflowRunListPageLink}" 
                               name="step" value="${row.stepIds[i]}" ${buttonsDisabled}>
                         Restart "${row.steps[i]}" step
                       </button>`;
          }
          result += `</div></div>${restartRunButton}</div></form>`;
          return result;
        },
        orderable: false
      }
    ],
    processing: true,
    serverSide: true,
    ordering: true,
    order: [[7, 'desc']],
    paging: true,
    pageLength: 100,
    scrollCollapse: true,
    ajax: {
      url: $.otp.createLink({
        controller: 'workflowRunList',
        action: 'data'
      }),
      data(data) {
        const json = data;
        json['workflow.id'] = $('#workflow').val();
        json.state = $('#state').val();
        json.name = $('#name').val();
        return json;
      },
      dataSrc(json) {
        setCount(json.count);
        return json.data;
      }
    }
  });

  table.on('draw', () => {
    $('[title]').tooltip({ html: true });
  });

  const setCount = function (values) {
    $('#allRuns').text(values[0]);
    $('#runningRuns').text(values[1]);
    $('#failedRuns').text(values[2]);
  };

  $('#workflow, #state, #name').on('change', () => {
    table.draw();
  });

  $('#selectAll').on('click', (e) => {
    const checkboxes = $('input.tableCheckbox:not([disabled])');
    const { checked } = e.target;
    checkboxes.each((i, el) => {
      // eslint-disable-next-line no-param-reassign
      el.checked = checked;
    });
  });

  $('#bulk').on('submit', (e) => {
    const checkboxes = $('input.tableCheckbox');
    let anySelected = false;
    checkboxes.each((i, el) => {
      if (el.checked) {
        anySelected = true;
      }
    });
    if (!anySelected) {
      e.preventDefault();
      alert('Select at least one row');
    }

    addSelection(e.target);
  });

  runsTable.on('submit', '.single', (e) => {
    addSelection(e.target);
  });

  const addSelection = function (form) {
    addHiddenField(form, 'redirect', $.otp.createLink({
      controller: 'workflowRunList',
      parameters: {
        'workflow.id': $('#workflow').val(),
        state: $('#state').val(),
        name: $('#name').val()
      }
    }).slice($.otp.contextPath.length));
  };

  const addHiddenField = function (form, name, value) {
    $('<input>', { type: 'hidden', name, value }).appendTo(form);
  };
});
