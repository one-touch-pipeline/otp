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

  const dataSwapBtn = $('button#data-swap-btn');
  const dataSwapSpinner = $('#data-swap-spinner');
  const validationTable = $('#data-swap-validation-table').DataTable();

  dataSwapBtn.on('click', (e) => {
    e.preventDefault();
    const form = document.getElementById('data-swap-form');
    const formData = new FormData(form);

    dataSwapSpinner.removeAttr('hidden');
    dataSwapBtn.attr('disabled', 'true');

    $.ajax({
      url: $.otp.createLink({
        controller: 'dataSwap',
        action: 'swapData'
      }),
      type: 'POST',
      data: formData,
      success(results) {
        const { problems } = results;
        validationTable.clear().rows.add(
          problems.map((p) => [p.message, p.affectedCells.length > 0 ? p.affectedCells.join(', ') : ' - '])
        ).draw();
        if (problems.length > 0) {
          $.otp.toaster.showWarningToast(
            'Problems occurred',
            'While evaluating the data swap file a problem occured. Please review the error(s) in the table below.'
          );
        } else if (formData.get('dryRun')) {
          $.otp.toaster.showSuccessToast(
            'Successfully validated data swap file',
            'No problems found when trying to perform the data swap.'
          );
        } else {
          $.otp.toaster.showSuccessToast(
            'Successfully performed Data Swap',
            'No problems occurred while performing the data swap.'
          );
        }
      },
      error(error) {
        $.otp.toaster.showErrorToast('Something failed', error.responseJSON.message);
      },
      complete() {
        dataSwapSpinner.attr('hidden', 'true');
        dataSwapBtn.removeAttr('disabled');
      },
      cache: false,
      contentType: false,
      processData: false
    });
  });
});
