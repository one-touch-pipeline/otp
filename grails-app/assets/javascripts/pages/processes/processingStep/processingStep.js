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

$.otp.workflows.processingStep = {
  processingStepId: null,
  /**
     * Creates the datatables view for the list of all ProcessingStepUpdates for a given ProcessingStep.
     * @param selector The JQuery selector for the table to create the datatable into
     */
  processingStepUpdates(selector) {
    'use strict';

    $.otp.createListView(selector, $.otp.createLink({
      controller: 'processes',
      action: 'processingStepDate',
      id: `${$.otp.workflows.processingStep.processingStepId}/`
    }), false, (json) => {
      let i; let
        rowData;
      for (i = 0; i < json.aaData.length; i += 1) {
        rowData = json.aaData[i];
        rowData[1] = $.otp.workflows.renderDate(rowData[1]);
        if (!rowData[3]) {
          rowData[3] = '-';
        } else if (rowData[3].stackTraceIdentifier) {
          rowData[3] = $.otp.createLinkMarkup({
            controller: 'processes',
            action: 'getProcessingErrorStackTrace',
            id: rowData[3].id,
            target: '_blank',
            text: rowData[3].errorMessage
          });
        } else {
          rowData[3] = rowData[3].errorMessage;
        }
      }
    }, [
      { bSortable: true, aTargets: [0] },
      { bSortable: false, aTargets: [1] },
      { bSortable: false, aTargets: [2] },
      { bSortable: false, aTargets: [3] }
    ]);
  },
  /**
     * Creates the datatables view for the list of Parameters (either input or output) for a given ProcessingStep.
     * @param selector The JQuery selector for the table to create the datatable into
     * @param inputOrOutput If {@code true} the input parameters are retrieved, if {@code false} the output
     */
  parameters(selector, inputOrOutput) {
    'use strict';

    $.otp.createListView(selector, $.otp.createLink({
      controller: 'processes',
      action: 'parameterData',
      id: `${$.otp.workflows.processingStep.processingStepId}/`
    }), true, (data) => {
      let i; let j; let rowData; let
        ids;
      for (i = 0; i < data.aaData.length; i += 1) {
        rowData = data.aaData[i];
        if (rowData[1] === '__pbsIds') {
          ids = rowData[3].split(',');
          rowData[3] = '<ul>';
          for (j = 0; j < ids.length; j += 1) {
            rowData[3] += '<li';
            if (j > 4) {
              rowData[3] += ' style="display: none;"';
            }
            rowData[3] += `>${ids[j]}</li>`;
          }
          if (ids.length >= 5) {
            rowData[3] += '<li><a href="#">...</a></li>';
          }
          rowData[3] += '</ul>';
        }
      }
    }, [
      { bSortable: true, aTargets: [0] },
      { bSortable: false, aTargets: [1] },
      { bSortable: false, aTargets: [2] },
      { bSortable: false, aTargets: [3] }
    ], [{ name: 'input', value: inputOrOutput }]);
    $(selector).on('click', 'tbody tr td ul li a', function () {
      $('li', $(this).parent().parent()).show();
      $(this).parent().hide();
    });
  },
  register(updatesSelector, inputParamSelector, outputParamSelector, stepId) {
    'use strict';

    $.otp.workflows.processingStep.processingStepId = stepId;
    $.otp.workflows.processingStep.processingStepUpdates(updatesSelector);
    $.otp.workflows.processingStep.parameters(inputParamSelector, true);
    $.otp.workflows.processingStep.parameters(outputParamSelector, false);
  },
  promptClusterJobIds(clusterJobIds) {
    'use strict';

    prompt('All cluster job IDs', clusterJobIds);
  }
};
