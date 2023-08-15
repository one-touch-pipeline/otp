/*
 * Copyright 2011-2023 The OTP authors
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

  $('input:checkbox#enableAutoExec').on('change', (e) => {
    if (e.target.checked) {
      $('.automaticSettings').show();
    } else {
      $('.automaticSettings').hide();
    }
  }).trigger('change');

  $('input:radio[name=expectedOrEnforcedCells]').on('change', (e) => {
    const target = $(e.target);
    const container = target.closest('div');
    const value = container.find('input:radio[name=expectedOrEnforcedCells]:checked').val();
    const expected = container.find('input[name=expectedCellsValue]');
    const enforced = container.find('input[name=enforcedCellsValue]');

    expected.prop('disabled', true);
    enforced.prop('disabled', true);
    if (value === 'expected') {
      expected.prop('disabled', false);
    } else if (value === 'enforced') {
      enforced.prop('disabled', false);
    }
  }).trigger('change');
});
