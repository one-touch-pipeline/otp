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

$.otp.clusterJobGeneral = {
  register() {
    'use strict';

    $.otp.clusterJobGeneralTable.register();
    $.otp.clusterJobGeneralGraph.register();
    $.otp.clusterJobGeneralProgress.register();

    $('.datePicker').datepicker(
      {
        dateFormat: 'yy-mm-dd',
        onSelect() {
          $.otp.clusterJobGeneralGraph.update();
          $.otp.clusterJobGeneralProgress.update();
          $.otp.clusterJobGeneralTable.update();
        },
        maxDate: $('#dpTo').val()
      }
    );
  }
};

$.otp.clusterJobGeneralTable = {
  register() {
    'use strict';

    $('#clusterJobGeneralTable').dataTable({
      dom: '<"top"filp<"clear">>rt',
      bFilter: true,
      bProcessing: true,
      bServerSide: true,
      bSort: true,
      bJQueryUI: false,
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
    $('#clusterJobGeneralTable').DataTable().destroy();
    $.otp.clusterJobGeneralTable.register();
  }
};

$.otp.clusterJobGeneralGraph = {

  colors: ['#81BEF7', '#A9BCF5', '#5882FA', '#0431B4', '#00BFFF', '#A9F5F2', '#088A85', '#9F81F7'],

  register() {
    'use strict';

    const from = $('#dpFrom').val();
    const to = $('#dpTo').val();

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllExitCodes',
      parameters: { from, to }
    }), function () {
      $.otp.clusterJobGeneralGraph.generatePieGraphic('generalGraphExitCode', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllExitStatuses',
      parameters: { from, to }
    }), function () {
      $.otp.clusterJobGeneralGraph.generatePieGraphic('generalGraphExitStatus', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllFailed',
      parameters: { from, to }
    }), function () {
      $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphFailed', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllStates',
      parameters: { from, to }
    }), function () {
      $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphStates', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllAvgCoreUsage',
      parameters: { from, to }
    }), function () {
      $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphCores', this);
    });

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllMemoryUsage',
      parameters: { from, to }
    }), function () {
      $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphMemory', this);
    });
  },

  update() {
    'use strict';

    $.otp.clusterJobGeneralGraph.register();
  },

  generatePieGraphic(id, data) {
    'use strict';

    const json = JSON.parse(data.response);
    RGraph.reset($(`#${id}`).get(0));
    new RGraph.Pie({
      id,
      data: json.data,
      options: {
        centerx: 120,
        colors: $.otp.clusterJobGeneralGraph.getColors(json.data.length),
        exploded: 3,
        key: json.labels,
        keyColors: $.otp.clusterJobGeneralGraph.getColors(json.data.length),
        keyRounded: false,
        linewidth: 1,
        radius: 80,
        shadowBlur: 15,
        shadowOffsetx: 5,
        shadowOffsety: 5,
        textSize: 8
      }
    }).draw();
  },

  generateLineGraphic(id, data) {
    'use strict';

    const json = JSON.parse(data.response);
    RGraph.reset($(`#${id}`).get(0));
    new RGraph.Line({
      id,
      data: json.data,
      options: {
        backgroundGridAutofitAlign: true,
        gutterBottom: 100,
        gutterLeft: 80,
        key: json.keys,
        labels: $.otp.clusterJobGeneralGraph.normalizeLabels(json.labels),
        numxticks: json.labels.length - 1,
        textAccessible: false,
        textAngle: 45,
        textSize: 8
      }
    }).draw();
  },

  getColors(elementCount) {
    'use strict';

    return this.colors.slice(0, elementCount);
  },

  normalizeLabels(labels) {
    'use strict';

    const step = Math.floor(labels.length / 24);
    const newLabels = labels.filter((ignoredValue, index) => index % step === 0);
    return newLabels;
  }
};

$.otp.clusterJobGeneralProgress = {
  register() {
    'use strict';

    RGraph.AJAX($.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllStatesTimeDistribution',
      parameters: { from: $('#dpFrom').val(), to: $('#dpTo').val() }
    }), function () {
      $.otp.clusterJobGeneralProgress.generateProgress('generalGraphQueuedStartedEndedProgress', this);
    });
  },

  update() {
    'use strict';

    // .multiprogressbar('destroy') does not work properly
    $('#generalGraphQueuedStartedEndedProgress').remove();
    $('<div id="generalGraphQueuedStartedEndedProgress" class="multiProgress"></div>')
      .appendTo('#progressBarContainer_generalGraphQueuedStartedEndedProgress');
    $.otp.clusterJobGeneralProgress.register();
  },

  generateProgress(id, data) {
    'use strict';

    const json = JSON.parse(data.response);
    $(`#${id}`).multiprogressbar({
      parts: [{
        value: json.data.queue[0],
        text: `${json.data.queue[0]}% (${json.data.queue[1]} hours)`,
        barClass: 'progressBarQueue',
        textClass: 'progressTextQueue'
      },
      {
        value: json.data.process[0],
        text: `${json.data.process[0]}% (${json.data.process[1]} hours)`,
        barClass: 'progressBarProcess',
        textClass: 'progressTextProcess'
      }]
    });
  }
};
