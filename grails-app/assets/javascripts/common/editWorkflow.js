/*
 * Copyright 2011-2025 The OTP authors
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

  const fillModal = (workflow, modal) => {
    $('.modal-title', modal).text(workflow.name);
    $('#modal-priority', modal).val(workflow.priority);
    $('#modal-max-runs', modal).val(workflow.maxParallelWorkflows);
    $('#modal-enabled', modal).prop('checked', workflow.enabled);

    if (workflow.supportedSeqTypes) {
      $('#modal-seqTypes', modal).val(workflow.supportedSeqTypes.map((seqType) => seqType.id));
    }
    $('#modal-seqTypes', modal).trigger('change');

    if (workflow.allowedRefGenomes) {
      $('#modal-refGenomes', modal).val(workflow.allowedRefGenomes.map((refGenome) => refGenome.id));
    }
    $('#modal-refGenomes', modal).trigger('change');

    const defaultVersion = $('#modal-defaultVersion', modal);
    defaultVersion.empty();
    defaultVersion.append('<option value="">No default version</option>');
    (workflow.versions || []).forEach((workflowVersion) => {
      const deprecated = workflowVersion?.deprecateDate &&
      workflowVersion.deprecateDate !== 'N/A' ? ' (deprecated)' : '';
      const option = document.createElement('option');
      option.value = String(workflowVersion.id);
      option.textContent = String(workflowVersion.name) + deprecated;
      defaultVersion.append(option);
    });
    if (workflow.defaultVersion?.id) {
      defaultVersion.val(workflow.defaultVersion.id);
    }
  };

  const buildPayload = (workflow, modal) => ({
    id: workflow.id,
    priority: Number($('#modal-priority', modal).val()),
    enabled: $('#modal-enabled', modal).prop('checked'),
    maxParallelWorkflows: Number($('#modal-max-runs', modal).val()),
    defaultVersion: $('#modal-defaultVersion', modal).val(),
    supportedSeqTypes: $('#modal-seqTypes', modal).select2('data').map((s) => Number(s.id)),
    allowedRefGenomes: $('#modal-refGenomes', modal).select2('data').map((rg) => Number(rg.id))
  });

  const postUpdate = (payload) => fetch($.otp.createLink({
    controller: 'workflowSystemConfig',
    action: 'updateWorkflow'
  }), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  }).then((resp) => {
    if (!resp.ok) {
      return resp.json().then((r) => { throw Error(r.message); });
    }
    return resp.json();
  });

  const openModal = (workflow, opts = {}) => {
    const modal = $('#editWorkflowModal');
    const confirmButton = modal.find('#confirmModal');
    const cancelButtons = modal.find('.closeModal');

    fillModal(workflow, modal);

    confirmButton.off('click').on('click', () => {
      const payload = buildPayload(workflow, modal);
      const workflowName = $('<div>').text(workflow.name).html();
      postUpdate(payload)
        .then((updated) => {
          $.otp.toaster.showSuccessToast(
            'Update successful',
            `<b>${workflowName}</b> configuration has been updated.`
          );
          if (typeof opts.onSuccess === 'function') {
            opts.onSuccess(updated, workflow, modal, payload);
          }
          modal.modal('hide');
        })
        .catch((e) => $.otp.toaster.showErrorToast('Error', `Update failed. ${e.message}`));
    });

    cancelButtons.off('click').on('click', () => modal.modal('hide'));
    modal.modal('show');
  };

  window.WorkflowEdit = { openModal };
}());
