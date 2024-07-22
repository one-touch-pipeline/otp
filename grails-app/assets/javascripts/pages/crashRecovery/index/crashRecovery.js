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

$.otp.crashRecovery = {};
$.otp.crashRecovery.processingStepIds = function () {
  'use strict';

  const ids = [];
  $('#crashRecoveryTable input[name=processingStep]:checked').each((index, element) => {
    ids.push($(element).val());
  });
  if (ids.length === 0) {
    $('#dialog-select-job').modal('show');
  }
  return ids;
};

$.otp.crashRecovery.createListView = function () {
  'use strict';

  $.otp.createListView(
    '#crashRecoveryTable',
    $.otp.createLink({
      controller: 'crashRecovery',
      action: 'datatable'
    }),
    true,
    (json) => {
      let i;
      let rowData;
      for (i = 0; i < json.aaData.length; i++) {
        rowData = json.aaData[i];
        rowData[0] = `<input type="checkbox" 
                           name="processingStep" 
                           value="${rowData[0]}" ${(rowData[5] ? 'checked' : '')} />`;
        rowData[1] = $.otp.createLinkMarkup({
          controller: 'processes',
          action: 'plan',
          id: rowData[1].id,
          text: rowData[1].name
        });
        rowData[2] = $.otp.createLinkMarkup({
          controller: 'processes',
          action: 'process',
          id: rowData[2],
          text: rowData[2]
        });
        rowData[3] = $.otp.createLinkMarkup({
          controller: 'processes',
          action: 'processingStep',
          id: rowData[3].id,
          text: rowData[3].name
        });
        rowData[4] = rowData[4].class;
      }
    },
    undefined,
    undefined,
    140,
    { sort: false }
  );
};

$.otp.crashRecovery.showParametersDialog = function (ids, target) {
  'use strict';

  $.get($.otp.createLink({
    controller: 'crashRecovery',
    action: 'parametersOfJob',
    parameters: { ids }
  }), (data) => {
    const dialogContainer = $('#dialog-parameters-of-job');
    $('.modal-body', dialogContainer).append($('#output-params-wrapper', data));
    dialogContainer.modal('show');
    $('#parameters-ok').on('click', () => {
      const parameters = [];
      $('ul li input', $(this)).each(function () {
        parameters[parameters.length] = {
          key: $(this).attr('name'),
          value: $(this).val()
        };
      });
      $.getJSON($.otp.createLink({
        controller: target.controller,
        action: target.action,
        parameters: {
          ids,
          parameters: JSON.stringify(parameters)
        }
      }), (response) => {
        $('#crashRecoveryTable').dataTable().fnDraw();
        $.otp.toaster.showInfoToast(response.success);
      });
      dialogContainer.modal('hide');
    });
  });
};

$.otp.crashRecovery.showFailedJobDialog = function (ids, target) {
  'use strict';

  const dialog = $('#dialog-error-message-job');
  dialog.modal('show');
  $('#markAsFailed').on('click', () => {
    let message = $('input', dialog).val();
    if (!message || message === '') {
      message = null;
      return;
    }
    dialog.modal('hide');
    $.getJSON($.otp.createLink({
      controller: target.controller,
      action: target.action,
      parameters: { ids }
    }), { message }, (data) => {
      $('#crashRecoveryTable').dataTable().fnDraw();
      $.otp.toaster.showInfoToast(data.success);
    });
  });
};

$.otp.crashRecovery.finishedButton = function () {
  'use strict';

  const ids = $.otp.crashRecovery.processingStepIds();
  if (ids.length === 0) {
    return;
  }
  $.otp.crashRecovery.showParametersDialog(ids, {
    controller: 'crashRecovery',
    action: 'markFinished'
  });
};

$.otp.crashRecovery.succeededButton = function () {
  'use strict';

  const ids = $.otp.crashRecovery.processingStepIds();
  if (ids.length === 0) {
    return;
  }
  $.otp.crashRecovery.showParametersDialog(ids, {
    controller: 'crashRecovery',
    action: 'markSucceeded'
  });
};

$.otp.crashRecovery.failedButton = function () {
  'use strict';

  const ids = $.otp.crashRecovery.processingStepIds();
  if (ids.length === 0) {
    return;
  }
  $.otp.crashRecovery.showFailedJobDialog(ids, {
    controller: 'crashRecovery',
    action: 'markFailed'
  });
};

$.otp.crashRecovery.restartButton = function () {
  'use strict';

  const ids = $.otp.crashRecovery.processingStepIds();
  if (ids.length === 0) {
    return;
  }
  $.otp.crashRecovery.showFailedJobDialog(ids, {
    controller: 'crashRecovery',
    action: 'restart'
  });
};

$.otp.crashRecovery.startSchedulerButton = function () {
  'use strict';

  $.getJSON($.otp.createLink({
    controller: 'crashRecovery',
    action: 'startScheduler'
  }), (data) => {
    if (data.success) {
      $.otp.toaster.showInfoToast('Scheduler successfully restarted');
    } else {
      $('#crashRecoveryTable').dataTable().fnDraw();
      $.otp.toaster.showErrorToast('Scheduler could not be restarted', 'check that all Jobs have been triaged');
    }
  });
};

$.otp.crashRecovery.setupView = function () {
  'use strict';

  $.otp.crashRecovery.createListView();
  $('#markFinished').on('click', $.otp.crashRecovery.finishedButton);
  $('#markSucceeded').on('click', $.otp.crashRecovery.succeededButton);
  $('#markFailed').on('click', $.otp.crashRecovery.failedButton);
  $('#restart').on('click', $.otp.crashRecovery.restartButton);
  $('#startScheduler').on('click', $.otp.crashRecovery.startSchedulerButton);
};
