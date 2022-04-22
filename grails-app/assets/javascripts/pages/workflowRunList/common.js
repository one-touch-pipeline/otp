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
const statusToClassName = function (status) {
  'use strict';

  switch (status) {
    case 'WAITING_FOR_USER':
      return 'dot blue';
    case 'FAILED':
    case 'FINISHED/FAILED':
      return 'dot red';
    case 'PENDING':
    case 'CREATED':
      return 'dot grey';
    case 'RUNNING_WES':
      return 'sm-loader darkgreen';
    case 'PLANNED_OR_RUNNING':
    case 'RUNNING':
    case 'RUNNING_OTP':
    case 'CHECKING':
      return 'sm-loader green';
    case 'SUCCESS':
    case 'FINISHED/COMPLETED':
      return 'dot green';
    case 'OMITTED':
    case 'OMITTED_MISSING_PRECONDITION':
      return 'dot purple';
    case 'FAILED_FINAL':
      return 'dot darkorange';
    case 'RESTARTED':
      return 'dot orange';
    case 'KILLED':
      return 'dot black';
    default:
      return '';
  }
};

// eslint-disable-next-line no-unused-vars
const button = function (action, value, title, buttonsDisabled, icon) {
  'use strict';

  return `<button class="btn btn-xs btn-primary" formaction="${action}"
                  name="step" value="${value}" title="${title}" ${buttonsDisabled}>
            <i class="bi-${icon}"></i>
          </button>`;
};

$(() => {
  'use strict';

  const statusDot = $('#statusDot');
  statusDot.addClass(statusToClassName(statusDot.data('status')));
  statusDot.tooltip();
});
