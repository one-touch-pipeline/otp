/*
 * Copyright 2011-2019 The OTP authors
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

$.setDisableForControls = function (value) {
  $('#button-save').prop('disabled', value);
  $('#button-cancel').prop('disabled', value);
};

$(() => {
  const container = $('#comment-box-container');
  const commentContent = container.find('#comment-content');
  let lastSavedComment = commentContent.val();

  commentContent.on('input', () => {
    $.setDisableForControls(false);
  });
  $('#button-save').on('click', function () {
    const controller = $(this).data('controller');
    const action = $(this).data('action');

    const id = container.find('#entity-id').val();
    const comment = commentContent.val();

    $.setDisableForControls(true);
    $.ajax({
      url: $.otp.createLink({ controller, action }),
      dataType: 'json',
      data: { id, comment },
      type: 'POST',
      encoding: 'UTF-8',
      success(data) {
        lastSavedComment = commentContent.val();
        $('#authorSpan').text(`${data.date} ${data.author}`);
      }
    });
  });
  $('#button-cancel').on('click', () => {
    'use strict';

    $.setDisableForControls(true);
    commentContent.val(lastSavedComment);
  });
});
