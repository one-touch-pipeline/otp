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

/**
 * Generates the list of Runs
 */
$.otp.run = {
  register() {
    'use strict';

    const searchCriteria = $.otp.dataTableFilter.register($('#searchCriteriaTable'), () => {
      $('#runTable').dataTable().fnDraw();
    });

    $('#runTable').dataTable({
      sDom: '<i> B rt<"clear">',
      buttons: $.otp.getDownloadButton('', 'list_of_runs'),
      bFilter: true,
      bProcessing: true,
      bServerSide: true,
      bSort: true,
      bAutoWidth: false,
      sAjaxSource: $.otp.createLink({
        controller: 'run',
        action: 'dataTableSource'
      }),
      bPaginate: false,
      bScrollCollapse: true,
      sScrollY: ($(window).height() - 453),
      bDeferRender: true,
      fnServerData(sSource, aoData, fnCallback) {
        aoData.push({
          name: 'filtering',
          value: JSON.stringify(searchCriteria())
        });
        aoData.push({
          name: 'filter',
          value: $.otp.run.search
        });
        $.ajax({
          dataType: 'json',
          type: 'POST',
          url: sSource,
          data: aoData,
          error() {
            fnCallback({ aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0 });
          },
          success(json) {
            const result = json;
            let i; let rowData; let
              row;
            for (i = 0; i < json.aaData.length; i += 1) {
              row = json.aaData[i];
              rowData = [
                $.otp.createLinkMarkup({
                  controller: 'run',
                  action: 'show',
                  id: row.id,
                  text: row.name
                }),
                row.seqCenters,
                row.dateCreated,
                row.dateExecuted
              ];
              result.aaData[i] = rowData;
            }
            fnCallback(result);
          }
        });
      }
    });
  }
};

$(() => {
  $.otp.run.register();
});
