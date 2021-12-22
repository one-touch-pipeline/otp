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
  $('form').on('submit', (e) => {
    $(e.target).find('input:submit').addClass('submitting');
  });

  const removeSubmitted = (target) => {
    $(target).find('input:submit').removeClass('submitting');
  };
  removeSubmitted(document);

  $('.js-confirm').on('submit', (e) => {
    let submit;
    const radios = $(e.target).find(':input:checked');
    if (radios.size() !== 1) {
      alert('Please select a run.');
      submit = false;
    } else if (radios.filter('[value=delete]').size() === 1) {
      submit = window.confirm(`All runs of this individual/sample type/seq.
       type/version/reference combination will be deleted on the filesystem. Continue?`);
    } else {
      submit = window.confirm(`If existing, all other runs of this individual/sample type/seq.
       type/version/reference combination will be deleted on the filesystem. Continue?`);
    }
    if (submit === false) {
      e.preventDefault();
      removeSubmitted(e.target);
    }
  });
});
