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

$.otp.userAdministration = {};
$.otp.userAdministration.enableUser = function (userId, field) {
  'use strict';

  $.ajax({
    url: $.otp.createLink({
      controller: 'userAdministration',
      action: 'enable',
      parameters: {
        'user.id': userId,
        flag: $(`#${field}`).is(':checked')
      }
    }),
    type: 'POST',
    dataType: 'json',
    cache: 'false',
    success(data) {
      // redraw the dataTable to reset all changes
      $('#userTable').dataTable().fnDraw();
      if (data.success) {
        $.otp.infoMessage('Data stored successfully');
      } else {
        $.otp.warningMessage(data.error);
      }
    },
    error(jqXHR, textStatus, errorThrown) {
      $.otp.warningMessage(`${textStatus} occurred while processing the data. Reason: ${errorThrown}`);
    }
  });
};

$.otp.userAdministration.loadUserList = function () {
  'use strict';

  $('#userTable').dataTable({
    aoColumns: [
      {
        mData(source) {
          return $.otp.createLinkMarkup({
            controller: 'userAdministration',
            action: 'show',
            text: source.username || '[no username]',
            title: source.id,
            parameters: {
              'user.id': source.id
            }
          });
        }
      },
      { mData: 'realname' },
      { mData: 'email' },
      { mData: 'deactivationDate' },
      {
        mData(source, type) {
          if (type === 'sort') {
            return source.enabled;
          }
          const checkboxId = `user-change-${source.id}`;
          return `<input type="checkbox" id="${checkboxId}"
                    ${source.enabled ? 'checked' : ''}/><input type="button" value="Update"
                    onclick="$.otp.userAdministration.enableUser(${source.id},'${checkboxId}')"/>`;
        }
      },
      {
        mData(source, type) {
          if (type === 'sort') {
            return source.acceptedPrivacyPolicy;
          }
          let checked = '';
          if (source.acceptedPrivacyPolicy) {
            checked = 'checked';
          }
          return `<input type="checkbox" disabled ${checked}/>`;
        }
      },
      {
        mData(source) {
          let result = '';
          if (source.username) {
            result = `<form action="${$.otp.createLink({ controller: 'impersonate' })}" 
                            method="POST">
                        <input type="hidden" name="username" value="${source.username}"/>
                          <button>Switch to ${source.username}</button>
                      </form>`;
          }
          return result;
        }
      }
    ],
    bFilter: false,
    bProcessing: true,
    bServerSide: false,
    bSort: true,
    bPaginate: false,
    bScrollCollapse: true,
    sScrollY: $(window).height(),
    aaSorting: [[1, 'asc']],
    sAjaxSource: $.otp.createLink({
      controller: 'userAdministration',
      action: 'dataTableSource'
    }),
    fnServerData(sSource, aoData, fnCallback) {
      $.ajax({
        dataType: 'json',
        type: 'POST',
        url: sSource,
        data: aoData,
        error() {
          // clear the table
          fnCallback({ aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0 });
        },
        success(json) {
          fnCallback(json);
        }
      });
    }
  });
  $.otp.resizeBodyInit('#userTable', 180);
};
