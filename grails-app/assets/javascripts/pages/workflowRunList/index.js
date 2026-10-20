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

  const getFilterParams = () => ({
    'workflow.id': $('#workflow').val(),
    state: $('#state').val(),
    name: $('#name').val(),
    stepFilter: $('#stepFilter').val()
  });

  const KILLABLE_STATES = ['PENDING', 'RUNNING_OTP', 'RUNNING_WES'];

  const isKillMode = () => $('input[name="selectionMode"]:checked').val() === 'kill';

  const runsTable = $('#runs');
  const table = runsTable.DataTable({
    dom: '<"top"i>rt<"bottom"lp><"clear">',
    columns: [
      {
        // name, value, and disabled state depend on the selection mode and are set by applySelectionMode
        data(row, type) {
          if (type === 'sort') {
            return null;
          }
          return `<input type="checkbox" form="bulk" class="tableCheckbox" data-state="${row.state}"
                         data-step-id="${row.stepId ?? ''}" data-run-id="${row.id}">`;
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
              parameters: getFilterParams()
            });
            return `<a title="${row.displayName.replaceAll('\n', '<br>')}" 
                       data-bs-toggle="tooltip" data-placement="bottom" href="${workflowRunDetailsPageLink}">
                      ${row.shortName}
                    </a>`;
          }
          return '';
        }
      },
      {
        data: 'step',
        orderable: false
      },
      { data: 'dateCreated' },
      { data: 'lastUpdated' },
      { data: 'firstJobStarted' },
      { data: 'lastJobFinished' },
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

          const buttonsDisabled = (row.state !== 'FAILED' || row.stepId === null) ? ' disabled ' : '';
          const stepsDisabled = (row.steps.length === 0) ? ' disabled ' : '';

          const failedFinalButton = button(
            $.otp.createLink({
              controller: 'workflowRunList',
              action: 'setFailedFinal'
            }),
            row.stepId,
            'Set failed final',
            'failed-final-btn',
            buttonsDisabled,
            'file-earmark-x'
          );
          const restartStepButton = button(
            $.otp.createLink({
              controller: 'workflowRunList',
              action: 'restartStep'
            }),
            row.stepId,
            `Restart ${row.step} step`,
            'restart-step-btn',
            buttonsDisabled,
            'reply'
          );
          const restartRunButton = button(
            $.otp.createLink({
              controller: 'workflowRunList',
              action: 'restartRun'
            }),
            row.stepId,
            'Restart run',
            'restart-run-btn',
            buttonsDisabled,
            'reply-all'
          );
          const killRunDisabled = KILLABLE_STATES.includes(row.state) ? '' : ' disabled ';
          const killRunLink = $.otp.createLink({
            controller: 'workflowRunList',
            action: 'killRun'
          });
          const killRunButton = `<button class="btn btn-xs btn-danger kill-run-btn" formaction="${killRunLink}"
                                         name="run" value="${row.id}" title="Kill run" ${killRunDisabled}>
                                   <i class="bi-x-octagon-fill"></i>
                                 </button>`;
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
                                      type="button" data-bs-toggle="dropdown" 
                                      aria-haspopup="true" aria-expanded="false" 
                                      title="Restart previous steps" ${buttonsDisabled} ${stepsDisabled}>
                              </button>
                              <div class="dropdown-menu dropdown-menu-right">`;

          for (let i = 0; i < row.steps.length; i++) {
            result += `<button class="dropdown-item" formaction="${workflowRunListPageLink}" 
                               name="step" value="${row.stepIds[i]}" ${buttonsDisabled}>
                         Restart "${row.steps[i]}" step
                       </button>`;
          }
          result += `</div></div>${restartRunButton}${killRunButton}</div></form>`;
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
        return Object.assign(data, getFilterParams());
      },
      dataSrc(json) {
        setCount(json.count);
        return json.data;
      }
    }
  });

  table.on('draw', () => {
    applySelectionMode();
    $('.tooltip').remove();

    const $els = $('#runs').find('[title], [data-bs-original-title]');

    $els.each((_, el) => {
      if (!el.getAttribute('title') && el.getAttribute('data-bs-original-title')) {
        el.setAttribute('title', el.getAttribute('data-bs-original-title'));
      }

      if (!bootstrap.Tooltip.getInstance(el)) {
        new bootstrap.Tooltip(el, {
          html: true,
          container: 'body',
          trigger: 'hover focus'
        });
      }
    });
  });

  const setCount = function (values) {
    $('#allRuns').text(values[0]);
    $('#runningRuns').text(values[1]);
    $('#failedRuns').text(values[2]);
  };

  $('#workflow, #state, #name').on('change', () => {
    table.draw();
  });

  const stepFilter = $('#stepFilter');
  const stepFilters = $('#stepFilterSuggestions [data-step-filter-suggestion]').map((_, element) => (
    $(element).data('step-filter-suggestion')
  )).get();

  const escapeHtml = function (value) {
    return $('<div>').text(value).html();
  };

  const findMatchingStepFilters = function (query, syncResults) {
    const lowerCaseQuery = query.toLowerCase();
    syncResults(stepFilters.filter((step) => (
      step.toLowerCase().includes(lowerCaseQuery)
    )).map((step) => ({
      name: step
    })));
  };

  stepFilter.typeahead(
    {
      highlight: true,
      hint: false
    },
    {
      display: 'name',
      limit: 1000,
      source: findMatchingStepFilters,
      templates: {
        empty: '<div>No matching workflow step.</div>',
        suggestion: (result) => `<div>${escapeHtml(result.name)}</div>`
      }
    }
  );

  let selectedStepFilter = stepFilter.val();
  stepFilter.on('typeahead:select typeahead:autocomplete change', () => {
    if (stepFilter.val() !== selectedStepFilter) {
      selectedStepFilter = stepFilter.val();
      table.draw();
    }
  });

  const updateSelectedCount = function () {
    $('#selectedCount').text($('input.tableCheckbox:checked').length);
  };

  /**
   * Restart mode selects FAILED runs by step ID, Kill mode selects unfinished runs by run ID.
   * Clears the selection, so that no bulk action submits a selection made in the other mode or on another page.
   */
  const applySelectionMode = function () {
    const killMode = isKillMode();
    $('input.tableCheckbox').each((_, el) => {
      const checkbox = $(el);
      const eligible = killMode ?
        KILLABLE_STATES.includes(checkbox.data('state')) :
        checkbox.data('state') === 'FAILED' && checkbox.attr('data-step-id') !== '';
      checkbox.attr('name', killMode ? 'run' : 'step')
        .val(killMode ? checkbox.attr('data-run-id') : checkbox.attr('data-step-id'))
        .prop('disabled', !eligible)
        .prop('checked', false);
    });
    $('#selectAll').prop('checked', false);
    $('#bulk .restart-mode-btn').prop('disabled', killMode);
    $('#bulk .kill-mode-btn').prop('disabled', !killMode);
    updateSelectedCount();
  };

  $('input[name="selectionMode"]').on('change', applySelectionMode);

  $('#selectAll').on('click', (e) => {
    const checkboxes = $('input.tableCheckbox:not([disabled])');
    checkboxes.prop('checked', e.target.checked);
    updateSelectedCount();
  });

  runsTable.on('change', 'input.tableCheckbox', updateSelectedCount);

  $('#bulk').on('submit', (e) => {
    const selectedCount = $('input.tableCheckbox:checked').length;
    if (selectedCount === 0) {
      e.preventDefault();
      alert('Select at least one row');
      return;
    }
    const submitter = e.originalEvent ? e.originalEvent.submitter : null;
    if (submitter && submitter.id === 'killRunsButton' &&
        !window.confirm(`Kill ${selectedCount} selected workflow run(s)?`)) {
      e.preventDefault();
      return;
    }

    addSelection(e.target);
  });

  runsTable.on('submit', '.single', (e) => {
    const submitter = e.originalEvent ? e.originalEvent.submitter : null;
    if (submitter && submitter.classList.contains('kill-run-btn') &&
        !window.confirm(`Kill workflow run ${submitter.value}?`)) {
      e.preventDefault();
      return;
    }
    addSelection(e.target);
  });

  const addSelection = function (form) {
    addHiddenField(
      form,
      'redirect',
      $.otp.createLink({
        controller: 'workflowRunList',
        parameters: getFilterParams()
      }).slice($.otp.contextPath.length)
    );
  };

  const addHiddenField = function (form, name, value) {
    $('<input>', { type: 'hidden', name, value }).appendTo(form);
  };
});
