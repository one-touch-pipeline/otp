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

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificMemories',
            parameters: {'jobClass': jobClassSelect, 'seqType': seqTypeSelect, 'from': startDate, 'to': $.otp.clusterJobJobTypeSpecific.getToday()}
        }),function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificMemoryUsage(this);
        });
    },

    update: function () {
        "use strict";
        RGraph.Clear($('canvas').get(0));
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

    getJobTypeSpecificMemoryUsage: function (data) {
        "use strict";
        $.otp.clusterJobJobTypeSpecificGraph.generateScatterGraphic('jobTypeSpecificGraphMemories', data);
    },

    generatePieGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.Reset($('#' + id).get(0));
        var graph = new RGraph.Pie(id, json.data);
        graph.Set('chart.shadow.offsetx', 5);
        graph.Set('chart.shadow.offsety', 5);
        graph.Set('chart.shadow.blur', 15);
        graph.Set('chart.colors', $.otp.clusterJobJobTypeSpecific.getColors(json.data.length));
        graph.Set('chart.linewidth', 2);
        graph.Set('chart.exploded', 3);
        graph.Set('chart.radius', 80);
        graph.Set('key', json.labels);
        graph.Set('key.colors', $.otp.clusterJobJobTypeSpecific.getColors(json.data.length));
        graph.Set('key.rounded', false);
        graph.Set('centerx', 120);
        graph.Draw();
    },

    generateLineGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.Reset($('#' + id).get(0));
        var graph = new RGraph.Line(id, json.data);
        graph.Set('labels', $.otp.clusterJobJobTypeSpecific.normalizeLabels(json.labels));
        graph.Set('text.angle', 45);
        graph.Set('text.size', 8);
        graph.Set('numxticks', json.labels.length - 1);
        graph.Set('background.grid.autofit.align', true);
        graph.Set('chart.gutter.bottom', 100);
        graph.Set('chart.gutter.left', 80);
        graph.Set('key', json.keys);
        graph.Draw();
    },

    generateScatterGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.Reset($('#' + id).get(0));
        var graph = new RGraph.Scatter(id, json.data);
        graph.Set('xmax', json.xMax);
        graph.Set('tickmarks', 'circle');
        graph.Set('ticksize', 10);
        graph.Set('title.xaxis', 'USED');
        graph.Set('title.yaxis', 'REQUESTED');
        graph.Set('chart.gutter.bottom', 100);
        graph.Set('chart.gutter.left', 120);
        graph.Set('title.yaxis.pos', 0.1);
        graph.Set('title.xaxis.pos', 0.3);
        graph.Set('labels', json.labels[0]);
        graph.Set('text.angle', 45);
        graph.Set('text.size', 8);
        graph.Set('events.click', function (e, shape) {
            var index = shape[4];
            var id = shape['object']['data'][0][index][2];
            window.location.href = $.otp.createLink({
                controller: 'clusterJobDetail',
                action: 'show',
                id: id
            });
        });
        graph.Set('events.mousemove', function (e, shape) {
            e.target.style.cursor = 'pointer';
        });
        graph.Draw();
    }
};
