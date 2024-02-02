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

  const failure = function (title, message) {
    if ($.otp.toaster) {
      $.otp.toaster.showErrorToast(title, message);
    } else {
      $.otp.warningMessage(title + message);
    }
  };

  const userAutocomplete = function (elements) {
    $(elements).typeahead(
      {
        highlight: true,
        hint: false
      },
      {
        display: 'username',
        limit: 1000,
        source: (query, syncResults, asyncResults) => {
          $.ajax({
            type: 'GET',
            url: $.otp.createLink({
              controller: 'ldap',
              action: 'getUserSearchSuggestions',
              parameters: {
                searchString: query
              }
            }),
            dataType: 'json',
            success(result) {
              asyncResults(result);
            },
            error(jqXHR, textStatus, errorThrown) {
              failure(`${textStatus} occurred while processing the data.`, `Reason: ${errorThrown}`);
            }
          });
        },
        templates: {
          empty: '<div>Unable to find user matching your query.</div>',
          pending: '<div>Searching...</div>',
          suggestion: (result) => (result.minLength ? '<strong>Please type at least three letters</strong>' :
            `<tr><td>${result.realName}</td><td>${result.username}</td>
             <td>${result.mail}</td><td>${result.department}</td></tr>`)

        }
      }
    );
  };

  // add autocompletion to inputs inside elements with class user-auto-complete, if it wasn't added yet
  $('.user-auto-complete').on('input', (e) => {
    const target = $(e.target);
    if (target.parent('.twitter-typeahead').length === 0) {
      userAutocomplete(target);
      // adding autocompletion makes the input lose focus
      target.focus();
    }
  });
});
