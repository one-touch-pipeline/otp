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

// eslint-disable-next-line no-unused-vars
function onEditUnixGroup() {
  $('#button-edit-unixGroup').hide();
  $('#button-save-unixGroup').show();
  $('#unixGroupInput').prop('disabled', false);
}

// eslint-disable-next-line no-unused-vars
function onSaveUnixGroup() {
  const unixGroupInputField = $('#unixGroupInput');
  const oldUnixGroup = unixGroupInputField.attr('data-fixed');

  $('#button-edit-unixGroup').show();
  $('#button-save-unixGroup').hide();
  unixGroupInputField.prop('disabled', true);

  updateUnixGroup(oldUnixGroup);
}

function updateUnixGroup(oldUnixGroup, force = false) {
  const unixGroupInputField = $('#unixGroupInput');
  const newUnixGroup = unixGroupInputField.val();

  if (oldUnixGroup === newUnixGroup) {
    return;
  }

  $.ajax({
    url: $.otp.createLink({
      controller: 'projectConfig',
      action: 'updateUnixGroup'
    }),
    dataType: 'json',
    type: 'POST',
    data: {
      force,
      unixGroup: newUnixGroup
    },
    success(result) {
      unixGroupInputField.val(result.unixGroup);
      unixGroupInputField.attr('data-fixed', result.unixGroup);
      $.otp.toaster.showSuccessToast(
        'Update was successful',
        `Unix group has successfully been changed to <b>${result.unixGroup}</b>.`
      );
    },
    error(error) {
      if (error && error.responseJSON && error.responseJSON.message) {
        if (error.status === 409) {
          openConfirmationModal(error.responseJSON.message, () => {
            updateUnixGroup(oldUnixGroup, true);
          }, () => {
            unixGroupInputField.val(oldUnixGroup);
          });
        } else {
          unixGroupInputField.val(oldUnixGroup);
          $.otp.toaster.showErrorToast('Update unix group failed', error.responseJSON.message);
        }
      } else {
        unixGroupInputField.val(oldUnixGroup);
        $.otp.toaster.showErrorToast('Update unix group failed', 'Unknown error during the unix group update.');
      }
    }
  });
}

// eslint-disable-next-line no-unused-vars
function onEditAnalysisDir() {
  $('#button-edit-analysisDir').hide();
  $('#button-save-analysisDir').show();
  $('#analysisDirInput').prop('disabled', false);
}

// eslint-disable-next-line no-unused-vars
function onSaveAnalysisDir() {
  const analysisDirInputField = $('#analysisDirInput');
  const oldAnalysisDir = analysisDirInputField.attr('data-fixed');

  $('#button-edit-analysisDir').show();
  $('#button-save-analysisDir').hide();
  analysisDirInputField.prop('disabled', true);

  updateAnalysisDir(oldAnalysisDir);
}

function updateAnalysisDir(oldAnalysisDir, force = false) {
  const analysisDirInputField = $('#analysisDirInput');
  const newAnalysisDir = analysisDirInputField.val();

  if (oldAnalysisDir === newAnalysisDir) {
    return;
  }

  $.ajax({
    url: $.otp.createLink({
      controller: 'projectConfig',
      action: 'updateAnalysisDir'
    }),
    dataType: 'json',
    type: 'POST',
    data: {
      force,
      analysisDir: newAnalysisDir
    },
    success(result) {
      analysisDirInputField.val(result.analysisDir);
      analysisDirInputField.attr('data-fixed', result.analysisDir);
      $.otp.toaster.showSuccessToast(
        'Update was successful',
        `Analysis directory has successfully been changed to <b>${result.analysisDir}</b>.`
      );
    },
    error(error) {
      if (error && error.responseJSON && error.responseJSON.message) {
        if (error.status === 418) {
          openConfirmationModal(error.responseJSON.message, () => {
            updateAnalysisDir(oldAnalysisDir, true);
          }, () => {
            analysisDirInputField.val(oldAnalysisDir);
          });
        } else {
          analysisDirInputField.val(oldAnalysisDir);
          $.otp.toaster.showErrorToast('Update analysis directory failed', error.responseJSON.message);
        }
      } else {
        analysisDirInputField.val(oldAnalysisDir);
        $.otp.toaster.showErrorToast(
          'Update analysis directory failed',
          'Unknown error during the analysis directory update.'
        );
      }
    }
  });
}

function openConfirmationModal(text, confirmCallback, cancelCallback) {
  const modal = $('#confirmationUserGroupModal');
  const modalBody = $('.modal-body', modal);
  const confirmButton = modal.find('#confirmModal');
  const cancelButton = modal.find('.closeModal');

  confirmButton.unbind('click');
  confirmButton.on('click', () => {
    modal.hide();
    confirmCallback();
  });

  cancelButton.unbind('click');
  cancelButton.on('click', () => {
    modal.hide();

    if (cancelCallback) {
      cancelCallback();
    }
  });

  modalBody.html(text);
  modal.modal('toggle').show();
}
