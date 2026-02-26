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

(function () {
  'use strict';

  $(() => {
    const tableElement = $('table#runs');
    const buttonElements = $('.toggleButton');
    const numberOfColumns = tableElement.find('thead tr:last th').length;
    const fixedColumnsLeft = 3;
    const fixedColumnsRight = 2;

    const columnsToShow = [];
    let i = fixedColumnsLeft;
    tableElement.find('thead tr:first th[colspan]').each(function () {
      columnsToShow.push(i);
      i += +$(this).attr('colspan') + 1;
    });

    const columnsToHide = [];
    for (let j = fixedColumnsLeft; j < numberOfColumns - fixedColumnsRight; j++) {
      if (columnsToShow.indexOf(j) === -1) {
        columnsToHide.push(j);
      }
    }

    const dataTable = tableElement.dataTable({
      dom: '<"top"i>rt<"bottom"lp><"clear">',
      aoColumnDefs: [{
        bVisible: false,
        aTargets: columnsToHide
      }],
      bPaginate: false,
      bSort: false
    });

    buttonElements.on('click', () => {
      buttonElements.toggle();
      for (let j = fixedColumnsLeft; j < numberOfColumns - fixedColumnsRight; j++) {
        dataTable.fnSetColumnVis(j, !dataTable.fnSettings().aoColumns[j].bVisible);
      }
    });

    // Preload workflows for edit reuse
    let workflowCacheById = null;
    fetch($.otp.createLink({
      controller: 'workflowSystemConfig',
      action: 'getWorkflows'
    }))
      .then((r) => r.json())
      .then((list) => {
        workflowCacheById = Object.fromEntries(list.map((w) => [w.id, w]));
      })
      .catch(() => $.otp.toaster.showErrorToast('Error', 'Failed to preload workflows'));

    $(document).on('click', '.dot.small[data-workflow-id]', function () {
      const id = Number(this.dataset.workflowId);
      if (!workflowCacheById) {
        $.otp.toaster.showErrorToast('Error', 'Workflows not loaded yet');
        return;
      }
      const wf = workflowCacheById[id];
      if (!wf) {
        $.otp.toaster.showErrorToast('Error', 'Workflow not found');
        return;
      }

      const dotEl = this;
      WorkflowEdit.openModal(wf, {
        onSuccess: (updated, original, modal, payload) => {
          dotEl.classList.toggle('green', payload.enabled);
          dotEl.classList.toggle('grey', !payload.enabled);
          dotEl.setAttribute('title', payload.enabled ? 'Enabled' : 'Disabled');
          workflowCacheById[updated.id] = updated;
        }
      });
    });
  });
}());
