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
 * Generates the list of individuals
 */
$.otp.individual = {
  register() {
    'use strict';

    const searchCriteria = $.otp.dataTableFilter.register($('#searchCriteriaTable'), () => {
      $('#individualTable').dataTable().fnDraw();
    });

    $('#individualTable').dataTable({
      sDom: '<i> B rt<"clear">',
      buttons: $.otp.getDownloadButton(),
      bFilter: true,
      bProcessing: true,
      bServerSide: true,
      bSort: true,
      bAutoWidth: false,
      sAjaxSource: $.otp.createLink({
        controller: 'individual',
        action: 'dataTableSource'
      }),
      bScrollCollapse: true,
      sScrollY: 'auto',
      bDeferRender: true,
      fnServerData(sSource, aoData, fnCallback) {
        aoData.push({
          name: 'filtering',
          value: JSON.stringify(searchCriteria())
        });
        aoData.push({
          name: 'filter',
          value: $.otp.individual.search
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
            let i; let rowData; let row;
            for (i = 0; i < json.aaData.length; i += 1) {
              row = json.aaData[i];
              rowData = [
                $.otp.createLinkMarkup({
                  controller: 'individual',
                  action: 'show',
                  id: row.id,
                  text: row.pid
                }),
                $.otp.createLinkMarkup({
                  controller: 'individual',
                  action: 'show',
                  id: row.id,
                  text: row.mockFullName
                }),
                $.otp.createLinkMarkup({
                  controller: 'individual',
                  action: 'show',
                  id: row.id,
                  text: row.mockPid
                }),
                $.otp.createLinkMarkup({
                  controller: 'projectOverview',
                  action: 'index',
                  parameters: {
                    [$.otp.projectParameter]: row.project
                  },
                  text: row.project
                }),
                row.type
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
  'use strict';

  $.otp.individual.register();
});
