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

/**
 * A handler to change the shown version to Not configured after one Fastqc workflow is configured
 * This js avoids to reload the backend
 */
workflowSelectionUpdateSuccessHandler = function (container) {
  'use strict';

  const rowIdx = container.parent().parent().parent().index('tr') === 2 ? 1 : 2;
  const row = container.parent().parent().parent().parent()
    .parent()
    .find('tr')
    .eq(rowIdx);

  // just update the text to match the value from backend, otherwise you have to refresh the page
  $('.edit-switch-label span', row).text('Not configured');
};

$.otp.workflowSelection = {
  currentAjaxCalls: {},
  removeButtons: {},
  /** Contains all html form elements, that are used to select a configuration */
  addButtons: {},
  selectElements: {},
  optionsActions: {},
  deleteConfigActions: {},
  saveConfigActions: {},
  /** Contains the mapping from data response to htmlElement */
  optionsMapping: {},
  getOptionsFormData: {},
  tables: {},
  rowDataMapping: {},
  /** Method to map the data from the backend to the data Table row */
  dataToRowMapping: {},
  convertDataToDisplayString(workflowType, newRowData, oldDataOrUndefined) {
    'use strict';

    const newData = $.otp.workflowSelection.rowDataMapping[workflowType](newRowData);
    const oldData = oldDataOrUndefined ?
      $.otp.workflowSelection.rowDataMapping[workflowType](oldDataOrUndefined) : newData;
    const oldDataInfo = Object.entries(oldData).reduce((dataInfo, [key, oldValue]) => ({
      ...dataInfo,
      [key]: (oldValue !== newData[key] ? `(previously: ${oldValue})` : '')
    }), {});
    let displayString = '<ul>';
    if (newData.workflow) {
      displayString += `<li>workflow: ${newData.workflow} ${oldDataInfo.workflow}</li>`;
    }
    if (newData.seqType) {
      displayString += `<li>seqType: ${newData.seqType} ${oldDataInfo.seqType}</li>`;
    }
    if (newData.workflowVersion) {
      displayString += `<li>workflow version: ${newData.workflowVersion} ${oldDataInfo.workflowVersion}</li>`;
    }
    if (newData.referenceGenome) {
      displayString += `<li>reference genome: ${newData.referenceGenome} ${oldDataInfo.referenceGenome}</li>`;
    }
    if (newData.speciesWithStrain) {
      displayString += `<li>species with strains: ${newData.speciesWithStrain} ${oldDataInfo.speciesWithStrain}</li>`;
    }
    displayString += '</ul>';
    return displayString;
  },
  /** Saves the configuration for the selected workflowType and handles disabling and enabling of the button */
  saveConfig(workflowType, button) {
    'use strict';

    button.prop('disabled', true);
    $.ajax({
      url: $.otp.createLink({
        controller: 'workflowSelection',
        action: $.otp.workflowSelection.saveConfigActions[workflowType]
      }),
      data: $.otp.workflowSelection.getOptionsFormData[workflowType](),
      contentType: false,
      processData: false,
      type: 'POST',
      success(data) {
        $.otp.workflowSelection.updateTableRowData(workflowType, data);
        $.otp.workflowSelection.clearFormFields(workflowType);
        $.otp.workflowSelection.getPossibleOptions(workflowType);
      },
      error(jqXHR, textStatus, errorThrown) {
        let errorMessage = 'Unknown error occurred.';
        if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
          errorMessage = jqXHR.responseJSON.message;
        } else if (errorThrown) {
          errorMessage = errorThrown;
        }
        $.otp.toaster.showErrorToast(`Saving ${workflowType} Configuration failed`, errorMessage);
      }
    }).always(() => {
      button.removeAttr('disabled');
    });
  },
  updateTableRowData(workflowType, data) {
    'use strict';

    const rowData = $.otp.workflowSelection.dataToRowMapping[workflowType](data);
    let alteredRow;
    const wvPreviousId = data.workflowVersionSelector ? data.workflowVersionSelector.previousId : undefined;
    const wvId = data.workflowVersionSelector ? data.workflowVersionSelector.id : undefined;
    const rgId = data.refGenSelectorId;

    const rgAttribute = rgId ? `[data-ref-genome-selector="${rgId}"]` : '';
    const wvAttribute = wvId ? `[data-version-selector="${wvId}"]` : '';
    const wvPreviousAttribute = wvPreviousId ? `[data-version-selector="${wvPreviousId}"]` : '';
    const table = $.otp.workflowSelection.tables[workflowType];
    let existingButton = table.$(`button${rgAttribute}${wvAttribute}`);
    if (existingButton.length === 0 && wvPreviousAttribute) {
      existingButton = table.$(`button${rgAttribute}${wvPreviousAttribute}`);
    }
    if (existingButton.length > 0) {
      const row = table.row(existingButton.parent().parent());
      const oldData = row.data();
      alteredRow = row.data(rowData).draw().node();
      const updatedData = $.otp.workflowSelection.convertDataToDisplayString(workflowType, row.data(), oldData);

      $.otp.toaster.showSuccessToast(
        `Successfully updated ${workflowType} Configuration`,
        `The ${workflowType} configuration ${updatedData} was updated.`
      );
    } else {
      const row = table.row.add(rowData).draw();
      const dataInfo = $.otp.workflowSelection.convertDataToDisplayString(workflowType, row.data());
      $.otp.toaster.showSuccessToast(
        `Successfully created ${workflowType} Configuration`,
        `The ${workflowType} configuration ${dataInfo} was created.`
      );
      alteredRow = row.node();
    }
    $(alteredRow).find('button').on('click', (e) => $.otp.workflowSelection.removeConfiguration(workflowType, e));
  },
  removeConfiguration(workflowType, event) {
    'use strict';

    $.otp.workflowSelection.disableRemoveButtons(workflowType);
    const target = $(event.target);
    const dataSelectors = {
      'workflowVersionSelector.id': +target.data('versionSelector'),
      'referenceGenomeSelector.id': +target.data('refGenomeSelector')
    };
    const dataRow = $.otp.workflowSelection.tables[workflowType].row(target.parent().parent());
    const dataInfo = $.otp.workflowSelection.convertDataToDisplayString(workflowType, dataRow.data());

    $.ajax({
      url: $.otp.createLink({
        controller: 'workflowSelection',
        action: $.otp.workflowSelection.deleteConfigActions[workflowType]
      }),
      data: dataSelectors,
      dataType: 'json',
      type: 'POST',
      success() {
        dataRow.remove().draw();
        $.otp.toaster.showSuccessToast(
          `Successfully deleted ${workflowType} Configuration`,
          `Deleting the ${workflowType} configuration ${dataInfo} was successful.`
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
          `Deleting ${workflowType} Configuration failed`,
          `Failed to delete ${workflowType} configuration ${dataInfo} ${errorMessage}`
        );
      }
    }).always(() => {
      $.otp.workflowSelection.enableRemoveButtons(workflowType);
    });
  },
  /** Method to receive all possible options from the backend and assign them to the select options */
  getPossibleOptions(workflowType) {
    'use strict';

    const currentAjaxCall = $.otp.workflowSelection.currentAjaxCalls[workflowType];
    const optionsMapping = $.otp.workflowSelection.optionsMapping[workflowType];
    const possibleOptionsCall = $.otp.workflowSelection.setPossibleOptions;
    if (currentAjaxCall) {
      currentAjaxCall.abort();
    }
    $.otp.workflowSelection.currentAjaxCalls[workflowType] = $.ajax({
      url: $.otp.createLink({
        controller: 'workflowSelection',
        action: $.otp.workflowSelection.optionsActions[workflowType]
      }),
      data: $.otp.workflowSelection.getOptionsFormData[workflowType](),
      contentType: false,
      processData: false,
      type: 'POST',
      success(data) {
        Object.entries(optionsMapping).forEach(([dataId, htmlElement]) => {
          possibleOptionsCall(htmlElement, data[dataId]);
        });
      },
      error(jqXHR) {
        if (jqXHR.statusText !== 'abort') {
          $.otp.toaster.showErrorToast('Workflow Configuration', 'Some error occurred.');
        }
      }
    });
  },
  setPossibleOptions(selectElement, idList) {
    'use strict';

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
  },
  /** Receives a list of formElement Items and clears these */
  clearFormFields(workflowType) {
    'use strict';

    $.otp.workflowSelection.selectElements[workflowType].forEach((formElement) => {
      formElement.val('');
      formElement.trigger('change.select2');
    });
  },
  /** Disables all remove buttons in corresponding table on workflowSelection page */
  disableRemoveButtons(workflowType) {
    'use strict';

    $.otp.workflowSelection.removeButtons[workflowType] = $.otp.workflowSelection.tables[workflowType]
      .$('button.remove-config-btn:not([disabled])');
    $.otp.workflowSelection.removeButtons[workflowType].prop('disabled', true);
  },
  /** Enables all remove buttons in corresponding table on workflowSelection page */
  enableRemoveButtons(workflowType) {
    'use strict';

    $.otp.workflowSelection.removeButtons[workflowType].removeAttr('disabled');
  }
};
