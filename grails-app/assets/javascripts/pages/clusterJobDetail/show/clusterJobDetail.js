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

$.otp.clusterJobDetailProgress = {
  register(id) {
    'use strict';

    const dataUrl = $.otp.createLink({
      controller: 'clusterJobDetail',
      action: 'getStatesTimeDistribution',
      parameters: { id }
    });

    Chart.register(ChartDataLabels);

    if (document.getElementById('delayPieChart') != null) {
      $.otp.chart.renderChartOnElement(
        'delayPieChart',
        dataUrl,
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
    }
  }
};
