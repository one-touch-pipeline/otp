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

    $('#clusterJobGeneralTable').DataTable({
      dom: '<"row"<"col-sm-12"f>><"row"<"col-sm-12"tr>><"row"<"col-sm-12 col-md-5"i><"col-sm-12 col-md-7"p>>',
      filter: true,
      processing: true,
      serverSide: true,
      sort: true,
      autoWidth: false,
      pageLength: 10,
      scrollY: 'auto',
      sorting: [[3, 'desc']],
      scrollX: 'auto',
      scrollCollapse: false,
      paginate: true,
      deferRender: true,
      ajax: (data, callback) => {
        $.ajax({
          dataType: 'json',
          type: 'POST',
          url: $.otp.createLink({
            controller: 'clusterJobGeneral',
            action: 'findAllClusterJobsByDateBetween',
            parameters: {
              from: $('#dpFrom').val(),
              to: $('#dpTo').val()
            }
          }),
          data,
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
            callback(json);
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

    const delayPieChartDataUrl = $.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllStatesTimeDistribution',
      parameters: { from, to }
    });

    Chart.register(ChartDataLabels);

    $.otp.chart.renderChartOnElement(
      'delayPieChart',
      delayPieChartDataUrl,
      (domContext, chartData) => {
        // eslint-disable-next-line no-new
        new Chart(domContext, {
          type: 'pie',
          data: {
            labels: chartData.keys,
            datasets: [{
              data: chartData.data,
              backgroundColor: $.otp.chart.colorList,
              datalabels: {
                display: true,
                color: '#fff',
                // eslint-disable-next-line no-mixed-operators
                formatter: (value, context) => `${Math.round(value / context.chart.getDatasetMeta(0).total * 100)}%`
              }
            }]
          },
          options: $.otp.chart.defaultChartOptions('', {
            scales: {
              y: {
                display: false
              }
            }
          })
        });
      }
    );

    const allExitCodesUrl = $.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllExitCodes',
      parameters: { from, to }
    });

    $.otp.chart.renderChartOnElement(
      'generalGraphExitCode',
      allExitCodesUrl,
      (domContext, chartData) => {
        // eslint-disable-next-line no-new
        new Chart(domContext, {
          type: 'pie',
          data: {
            labels: chartData.keys,
            datasets: [{
              data: chartData.data,
              backgroundColor: $.otp.chart.colorList,
              datalabels: {
                display: true,
                color: '#fff',
                // eslint-disable-next-line no-mixed-operators
                formatter: (value, context) => `${Math.round(value / context.chart.getDatasetMeta(0).total * 100)}%`
              }
            }]
          },
          options: $.otp.chart.defaultChartOptions('', {
            scales: {
              y: {
                display: false
              }
            }
          })
        });
      }
    );

    const allExitStatusesUrl = $.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllExitStatuses',
      parameters: { from, to }
    });

    $.otp.chart.renderChartOnElement(
      'generalGraphExitStatus',
      allExitStatusesUrl,
      (domContext, chartData) => {
        // eslint-disable-next-line no-new
        new Chart(domContext, {
          type: 'pie',
          data: {
            labels: chartData.keys,
            datasets: [{
              data: chartData.data,
              backgroundColor: $.otp.chart.colorList,
              datalabels: {
                display: true,
                color: '#fff',
                // eslint-disable-next-line no-mixed-operators
                formatter: (value, context) => `${Math.round(value / context.chart.getDatasetMeta(0).total * 100)}%`
              }
            }]
          },
          options: $.otp.chart.defaultChartOptions('', {
            scales: {
              y: {
                display: false
              }
            }
          })
        });
      }
    );

    const allStatesUrl = $.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllStates',
      parameters: { from, to }
    });

    $.otp.chart.renderChartOnElement(
      'generalGraphStates',
      allStatesUrl,
      (domContext, chartData) => {
        // eslint-disable-next-line no-new
        new Chart(domContext, {
          type: 'line',
          data: {
            labels: chartData.labels,
            datasets: [{
              data: chartData.data[0],
              label: chartData.keys[0],
              backgroundColor: $.otp.chart.colorList[0],
              borderColor: $.otp.chart.colorList[0]
            }, {
              data: chartData.data[1],
              label: chartData.keys[1],
              backgroundColor: $.otp.chart.colorList[1],
              borderColor: $.otp.chart.colorList[1]
            }, {
              data: chartData.data[2],
              label: chartData.keys[2],
              backgroundColor: $.otp.chart.colorList[2],
              borderColor: $.otp.chart.colorList[2]
            }]
          },
          options: $.otp.chart.defaultChartOptions('', {
            showLine: true
          })
        });
      }
    );

    const allFailedUrl = $.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllFailed',
      parameters: { from, to }
    });

    $.otp.chart.renderChartOnElement(
      'generalGraphFailed',
      allFailedUrl,
      (domContext, chartData) => {
        // eslint-disable-next-line no-new
        new Chart(domContext, {
          type: 'line',
          data: {
            labels: chartData.labels,
            datasets: [{
              data: chartData.data,
              label: chartData.keys,
              backgroundColor: $.otp.chart.colorList[0],
              borderColor: $.otp.chart.colorList[0]
            }]
          },
          options: $.otp.chart.defaultChartOptions('', {
            showLine: true
          })
        });
      }
    );

    const allAvgCoreUsageUrl = $.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllAvgCoreUsage',
      parameters: { from, to }
    });

    $.otp.chart.renderChartOnElement(
      'generalGraphCores',
      allAvgCoreUsageUrl,
      (domContext, chartData) => {
        // eslint-disable-next-line no-new
        new Chart(domContext, {
          type: 'line',
          data: {
            labels: chartData.labels,
            datasets: [{
              label: chartData.keys,
              data: chartData.data,
              backgroundColor: $.otp.chart.colorList[0],
              borderColor: $.otp.chart.colorList[0]
            }]
          },
          options: {
            scales: {
              y: {
                min: 0
              }
            },
            showLine: true,
            plugins: {
              datalabels: {
                display: false
              },
              legend: {
                position: 'bottom'
              }
            }
          }
        });
      }
    );

    const allMemoryUsage = $.otp.createLink({
      controller: 'clusterJobGeneral',
      action: 'getAllMemoryUsage',
      parameters: { from, to }
    });

    $.otp.chart.renderChartOnElement(
      'generalGraphMemory',
      allMemoryUsage,
      (domContext, chartData) => {
        // eslint-disable-next-line no-new
        new Chart(domContext, {
          type: 'line',
          data: {
            labels: chartData.labels,
            datasets: [{
              label: chartData.keys,
              data: chartData.data,
              backgroundColor: $.otp.chart.colorList[0],
              borderColor: $.otp.chart.colorList[0]
            }]
          },
          options: {
            scales: {
              y: {
                min: 0
              }
            },
            showLine: true,
            plugins: {
              datalabels: {
                display: false
              },
              legend: {
                position: 'bottom'
              }
            }
          }
        });
      }
    );
  }
};
