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

$.otp.chart = {
  colorList: [
    'rgba(54, 162, 235)',
    'rgba(255, 99, 132)',
    'rgba(255, 206, 86)',
    'rgba(75, 192, 192)',
    'rgba(255, 159, 64)',
    'rgba(153, 102, 255)',
    'rgba(54, 162, 235, 0.2)',
    'rgba(255, 99, 132, 0.2)',
    'rgba(255, 206, 86, 0.2)',
    'rgba(75, 192, 192, 0.2)',
    'rgba(255, 159, 64, 0.2)',
    'rgba(153, 102, 255, 0.2)'
  ],

  /**
   * Generate the most used options for charts in otp.
   *
   * @param title for the chart, default is no title
   * @param additionalOptions any additional or override chart options
   * @returns object with default options
   */
  // eslint-disable-next-line strict
  defaultChartOptions(title, additionalOptions = {}) {
    // eslint-disable-next-line prefer-object-spread
    return {
      showLine: false,
      scales: {
        y: {
          min: 0,
          ticks: {
            stepSize: 1
          }
        },
        x: {
          offset: true
        }
      },
      plugins: {
        datalabels: {
          display: false
        },
        title: {
          display: !!title,
          font: { size: '14px' },
          text: title
        },
        legend: {
          position: 'bottom'
        }
      },
      ...additionalOptions
    };
  },

  /**
   * Default render function for charts. It fetches the data on the provided url and renders the
   * chart into the provided elementId with the given chartFunction.
   *
   * @param elementId, dom id of the chart canvas
   * @param dataUrl, url of the controller method for the chart data
   * @param chartFunction, callback function which contains the chart definition
   */
  // eslint-disable-next-line strict
  renderChartOnElement(elementId, dataUrl, chartFunction) {
    $.otp.chart.destroyChartOnElementIfExists(elementId);
    fetch(dataUrl)
      .then((response) => response.json())
      .then((chartData) => {
        const ctx = document.getElementById(elementId);
        chartFunction(ctx, chartData);
      })
      .catch((error) => $.otp.toaster.showErrorToast(`Loading data for ${elementId} failed`, error));
  },

  destroyChartOnElementIfExists(elementId) {
    const chart = Chart.getChart(elementId);
    if (chart) {
      chart.destroy();
    }
  }
};
