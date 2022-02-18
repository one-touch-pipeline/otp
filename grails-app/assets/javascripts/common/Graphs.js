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

$.Graphs = {
  colors: ['#81BEF7', '#A9BCF5', '#5882FA', '#0431B4', '#00BFFF', '#A9F5F2', '#088A85', '#9F81F7'],

  drawPieGraph(id, data) {
    'use strict';

    const json = JSON.parse(data.response);
    RGraph.reset($(`#${id}`).get(0));
    new RGraph.Pie({
      id,
      data: json.data,
      options: {
        centerx: 120,
        colors: $.Graphs.getColors(json.data.length),
        exploded: 3,
        key: json.keys,
        keyColors: $.Graphs.getColors(json.data.length),
        keyRounded: false,
        labels: json.labels,
        linewidth: 1,
        radius: 80,
        shadowBlur: 15,
        shadowOffsetx: 5,
        shadowOffsety: 5,
        textSize: 8
      }
    }).draw();
  },

  drawLineGraph(id, data) {
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
        labels: $.Graphs.normalizeLabels(json.labels),
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
