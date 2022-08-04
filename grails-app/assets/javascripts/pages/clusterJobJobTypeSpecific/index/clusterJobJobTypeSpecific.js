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

$.otp.clusterJobJobTypeSpecific = {

  register() {
    'use strict';

    $.otp.clusterJobJobTypeSpecificCharts.renderCharts();

    $('.datePicker').on('change', () => {
      $('#dpTo').attr('min', $('#dpFrom').val());
      $('#dpFrom').attr('max', $('#dpTo').val());
      $.otp.clusterJobJobTypeSpecificCharts.renderCharts();
    });
    $('#jobClassSelect').on('change', () => {
      $.otp.clusterJobJobTypeSpecificCharts.renderCharts();
    });
    $('#seqTypeSelect').on('change', () => {
      $.otp.clusterJobJobTypeSpecificCharts.renderCharts();
    });
    $('#basesInput').on('change', () => {
      $.otp.clusterJobJobTypeSpecificCharts.renderCharts();
    });
    $('#coverageInput').on('change', () => {
      $.otp.clusterJobJobTypeSpecificCharts.renderCharts();
    });
  }
};

$.otp.clusterJobJobTypeSpecificCharts = {
  renderCharts() {
    'use strict';

    const startDate = $('#dpFrom').val();
    const endDate = $('#dpTo').val();

    const jobClassSelect = $('#jobClassSelect').val();
    const seqTypeSelect = $('#seqTypeSelect').val();

    const jobTypeSpecificExitCodesUrl = $.otp.createLink({
      controller: 'clusterJobJobTypeSpecific',
      action: 'getJobTypeSpecificExitCodes',
      parameters: {
        jobClass: jobClassSelect,
        seqType: seqTypeSelect,
        from: startDate,
        to: endDate
      }
    });

    Chart.register(ChartDataLabels);

    $.otp.chart.renderChartOnElement(
      'jobTypeSpecificGraphExitCode',
      jobTypeSpecificExitCodesUrl,
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

    const jobTypeSpecificExitStatusesUrl = $.otp.createLink({
      controller: 'clusterJobJobTypeSpecific',
      action: 'getJobTypeSpecificExitStatuses',
      parameters: {
        jobClass: jobClassSelect,
        seqType: seqTypeSelect,
        from: startDate,
        to: endDate
      }
    });

    $.otp.chart.renderChartOnElement(
      'jobTypeSpecificGraphExitStatus',
      jobTypeSpecificExitStatusesUrl,
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

    const jobTypeSpecificStatesUrl = $.otp.createLink({
      controller: 'clusterJobJobTypeSpecific',
      action: 'getJobTypeSpecificStates',
      parameters: {
        jobClass: jobClassSelect,
        seqType: seqTypeSelect,
        from: startDate,
        to: endDate
      }
    });

    $.otp.chart.renderChartOnElement(
      'jobTypeSpecificGraphStates',
      jobTypeSpecificStatesUrl,
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
            },
            {
              data: chartData.data[1],
              label: chartData.keys[1],
              backgroundColor: $.otp.chart.colorList[1],
              borderColor: $.otp.chart.colorList[1]
            },
            {
              data: chartData.data[2],
              label: chartData.keys[2],
              backgroundColor: $.otp.chart.colorList[2],
              borderColor: $.otp.chart.colorList[2]
            }]
          },
          options: {
            scales: {
              y: {
                min: 0,
                ticks: {
                  stepSize: 1
                }
              }
            },
            showLine: true,
            plugins: {
              legend: {
                position: 'bottom'
              }
            }
          }
        });
      }
    );

    const jobTypeSpecificWalltimes = $.otp.createLink({
      controller: 'clusterJobJobTypeSpecific',
      action: 'getJobTypeSpecificWalltimes',
      parameters: {
        jobClass: jobClassSelect,
        seqType: seqTypeSelect,
        from: startDate,
        to: endDate
      }
    });

    $.otp.chart.renderChartOnElement(
      'jobTypeSpecificGraphWalltimes',
      jobTypeSpecificWalltimes,
      (domContext, chartData) => {
        // eslint-disable-next-line no-new
        new Chart(domContext, {
          type: 'scatter',
          data: {
            labels: chartData.labels,
            datasets: [{
              data: chartData.data[0],
              label: chartData.keys[0],
              backgroundColor: $.otp.chart.colorList[0],
              borderColor: $.otp.chart.colorList[0]
            },
            {
              data: chartData.data[1],
              label: chartData.keys[1],
              backgroundColor: $.otp.chart.colorList[1],
              borderColor: $.otp.chart.colorList[1]
            },
            {
              data: chartData.data[2],
              label: chartData.keys[2],
              backgroundColor: $.otp.chart.colorList[2],
              borderColor: $.otp.chart.colorList[2]
            }]
          },
          options: {
            plugins: {
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
