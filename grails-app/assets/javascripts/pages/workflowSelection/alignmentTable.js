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

$(() => {
  'use strict';

  $('select.use-select-2').select2({
    theme: 'bootstrap4',
    width: '100%',
    minimumResultsForSearch: 10
  });

  const alignmentTable = $('#alignment').DataTable({ paging: false });
  let currentAjaxCall;

  const workflowSelect = $('#alignment-workflow-select');
  const versionSelect = $('#alignment-version-select');
  const seqTypeSelect = $('#alignment-seq-type-select');
  const speciesSelect = $('#alignment-species-select');
  const refGenomeSelect = $('#alignment-ref-genome-select');
  let removeButtons;
  const addButton = $('.add-alignment-config-btn');

  function setPossibleOptions(selectElement, idList) {
    const valueBefore = selectElement.val();
    selectElement.find('option').each((index, option) => {
      const el = $(option);
      const value = el.val();
      if (el && value && +value) {
        el.prop('disabled', !idList.includes(+value));
      }
    });
    if (selectElement.val() === null || JSON.stringify(selectElement.val()) === '[]') {
      selectElement.val('');
    }
    const valueAfter = selectElement.val();
    if (JSON.stringify(valueBefore) !== JSON.stringify(valueAfter)) {
      selectElement.trigger('change');
    }
  }

  function getPossibleOptions() {
    if (currentAjaxCall) {
      currentAjaxCall.abort();
    }
    currentAjaxCall = $.ajax({
      url: $.otp.createLink({
        controller: 'workflowSelection',
        action: 'possibleAlignmentOptions'
      }),
      data: getAlignmentOptionFormData(),
      contentType: false,
      processData: false,
      type: 'POST',
      success(data) {
        setPossibleOptions(workflowSelect, data.workflowIds);
        setPossibleOptions(versionSelect, data.versionIds);
        setPossibleOptions(seqTypeSelect, data.seqTypeIds);
        setPossibleOptions(speciesSelect, data.speciesIds);
        setPossibleOptions(refGenomeSelect, data.refGenomeIds);
      },
      error(jqXHR) {
        if (jqXHR.statusText !== 'abort') {
          $.otp.toaster.showErrorToast('Workflow Configuration', 'Some error occurred.');
        }
      }
    });
  }

  if (speciesSelect.length > 0) {
    getPossibleOptions();
  }

  function clearFormFields() {
    workflowSelect.val('');
    workflowSelect.trigger('change.select2');
    versionSelect.val('');
    versionSelect.trigger('change.select2');
    seqTypeSelect.val('');
    seqTypeSelect.trigger('change.select2');
    speciesSelect.val('');
    speciesSelect.trigger('change.select2');
    refGenomeSelect.val('');
    refGenomeSelect.trigger('change.select2');
    getPossibleOptions();
  }

  workflowSelect.add(versionSelect).add(seqTypeSelect).add(speciesSelect).add(refGenomeSelect)
    .on('change', () => {
      getPossibleOptions();
    });

  function getAlignmentOptionFormData() {
    const formData = new FormData();
    formData.append('workflow.id', workflowSelect.val());
    formData.append('seqType.id', seqTypeSelect.val());
    formData.append('workflowVersion.id', versionSelect.val());
    speciesSelect.val().forEach((species) => {
      formData.append('speciesWithStrains', species);
    });
    formData.append('referenceGenome.id', refGenomeSelect.val());
    return formData;
  }

  function updateTableRowData(data) {
    const rowData = [
      data.workflow.displayName,
      data.seqType.displayName,
      data.version.displayName,
      data.refGenome.displayName,
      data.species.map((specie) => specie.displayName).join(' + '),
      `<button class="btn float-right btn-primary remove-alignment-config-btn"
                          data-ref-genome-selector="${data.refGenSelectorId}"
                          data-version-selector="${data.workflowVersionSelector.id}">Remove Config</button>`
    ];
    let alteredRow;
    const rgId = data.refGenSelectorId;
    const wvId = data.workflowVersionSelector.id;
    const wvPreviousId = data.workflowVersionSelector.previousId;
    let existingButton = $(`button[data-ref-genome-selector="${rgId}"][data-version-selector="${wvId}"]`);
    if (existingButton.length === 0) {
      existingButton = $(`button[data-ref-genome-selector="${rgId}"][data-version-selector="${wvPreviousId}"]`);
    }
    if (existingButton.length > 0) {
      const row = alignmentTable.row(existingButton.parent().parent());
      const oldData = row.data();
      alteredRow = row.data(rowData).draw().node();
      const updatedData = convertDataToDisplayString(row.data(), oldData);

      $.otp.toaster.showSuccessToast(
        'Successfully updated Alignment Configuration',
        `The alignment configuration ${updatedData} was updated.`
      );
    } else {
      const row = alignmentTable.row.add(rowData).draw();
      const dataInfo = convertDataToDisplayString(row.data());
      $.otp.toaster.showSuccessToast(
        'Successfully created Alignment Configuration',
        `The alignment configuration ${dataInfo} was created.`
      );
      alteredRow = row.node();
    }
    $(alteredRow).find('button').on('click', (e) => removeConfiguration(e));
  }

  addButton.on('click', () => {
    disableAddButton();
    $.ajax({
      url: $.otp.createLink({
        controller: 'workflowSelection',
        action: 'saveAlignmentConfiguration'
      }),
      data: getAlignmentOptionFormData(),
      contentType: false,
      processData: false,
      type: 'POST',
      success(data) {
        updateTableRowData(data);
        clearFormFields();
      },
      error(jqXHR, textStatus, errorThrown) {
        let errorMessage = 'Unknown error occurred.';
        if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
          errorMessage = jqXHR.responseJSON.message;
        } else if (errorThrown) {
          errorMessage = errorThrown;
        }
        $.otp.toaster.showErrorToast('Saving Alignment Configuration failed', errorMessage);
      }
    }).always(() => {
      enableAddButton();
    });
  });

  function disableRemoveButtons() {
    removeButtons = $('button.remove-alignment-config-btn:not([disabled])');
    removeButtons.prop('disabled', true);
  }

  function enableRemoveButtons() {
    removeButtons.removeAttr('disabled');
  }

  function disableAddButton() {
    addButton.prop('disabled', true);
  }

  function enableAddButton() {
    addButton.removeAttr('disabled');
  }

  $('.remove-alignment-config-btn').on('click', (e) => {
    removeConfiguration(e);
  });

  function convertDataToDisplayString(newData, oldDataOrUndefined) {
    const oldData = oldDataOrUndefined || newData;
    let displayString = '<ul>';
    const oldDataInfo = oldData.map((oldVal, index) => (oldVal !== newData[index] ? `(previously: ${oldVal})` : ''));
    displayString += `<li>workflow: ${newData[0]} ${oldDataInfo[0]}</li>`;
    displayString += `<li>seqType: ${newData[1]} ${oldDataInfo[1]}</li>`;
    displayString += `<li>workflow version: ${newData[2]} ${oldDataInfo[2]}</li>`;
    displayString += `<li>reference genome: ${newData[3]} ${oldDataInfo[3]}</li>`;
    displayString += `<li>species with strains: ${newData[4]} ${oldDataInfo[4]}</li>`;
    displayString += '</ul>';
    return displayString;
  }

  function removeConfiguration(e) {
    disableRemoveButtons();
    const target = $(e.target);
    const dataSelectors = {
      'workflowVersionSelector.id': +target.data('versionSelector'),
      'referenceGenomeSelector.id': +target.data('refGenomeSelector')
    };
    const dataRow = alignmentTable.row(target.parent().parent());
    const dataInfo = convertDataToDisplayString(dataRow.data());

    $.ajax({
      url: $.otp.createLink({
        controller: 'workflowSelection',
        action: 'deleteAlignmentConfiguration'
      }),
      data: dataSelectors,
      dataType: 'json',
      type: 'POST',
      success() {
        dataRow.remove().draw();
        $.otp.toaster.showSuccessToast(
          'Successfully deleted Alignment Configuration',
          `Deleting the alignment configuration ${dataInfo} was successful.`
        );
      },
      error(jqXHR, textStatus, errorThrown) {
        let errorMessage = 'Unknown error occurred.';
        if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
          errorMessage = jqXHR.responseJSON.message;
        } else if (errorThrown) {
          errorMessage = errorThrown;
        }
        $.otp.toaster.showErrorToast(
          'Deleting Alignment Configuration failed',
          `Failed delete alignment configuration ${dataInfo} ${errorMessage}`
        );
      }
    }).always(() => {
      enableRemoveButtons();
    });
  }
});
