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

  $('.add-button').on('click', (event) => {
    event.preventDefault();
    // Subtract the hidden element
    const existingSamples = $('tr.sample').not('.hidden');

    // also copy event handlers on clone
    const sampleBoxes = $('tr.hidden.template').clone(true, true).removeClass('hidden');
    sampleBoxes.insertAfter($('.sampleIdentifier:last'));
    sampleBoxes.find('select').attr('name', `samples[${existingSamples.length}].sampleType`);
    sampleBoxes.find('input').attr('name', `samples[${existingSamples.length}].sampleIdentifiers`);
    // The templates contain a select-tag that needs to become a select2 searchable dropdown.
    $.otp.applySelect2($('select.use-select-2-after-clone', sampleBoxes));
  });
});
