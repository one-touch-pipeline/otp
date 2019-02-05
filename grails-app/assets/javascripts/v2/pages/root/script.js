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

/*jslint browser: true */
/*global $ */
//= require v2/rgraph

OTP = OTP || {};
OTP.pages = OTP.pages || {};
OTP.pages.root = OTP.pages.root || {};

OTP.pages.root.intro = {
    init: function () {
        "use strict";

        $('.js-account').focus();

        $.ajax({
            url: OTP.createLink({
                controller : 'root',
                action : 'introPatientsBySeqType'
            }),
            success: OTP.pages.root.intro.patientsCountPerSequenceType
        });

        $.ajax({
            url: OTP.createLink({
                controller : 'root',
                action : 'introSamplesBySeqType'
            }),
            success: OTP.pages.root.intro.sampleCountPerSequenceType
        });
    },

    patientsCountPerSequenceType : function(json) {
        "use strict";
        RGraph.reset(document.getElementById('js-patientsCountPerSequenceType'));
        new RGraph.Bar({
            id: 'js-patientsCountPerSequenceType',
            data: json.data,
            options: {
                backgroundGrid: false,
                colors: ['#025B9A'],
                gutterBottom: 80,
                labels: json.labels,
                labelsAbove: true,
                labelsAboveAngle: null,
                labelsAboveSize: 9,
                labelsOffsetx: 9,
                linewidth: 3,
                noaxes: true,
                strokestyle: 'white',
                textAccessible: false,
                textAngle: 45,
                textSize: 10,
                ylabels: false,
            }
        }).draw();
    },

    sampleCountPerSequenceType: function(json) {
        "use strict";
        RGraph.reset(document.getElementById('js-sampleCountPerSequenceTypePie'));
        new RGraph.Pie({
            id: 'js-sampleCountPerSequenceTypePie',
            data: json.data,
            options: {
                colors: ["#4A5964", "#025B9A", "#A4ACB1", "#32AE32", "#E6E6E5", "#FD7202", "#000000"],
                labels: json.labels,
                labelsSticks: true,
                labelsSticksLength: 15,
                linewidth: 3,
                radius: 80,
                stroke: 'white',
                strokestyle: 'white',
                textAccessible: false,
                textSize: 10,
            }
        }).draw();
    }
};
