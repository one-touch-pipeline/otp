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

// eslint-disable-next-line no-unused-vars
function confirmDataTransferDeletion(event) {
  'use strict';

  if (!window.confirm(`Are you sure that you want to permanently
   delete this document and any transfers based on it?`)) {
    event.preventDefault();
    return;
  }
  if (!window.confirm('Are you really sure? This can not be undone!')) {
    event.preventDefault();
  }
}

// eslint-disable-next-line no-unused-vars
function confirmCompleteTransfer(event) {
  'use strict';

  if (!window.confirm(`Are you sure that this transfer is completed?
   (receipt acknowledged? files deleted? disks wiped? etc..)`)) {
    event.preventDefault();
  }
}

/**
 * Updates the visible label of the file input field. It replaces the placeholder with the selected file names.
 *
 * @param fileInputFieldId
 */
// eslint-disable-next-line no-unused-vars
function updateFileNameOfFileInput(fileInputFieldId) {
  'use strict';

  const inputField = $(fileInputFieldId);
  const fileName = Array.from(inputField.prop('files')).map((val) => val.name).reduce((acc, val) => `${acc}, ${val}`);
  inputField.next('.custom-file-label').html(fileName);
}

/**
 * Update the visible label of the file input field.
 * It replaces the names of the selected files with the default placeholder.
 * It is triggered when the upload response from the server is received.
 *
 * @param id to find the correct input field
 */
function updateFileNameOfFileInputToDefaultPlaceholder(id) {
  'use strict';

  const fileInputField = $(`#dtaFileInput-${id}`);
  const defaultPlaceholder = 'Choose one or more files';

  fileInputField.val('');
  fileInputField.next('.custom-file-label').html(defaultPlaceholder);
}

/**
 * Triggered when a file upload is started. This event is behind the add button of the _uploadFiles_gsp template.
 * It is generic and used for file uploads to the DTAs and also for the transfers.
 *
 * @param submitBtn, submit button from which the event is triggered
 * @param id to find the correct position of the file list to add the new file names
 * @param action for the ajax query target
 */
// eslint-disable-next-line no-unused-vars
function onUploadFiles(submitBtn, id, action) {
  'use strict';

  const uploadForm = submitBtn.closest('form');
  const data = new FormData(uploadForm);
  const loadingContent = $(submitBtn).children('.loading-content');
  const actionContent = $(submitBtn).children('.action-content');

  if (!data.get('files')) {
    $.otp.toaster.showErrorToast('Upload Failed', 'No file has been selected. Please select one or more files.');
    return;
  }

  loadingContent.show();
  actionContent.hide();
  $(submitBtn).prop('disabled', true);

  $.ajax({
    url: $.otp.createLink({
      controller: 'dataTransfer',
      action
    }),
    data,
    cache: false,
    contentType: false,
    processData: false,
    method: 'POST',
    type: 'POST',
    success: (updatedDocuments) => {
      if (action === 'addFilesToTransferAgreement') {
        rerenderDtaFiles(updatedDocuments, id);
      } else if (action === 'addFilesToTransfer') {
        rerenderDataTransferFiles(updatedDocuments, id);
      }
      $.otp.toaster.showSuccessToast('Success', 'Files are added.');
      $(submitBtn).prop('disabled', false);
      loadingContent.hide();
      actionContent.show();
      updateFileNameOfFileInputToDefaultPlaceholder(id);
    },
    error: (error) => {
      if (error && error.responseJSON && error.responseJSON.message) {
        $.otp.toaster.showErrorToast('Upload Failed', error.responseJSON.message);
      } else {
        $.otp.toaster.showErrorToast('Upload Failed', 'Unknown error during the upload occurred.');
      }
      $(submitBtn).prop('disabled', false);
      loadingContent.hide();
      actionContent.show();
      updateFileNameOfFileInputToDefaultPlaceholder(id);
    }
  });
}

/**
 * Add the new (just uploaded) DTA files into the file list.
 *
 * @param dtaDocuments list of the file objects to add
 * @param domPositionId position in the DOM of the file list
 */
function rerenderDtaFiles(dtaDocuments, domPositionId) {
  'use strict';

  const dtaFilesList = $(`#dtaDocuments-${domPositionId}`);

  dtaFilesList.html(dtaDocuments.map((document) => `<li>
    <a href="${$.otp.createLink({
    controller: 'dataTransfer',
    action: 'downloadDataTransferAgreementDocument',
    parameters: { 'dataTransferAgreementDocument.id': document.id }
  })}">${document.fileName}</a></li>`).reduce((acc, val) => acc + val));
}

/**
 * Add the new (just uploaded) Transfer files into the file list.
 *
 * @param transferDocuments list of the file objects to add
 * @param domPositionId position in the DOM of the file list
 */
function rerenderDataTransferFiles(transferDocuments, domPositionId) {
  'use strict';

  const dtaFilesList = $(`#transferDocuments-${domPositionId}`);

  dtaFilesList.html(transferDocuments.map((document) => `<li><a href="${$.otp.createLink({
    controller: 'dataTransfer',
    action: 'downloadDataTransferDocument',
    parameters: { 'dataTransferDocument.id': document.id }
  })}">${document.fileName}</a></li>`).reduce((acc, val) => acc + val));
}
