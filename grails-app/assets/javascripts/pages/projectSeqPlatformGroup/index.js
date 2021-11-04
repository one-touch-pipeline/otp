/*
 * Copyright 2011-2021 The OTP authors
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

  // configure merging criteria
  $('#useSeqPlatformGroup').on('change', function () {
    this.form.submit();
  });
  $('#libPrepKit').on('change', function () {
    this.form.submit();
  });

  // seq. platform search
  const copyFromProjectSelector = $('#selectedProjectToCopyFrom');
  const copyFromSeqTypeSelector = $('#selectedSeqTypeToCopyFrom');

  copyFromProjectSelector.on('change', function () {
    if (this.value === 'null') {
      // Switch to system default seq. platforms
      this.form.submit();
    } else if (copyFromSeqTypeSelector.prop('value') === 'null') {
      setTimeout(() => {
        copyFromSeqTypeSelector.prop('disabled', false);
      }, 0);
      setTimeout(() => {
        copyFromSeqTypeSelector.select2('open');
        $(copyFromSeqTypeSelector, '.select2-search__field').focus();
      }, 0);
    } else {
      this.form.submit();
    }
  });

  copyFromSeqTypeSelector.on('change', function () {
    if (copyFromProjectSelector.prop('value') !== 'null' && copyFromSeqTypeSelector.prop('value') !== 'null') {
      // Search for the seq. platforms of the other projects
      this.form.submit();
    } else {
      copyFromSeqTypeSelector.addClass('is-invalid');
    }
  });
});
