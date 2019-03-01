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

$.otp.clusterJobJobTypeSpecific = {

    colors : ['#81BEF7', '#A9BCF5', '#5882FA', '#0431B4', '#00BFFF', '#A9F5F2', '#088A85', '#9F81F7'],

    register : function () {
        "use strict";
        $('#dpFrom').datepicker({
                dateFormat: 'yy-mm-dd',
                onSelect: function (selected) {
                    $.otp.clusterJobJobTypeSpecificGraph.update();
                    $.otp.clusterJobJobTypeSpecific.updateJobClassSelect();
                    $('#dpTo').datepicker('option', 'minDate', selected);
                },
                maxDate: $('#dpTo').val()
        });
        $('#dpTo').datepicker({
                dateFormat: 'yy-mm-dd',
                onSelect: function (selected) {
                    $.otp.clusterJobJobTypeSpecificGraph.update();
                    $.otp.clusterJobJobTypeSpecific.updateJobClassSelect();
                    $('#dpFrom').datepicker('option', 'maxDate', selected);
                },
                minDate: $('#dpFrom').val(),
                maxDate: $('#dpTo').val()
        });
        $('#jobClassSelect').change(function () {
            $.otp.clusterJobJobTypeSpecific.updateSeqTypeSelect();
        });
        $('#seqTypeSelect').change(function () {
            $.otp.clusterJobJobTypeSpecificGraph.update();
            $.otp.clusterJobJobTypeSpecific.updateAvgValues();
        });
        $('#basesInput').change(function () {
            $.otp.clusterJobJobTypeSpecificGraph.update();
            $.otp.clusterJobJobTypeSpecific.updateAvgValues();
        });
        $('#coverageInput').change(function () {
            $.otp.clusterJobJobTypeSpecificGraph.update();
            $.otp.clusterJobJobTypeSpecific.updateAvgValues();
        });
    },

    updateAvgValues : function () {
        "use strict";
        var startDate = $('#dpFrom').val();
        var endDate = $('#dpTo').val();

        var jobClassSelect = $('#jobClassSelect').val();
        var seqTypeSelect = $('#seqTypeSelect').val();
        var basesInput = $('#basesInput').val();
        var coverageInput = $('#coverageInput').val();

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificAvgMemory',
            parameters: {'jobClass': jobClassSelect, 'seqType': seqTypeSelect, 'from': startDate, 'to': endDate}
        }), function () {
            var json = JSON.parse(this.response);
            $('#jobTypeSpecificAvgMemory').html(json.data);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificAvgCoreUsage',
            parameters: {'jobClass': jobClassSelect, 'seqType': seqTypeSelect, 'from': startDate, 'to': endDate}
        }), function () {
            var json = JSON.parse(this.response);
            $('#jobTypeSpecificAvgCPU').html(json.data);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificStatesTimeDistribution',
            parameters: {'jobClass': jobClassSelect, 'seqType': seqTypeSelect, 'bases': basesInput, 'coverage': coverageInput, 'from': startDate, 'to': endDate}
        }), function () {
            var json = JSON.parse(this.response);
            $("#jobTypeSpecificAvgDelay").html(json.data.avgQueue);
            $("#jobTypeSpecificAvgProcessing").html(json.data.avgProcess);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificCoverageStatistics',
            parameters: {'jobClass': jobClassSelect, 'seqType': seqTypeSelect, 'bases': basesInput, 'from': startDate, 'to': endDate}
        }), function () {
            var json = JSON.parse(this.response);
            $("#jobTypeSpecificMinCov").html(json.data.minCov);
            $("#jobTypeSpecificAvgCov").html(json.data.avgCov);
            $("#jobTypeSpecificMaxCov").html(json.data.maxCov);
            $("#jobTypeSpecificMedianCov").html(json.data.medianCov);
        });

    },

    updateJobClassSelect : function () {
        "use strict";
        var jobClassSelect = $('#jobClassSelect');
        var currentJobClass = jobClassSelect.val();
        jobClassSelect.find('option').remove();
        RGraph.AJAX($.otp.createLink({
            controller: 'clusterJobJobTypeSpecific',
            action: 'getJobClassesByDate',
            parameters: {'from': $('#dpFrom').val(), 'to': $('#dpTo').val()}
        }), function () {
            var json = JSON.parse(this.response);
            $.each(json.data, function () {
                var cOption = $('<option>', {
                                   value: this,
                                   text: this
                                });
                jobClassSelect.append(cOption);
                if (currentJobClass == this) {
                    cOption.attr('selected','selected');
                }
            });
            $.otp.clusterJobJobTypeSpecific.updateSeqTypeSelect();
        });
    },

    updateSeqTypeSelect : function () {
        "use strict";
        var seqTypeSelect = $('#seqTypeSelect');
        var currentSeqType = seqTypeSelect.val();
        seqTypeSelect.find('option').remove();
        RGraph.AJAX($.otp.createLink({
            controller: 'clusterJobJobTypeSpecific',
            action: 'getSeqTypesByJobClass',
            parameters: {'jobClass': $('#jobClassSelect').val(), 'from': $('#dpFrom').val(), 'to': $('#dpTo').val()}
        }), function () {
            var json = JSON.parse(this.response);
            $.each(json.data, function () {
                var cOption = $('<option>', {
                                  value: this.id,
                                  text: this.name + " " + this.libraryLayout
                              });
                seqTypeSelect.append(cOption);
                if (currentSeqType == this.id) {
                    cOption.attr('selected','selected');
                }
            });
            $.otp.clusterJobJobTypeSpecificGraph.update();
            $.otp.clusterJobJobTypeSpecific.updateAvgValues();
        });
    },

    getColors : function (elementCount) {
        "use strict";
        return this.colors.slice(0, elementCount);
    },

    getToday : function () {
        "use strict";
        return $.datepicker.formatDate("yy-mm-dd", new Date());
    },

    normalizeLabels : function (labels) {
        "use strict";
        var step = Math.floor(labels.length / 24);
        return labels.filter(function (value_ignored, index) {
            return index % step === 0
        });
    }
};

$.otp.clusterJobJobTypeSpecificGraph = {
    register : function () {
        "use strict";
        var startDate = $('#dpFrom').val();
        var endDate = $('#dpTo').val();

        var jobClassSelect = $('#jobClassSelect').val();
        var seqTypeSelect = $('#seqTypeSelect').val();

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificExitCodes',
            parameters: {'jobClass': jobClassSelect, 'seqType': seqTypeSelect, 'from': startDate, 'to': endDate}
        }), function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificExitCodes(this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificExitStatuses',
            parameters: {'jobClass': jobClassSelect, 'seqType': seqTypeSelect, 'from': startDate, 'to': endDate}
        }), function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificExitStatuses(this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificStates',
            parameters: {'jobClass': jobClassSelect, 'seqType': seqTypeSelect, 'from': startDate, 'to': endDate}
        }), function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificStates(this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificWalltimes',
            parameters: {'jobClass': jobClassSelect, 'seqType': seqTypeSelect, 'from': startDate, 'to': endDate}
        }), function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificWalltimes(this);
        });
    },

    update: function () {
        "use strict";
        $.otp.clusterJobJobTypeSpecificGraph.register();
    },

    getJobTypeSpecificExitCodes : function (data) {
        "use strict";
        $.otp.clusterJobJobTypeSpecificGraph.generatePieGraphic('jobTypeSpecificGraphExitCode', data);
    },

    getJobTypeSpecificExitStatuses : function (data) {
        "use strict";
        $.otp.clusterJobJobTypeSpecificGraph.generatePieGraphic('jobTypeSpecificGraphExitStatus', data);
    },

    getJobTypeSpecificStates : function (data) {
        "use strict";
        $.otp.clusterJobJobTypeSpecificGraph.generateLineGraphic('jobTypeSpecificGraphStates', data);
    },

    getJobTypeSpecificWalltimes: function (data) {
        "use strict";
        $.otp.clusterJobJobTypeSpecificGraph.generateScatterGraphic('jobTypeSpecificGraphWalltimes', data);
    },

    generatePieGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.reset($('#' + id).get(0));
        new RGraph.Pie({
            id: id,
            data: json.data,
            options: {
                centerx: 120,
                colors: $.otp.clusterJobJobTypeSpecific.getColors(json.data.length),
                exploded: 3,
                key: json.labels,
                keyColors: $.otp.clusterJobJobTypeSpecific.getColors(json.data.length),
                keyRounded: false,
                linewidth: 1,
                radius: 80,
                shadowBlur: 15,
                shadowOffsetx: 5,
                shadowOffsety: 5,
                textSize: 8,
            }
        }).draw();
    },

    generateLineGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.reset($('#' + id).get(0));
        new RGraph.Line({
            id: id,
            data: json.data,
            options: {
                backgroundGridAutofitAlign: true,
                gutterBottom: 100,
                gutterLeft: 80,
                key: json.keys,
                labels: $.otp.clusterJobJobTypeSpecific.normalizeLabels(json.labels),
                numxticks: json.labels.length - 1,
                textAccessible: false,
                textAngle: 45,
                textSize: 8,
            }
        }).draw();
    },

    generateScatterGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.reset($('#' + id).get(0));
        new RGraph.Scatter({
            id: id,
            data: json.data,
            options: {
                gutterBottom: 100,
                gutterLeft: 120,
                labels: json.labels[0],
                textAccessible: false,
                textAngle: 45,
                textSize: 8,
                tickmarks: 'circle',
                ticksize: 10,
                titleXaxis: 'Million Reads',
                titleXaxisPos: 0.3,
                titleYaxis: 'Walltime in Minutes',
                titleYaxisPos: 0.1,
                xmax: json.xMax,
                eventsClick: function (e, shape) {
                    var index = shape[4];
                    var id = shape['object']['data'][0][index][3];
                    location.href = $.otp.createLink({
                        controller: 'clusterJobDetail',
                        action: 'show',
                        id: id
                    });
                },
                eventsMousemove: function (e, shape) {
                    return true;
                }
            }
        }).draw();
    }
};
