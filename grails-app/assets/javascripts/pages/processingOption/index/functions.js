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
function onObsolete(rowIndex, name, type, specificProject) {
  const inputField = $(`#value-${rowIndex}`);
  const optionNumber = parseInt(rowIndex, 10) + 1;

  $.ajax({
    url: $.otp.createLink({
      controller: 'processingOption',
      action: 'obsolete'
    }),
    dataType: 'json',
    type: 'POST',
    data: {
      optionName: name,
      type,
      specificProject
    },
    success() {
      inputField.val('');
      $.otp.toaster.showSuccessToast(
        `Obsoleted (Option ${optionNumber})`,
        `Option ${optionNumber} has successfully been obsoleted.\n\n(${name}, ${type})`
      );
    },
    error(error) {
      if (error && error.responseJSON && error.responseJSON.message) {
        $.otp.toaster.showErrorToast(
          `Obsoletion failed (Option ${optionNumber})`,
          `${error.responseJSON.message}\n\n(${name}, ${type})`
        );
      } else {
        $.otp.toaster.showErrorToast(
          `Obsoletion failed (Option ${optionNumber})`,
          `Unknown error on obsoletion.\n\n(${name}, ${type})`
        );
      }
    }
  });
}

// eslint-disable-next-line no-unused-vars
function onEdit(rowIndex) {
  $(`#button-save-${rowIndex}`).show();
  $(`#button-edit-${rowIndex}`).hide();
  $(`#value-${rowIndex}`).prop('disabled', false);
}

// eslint-disable-next-line no-unused-vars
function onSave(rowIndex, name, oldValue, type, specificProject) {
  const optionNumber = parseInt(rowIndex, 10) + 1;
  const inputField = $(`#value-${rowIndex}`);
  const newValue = inputField.val();

  $(`#button-save-${rowIndex}`).hide();
  $(`#button-edit-${rowIndex}`).show();
  inputField.prop('disabled', true);

  if (oldValue === newValue) {
    return;
  }

  $.ajax({
    url: $.otp.createLink({
      controller: 'processingOption',
      action: 'update'
    }),
    dataType: 'json',
    type: 'POST',
    data: {
      optionName: name,
      value: newValue,
      type,
      specificProject
    },
    success(result) {
      inputField.val(result.value.value);
      switchErrorColorOnValueCell(rowIndex, result.value.warnColor);
      $.otp.toaster.showSuccessToast(
        `Saved (Option ${optionNumber})`,
        `Option ${optionNumber} has successfully been saved.\n\n (${name}, ${type})`
      );
    },
    error(error) {
      inputField.val(oldValue);
      if (error && error.responseJSON && error.responseJSON.message) {
        $.otp.toaster.showErrorToast(
          `Saving failed (Option ${optionNumber})`,
          `${error.responseJSON.message}\n\n(${name}, ${type})`
        );
      } else {
        $.otp.toaster.showErrorToast(
          `Saving failed (Option ${optionNumber})`,
          `Unknown error during save.\n\n(${name}, ${type})`
        );
      }
    }
  });
}

function switchErrorColorOnValueCell(rowIndex, warnColorType) {
  const valueCell = $(`#value-cell-${rowIndex}`);

  if (warnColorType === 'ERROR') {
    valueCell.addClass('table-danger');
  } else {
    valueCell.removeClass('table-danger');
  }
}
