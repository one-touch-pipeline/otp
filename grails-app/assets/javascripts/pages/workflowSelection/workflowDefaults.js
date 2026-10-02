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

  const defaultSelect = $('#analysis-default-select');
  const addButton = $('#add-analysis-default-btn');

  if (defaultSelect.length === 0) {
    return;
  }

  defaultSelect.select2({
    theme: 'bootstrap4',
    placeholder: defaultSelect.data('placeholder'),
    minimumInputLength: 0,
    ajax: {
      url: $.otp.createLink({ controller: 'workflowSelection', action: 'searchWorkflowDefaultGroups' }),
      type: 'POST',
      dataType: 'json',
      delay: 250,
      data: (params) => ({ query: params.term }),
      processResults: (data) => ({ results: data })
    }
  });

  addButton.on('click', () => {
    const groupId = defaultSelect.val();
    if (!groupId) {
      return;
    }
    addButton.prop('disabled', true);
    $.ajax({
      url: $.otp.createLink({ controller: 'workflowSelection', action: 'applyWorkflowDefaultGroup' }),
      type: 'POST',
      data: { 'group.id': groupId },
      success(data) {
        data.forEach((entry) => {
          const workflowType = entry.refGenSelectorId ? 'alignment' : 'analysis';
          $.otp.workflowSelection.updateTableRowData(workflowType, entry);
        });
        $.otp.toaster.showSuccessToast('Success', 'Default workflows added.');
      },
      error(jqXHR, textStatus, errorThrown) {
        let errorMessage = 'Unknown error occurred.';
        if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
          errorMessage = jqXHR.responseJSON.message;
        } else if (errorThrown) {
          errorMessage = errorThrown;
        }
        $.otp.toaster.showErrorToast('Adding default workflows failed', errorMessage);
      }
    }).always(() => {
      addButton.removeAttr('disabled');
    });
  });
});
