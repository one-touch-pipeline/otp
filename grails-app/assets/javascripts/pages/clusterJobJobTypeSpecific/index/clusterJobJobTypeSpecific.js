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

$.otp.clusterJobJobTypeSpecific = {

  register() {
    'use strict';

    $.otp.clusterJobJobTypeSpecific.renderCharts();

    $('.datePicker').on('change', () => {
      $.otp.clusterJobJobTypeSpecific.updateJobClassSelect();
      $('#dpTo').attr('min', $('#dpFrom').val());
      $('#dpFrom').attr('max', $('#dpTo').val());
      $.otp.clusterJobJobTypeSpecific.renderCharts();
    });
    $('#jobClassSelect').on('change', () => {
      $.otp.clusterJobJobTypeSpecific.updateSeqTypeSelect();
    });
    $('#seqTypeSelect').on('change', () => {
      $.otp.clusterJobJobTypeSpecific.updateAvgValues();
      $.otp.clusterJobJobTypeSpecific.renderCharts();
    });
    $('#basesInput').on('change', () => {
      $.otp.clusterJobJobTypeSpecific.updateAvgValues();
      $.otp.clusterJobJobTypeSpecific.renderCharts();
    });
    $('#coverageInput').on('change', () => {
      $.otp.clusterJobJobTypeSpecific.updateAvgValues();
      $.otp.clusterJobJobTypeSpecific.renderCharts();
    });
  },

  updateJobClassSelect() {
    'use strict';

    const jobClassSelect = $('#jobClassSelect');
    const currentJobClass = jobClassSelect.val();
    jobClassSelect.find('option').remove();

    const dataUrl = $.otp.createLink({
      controller: 'clusterJobJobTypeSpecific',
      action: 'getJobClassesByDate',
      parameters: {
        from: $('#dpFrom').val(),
        to: $('#dpTo').val()
      }
    });

    fetch(dataUrl)
      .then((response) => response.json())
      .then((json) => {
        $.each(json.data, function () {
          const cOption = $('<option>', {
            value: this,
            text: this
          });
          jobClassSelect.append(cOption);
          if (currentJobClass === this) {
            cOption.attr('selected', 'selected');
          }
        });
        $.otp.clusterJobJobTypeSpecific.updateSeqTypeSelect();
      });
  },

  updateSeqTypeSelect() {
    'use strict';

    const seqTypeSelect = $('#seqTypeSelect');
    const currentSeqType = seqTypeSelect.val();
    seqTypeSelect.find('option').remove();

    const dataUrl = $.otp.createLink({
      controller: 'clusterJobJobTypeSpecific',
      action: 'getSeqTypesByJobClass',
      parameters: {
        jobClass: $('#jobClassSelect').val(),
        from: $('#dpFrom').val(),
        to: $('#dpTo').val()
      }
    });

    fetch(dataUrl)
      .then((response) => response.json())
      .then((json) => {
        $.each(json.data, function () {
          const cOption = $('<option>', {
            value: this.id,
            text: `${this.name}`
          });
          seqTypeSelect.append(cOption);
          if (currentSeqType === this.id) {
            cOption.attr('selected', 'selected');
          }
        });
        $.otp.clusterJobJobTypeSpecific.renderCharts();
        $.otp.clusterJobJobTypeSpecific.updateAvgValues();
      });
  },

  updateAvgValues() {
    'use strict';

    const startDate = $('#dpFrom').val();
    const endDate = $('#dpTo').val();

    const jobClassSelect = $('#jobClassSelect').val();
    const seqTypeSelect = $('#seqTypeSelect').val();
    const basesInput = $('#basesInput').val();
    const coverageInput = $('#coverageInput').val();

    const avgMemoryDataUrl = $.otp.createLink({
      controller: 'clusterJobJobTypeSpecific',
      action: 'getJobTypeSpecificAvgMemory',
      parameters: {
        jobClass: jobClassSelect,
        seqType: seqTypeSelect,
        from: startDate,
        to: endDate
      }
    });

    fetch(avgMemoryDataUrl)
      .then((response) => response.json())
      .then((json) => {
        $('#jobTypeSpecificAvgMemory').html(json.data);
      });

    const avgCoreUsageDataUrl = $.otp.createLink({
      controller: 'clusterJobJobTypeSpecific',
      action: 'getJobTypeSpecificAvgCoreUsage',
      parameters: {
        jobClass: jobClassSelect,
        seqType: seqTypeSelect,
        from: startDate,
        to: endDate
      }
    });

    fetch(avgCoreUsageDataUrl)
      .then((response) => response.json())
      .then((json) => {
        $('#jobTypeSpecificAvgCPU').html(json.data);
      });

    const avgSpecificStatesDataUrl = $.otp.createLink({
      controller: 'clusterJobJobTypeSpecific',
      action: 'getJobTypeSpecificStatesTimeDistribution',
      parameters: {
        jobClass: jobClassSelect,
        seqType: seqTypeSelect,
        bases: basesInput,
        coverage: coverageInput,
        from: startDate,
        to: endDate
      }
    });

    fetch(avgSpecificStatesDataUrl)
      .then((response) => response.json())
      .then((json) => {
        $('#jobTypeSpecificAvgDelay').html(json.data.avgQueue);
        $('#jobTypeSpecificAvgProcessing').html(json.data.avgProcess);
      });

    const coverageStatsDataUrl = $.otp.createLink({
      controller: 'clusterJobJobTypeSpecific',
      action: 'getJobTypeSpecificCoverageStatistics',
      parameters: {
        jobClass: jobClassSelect,
        seqType: seqTypeSelect,
        bases: basesInput,
        from: startDate,
        to: endDate
      }
    });

    fetch(coverageStatsDataUrl)
      .then((response) => response.json())
      .then((json) => {
        $('#jobTypeSpecificMinCov').html(json.data.minCov);
        $('#jobTypeSpecificAvgCov').html(json.data.avgCov);
        $('#jobTypeSpecificMaxCov').html(json.data.maxCov);
        $('#jobTypeSpecificMedianCov').html(json.data.medianCov);
      });
  },

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
        const formattedData = {
          data: chartData.data.map((dataSet) => ({
            x: dataSet[0],
            y: dataSet[1]
          }
          ))
        };

        const colorList = chartData.data.reduce((acc, val) => {
          acc.push(val[2]);
          return acc;
        }, []);

        // eslint-disable-next-line no-new
        new Chart(domContext, {
          type: 'scatter',
          data: {
            datasets: [{
              data: formattedData.data,
              label: 'Walltime',
              backgroundColor: colorList,
              borderColor: colorList
            }]
          },
          options: {
            plugins: {
              datalabels: {
                display: false
              },
              legend: {
                display: false
              }
            },
            scales: {
              yAxes: {
                title: {
                  display: true,
                  font: { size: '14px' },
                  text: 'Walltime in Minutes'
                },
                min: 0
              },
              xAxes: {
                title: {
                  display: true,
                  font: { size: '14px' },
                  text: 'Million Reads'
                }
              }
            }
          }
        });
      }
    );
  }
};
