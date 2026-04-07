/*
 * Copyright 2011-2026 The OTP authors
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

  const workflowSelect = $('#alignment-workflow-select');
  const versionSelect = $('#alignment-version-select');
  const seqTypeSelect = $('#alignment-seq-type-select');
  const speciesSelect = $('#alignment-species-select');
  const refGenomeSelect = $('#alignment-ref-genome-select');
  const workflowType = 'alignment';

  $.otp.workflowSelection.tables[workflowType] = $('#alignmentTable').DataTable({
    paging: false,
    dom: 'it'
  });
  $.otp.workflowSelection.selectElements[workflowType] = [
    workflowSelect, versionSelect, seqTypeSelect, speciesSelect, refGenomeSelect
  ];
  $.otp.workflowSelection.addButtons[workflowType] = $('#add-alignment-config-btn');
  $.otp.workflowSelection.saveConfigActions[workflowType] = 'saveAlignmentConfiguration';
  $.otp.workflowSelection.deleteConfigActions[workflowType] = 'deleteConfiguration';
  $.otp.workflowSelection.optionsActions[workflowType] = 'possibleAlignmentOptions';
  $.otp.workflowSelection.optionsMapping[workflowType] = {
    workflowIds: workflowSelect,
    versionIds: versionSelect,
    seqTypeIds: seqTypeSelect,
    speciesIds: speciesSelect,
    refGenomeIds: refGenomeSelect
  };
  $.otp.workflowSelection.getOptionsFormData[workflowType] = () => {
    const formData = new FormData();
    formData.append('workflow.id', workflowSelect.val());
    formData.append('seqType.id', seqTypeSelect.val());
    formData.append('workflowVersion.id', versionSelect.val());
    formData.append('referenceGenome.id', refGenomeSelect.val());
    if (speciesSelect.val()) {
      speciesSelect.val().forEach((species) => {
        formData.append('speciesWithStrains', species);
      });
    }
    return formData;
  };
  $.otp.workflowSelection.rowDataMapping[workflowType] = (rowData) => ({
    workflow: rowData[0],
    seqType: rowData[1],
    workflowVersion: rowData[2],
    referenceGenome: rowData[3],
    speciesWithStrains: rowData[4]
  });
  $.otp.workflowSelection.dataToRowMapping[workflowType] = (data) => [
    data.workflow.displayName,
    data.seqType.displayName,
    data.version.displayName,
    data.refGenome.displayName,
    data.species.map((specie) => specie.displayName).join(' + '),
    `<button class="btn float-end btn-outline-danger remove-config-btn"
         data-ref-genome-selector="${data.refGenSelectorId}"
         data-version-selector="${data.workflowVersionSelector.id}">
         <i class="bi bi-trash"></i> Remove
     </button>`
  ];
});
