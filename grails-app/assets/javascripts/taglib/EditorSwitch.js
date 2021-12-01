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
  const success = function (title = 'Operation completed', message = 'The change was successful.') {
    if ($.otp.toaster) {
      $.otp.toaster.showSuccessToast(title, message);
    } else {
      $.otp.infoMessage(title + ': ' + message);
    }
  };

  const failure = function (title = 'Unknown error', message = 'An unknown error occurred.') {
    if ($.otp.toaster) {
      $.otp.toaster.showErrorToast(title, message);
    } else {
      $.otp.warningMessage(title + '. ' + message);
    }
  };

  $('td.add-table-buttons button.add').on('click', function (event) {
    'use strict';

    event.preventDefault();
    const p = $(this).parent();
    $('.save', p).show();
    $('.cancel', p).show();
    $('.add', p).hide();
    $('.add-table-fields', p.parent().parent()).show();
  });

  $('td.add-table-buttons button.cancel').on('click', function (event) {
    'use strict';

    event.preventDefault();
    const p = $(this).parent();
    $('.save', p).hide();
    $('.cancel', p).hide();
    $('.add', p).show();
    $('.add-table-fields', p.parent().parent()).hide();
  });

  $('tr.edit-table-buttons button.button-edit').on('click', function (event) {
    'use strict';

    event.preventDefault();
    const p = $(this).parent();
    $('.save', p).show();
    $('.cancel', p).show();
    $('.button-edit', p).hide();
    $('.edit-fields', p.parent()).show();
    $('.show-fields', p.parent()).hide();
  });

  $('tr.edit-table-buttons button.cancel').on('click', function (event) {
    'use strict';

    event.preventDefault();
    const p = $(this).parent();
    $('.save', p).hide();
    $('.cancel', p).hide();
    $('.button-edit', p).show();
    $('.edit-fields', p.parent()).hide();
    $('.show-fields', p.parent()).show();
  });

  $.otp.editorswitch = {};
  $('div.edit-switch .edit-switch-label button.js-edit').on('click', function () { outerContainerSwitch($(this)); });

  $('div.edit-switch .edit-switch-label button.js-remove').on('click', function () { outerContainerSwitch($(this)); });

  function outerContainerSwitch(that) {
    'use strict';

    const outerContainer = that.parent().parent();
    $('.edit-switch-editor', outerContainer).show();
    $('.edit-switch-label', outerContainer).hide();
  }

  // eslint-disable-next-line max-len
  $('div.edit-switch-text .edit-switch-editor button.save, div.edit-switch-integer .edit-switch-editor button.save').on('click', function () {
    'use strict';

    let container; let outerContainer; let
      inputField;
    container = $(this).parent();
    outerContainer = container.parent();
    inputField = $('input[name=value]', container);
    if (!inputField[0].validity.valid) {
      window.alert('The input is not valid. Please provide a valid input value.');
      return;
    }
    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: { value: inputField.val() },
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          $('p.edit-switch-label span', outerContainer).text($('input[name=value]', container).val());
        } else {
          failure('Data could not be stored', data.error);
          $('input[name=value]', container).val($('p.edit-switch-label span', outerContainer).text());
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
        $('input[name=value]', container).val($('p.edit-switch-label span', outerContainer).text());
      }
    });
    $('p.edit-switch-editor', outerContainer).hide();
    $('p.edit-switch-label', outerContainer).show();
  });

  $('div.edit-switch-text-area p.edit-switch-editor button.save').on('click', function () {
    'use strict';

    let container; let
      outerContainer;
    container = $(this).parent();
    outerContainer = container.parent();
    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: { value: $('textarea', container).val() },
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          $('p.edit-switch-label span', outerContainer).text($('textarea', container).val());
          if (data.updateMap) {
            for (const key in data.updateMap) {
              if ({}.hasOwnProperty.call(data.updateMap, key)) {
                const el = $('.' + key, outerContainer);
                el.text(data.updateMap[key]);
              }
            }
          }
        } else {
          failure('Data could not be stored', data.error);
          $('textarea', container).val($('p.edit-switch-label span', outerContainer).text());
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
        $('textarea', container).val($('p.edit-switch-label span', outerContainer).text());
      }
    });
    $('p.edit-switch-editor', outerContainer).hide();
    $('p.edit-switch-label', outerContainer).show();
  });

  $('div.edit-switch-url p.edit-switch-editor button.save').on('click', function () {
    'use strict';

    let container; let
      outerContainer;
    container = $(this).parent();
    outerContainer = container.parent();
    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: { value: $('input:text[name=value]', container).val() },
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          $('p.edit-switch-label span', outerContainer).text($('input:text[name=value]', container).val());
        } else {
          failure('Data could not be stored', data.error);
          $('input:text[name=value]', container).val($('p.edit-switch-label span', outerContainer).text());
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Data could not be stored', jqXHR.responseJSON.message);
        } else {
          failure('Data could not be stored', 'Reason: ' + errorThrown);
        }
      }
    });
    $('p.edit-switch-editor', outerContainer).hide();
    $('p.edit-switch-label', outerContainer).show();
  });

  $('div.edit-switch-drop-down p.edit-switch-editor button.delete').on('click', function () { deleteRole($(this)); });

  function deleteRole(that) {
    'use strict';

    let container; let outerContainer; let
      outerOuterContainer;
    container = that.parent();
    outerContainer = container.parent();
    outerOuterContainer = outerContainer.parent();

    const confirmationText = $('button[data-confirmation]', container).attr('data-confirmation');
    if (confirmationText) {
      const confirmed = confirm(confirmationText);
      if (!confirmed) {
        return;
      }
    }
    $.ajax({
      url: $('input:hidden[name=targetDelete]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: { value: $('input:hidden[name=targetDeleteValue]', container).attr('value') },
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          $(outerContainer).remove();
          const submitContainer = $('div[class=submit-container]', outerOuterContainer);
          const selectRoles = $('select[name=newRoles]', submitContainer);
          selectRoles.append('<option value=' + data.currentRole + '>' + data.currentRole + '</option>');
        } else {
          failure('Data could not be stored', data.error);
          $('p.edit-switch-editor', outerContainer).hide();
          $('p.edit-switch-label', outerContainer).show();
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
        $('p.edit-switch-editor', outerContainer).hide();
        $('p.edit-switch-label', outerContainer).show();
      }
    });
  }

  $('div.submit-container button.addRole').on('click', function () {
    'use strict';

    let container; let
      outerContainer;
    container = $(this).parent();
    outerContainer = container.parent();

    const selectedNewRoles = [];
    const selectNewRoles = $('select[name=newRoles]', container).children();
    for (let i = 0; i < selectNewRoles.length; i++) {
      if (selectNewRoles[i].selected) {
        selectedNewRoles.push(selectNewRoles[i].value);
      }
    }

    if (selectedNewRoles.length !== 0) {
      const confirmationText = $('button[data-confirmation]', container).attr('data-confirmation');
      if (confirmationText) {
        const confirmed = confirm(confirmationText);
        if (confirmed === false) {
          return;
        }
      }
    }

    $.ajax({
      url: $('input:hidden[name=targetAddRole]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: { value: JSON.stringify(selectedNewRoles) },
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          // Update drop down menue
          for (let i = 0; i < selectNewRoles.length; i++) {
            for (let j = 0; j < data.currentProjectRole.length; j++) {
              if (selectNewRoles[i].value === data.currentProjectRole[j]) {
                $(selectNewRoles[i]).remove();
              }
            }
          }
          // Update Role-column
          for (let j = 0; j < data.newProjectRolesNodes.length; j++) {
            outerContainer.prepend(data.newProjectRolesNodes[j]);
            // update event listener for added object only
            $(outerContainer.children()[0]).find('button.js-remove').on('click', function () {
              outerContainerSwitch($(this));
            });
            $(outerContainer.children()[0]).find('button.delete').on('click', function () {
              deleteRole($(this));
            });
            $(outerContainer.children()[0]).find('button.cancel').on('click', function () {
              cancelAddRoleCommand($(this));
            });
          }
        } else {
          failure('Data could not be stored', data.error);
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
      }
    });
  });

  $('div.edit-switch-drop-down p.edit-switch-editor button.save').on('click', function () { saveRole($(this)); });

  function saveRole(that) {
    'use strict';

    let container; let
      outerContainer;
    container = that.parent();
    outerContainer = container.parent();

    const confirmationText = $('button[data-confirmation]', container).attr('data-confirmation');
    if (confirmationText) {
      const confirmed = confirm(confirmationText);
      if (confirmed === false) {
        return;
      }
    }
    const successCallback = $(that).attr('data-success-handler');

    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: { value: $('select option:selected', container).attr('value') },
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          $('p.edit-switch-label span', outerContainer).text($('select option:selected', container).text());
          if (successCallback) {
            window[successCallback](container, data.additionalData);
          }
        } else {
          failure('Data could not be stored', data.error);
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
      }
    });
    $('p.edit-switch-editor', outerContainer).hide();
    $('p.edit-switch-label', outerContainer).show();
  }

  $('div.edit-switch-multi-drop-down p.edit-switch-editor button.save').on('click', function () {
    saveMultiDropDownOptions($(this));
  });

  /**
     * Save data of multi drop down editor switch.
     */
  function saveMultiDropDownOptions(that) {
    'use strict';

    let container; let
      outerContainer;
    container = that.parent();
    outerContainer = container.parent();

    const confirmationText = $('button[data-confirmation]', container).attr('data-confirmation');
    if (confirmationText) {
      const confirmed = confirm(confirmationText);
      if (!confirmed) {
        return;
      }
    }

    const values = [];
    const selectOptions = container.find('select option:selected').toArray();

    selectOptions.forEach((option) => {
      values.push(option.value);
    });

    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      contentType: 'application/x-www-form-urlencoded',
      dataType: '*',
      type: 'POST',
      data: {
        selectedValues: JSON.stringify(values)
      },
      success() {
        success('Success', 'Data stored successfully');
        $('p.edit-switch-label span', outerContainer).text($('select option:selected', container).text());
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
      }
    });
    $('p.edit-switch-editor', outerContainer).hide();
    $('p.edit-switch-label', outerContainer).show();
  }

  $('div.edit-switch-date p.edit-switch-editor button.save').on('click', function () {
    'use strict';

    let container; let
      outerContainer;
    container = $(this).parent();
    outerContainer = container.parent();
    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: { value: $('input[type="date"]', container).val() },
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          $('p.edit-switch-label span', outerContainer).text($('input[type="date"]', container).val());
        } else {
          failure('Data could not be stored', data.error);
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
      }
    });
    $('p.edit-switch-editor', outerContainer).hide();
    $('p.edit-switch-label', outerContainer).show();
  });

  $('div.edit-switch-checkboxes p.edit-switch-editor button.save').on('click', function () {
    'use strict';

    let container; let outerContainer; let
      value;
    container = $(this).parent();
    outerContainer = container.parent();
    value = {};
    $('input:checked', container).each(function (index) {
      value['value[' + index + ']'] = this.name;
    });
    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: value,
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          const names = [];
          $('input:checkbox', container).each(function () {
            if (this.checked) {
              names.push(this.name);
              $(this).data('checked', true);
            } else {
              $(this).data('checked', false);
            }
          });
          $('p.edit-switch-label span', outerContainer).text(names.join(', ') || '(None selected)');
        } else {
          failure('Data could not be stored', data.error);
          resetCheckboxes(container);
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
        resetCheckboxes(container);
      }
    });
    $('p.edit-switch-editor', outerContainer).hide();
    $('p.edit-switch-label', outerContainer).show();
  });

  function resetCheckboxes(container) {
    $('input:checkbox', container).each(function () {
      this.checked = $(this).data('checked');
    });
  }

  $('div.edit-switch-new-value p.edit-switch-editor button.save').on('click', function () {
    'use strict';

    let container; let
      outerContainer;
    container = $(this).parent();
    outerContainer = container.parent();
    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: { value: $('select option:selected', container).text() },
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          $('p.edit-switch-label span', outerContainer).text($('input:text[name=value]', container).val());
        } else {
          failure('Data could not be stored', data.error);
          $('input:text[name=value]', container).val($('p.edit-switch-label span', outerContainer).text());
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
        $('input:text[name=value]', container).val($('p.edit-switch-label span', outerContainer).text());
      }
    });
    $('p.edit-switch-editor', outerContainer).hide();
    $('p.edit-switch-label', outerContainer).show();
    window.setTimeout(() => { location.reload(); }, 300); // reloads page after 0.3 seconds
  });

  $('div.edit-switch-new-free-text-value p.edit-switch-editor button.save').on('click', function () {
    'use strict';

    let container; let
      outerContainer;
    container = $(this).parent();
    outerContainer = container.parent();
    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: { value: $('input:text[name=value]', container).val() },
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          $('p.edit-switch-label span', outerContainer).text($('input:text[name=value]', container).val());
        } else {
          failure('Data could not be stored', data.error);
          $('input:text[name=value]', container).val($('p.edit-switch-label span', outerContainer).text());
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
        $('input:text[name=value]', container).val($('p.edit-switch-label span', outerContainer).text());
      }
    });
    $('p.edit-switch-editor', outerContainer).hide();
    $('p.edit-switch-label', outerContainer).show();
    window.setTimeout(() => { location.reload(); }, 300); // reloads page after 0.3 seconds
  });

  $('div.edit-switch-new-free-text-values span.edit-switch-editor button.save').on('click', function () {
    'use strict';

    let container; let outerContainer; const
      data = {};
    container = $(this).parent();
    outerContainer = container.parent();
    $('input:text', container).each(function () {
      data[this.name] = $(this).val();
    });
    $('input:checked', container).each(function () {
      data[this.name] = $(this).is(':checked');
    });
    $('select', container).each(function () {
      data[this.name] = $(this).val();
    });
    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data,
      success(response) {
        if (response.success) {
          success('Success', 'Data stored successfully');
          window.setTimeout(() => { location.reload(); }, 300);
        } else {
          failure('Data could not be stored', response.error);
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
      }
    });
    $('span.edit-switch-editor', outerContainer).hide();
    $('span.edit-switch-label', outerContainer).show();
  });

  $('div.edit-switch-toggle p.edit-switch-editor button.toggle').on('click', function () {
    'use strict';

    let container; let outerContainer; let orgVal; let
      invVal;
    container = $(this).parent();
    outerContainer = container.parent();

    const confirmationText = $('button[data-confirmation]', container).attr('data-confirmation');
    if (confirmationText) {
      const confirmed = confirm(confirmationText);
      if (confirmed === false) {
        return;
      }
    }

    orgVal = $('input:hidden[name=value]', container).val();
    invVal = (orgVal === 'true' ? 'false' : 'true');
    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: { value: invVal },
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          $.each(['label', 'editor'], function () {
            $('p.edit-switch-' + this + ' span', outerContainer)
              .removeClass('icon-' + orgVal).addClass('icon-' + invVal);
          });
          if (data.tooltip) {
            $('p.edit-switch-label', outerContainer)[0].title = data.tooltip;
          }
          $('input:hidden[name=value]', container).attr('value', invVal);
          const pageReload = $('button[data-pageReload]', container).attr('data-pageReload');
          if (pageReload === 'true') {
            window.setTimeout(() => { location.reload(); }, 100); // reloads page after 0.5 seconds
          }
        } else {
          failure('Data could not be stored', data.error);
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
      }
    });
    $('p.edit-switch-editor', outerContainer).hide();
    $('p.edit-switch-label', outerContainer).show();
  });

  $('div.edit-switch .edit-switch-editor button.cancel').on('click', function () { cancelAddRoleCommand($(this)); });

  function cancelAddRoleCommand(that) {
    'use strict';

    const outerContainer = that.parent().parent();
    $('.edit-switch-editor', outerContainer).hide();
    $('.edit-switch-label', outerContainer).show();
    $('.edit-switch-input', $(this).parent()).val($('p.edit-switch-label span', outerContainer).text());
    if (outerContainer.hasClass('edit-switch-checkboxes')) {
      resetCheckboxes(outerContainer);
    }
  }

  $('div.multi-edit-switch .edit-switch-label button.js-edit').on('click', function () {
    'use strict';

    const outerContainer = $(this).parent().parent();
    const multiInputField = $('.multi-input-field', outerContainer);
    const template = $('.inputTemplate', outerContainer);
    const dataValues = multiInputField.data('values');
    let first = true;
    for (const key in dataValues) {
      if ({}.hasOwnProperty.call(dataValues, key)) {
        const field = $('<div class="field"></div>');
        const clonedField = template.clone();
        clonedField.removeClass('inputTemplate');
        clonedField.val(dataValues[key]);
        field.append(clonedField);
        if (first) {
          field.append(' <button class="add-field">+</button>');
          first = false;
        } else {
          field.append(' <button class="remove-field">-</button>');
        }
        $.otp.applySelect2($('select.use-select-2', clonedField));
        multiInputField.append(field);
      }
    }
    $('.edit-switch-editor', outerContainer).show();
    $('.edit-switch-label', outerContainer).hide();
  });

  $('div.multi-edit-switch .edit-switch-editor button.cancel').on('click', function () {
    'use strict';

    const outerContainer = $(this).parent().parent();
    $('.edit-switch-editor', outerContainer).hide();
    $('.edit-switch-label', outerContainer).show();
    $('.multi-input-field .field', outerContainer).remove();
  });

  $('div.edit-switch-multi-input .edit-switch-editor button.save').on('click', function () {
    'use strict';

    let container; let outerContainer; const dataValues = {}; let i = 0;
    let displayValue = '';
    container = $(this).parent();
    outerContainer = container.parent();
    const multiInputField = $('.multi-input-field', outerContainer);
    let valid = true;
    $('input', multiInputField).each(function () {
      if (!($(this)[0].validity.valid)) {
        window.alert('The input is not valid. Please provide a valid input value.');
        valid = false;
        return;
      }
      dataValues['value[' + i + ']'] = $(this).val();
      displayValue = displayValue + $(this).val() + ', ';
      i += 1;
    });
    if (!valid) {
      return;
    }
    $.ajax({
      url: $('input:hidden[name=target]', container).val(),
      dataType: 'json',
      type: 'POST',
      data: dataValues,
      success(data) {
        if (data.success) {
          success('Success', 'Data stored successfully');
          $('.edit-switch-label span', outerContainer).text(displayValue);
          multiInputField.data('values', dataValues);
        } else {
          failure('Data could not be stored', data.error);
        }
      },
      error(jqXHR, textStatus, errorThrown) {
        if (jqXHR && jqXHR.responseJSON && jqXHR.responseJSON.message) {
          failure('Request failed', jqXHR.responseJSON.message);
        } else {
          failure(textStatus + ' occurred while processing the data', 'Reason: ' + errorThrown);
        }
      }
    });
    $('.edit-switch-editor', outerContainer).hide();
    $('.edit-switch-label', outerContainer).show();
    $('.field', multiInputField).remove();
  });
});
