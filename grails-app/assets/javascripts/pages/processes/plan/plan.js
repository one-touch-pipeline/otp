/*
 * Copyright 2011-2020 The OTP authors
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

/**
 * Creates the datatables view for the list of all Processes for a given JobExecutionPlan
 * @param selector The JQuery selector for the table to create the datatable into
 * @param planId The id of the JobExecutionPlan for which the list of Processes should be retrieved.
 * @param state Whether to limit to specific processes (failed, running) or include all processes
 */
$.otp.workflows.registerProcesses = function (selector, planId, state) {
  'use strict';

  $.otp.createListView(selector, $.otp.createLink({
    controller: 'processes',
    action: 'planData',
    id: planId,
    parameters: {
      state
    }
  }), false, (json) => {
    let i; let j; let rowData; let stepId; let
      actions;
    for (i = 0; i < json.aaData.length; i += 1) {
      rowData = json.aaData[i];
      rowData[0] = $.otp.createLinkMarkup({
        controller: 'processes',
        action: 'process',
        id: rowData[0],
        text: rowData[0]
      });
      rowData[1] = $.otp.workflows.statusDivHtml(rowData[1]);
      if (Object.keys(rowData[2]).length !== 0) {
        rowData[2] = rowData[2].text;
      } else {
        rowData[2] = '-';
      }
      rowData[3] = $.otp.workflows.renderDate(rowData[3]);
      rowData[4] = $.otp.workflows.renderDate(rowData[4]);
      stepId = rowData[6].id;
      if (rowData[6].error) {
        rowData[6] = $.otp.createLinkMarkup({
          controller: 'processes',
          action: 'processingStep',
          id: stepId,
          text: rowData[6].state
        });
      } else {
        rowData[6] = rowData[6].state;
      }
      if (rowData[7] != null && rowData[7].length > 0) {
        rowData[7] = `<div class="commentSet" title="${rowData[7]}">&nbsp;</div>`;
      } else {
        rowData[7] = '';
      }
      actions = rowData[8].actions;
      rowData[8] = '';
      for (j = 0; j < actions.length; j += 1) {
        if (actions[j] === 'restart') {
          rowData[8] += $.otp.workflows.createRestartProcessingStepLink(stepId, selector);
        }
      }
    }
  }, [
    { bSortable: true, aTargets: [0] },
    { bSortable: false, aTargets: [1] },
    { bSortable: false, aTargets: [2] },
    { bSortable: true, aTargets: [3] },
    { bSortable: false, aTargets: [4] },
    { bSortable: false, aTargets: [5] },
    { bSortable: false, aTargets: [6] },
    { bSortable: false, aTargets: [7] },
    { bSortable: false, aTargets: [8] }
  ], undefined, undefined, {
    iDisplayLength: 25,
    bPaginate: true,
    sPaginationType: 'listbox',
    bScrollCollapse: false,
    sDom: '<i>prtp<"clear">'
  });
  $('#enable-workflow-button').on('click', () => {
    $.post($.otp.createLink({
      controller: 'processes',
      action: 'enablePlan',
      id: planId
    }), (data) => {
      if (data !== 'true') {
        $.otp.warningMessage('Could not enable Workflow');
        return;
      }
      $('#enable-workflow-button').hide();
      $('#disable-workflow-button').show();
      $('strong', $('#disable-workflow-button').parent()).text('enabled');
      $('#workflow-status').removeClass('grey').addClass('green');
    });
  });
  $('#disable-workflow-button').on('click', () => {
    $.post($.otp.createLink({
      controller: 'processes',
      action: 'disablePlan',
      id: planId
    }), (data) => {
      if (data !== 'false') {
        $.otp.warningMessage('Could not disable Workflow');
        return;
      }
      $('#disable-workflow-button').hide();
      $('#enable-workflow-button').show();
      $('strong', $('#disable-workflow-button').parent()).text('disabled');
      $('#workflow-status').removeClass('green').addClass('grey');
    });
  });
};
