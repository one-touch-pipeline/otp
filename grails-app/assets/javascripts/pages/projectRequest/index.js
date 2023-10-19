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
          const piList = [];

          $.each(data, (key, value) => {
            piList.push({ key, value });
          });
          piSelectors.append($('<option disabled selected></option>').val('')
            .html('Please choose a deputy PI from the list below'));
          for (let index = 0; index < piList.length; index++) {
            // eslint-disable-next-line max-len
            piSelectors.append($('<option></option>').val(piList[index].key).html(`${piList[index].value} (${piList[index].key})`));
          }
          const forms = $('.piUsers');
          for (let newIndex = 0; newIndex < forms.length - 1; newIndex++) {
            const hiddenFieldName = `piUsers${newIndex}_username_hiddenField`;
            const hiddenUsername = $(`#${hiddenFieldName}`);
            hiddenUsername[0].parentElement.children[1].value = hiddenUsername.val();
          }
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

  function addEventHandlerToOU() {
    $('#organizationalunit').on('change', (e) => {
      setPossiblePIs($(e.target).val());
    }).trigger('change');
  }
});
