/*
 * Copyright 2011-2022 The OTP authors
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

$.otp.sharedCharts = {

  // eslint-disable-next-line strict
  renderSampleCountPerSeqType(projectGroup = 'All projects') {
    Chart.register(ChartDataLabels);

    const dataUrl = $.otp.createLink({
      controller: 'statistic',
      action: 'sampleCountPerSequenceType',
      parameters: { projectGroupName: projectGroup }
    });

    $.otp.chart.renderChartOnElement('sampleCountPerSequenceTypePie', dataUrl, (domContext, chartData) => {
      // eslint-disable-next-line no-new
      new Chart(domContext, {
        type: 'pie',
        data: {
          labels: chartData.labels,
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
        options: $.otp.chart.defaultChartOptions('Samples by Sequencing Technologies', {
          scales: {
            y: {
              display: false
            }
          }
        })
      });
    });
  },

  // eslint-disable-next-line strict
  renderSampleCountPerSeqTypeBySelectedProject() {
    Chart.register(ChartDataLabels);

    const dataUrl = $.otp.createLink({
      controller: 'statistic',
      action: 'sampleTypeCountBySeqType'
    });

    $.otp.chart.renderChartOnElement('sampleCountPerSequenceTypePie', dataUrl, (domContext, chartData) => {
      // eslint-disable-next-line no-new
      new Chart(domContext, {
        type: 'pie',
        data: {
          labels: chartData.labels,
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
        options: $.otp.chart.defaultChartOptions('Samples by Sequencing Technologies', {
          scales: {
            y: {
              display: false
            }
          }
        })
      });
    });
  },

  // eslint-disable-next-line strict
  renderProjectCountPerDate(projectGroup = 'All projects') {
    const dataUrl = $.otp.createLink({
      controller: 'statistic',
      action: 'projectCountPerDate',
      parameters: { projectGroupName: projectGroup }
    });

    $.otp.chart.renderChartOnElement('projectCountPerDate', dataUrl, (domContext, chartData) => {
      // eslint-disable-next-line no-new
      new Chart(domContext, {
        type: 'line',
        data: {
          labels: chartData.labels,
          datasets: [{
            label: 'Number of Projects',
            data: chartData.data,
            backgroundColor: $.otp.chart.colorList[0]
          }]
        },
        options: $.otp.chart.defaultChartOptions('Number of Projects')
      });
    });
  },

  // eslint-disable-next-line strict
  renderLaneCountPerDate(projectGroup = 'All projects') {
    const dataUrl = $.otp.createLink({
      controller: 'statistic',
      action: 'laneCountPerDate',
      parameters: { projectGroupName: projectGroup }
    });

    $.otp.chart.renderChartOnElement('laneCountPerDate', dataUrl, (domContext, chartData) => {
      // eslint-disable-next-line no-new
      new Chart(domContext, {
        type: 'line',
        data: {
          datasets: [{
            label: 'Lane Count per Date',
            data: chartData.data,
            backgroundColor: $.otp.chart.colorList[0]
          }]
        },
        options: $.otp.chart.defaultChartOptions('Number of Sequence Lanes')
      });
    });
  },

  // eslint-disable-next-line strict
  renderLaneCountPerDateBySelectedProject() {
    const dataUrl = $.otp.createLink({
      controller: 'statistic',
      action: 'laneCountPerDateByProject'
    });

    $.otp.chart.renderChartOnElement('laneCountPerDate', dataUrl, (domContext, chartData) => {
      // eslint-disable-next-line no-new
      new Chart(domContext, {
        type: 'line',
        data: {
          datasets: [{
            label: 'Lane Count per Date',
            data: chartData.data,
            backgroundColor: $.otp.chart.colorList[0]
          }]
        },
        options: $.otp.chart.defaultChartOptions('Number of Sequence Lanes')
      });
    });
  },

  // eslint-disable-next-line strict
  renderGigaBasesPerDay(projectGroup = 'All projects') {
    const dataUrl = $.otp.createLink({
      controller: 'statistic',
      action: 'gigaBasesPerDay',
      parameters: { projectGroupName: projectGroup }
    });

    $.otp.chart.renderChartOnElement('gigaBasesPerDay', dataUrl, (domContext, chartData) => {
      // eslint-disable-next-line no-new
      new Chart(domContext, {
        type: 'line',
        data: {
          datasets: [{
            label: 'Giga Bases per Date',
            data: chartData.data,
            backgroundColor: $.otp.chart.colorList[0]
          }]
        },
        options: $.otp.chart.defaultChartOptions('Giga Bases')
      });
    });
  },

  // eslint-disable-next-line strict
  renderPatientsCountPerSequenceType(projectGroup = 'All projects') {
    const dataUrl = $.otp.createLink({
      controller: 'statistic',
      action: 'patientsCountPerSequenceType',
      parameters: { projectGroupName: projectGroup }
    });

    $.otp.chart.renderChartOnElement('patientsCountPerSequenceType', dataUrl, (domContext, chartData) => {
      // eslint-disable-next-line no-new
      new Chart(domContext, {
        type: 'bar',
        data: {
          labels: chartData.labels,
          datasets: [{
            label: 'Sequencing Technologies',
            data: chartData.data,
            backgroundColor: $.otp.chart.colorList[0]
          }]
        },
        options: $.otp.chart.defaultChartOptions('Number of Patients using Sequencing Technologies')
      });
    });
  },

  // eslint-disable-next-line strict
  renderProjectCountPerSequenceType(projectGroup = 'All projects') {
    const dataUrl = $.otp.createLink({
      controller: 'statistic',
      action: 'projectCountPerSequenceType',
      parameters: { projectGroupName: projectGroup }
    });

    $.otp.chart.renderChartOnElement('projectCountPerSequenceType', dataUrl, (domContext, chartData) => {
      // eslint-disable-next-line no-new
      new Chart(domContext, {
        type: 'bar',
        data: {
          labels: chartData.labels,
          datasets: [{
            label: 'Sequencing Technologies',
            data: chartData.data,
            backgroundColor: $.otp.chart.colorList[0]
          }]
        },
        options: $.otp.chart.defaultChartOptions('Number of Projects using Sequencing Technologies')
      });
    });
  }
};
