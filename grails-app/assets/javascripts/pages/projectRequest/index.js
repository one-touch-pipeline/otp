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
        url: $.otp.createLink({ controller: 'projectRequest', action: 'getAdditionalFields' }),
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
            htmlContent += `<div class="form-group row">
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
                                <${abstractField.fieldType} id="additionalFieldValue[${abstractField.id}]"
                                              class="form-control"
                                              type="${abstractField.inputType}"
                                              name="additionalFieldValue[${abstractField.id}]"
                                              value="${value}"
                                              ${(abstractField.required === 'true') ? 'required="required"' : ''}/>
                                </div>       
                            </div>`;
          });
          abstractFieldContainer.html(htmlContent);
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

  // if the PI role is selected, manage users should be checked and disabled
  $('.project-role-select').on('change', (e) => {
    const manageUsersBox = $(e.target).parent().parent().parent()
      .find('.set-for-authority');
    manageUsersBox.prop('disabled', false);
    $(e.target).find('option:selected').each(function () {
      if (this.text === 'PI') {
        manageUsersBox.prop('checked', true);
        manageUsersBox.prop('disabled', true);
      }
    });
  }).trigger('change');
});
