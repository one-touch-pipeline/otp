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

/*jslint browser: true, devel: true */
/*global $, RGraph */

$.otp = $.otp || {};
$.otp.graph = {};

$.otp.graph.overview = {
    init : function() {
        "use strict";
        var url = $.otp.contextPath,
            projectGroup = $('#projectGroup_select').val();
        RGraph.AJAX(url + '/statistic/projectCountPerDate?projectGroupName='
                + projectGroup, function () {
                $.otp.graph.overview.projectCountPerDate(this, projectGroup);
            });
        RGraph.AJAX(url + '/statistic/laneCountPerDate?projectGroupName='
               + projectGroup, function () {
                $.otp.graph.overview.laneCountPerDate(this, projectGroup);
            });
        RGraph.AJAX(url + '/statistic/gigaBasesPerDay?projectGroupName='
            + projectGroup, function () {
            $.otp.graph.overview.gigaBasesPerDay(this, projectGroup);
        });
        RGraph.AJAX(url + '/statistic/sampleCountPerSequenceType?projectGroupName='
                + projectGroup, $.otp.graph.overview.sampleCountPerSequenceType);
        RGraph.AJAX(url + '/statistic/patientsCountPerSequenceType?projectGroupName='
                + projectGroup, $.otp.graph.overview.patientsCountPerSequenceType);
        RGraph.AJAX(url + '/statistic/projectCountPerSequenceType?projectGroupName='
                + projectGroup, $.otp.graph.overview.projectCountPerSequenceType);
    },

    projectCountPerDate : function(data, project) {
        "use strict";
        var json = JSON.parse(data.responseText);
        var count = json.count;
        if (count > 10) {
            count = 10;
        }
        RGraph.reset(document.getElementById('projectCountPerDate'));
        new RGraph.Scatter({
            id: 'projectCountPerDate',
            data: json.data,
            options: {
                backgroundGridAutofitNumvlines: json.labels.length,
                defaultcolor: '#1E5CA4',
                gutterBottom: 120,
                gutterLeft: 70,
                gutterRight: 70,
                gutterTop: 105,
                labels: json.labels,
                textAccessible: false,
                textAngle: 45,
                textSize: 8,
                tickmarks: 'circle',
                ticksize: 7,
                title: 'Number of projects in ' + project,
                titleColor: 'black',
                titleSize: 11,
                titleY: 39,
                xmax: json.daysCount,
                xmin: 0,
                ylabelsCount: count,
                ymax: json.count,
                ymin: 0,
            }
        }).draw();
    },

    laneCountPerDate : function(data, project) {
        "use strict";
        var json = JSON.parse(data.responseText);
        RGraph.reset(document.getElementById('laneCountPerDate'));
        new RGraph.Scatter({
            id: 'laneCountPerDate',
            data: json.data,
            options: {
                backgroundGridAutofitNumvlines: json.labels.length,
                defaultcolor: '#1E5CA4',
                gutterBottom: 100,
                gutterLeft: 70,
                gutterRight: 70,
                gutterTop: 110,
                labels: json.labels,
                textAccessible: false,
                textAngle: 45,
                textSize: 8,
                tickmarks: 'circle',
                ticksize: 6,
                title: 'Number of sequence lanes registered in ' + project,
                titleColor: 'black',
                titleSize: 11,
                titleY: 40,
                xmax: json.daysCount,
                xmin: 1,
                ylabels: true,
                ymin: 0,
            }
        }).draw();
    },

    gigaBasesPerDay : function(data, project) {
        "use strict";
        var json = JSON.parse(data.responseText);
        RGraph.reset(document.getElementById('gigaBasesPerDay'));
        new RGraph.Scatter({
            id: 'gigaBasesPerDay',
            data: json.data,
            options: {
                backgroundGridAutofitNumvlines: json.labels.length,
                defaultcolor: '#1E5CA4',
                gutterBottom: 100,
                gutterLeft: 70,
                gutterRight: 70,
                gutterTop: 110,
                labels: json.labels,
                textAccessible: false,
                textAngle: 45,
                textSize: 8,
                tickmarks: 'circle',
                ticksize: 6,
                title: 'giga bases registered in ' + project,
                titleColor: 'black',
                titleSize: 11,
                titleY: 40,
                xmax: json.daysCount,
                xmin: 1,
                ylabels: true,
                ymin: 0,
            }
        }).draw();
    },

    sampleCountPerSequenceType: function() {
        "use strict";
        var json = JSON.parse(this.responseText);
        RGraph.reset(document.getElementById('sampleCountPerSequenceTypePie'));
        new RGraph.Pie({
            id: 'sampleCountPerSequenceTypePie',
            data: json.data,
            options: {
                colors: [ '#f4858e', '#bfe2ca', '#a6daef', '#fc9b7a', '#bfb1d5', '#adddcf',
                    '#fed1be', '#f0e0a2', '#e8e7e5', '#c06c84', '#fed88f'],
                exploded: 5,
                gutterBottom: 80,
                gutterLeft: 60,
                gutterRight: 25,
                gutterTop: 80,
                labels: json.labelsPercentage,
                labelsSticks: true,
                labelsSticksLength: 15,
                linewidth: 1,
                radius: 120,
                shadow: true,
                shadowBlur: 4,
                shadowOffsetx: 12,
                shadowOffsety: 2,
                stroke: 'white',
                strokestyle: 'white',
                textSize: 8,
                title: 'Samples processed by sequencing technologies in ' + $('#projectGroup_select').val(),
                titleColor: 'black',
                titleSize: 11,
                titleX: 'center',
                titleY: 37,
            }
        }).roundRobin({frames: 13});
    },

    projectCountPerSequenceType : function() {
        "use strict";
        var json = JSON.parse(this.responseText);
        RGraph.reset(document.getElementById('projectCountPerSequenceTypeBar'));
        new RGraph.Bar({
            id: 'projectCountPerSequenceTypeBar',
            data: json.data,
            options: {
                backgroundGrid: false,
                backgroundGridBorder: false,
                colors: ['#1E5CA4'],
                gutterBottom: 120,
                gutterLeft: 40,
                gutterRight: 70,
                gutterTop: 110,
                hmargin: 9,
                hmarginGrouped: 1,
                labels: json.labels,
                labelsAbove: true,
                labelsAboveAngle: null,
                labelsAboveSize: 9,
                labelsOffsetx: 9,
                shadow: true,
                shadowBlur: 10,
                shadowColor: '#666',
                textAccessible: false,
                textAngle: 45,
                textSize: 7.4,
                ticksize: 7,
                title: 'Number of projects using sequencing technologies in ' + $('#projectGroup_select').val(),
                titleColor: 'black',
                titleSize: 11,
                titleX: 'center',
                titleY: 45,
            }
        }).draw();
    },

    patientsCountPerSequenceType : function() {
        "use strict";
        var json = JSON.parse(this.responseText);
        RGraph.reset(document.getElementById('patientsCountPerSequenceType'));
        new RGraph.Bar({
            id: 'patientsCountPerSequenceType',
            data: json.data,
            options: {
                backgroundGrid: false,
                backgroundGridBorder: false,
                colors: [ '#1E5CA4' ],
                gutterBottom: 140,
                gutterLeft: 40,
                gutterRight: 70,
                gutterTop: 110,
                hmargin: 9,
                hmarginGrouped: 1,
                labels: json.labels,
                labelsAbove: true,
                labelsAboveAngle: null,
                labelsAboveSize: 9,
                labelsOffsetx: 9,
                shadow: true,
                shadowBlur: 10,
                shadowColor: '#666',
                textAccessible: false,
                textAngle: 45,
                textSize: 7.4,
                ticksize: 7,
                title: 'Number of patients using sequencing technologies in ' + $('#projectGroup_select').val(),
                titleColor: 'black',
                titleSize: 11,
                titleX: 'center',
                titleY: 45,
            }
        }).draw();
    }
};

$.otp.graph.project = {
    init : function() {
        "use strict";
        var project = $('#project').find('option:selected').text();
        RGraph.AJAX(
            $.otp.createLink({
                controller: 'statistic',
                action: 'sampleTypeCountBySeqType',
                parameters: {projectName: project}
            }), $.otp.graph.project.sampleTypeCountBySeqType
        );
        RGraph.AJAX(
            $.otp.createLink({
                controller: 'statistic',
                action: 'sampleTypeCountByPatient',
                parameters: {projectName: project}
            }), $.otp.graph.project.sampleTypeCountByPatient
        );
        RGraph.AJAX(
            $.otp.createLink({
                controller: 'statistic',
                action: 'laneCountPerDateByProject',
                parameters: {projectName: project}
            }), $.otp.graph.project.laneCountPerDateByProject
        );
    },

    sampleTypeCountBySeqType : function() {
        "use strict";
        var json = JSON.parse(this.responseText);
        RGraph.reset(document.getElementById('sampleTypeCountBySeqType'));
        new RGraph.Pie({
            id: 'sampleTypeCountBySeqType',
            data: json.data,
            options: {
                colors: [ '#858137', '#2D6122', '#373502', '#ACAB90', '#7A5D07', '#5A1B02',
                    '#863312', '#1E5CA4', '#565B7A', '#9C702A' ],
                exploded: 1,
                gutterBottom: 80,
                gutterLeft: 60,
                gutterRight: 25,
                gutterTop: 80,
                labels: json.labelsPercentage,
                labelsSticks: true,
                labelsSticksLength: 15,
                linewidth: 1,
                radius: 80,
                shadow: true,
                shadowBlur: 4,
                shadowOffsetx: 12,
                shadowOffsety: 2,
                stroke: 'white',
                strokestyle: 'white',
                textSize: 8,
                title: 'Samples processed by different sequencing technologies',
                titleColor: 'black',
                titleSize: 11,
                titleX: 'center',
                titleY: 18,
            }
        }).draw();
    },

    laneCountPerDateByProject : function() {
        "use strict";
        var json = JSON.parse(this.responseText);
        var count = json.count;
        if (count > 10) {
            count = 10;
        }
        RGraph.reset(document.getElementById('laneCountPerDateByProject'));
        new RGraph.Scatter({
            id: 'laneCountPerDateByProject',
            data: json.data,
            options: {
                backgroundGridAutofitNumvlines: json.labels.length,
                defaultcolor: '#1E5CA4',
                gutterBottom: 110,
                gutterLeft: 90,
                gutterRight: 70,
                gutterTop: 100,
                labels: json.labels,
                textAccessible: false,
                textAngle: 65,
                textSize: 8,
                tickmarks: 'circle',
                ticksize: 7,
                title: 'Number of sequence lanes registered for ' + $('#project_select').val(),
                titleColor: 'black',
                titleSize: 11,
                titleY: 18,
                xmax: json.daysCount,
                xmin: 0,
                ylabelsCount: count,
                ymax: json.count,
                ymin: 0,
            }
        }).draw();
    },

    sampleTypeCountByPatient : function() {
        "use strict";
        var json = JSON.parse(this.responseText);
        var canvas = document.getElementById('sampleTypeCountByPatient');
        canvas.height = ((json.count * 15) + 250);

        RGraph.reset(canvas);
        new RGraph.HBar({
            id: 'sampleTypeCountByPatient',
            data: json.data,
            options: {
                backgroundGrid: false,
                colors: [ '#1E5CA4' ],
                gutterBottom: 150,
                gutterLeft: 175,
                gutterTop: 70,
                labels: json.labels,
                labelsAbove: true,
                labelsAboveAngle: null,
                labelsAboveSize: 9,
                linewidth: 50,
                margin: 3,
                noxaxis: true,
                noxtickmarks: true,
                shadow: true,
                shadowColor: '#aaa',
                shadowOffsetx: 0,
                shadowOffsety: 0,
                strokestyle: 'rgba(0,0,0,0)',
                textAngle: 30,
                textSize: 8,
                title: 'Patients and the number of samples (non-redundant)',
                titleColor: 'black',
                titleSize: 11,
                titleX: 300,
                unitsPost: ' Sample(s)',
                xlabels: false,
                xmax: 13,
                eventsClick: function (e, shape) {
                    var idx = shape[5];
                    location.href =  $.otp.createLink({
                        controller: 'individual',
                        action: 'show',
                        id: json.labels[idx]
                    });
                },
                eventsMousemove: function (e, shape) {
                    return true;
                }
            }
        }).draw();
    }
};

$.otp.graph.info = {
    init: function () {
        "use strict";
        var url = $.otp.contextPath;
        RGraph.AJAX(url + '/info/projectCountPerDate', function () {
            $.otp.graph.overview.projectCountPerDate(this, "OTP");
        });
        RGraph.AJAX(url + '/info/laneCountPerDate', function () {
            $.otp.graph.overview.laneCountPerDate(this, "OTP");
        });
    }
};
