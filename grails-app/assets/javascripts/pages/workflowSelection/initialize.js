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

$(document).ready(() => {
  'use strict';

  Object.entries($.otp.workflowSelection.addButtons).forEach(([workflowType, addButton]) => {
    addButton.on('click', () => {
      $.otp.workflowSelection.saveConfig(workflowType, addButton);
    });
  });

  Object.entries($.otp.workflowSelection.selectElements).forEach(([workflowType, selectElements]) => {
    selectElements.forEach((selectElement) => selectElement.on('change', () => {
      $.otp.workflowSelection.getPossibleOptions(workflowType);
    }));
  });

  Object.entries($.otp.workflowSelection.tables).forEach(([workflowType, table]) => {
    table.$('.remove-config-btn').on('click', (e) => {
      $.otp.workflowSelection.removeConfiguration(workflowType, e);
    });

    /** Initialize possible Options */
    $.otp.workflowSelection.getPossibleOptions(workflowType);
  });

  $('#fastqcTable').dataTable({
    paging: false,
    info: false
  });

  $('#mergingCriteriaTable').dataTable({
    paging: false,
    info: false
  });
});
