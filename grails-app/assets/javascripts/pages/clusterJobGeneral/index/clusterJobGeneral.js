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

  $.otp.clusterJobGeneralTable.register();
  $.otp.clusterJobGeneralGraph.update();

  $('.datePicker').on('change', () => {
    $.otp.clusterJobGeneralTable.update();
    $.otp.clusterJobGeneralGraph.update();
    $('#dpTo').attr('min', $('#dpFrom').val());
    $('#dpFrom').attr('max', $('#dpTo').val());
  });
});

$.otp.clusterJobGeneralTable = {
  register() {
    'use strict';

    $('#clusterJobGeneralTable').dataTable({
      dom: '<"row"<"col-sm-12"f>><"row"<"col-sm-12"tr>><"row"<"col-sm-12 col-md-5"i><"col-sm-12 col-md-7"p>>',
      bFilter: true,
      bProcessing: true,
      bServerSide: true,
      bSort: true,
      bAutoWidth: false,
      pageLength: 10,
      sAjaxSource: $.otp.createLink({
        controller: 'clusterJobGeneral',
        action: 'findAllClusterJobsByDateBetween',
        parameters: { from: $('#dpFrom').val(), to: $('#dpTo').val() }
      }),
      sScrollY: 'auto',
      aaSorting: [[3, 'desc']],
      sScrollX: 'auto',
      bScrollCollapse: false,
      bPaginate: true,
      bDeferRender: true,
      fnServerData(sSource, aoData, fnCallback) {
        $.ajax({
          dataType: 'json',
          type: 'POST',
          url: sSource,
          data: aoData,
          error() {
          },
          success(json) {
            for (let i = 0; i < json.aaData.length; i += 1) {
              const row = json.aaData[i];
              if (row[2]) {
                row[0] = $.otp.createLinkMarkup({
                  controller: 'clusterJobDetail',
                  action: 'show',
                  id: row[6],
                  text: row[0]
                });
              } else {
                row[2] = 'IN PROGRESS';
              }
              row[1] = row[1].substr(9);
              row.pop();
            }
            fnCallback(json);
          }
        });
      }
    });
  },

  update() {
    'use strict';

    $('#clusterJobGeneralTable').DataTable().destroy();
    $.otp.clusterJobGeneralTable.register();
  }
};

$.otp.clusterJobGeneralGraph = {
  update() {
    'use strict';

    const from = $('#dpFrom').val();
    const to = $('#dpTo').val();

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllStatesTimeDistribution',
      parameters: { from, to }
    }), function () {
      $.Graphs.drawPieGraph('delayPieChart', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllExitCodes',
      parameters: { from, to }
    }), function () {
      $.Graphs.drawPieGraph('generalGraphExitCode', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllExitStatuses',
      parameters: { from, to }
    }), function () {
      $.Graphs.drawPieGraph('generalGraphExitStatus', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllFailed',
      parameters: { from, to }
    }), function () {
      $.Graphs.drawLineGraph('generalGraphFailed', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllStates',
      parameters: { from, to }
    }), function () {
      $.Graphs.drawLineGraph('generalGraphStates', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllAvgCoreUsage',
      parameters: { from, to }
    }), function () {
      $.Graphs.drawLineGraph('generalGraphCores', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllMemoryUsage',
      parameters: { from, to }
    }), function () {
      $.Graphs.drawLineGraph('generalGraphMemory', this);
    });
  }
};
