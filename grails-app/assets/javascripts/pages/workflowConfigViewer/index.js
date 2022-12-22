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
  'use strict';

  $('select').select2({
    allowClear: true,
    theme: 'bootstrap4'
  });

  const selectorTypeSelector = $('#relatedSelectorType');

  const filterBySelectorType = () => {
    const types = selectorTypeSelector.val();
    $('#relatedSelectors').children('div').each(function () {
      if (types !== null && types.length > 0 && types.indexOf($(this).data('type')) === -1) {
        $(this).hide();
      } else {
        $(this).show();
      }
    });
  };

  selectorTypeSelector.on('change', () => {
    filterBySelectorType();
  });

  const form = $('form.selector');
  form.on('change', 'select', (e) => {
    if (e.target.id === 'workflowSelector') {
      // clear selected workflow versions
      $('#workflowVersionSelector').val(null).trigger('change');
    } else {
      build();
    }
  });
  form.on('change', 'input', build);
  form.on('keyup', 'input[type=text]', build);

  // hold the workflows selected
  let selectedWorkflows = [];
  $('#workflowVersionSelector').on('select2:opening', () => {
    selectedWorkflows = $('#workflowSelector').select2('data');
  });

  // filter the workflow versions options shown
  $('#workflowVersionSelector').select2({
    templateResult: (data) => {
      /* eslint-disable-next-line */
      for (const workflow of selectedWorkflows) {
        if (data.text.startsWith(workflow.text)) {
          return data.text;
        }
      }
      return null;
    }
  });

  function build() {
    const workflowId = $('#workflowSelector').val();
    const workflowVersionId = $('#workflowVersionSelector').val();
    const projectId = $('#projectSelector').val();
    const seqTypeId = $('#seqTypeSelector').val();
    const refGenId = $('#refGenSelector').val();
    const libPrepKitId = $('#libPrepKitSelector').val();

    const configValue = $('#configValue');
    const relatedSelectors = $('#relatedSelectors');

    if (workflowId || workflowVersionId || projectId || seqTypeId || refGenId || libPrepKitId) {
      $.ajax({
        url: $.otp.createLink({
          controller: 'workflowConfigViewer',
          action: 'build'
        }),
        dataType: 'json',
        type: 'POST',
        data: {
          workflow: workflowId,
          workflowVersion: workflowVersionId,
          selectedProject: projectId,
          seqType: seqTypeId,
          referenceGenome: refGenId,
          libraryPreparationKit: libPrepKitId
        },
        success(response) {
          const selectorHtml = (response.selectors);
          relatedSelectors.html(selectorHtml);

          $('.expandable-button').on('click', function () {
            $(this).siblings('.expandable-container').toggleClass('collapsed');
            $(this).siblings('.expandable-container').toggleClass('expanded');
          });

          if (/<\/?[a-z][\s\S]*>/i.test(selectorHtml)) {
            configValue.val(JSON.stringify(JSON.parse(response.config), null, 2));
          } else {
            configValue.val('');
            $.otp.toaster.showInfoToast('No selectors found', 'There are no related selectors to this selection');
          }
          filterBySelectorType();
        },
        error(error) {
          if (error && error.responseJSON && error.responseJSON.message) {
            $.otp.toaster.showErrorToast('Error while processing', error.responseJSON.message);
          } else {
            $.otp.toaster.showErrorToast('Error while processing', 'Unknown error');
          }
        }
      });
    } else {
      configValue.val('');
      relatedSelectors.html('');
    }
  }
});
