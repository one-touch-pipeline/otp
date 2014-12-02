/*jslint browser: true */
/*global $ */

$.otp.clusterJobJobTypeSpecific = {
<<<<<<< HEAD

    colors : ['#81BEF7', '#A9BCF5', '#5882FA', '#0431B4', '#00BFFF', '#A9F5F2', '#088A85', '#9F81F7'],

    register : function () {
        "use strict";
=======
    register : function () {
>>>>>>> detailPage
        $('.datePicker').datepicker(
            {
                dateFormat: 'yy-mm-dd',
                onSelect: function () {
                    $.otp.clusterJobJobTypeSpecificGraph.update();
                    $.otp.clusterJobJobTypeSpecific.updateJobClassSelect();
                }
            }
        );
<<<<<<< HEAD
        $('#jobClassSelect').change(function () {
            $.otp.clusterJobJobTypeSpecific.updateSeqTypeSelect();
        })
        $('#seqTypeSelect').change(function () {
=======
        $('#jobClassSelect').change(function() {
            $.otp.clusterJobJobTypeSpecific.updateSeqTypeSelect();
        })
        $('#seqTypeSelect').change(function() {
>>>>>>> detailPage
            $.otp.clusterJobJobTypeSpecificGraph.update();
            $.otp.clusterJobJobTypeSpecific.updateAvgValues();
        })
    },

    updateAvgValues : function () {
<<<<<<< HEAD
        "use strict";
=======
>>>>>>> detailPage

        var startDate = $('#dpFrom').val();
        var endDate = $('#dpTo').val();

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificAvgMemory',
<<<<<<< HEAD
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(), 'from': startDate, 'to': endDate}
=======
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(),'from': startDate, 'to': endDate}
>>>>>>> detailPage
        }), function () {
            var json = JSON.parse(this.response);
            $('#jobTypeSpecificAvgMemory').html(json.data);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificAvgCoreUsage',
<<<<<<< HEAD
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(), 'from': startDate, 'to': endDate}
=======
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(),'from': startDate, 'to': endDate}
>>>>>>> detailPage
        }), function () {
            var json = JSON.parse(this.response);
            $('#jobTypeSpecificAvgCPU').html(json.data);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificStatesTimeDistribution',
<<<<<<< HEAD
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(), 'from': startDate, 'to': endDate}
        }), function () {
            var json = JSON.parse(this.response);
            $("#jobTypeSpecificAvgDelay").html(json.data.avgQueue);
            $("#jobTypeSpecificAvgProcessing").html(json.data.avgProcess);
=======
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(),'from': startDate, 'to': endDate}
        }), function () {
            var json = JSON.parse(this.response);
            $("#jobTypeSpecificAvgDelay").html(json.data['avgQueue']);
            $("#jobTypeSpecificAvgProcessing").html(json.data['avgProcess']);
>>>>>>> detailPage
        });

    },

    updateJobClassSelect : function () {
<<<<<<< HEAD
        "use strict";
=======
>>>>>>> detailPage
        $('#jobClassSelect').find('option').remove();
        RGraph.AJAX($.otp.createLink({
            controller: 'clusterJobJobTypeSpecific',
            action: 'getJobClassesByDate',
            parameters: {'from': $('#dpFrom').val(), 'to': $('#dpTo').val()}
        }), function () {
            var json = JSON.parse(this.response);
            $.each(json.data, function () {
                $('#jobClassSelect').append($('<option>', {
                   value: this,
                   text: this
               }));
<<<<<<< HEAD
            });
            $.otp.clusterJobJobTypeSpecific.updateSeqTypeSelect();
        });
    },

    updateSeqTypeSelect : function () {
        "use strict";
=======
            })
            $.otp.clusterJobJobTypeSpecific.updateSeqTypeSelect();
        })
    },

    updateSeqTypeSelect : function () {
>>>>>>> detailPage
        $('#seqTypeSelect').find('option').remove();
        RGraph.AJAX($.otp.createLink({
            controller: 'clusterJobJobTypeSpecific',
            action: 'getSeqTypesByJobClass',
            parameters: {'jobClass': $('#jobClassSelect').val()}
        }), function () {
            var json = JSON.parse(this.response);
<<<<<<< HEAD
            $.each(json.data, function () {
=======
            $.each(json.data, function() {
>>>>>>> detailPage
                $('#seqTypeSelect').append($('<option>', {
                    value: this.id,
                    text: this.name + " " + this.libraryLayout
                }));
            });
            $.otp.clusterJobJobTypeSpecificGraph.update();
            $.otp.clusterJobJobTypeSpecific.updateAvgValues();
        });
<<<<<<< HEAD
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
        var step = Math.floor(labels.length / 24)
        var newLabels = labels.filter( function (value_ignored, index) { return index % step === 0 } )
        return newLabels;
    },
};
=======
    }
}
>>>>>>> detailPage

$.otp.clusterJobJobTypeSpecificGraph = {
    register : function () {
        "use strict";

        var startDate = $('#dpFrom').val();
        var endDate = $('#dpTo').val();

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificExitCodes',
<<<<<<< HEAD
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(), 'from': startDate, 'to': endDate}
=======
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(),'from': startDate, 'to': endDate}
>>>>>>> detailPage
        }), function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificExitCodes(this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificExitStatuses',
<<<<<<< HEAD
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(), 'from': startDate, 'to': endDate}
=======
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(),'from': startDate, 'to': endDate}
>>>>>>> detailPage
        }), function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificExitStatuses(this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificStates',
<<<<<<< HEAD
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(), 'from': startDate, 'to': endDate}
=======
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(),'from': startDate, 'to': endDate}
>>>>>>> detailPage
        }), function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificStates(this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificWalltimes',
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(),'from': startDate, 'to': endDate}
        }), function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificWalltimes(this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
<<<<<<< HEAD
            action : 'getJobTypeSpecificMemories',
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(), 'from': startDate, 'to': $.otp.clusterJobJobTypeSpecific.getToday()}
=======
            action : 'getJobTypeSpecificCores',
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(),'from': startDate, 'to': endDate}
        }), function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificCoreUsage(this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobJobTypeSpecific',
            action : 'getJobTypeSpecificMemories',
            parameters: {'jobClass': $('#jobClassSelect').val(), 'seqType': $('#seqTypeSelect').val(),'from': startDate, 'to': getToday()}
>>>>>>> detailPage
        }),function () {
            $.otp.clusterJobJobTypeSpecificGraph.getJobTypeSpecificMemoryUsage(this);
        });
    },

    update: function () {
<<<<<<< HEAD
        "use strict";
=======
        "use strict"
>>>>>>> detailPage
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

<<<<<<< HEAD
=======
    getJobTypeSpecificCoreUsage: function (data) {
        "use strict";
        $.otp.clusterJobJobTypeSpecificGraph.generateScatterGraphic('jobTypeSpecificGraphCores', data);
    },

>>>>>>> detailPage
    getJobTypeSpecificMemoryUsage: function (data) {
        "use strict";
        $.otp.clusterJobJobTypeSpecificGraph.generateScatterGraphic('jobTypeSpecificGraphMemories', data);
    },

    generatePieGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.Reset($('#' + id).get(0));
        var graph = new RGraph.Pie(id, json.data);
        graph.Set('chart.labels', json.labels);
        graph.Set('chart.labels.sticks', true);
<<<<<<< HEAD
        graph.Set('chart.shadow.offsetx', 5);
        graph.Set('chart.shadow.offsety', 5);
        graph.Set('chart.shadow.blur', 15);
        graph.Set('chart.colors', $.otp.clusterJobJobTypeSpecific.getColors(json.data.length));
=======
        graph.Set('chart.tooltips', json.tooltips);
        graph.Set('chart.shadow.offsetx', 5);
        graph.Set('chart.shadow.offsety', 5);
        graph.Set('chart.shadow.blur', 15);
        graph.Set('chart.colors', getColors(json.data.length));
>>>>>>> detailPage
        graph.Set('chart.linewidth', 2);
        graph.Set('chart.exploded', 3);
        graph.Set('chart.radius', 80);
        graph.Draw();
    },

    generateLineGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.Reset($('#' + id).get(0));
        var graph = new RGraph.Line(id, json.data);
<<<<<<< HEAD
        graph.Set('labels', $.otp.clusterJobJobTypeSpecific.normalizeLabels(json.labels));
=======
        graph.Set('labels', normalizeLabels(json.labels));
>>>>>>> detailPage
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
<<<<<<< HEAD
        "use strict";
=======
        "use strict"
>>>>>>> detailPage
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
        graph.Set('labels', json.labels);
        graph.Set('text.angle', 45);
        graph.Set('text.size', 8);
<<<<<<< HEAD
        graph.Set('events.click', function (e, shape) {
=======
        graph.Set('events.click', function(e, shape) {
>>>>>>> detailPage
            var index = shape[4];
            var id = shape['object']['data'][0][index][2];
            var link = $.otp.createLink({
                controller: 'clusterJobDetail',
                action: 'show',
                id: id
            });
            window.location.href = link;
        })
<<<<<<< HEAD
        graph.Set('events.mousemove', function (e, shape) {
=======
        graph.Set('events.mousemove', function(e, shape) {
>>>>>>> detailPage
            e.target.style.cursor = 'pointer';
        })
        graph.Draw();
    }
<<<<<<< HEAD
};
=======
}
>>>>>>> detailPage

function getColors(elements) {
    var c = new Array();
    var colors = new Array('#81BEF7','#A9BCF5','#5882FA','#0431B4','#00BFFF','#A9F5F2','#088A85','#9F81F7');
    for (var i = 0; i <= elements - 1; i++) {
        c[i] = colors[i];
    }
    return c;
}

function getToday() {
    var date = new Date();
    return (date.getFullYear().toString()) + "-" + ("0" + (date.getMonth() + 1).toString()).substr(-2) + "-" + ("0" + date.getDate().toString()).substr(-2);
}

function normalizeLabels(labels) {
    var quot = labels.length / 24;
    var newLabels = [];
    for (var i = 0; i <= labels.length - 1; i = i + quot) {
        newLabels.push(labels[i]);
    }
    return newLabels;
<<<<<<< HEAD
}
=======
}
>>>>>>> detailPage
