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

// activate the possibility to provide own input for tag-select items
$(document).ready(() => {
  'use strict';

  $('.tag-select').select2({
    tags: true,
    theme: 'bootstrap4'
  });
});

$(() => {
  'use strict';

  // disable and enable the date picker for storageUntil when defined by a user
  const storageUntil = $('input#storageUntil');
  $('select#storagePeriod').on('change', (e) => {
    if ($(e.target).val() === 'USER_DEFINED') {
      storageUntil.prop('disabled', false);
    } else {
      storageUntil.prop('disabled', true);
      storageUntil.val(' ');
    }
  }).trigger('change');

  // Functions to handle required assignment
  const removeAllStars = (element) => {
    element.html(element.html().replace('*', ''));
  };
  const lastCharIsAStar = (element) => {
    removeAllStars(element);
    element.html(`${element.html()}*`);
  };
  const addRequired = (inputElement, labelElement) => {
    lastCharIsAStar(labelElement);
    inputElement.prop('required', true);
  };
  const removeRequired = (inputElement, labelElement) => {
    removeAllStars(labelElement);
    inputElement.prop('required', false);
  };

  // Function to set the additional Fields. This is called when projectType changes.
  const setAdditionalFields = (projectType, projectRequest) => {
    // first delete all addition Field entries
    // fetch the data with ajax
    if (projectType) {
      $.ajax({
        url: $.otp.createLink({
          controller: 'projectRequest',
          action: 'getAdditionalFields'
        }),
        dataType: 'json',
        type: 'POST',
        data: {
          projectType,
          projectRequest
        },
        success(result) {
          const abstractFieldContainer = $('.abstract-fields-container');
          let htmlContent = '';
          const { abstractFields } = result;
          abstractFields.forEach((abstractField, index) => {
            const tempAbstractValue = $(`#tempAbstractValue_${abstractField.id}`).val();
            const value = tempAbstractValue || abstractField.value;
            htmlContent += `<div class="mb-3 row">
                              <input type="hidden" name="additionalFieldName[${index}]" 
                                     value="${abstractField.name}"
                                     id="additionalFieldName[${index}]"/>
                              <div class="col-sm-2">
                                <label class="col-form-label" 
                                       for="additionalFieldValue[${abstractField.id}]">
                                  ${abstractField.name}${(abstractField.required === 'true') ? '*' : ''}
                                </label>
                                <i class="helper-icon bi bi-question-circle-fill" 
                                   title="${abstractField.descriptionRequest}">
                                </i>
                              </div>        
                              <div class="col-sm-10">
                                <${abstractField.fieldType} id="${abstractField.name.toLowerCase().replace(' ', '')}"
                                              class="form-control"
                                              type="${abstractField.inputType}"
                                              name="additionalFieldValue[${abstractField.id}]"
                                              value="${value}"
                                              ${(abstractField.required === 'true') ? 'required="required"' : ''}/>
                                </div>       
                            </div>`;
          });
          abstractFieldContainer.html(htmlContent);
          addEventHandlerToOU();
        },
        error(error) {
          if (error && error.responseJSON && error.responseJSON.message) {
            $.otp.toaster.showErrorToast('Fetching additional fields failed', error.responseJSON.message);
          } else {
            $.otp.toaster.showErrorToast('Fetching additional fields failed', 'Unknown error during fetching.');
          }
        }
      });
    }
  };

  // Approximate Number of Samples and SeqTypes is only required when Project Type "Sequencing" is selected
  // Also check whether the addition abstractFields are required
  const numberSamples = $('input#approxNoOfSamples');
  const numberSamplesLabel = $('label#approxNoOfSamplesLabel');

  const seqTypes = $('select#seqTypes');
  const seqTypesLabel = $('label#seqTypesLabel');

  const speciesWithStrain = $('select#speciesWithStrainList');
  const speciesWithStrainLabel = $('label#speciesWithStrainLabel');

  const numberOfAdditionFields = $('#numberOfAdditionFields').val();
  const additionFieldRequiredForSequencing = [];
  const additionFieldRequiredForUserManagement = [];
  for (let i = 0; i < numberOfAdditionFields; i++) {
    additionFieldRequiredForSequencing.add($(`additionalField[${i}].requiredForUserManagement`));
    additionFieldRequiredForUserManagement.add($(`additionalField[${i}].requiredForSequencing`));
  }

  $('select#projectType').on('change', (e) => {
    setAdditionalFields($(e.target).val(), $('#projectRequestId').val());
    if ($(e.target).val() === 'SEQUENCING') {
      addRequired(speciesWithStrain, speciesWithStrainLabel);
      addRequired(numberSamples, numberSamplesLabel);
      addRequired(seqTypes, seqTypesLabel);
      additionFieldRequiredForSequencing.forEach((currentValue, index) => {
        if (currentValue) {
          addRequired($(`additionalFieldValue[${index}]`), $(`additionalFieldValueLabel[${index}]`));
        } else {
          removeRequired($(`additionalFieldValue[${index}]`), $(`additionalFieldValueLabel[${index}]`));
        }
      });
    } else if ($(e.target).val() === 'USER_MANAGEMENT') {
      removeRequired(speciesWithStrain, speciesWithStrainLabel);
      removeRequired(numberSamples, numberSamplesLabel);
      removeRequired(seqTypes, seqTypesLabel);
      additionFieldRequiredForUserManagement.forEach((currentValue, index) => {
        if (currentValue) {
          addRequired($(`additionalFieldValue[${index}]`), $(`additionalFieldValueLabel[${index}]`));
        } else {
          removeRequired($(`additionalFieldValue[${index}]`), $(`additionalFieldValueLabel[${index}]`));
        }
      });
    }
  }).trigger('change');

  // function to set the list of possible PIs information
  const setPossiblePIs = (department) => {
    const piSelectors = $('.pi-selector');
    piSelectors.empty();
    // fetch the data with ajax
    if (department) {
      $.ajax({
        url: $.otp.createLink({
          controller: 'projectRequest',
          action: 'getPIs'
        }),
        dataType: 'json',
        type: 'GET',
        data: {
          department
        },
        success(data) {
          if (!data) {
            return;
          }
          piSelectors.append($('<option disabled selected></option>').val('')
            .html('Please choose a deputy PI from the list below'));

          if (data.deputies) {
            data.deputies.forEach((deputy) => {
              piSelectors.append($('<option></option>').val(deputy.username).html(deputy.displayName));
            });
          }

          piSelectors.each((index, piSelector) => {
            const selectElement = $(piSelector);
            const showHeadCheckbox = selectElement.parent().parent().find('.show-head-checkbox');
            const initialUsername = selectElement.parent().find('.initial-pi-user').val();

            // Automatically enable show Heads, when there are no deputies
            // or currently selected user is not deputy but head
            const initialUsernameIsHead = data.departmentHeads.some((head) => head.username === initialUsername);
            const initialUsernameIsDeputy = data.deputies.some((deputy) => deputy.username === initialUsername);

            if ((data.deputies && data.deputies.length === 0) ||
              (!initialUsernameIsDeputy && initialUsernameIsHead)) {
              showHeadCheckbox.prop('checked', true);
            }
            const showHeads = showHeadCheckbox.prop('checked');

            data.departmentHeads.forEach((head) => {
              $(piSelector).append($(`<option class="head" ${showHeads ? '' : 'hidden'}></option>`).val(head.username)
                .html(head.displayName));
            });

            selectElement.val(initialUsername);
          });
        },
        error(error) {
          if (error && error.responseJSON && error.responseJSON.message) {
            $.otp.toaster.showErrorToast('Fetching PIs failed', error.responseJSON.message);
          } else {
            $.otp.toaster.showErrorToast('Fetching PIs failed', 'Unknown error during fetching.');
          }
        }
      });
    }
  };

  // Should always add pi role, when its deleted or changed, such that it cant be deselected
  $('.pi-role-select').on('change', (e) => {
    const target = $(e.target);

    // Search value of PI option
    const piValue = Object.values(target.find('option')).find((option) => option.label === 'PI').value;

    // Add PI option to selected options
    const values = target.val();
    values.push(piValue);
    target.val(values).trigger('change.select2');
  });

  $('.show-head-checkbox').on('change', (e) => {
    const target = $(e.target);
    const showHead = target.prop('checked');
    const piOptions = target.parent().parent().find('.pi-selector').find('option.head');

    if (showHead) {
      piOptions.removeAttr('hidden');
    } else {
      piOptions.prop('hidden', true);
    }
  });

  function addEventHandlerToOU() {
    $('#organizationalunit').on('change', (e) => {
      setPossiblePIs($(e.target).val());
    });

    $('#organizationalunit').on('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        setPossiblePIs($(e.target).val());
      }
    });
  }
});
