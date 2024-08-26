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

/**
 * Creates the datatables view for the list of all ProcessingSteps for a given Process.
 * @param selector The JQuery selector for the table to create the datatable into
 * @param processId The id of the Process for which the list of ProcessingSteps should be retrieved.
 */
$.otp.workflows.registerProcessingStep = function (selector, processId) {
  'use strict';

  $.otp.createListView(selector, $.otp.createLink({
    controller: 'processes',
    action: 'processData',
    id: `${processId}/`
  }), false, (json) => {
    const result = json;
    result.aaData.forEach((data, index) => {
      const row = [];
      row[0] = $.otp.createLinkMarkup({
        controller: 'processes',
        action: 'processingStep',
        id: data.processingStep.id,
        text: `<div style="padding: 17px 0px 17px 10px;">${data.processingStep.id}</div>`
      });
      row[1] = $.otp.workflows.statusDivHtml(data.lastUpdate.state);
      row[2] = data.processingStep.jobName;

      let jobClass = '-';
      if (data.processingStep && data.processingStep.jobClass) {
        jobClass = data.processingStep.jobClass;
      }
      row[3] = `<span title="${jobClass}">${jobClass.split('.').pop()}</span><br/>`;

      row[4] = $.otp.workflows.renderDate(data.times.creation);
      row[5] = $.otp.workflows.renderDate(data.times.lastUpdate);

      row[6] = '-';
      if (data.times.duration) {
        row[6] = $.otp.workflows.formatTimespan(data.times.duration);
      }

      row[7] = data.lastUpdate.state;

      row[8] = '';

      data.actions.forEach((action) => {
        switch (action) {
          case 'restart':
            row[8] += $.otp.workflows.createRestartProcessingStepLink(data.processingStep.id, selector);
            break;
          default:
            // nothing
            break;
        }
      });

      result.aaData[index] = row;
    });
  }, [
    { bSortable: true, aTargets: [0] },
    { bSortable: false, aTargets: [1] },
    { bSortable: false, aTargets: [2] },
    { bSortable: false, aTargets: [3] },
    { bSortable: false, aTargets: [4] },
    { bSortable: false, aTargets: [5] },
    { bSortable: false, aTargets: [6] },
    { bSortable: false, aTargets: [7] },
    { bSortable: false, aTargets: [8] }
  ], undefined, 100);
};
