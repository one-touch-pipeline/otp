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
 * Creates the datatables view for the list of all JobExecutionPlans
 * @param selector The JQuery selector for the table to create the datatable into
 */
$.otp.workflows.registerJobExecutionPlan = function (selector) {
  'use strict';

  $.otp.createListView(
    selector,
    $.otp.createLink({
      controller: 'processes',
      action: 'listData'
    }),
    true,
    undefined,
    [
      {
        render(data, type, row) {
          return $.otp.createLinkMarkup({
            controller: 'processes',
            action: 'plan',
            id: row.id,
            text: row.name
          });
        },
        targets: [0]
      },
      {
        render(data, type, row) {
          if (row.allProcessesCount) {
            return $.otp.createLinkMarkup({
              controller: 'processes',
              action: 'plan',
              id: row.id,
              text: row.allProcessesCount
            });
          }
          return '-';
        },
        targets: [1]
      },
      {
        render(data, type, row) {
          if (row.failedProcessesCount) {
            return $.otp.createLinkMarkup({
              controller: 'processes',
              action: 'plan',
              id: row.id,
              parameters: {
                state: 'FAILURE'
              },
              text: row.failedProcessesCount
            });
          }
          return '-';
        },
        targets: [2]
      },
      {
        render(data, type, row) {
          if (row.runningProcessesCount) {
            return $.otp.createLinkMarkup({
              controller: 'processes',
              action: 'plan',
              id: row.id,
              parameters: {
                state: 'RUNNING'
              },
              text: row.runningProcessesCount
            });
          }
          return '-';
        },
        targets: [3]
      },
      {
        render(data, type, row) {
          return $.otp.workflows.renderDate(row.lastSuccessfulDate);
        },
        targets: [4]
      },
      {
        render(data, type, row) {
          return $.otp.workflows.renderDate(row.lastFailureDate);
        },
        targets: [5]
      }
    ],
    undefined,
    140,
    {
      sorting: [[5, 'desc']],
      rowCallback(row, data) {
        if (data.enabled === false) {
          $(row).addClass('withdrawn');
        }
      }
    }
  );
};
