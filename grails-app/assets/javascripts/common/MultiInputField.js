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

  const wrapper = $('.multi-input-field');
  wrapper.on('click', '.add-field', (e) => {
    e.preventDefault();

    // basic context
    const parent = $(e.target).parents('.multi-input-field').first();
    const baseField = $('.field', parent).first();

    // select2, with all its custom event handlers, doesn't take well to cloning.
    // Remove any that might be in there, to be recreated afterwards.
    const origSelect2 = $('select.use-select-2', baseField).select2('destroy');

    // use base field as template for new one, but empty any selections.
    const clonedField = baseField.clone();
    $('input, textarea, select', clonedField).val('');

    // update buttons on our new field; only the first field can be cloned, user-added fields can only be removed.
    $('button.add-field', clonedField).remove();
    // the whitespace at the start prevents the button from sticking to the input element
    clonedField.append(' <button class="remove-field btn btn-outline-secondary p-0">-</button>');

    // add our shiny new field to the DOM
    // must be done before applying any potential select2; styling rules don't apply yet to elements outside the DOM,
    // so select2 can't copy them to its generated elements.
    parent.append(clonedField);

    // if there are any select2-wannabe's in there, (re-)apply now that we're done.
    $.otp.applySelect2(origSelect2);
    $.otp.applySelect2($('select.use-select-2', clonedField));
  });

  wrapper.on('click', '.remove-field', (e) => {
    e.preventDefault();
    $(e.target).parent('div.field').remove();
  });
});

$.otp.multiInputField = {
  cancel(target) {
    'use strict';

    target.find($('.remove-field').trigger('click'));
  }
};
