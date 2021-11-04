/*
 * Copyright 2011-2019 The OTP authors
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

  $('#overviewTableMMMLMappingId').dataTable({
    sDom: '<i> B rt<"clear">',
    buttons: $.otp.getDownloadButton(),
    bFilter: true,
    bProcessing: true,
    bServerSide: false,
    bSort: true,
    bJQueryUI: false,
    bAutoWidth: false,
    sAjaxSource: $.otp.createLink({
      controller: 'mmml',
      action: 'dataTableMMMLMapping'
    }),
    bPaginate: false,
    sScrollY: 540,
    sScrollX: 'auto',
    bScrollCollapse: false,
    bDeferRender: true,
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
          for (let i = 0; i < json.aaData.length; i += 1) {
            const row = json.aaData[i];
            const mockPid = row[1];
            row[0] = $.otp.createLinkMarkup({
              controller: 'individual',
              action: 'show',
              text: row[0],
              parameters: {
                mockPid
              }
            });
            row[1] = $.otp.createLinkMarkup({
              controller: 'individual',
              action: 'show',
              text: row[1],
              parameters: {
                mockPid
              }
            });
            row[2] = $.otp.createLinkMarkup({
              controller: 'individual',
              action: 'show',
              text: row[2],
              parameters: {
                mockPid
              }
            });
          }
          fnCallback(json);
        }
      });
    }
  });
});
