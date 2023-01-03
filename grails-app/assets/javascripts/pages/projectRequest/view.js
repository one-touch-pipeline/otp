/*
 * Copyright 2011-2022 The OTP authors
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

// activate the possibility to provide own input for tag-select items
$(document).ready(() => {
  'use strict';

  $('.tag-select').select2({
    tags: true,
    theme: 'bootstrap4'
  });

  const deleteButton = $('#delete-request-btn');
  deleteButton.on('click', () => {
    const modal = $('#confirmationModal');
    const modalBody = $('.modal-body', modal);
    const confirmButton = modal.find('#confirmModal');
    const cancelButtons = modal.find('.closeModal');

    confirmButton.unbind('click');
    confirmButton.on('click', () => {
      modal.hide();
      $.ajax({
        url: $.otp.createLink({ controller: 'projectRequest', action: 'delete' }),
        type: 'POST',
        dataType: 'json',
        data: {
          id: deleteButton.data('req-id')
        },
        success() {
          $.otp.toaster.showSuccessToast('Deletion was successful.');
          window.location.href = $.otp.createLink({ controller: 'projectRequest', action: 'unresolved' });
        },
        error(jqXHR, textStatus, errorThrown) {
          $.otp.toaster.showErrorToast(`${textStatus} occurred while processing the data.`, errorThrown);
        }
      });
    });

    cancelButtons.unbind('click');
    cancelButtons.on('click', () => {
      modal.hide();
    });

    modalBody.html('Are you sure?');
    modal.modal('toggle').show();
  });
});
