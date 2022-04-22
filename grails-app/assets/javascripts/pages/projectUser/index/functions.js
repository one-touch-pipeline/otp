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

// eslint-disable-next-line no-unused-vars
function disableNotification() {
  'use strict';

  const otpcheckbox = $('input[type = checkbox][name=accessToOtp]');
  const notificationcheckbox = $('input[type=checkbox][name=receivesNotifications]');

  if (!otpcheckbox.is(':checked')) {
    notificationcheckbox.prop('checked', false);
    notificationcheckbox.value = false;
  }
}

function onToggleAccessToFiles(context) {
  'use strict';

  const modalText = $('input:hidden[name=confirmationTextHtml]').val();

  if (modalText) {
    openConfirmationModal(modalText, () => {
      postFileAccessChange(context);
    });
  } else {
    postFileAccessChange(context);
  }

  hideLabelAndShowEditor(context);
}

function postFileAccessChange(context) {
  'use strict';

  const container = $(context).closest('.modal-editor-switch');
  const orgVal = $('input:hidden[name=hasFileAccess]', container).val();
  const invVal = (orgVal === 'true' ? 'false' : 'true');

  $.ajax({
    url: $('input:hidden[name=target]', container).val(),
    dataType: 'json',
    type: 'POST',
    data: { value: invVal },
    success(response) {
      if (response.success) {
        $.each(['label', 'editor'], function () {
          $(`.modal-editor-switch-${this} span`, container)
            .removeClass(`icon-${orgVal}`).addClass(`icon-${invVal}`);
        });
        if (response.tooltip) {
          $('.modal-editor-switch-label', container).attr('data-original-title', response.tooltip);
        }
        $('input:hidden[name=hasFileAccess]', container).val(invVal);
        $('input:hidden[name=permissionState]', container).val(response.permissionState);
      } else {
        $.otp.toaster.showErrorToast('Saving failed.', response.error);
      }
    },
    error(jqXHR, textStatus, errorThrown) {
      $.otp.toaster.showErrorToast(`${textStatus} occurred while processing the data.`, errorThrown);
    },
    complete() {
      hideEditorAndShowLabel(context);
    }
  });
}

// eslint-disable-next-line no-unused-vars
function onFileAccessEditClick(context) {
  'use strict';

  const outerContainer = $(context).closest('.modal-editor-switch');
  const permissionState = $('input:hidden[name=permissionState]', outerContainer).val();
  const modalText = `A change has been made recently and the admin was notified.
<br />Are you really sure that you want to change it again?`;

  if (permissionState === 'PENDING_DENIAL' ||
        permissionState === 'PENDING_APPROVAL' ||
        permissionState === 'pendingDenial' ||
        permissionState === 'pendingApproval') {
    openConfirmationModal(modalText, () => {
      onToggleAccessToFiles(context);
    });
  } else {
    hideLabelAndShowEditor(context);
  }
}

function openConfirmationModal(text, confirmCallback, cancelCallback) {
  'use strict';

  const modal = $('#confirmationModal');
  const modalBody = $('.modal-body', modal);
  const confirmButton = modal.find('#confirmModal');
  const cancelButtons = modal.find('.closeModal');

  confirmButton.unbind('click');
  confirmButton.on('click', () => {
    modal.hide();
    confirmCallback();
  });

  cancelButtons.unbind('click');
  cancelButtons.on('click', () => {
    modal.hide();

    if (cancelCallback) {
      cancelCallback();
    }
  });

  modalBody.html(text);
  modal.modal('toggle').show();
}

function hideLabelAndShowEditor(context) {
  'use strict';

  const outerContainer = $(context).closest('.modal-editor-switch');
  $('.modal-editor-switch-editor', outerContainer).show();
  $('.modal-editor-switch-label', outerContainer).hide();
}

function hideEditorAndShowLabel(context) {
  'use strict';

  const outerContainer = $(context).closest('.modal-editor-switch');
  $('.modal-editor-switch-editor', outerContainer).hide();
  $('.modal-editor-switch-label', outerContainer).show();
}

$(() => {
  'use strict';

  $('input[type=radio][name=addViaLdap]').on('change', function () {
    const ldapUserChecked = (this.value === 'true');
    $('.inputField.ldapUser').prop('disabled', !ldapUserChecked);
    $('.inputField.nonLdapUser').prop('disabled', ldapUserChecked);
  }).filter(':checked').trigger('change');

  $('#listEmails').on('click', function () {
    const project = $(this).data('project');
    const emails = $(this).data('emails');
    prompt(`Emails for ${project}`, emails);
  });

  $('#add-button').on('click', function () {
    const modalText = $('input:hidden[name=confirmationTextHtml]').val();

    // This is needed to prevent multiple submits
    this.disabled = true;

    if (modalText) {
      openConfirmationModal(modalText, () => {
        $(this).parents('form').submit();
      }, () => {
        this.disabled = false;
      });
    } else {
      this.disabled = false;
      $(this).parents('form').submit();
    }
  });

  $('div.submit-container button.changeProjectAccess').on('click', function () {
    const container = $(this).parent();
    const postUrl = $('input:hidden[name=changeProjectAccessButton]', container).val();
    const modalText = $('button[data-confirmation]', container).attr('data-confirmation');

    if (modalText) {
      openConfirmationModal(modalText, () => {
        submitChangeProjectAccess(postUrl);
      });
    } else {
      submitChangeProjectAccess(postUrl);
    }
  });
});

function submitChangeProjectAccess(postUrl) {
  'use strict';

  $.ajax({
    url: postUrl,
    dataType: 'json',
    type: 'POST',
    success(data) {
      if (data.success) {
        window.location.reload();
      } else {
        $.otp.toaster.showErrorToast('Saving failed.', data.error);
      }
    },
    error(jqXHR, textStatus, errorThrown) {
      $.otp.toaster.showErrorToast(`${textStatus} occurred while processing the data.`, errorThrown);
    }
  });
}

$(document).ready(() => {
  'use strict';

  $('.loaded-content').show();
  $('.loader').hide();
  $('[data-toggle="tooltip"]').tooltip();
  $('select.use-select-2').select2({
    allowClear: true,
    theme: 'bootstrap4'
  });
});
