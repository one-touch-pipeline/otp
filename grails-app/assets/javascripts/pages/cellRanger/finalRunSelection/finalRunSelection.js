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

$(() => {
  'use strict';

  $('#confirmDeleteModal').on('show.bs.modal', function (event) {
    const button = $(event.relatedTarget);
    const sample = button.data('sample');
    const seqType = button.data('seq-type');
    const programVersion = button.data('program-version');
    const reference = button.data('reference');
    const mwps = button.data('mwps');
    const modal = $(this);
    modal.find('input[type=hidden]').remove();
    modal.find('#modal-form').append(`<input type="hidden" name="sample" value="${sample}"/>`);
    modal.find('#modal-form').append(`<input type="hidden" name="seqType" value="${seqType}"/>`);
    modal.find('#modal-form').append(`<input type="hidden" name="programVersion" value="${programVersion}"/>`);
    modal.find('#modal-form').append(`<input type="hidden" name="reference" value="${reference}"/>`);
    mwps.forEach((mwp, index) => {
      modal.find('#modal-form').append(`<input type="hidden" name="mwps[${index}]" value="${mwp}"/>`);
    });
    modal.find('#modal-form').prop('action', $.otp.createLink({
      controller: 'cellRanger',
      action: 'deleteFinalSelectedRun'
    }));
  });
});
