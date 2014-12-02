/*jslint browser: true */
/*global $ */

$.otp.clusterJobGeneral = {
    register: function () {
        "use strict";
        $.otp.clusterJobGeneralTable.register();
        $.otp.clusterJobGeneralGraph.register();
        $.otp.clusterJobGeneralProgress.register();

        $('.datePicker').datepicker(
            {
                dateFormat: 'yy-mm-dd',
                onSelect: function () {
                    $.otp.clusterJobGeneralGraph.update();
                    $.otp.clusterJobGeneralProgress.update();
                    $.otp.clusterJobGeneralTable.update();
                },
                maxDate: $('#dpTo').val()
            }
        );
    }
}

$.otp.clusterJobGeneralTable = {
    register : function () {
        "use strict";
        $("#clusterJobGeneralTable").dataTable({
            dom: '<"top"filp<"clear">>rt',
            bFilter : true,
            bProcessing : true,
            bServerSide : false,
            bSort : true,
            bJQueryUI : false,
            bAutoWidth : false,
            pageLength : 10,
            sAjaxSource : $.otp.createLink({
                controller : 'clusterJobGeneral',
                action : 'findAllClusterJobsByDateBetween',
                parameters : {'from': $('#dpFrom').val(), 'to': $('#dpTo').val()}
            }),
            sScrollY: 'auto',
            aaSorting: [[3, 'desc']],
            sScrollX: 'auto',
            bScrollCollapse: false,
            bPaginate: true,
            bDeferRender : true,
            fnServerData : function (sSource, aoData, fnCallback) {
                $.ajax({
                    "dataType" : 'json',
                    "type" : "POST",
                    "url" : sSource,
                    "data" : aoData,
                    "error" : function () {
                    },
                    "success" : function (json) {
                        for (var i = 0; i < json.aaData.length; i += 1) {
                            var row = json.aaData[i];
                            if(row[2]) {
                                row[0] = $.otp.createLinkMarkup({
                                            controller: 'clusterJobDetail',
                                            action: 'show',
                                            id: row[6],
                                            text: row[0]
                                        });
                            } else {
                                row[2] = "IN PROGRESS";
                            }
                            row[1] = row[1].substr(9);
                            row.pop();
                        }
                        fnCallback(json);
                    }
                });
            }
        });
    },

    update: function () {
        $("#clusterJobGeneralTable").DataTable().destroy();
        $.otp.clusterJobGeneralTable.register();
    }
}

$.otp.clusterJobGeneralGraph = {

    colors : ['#81BEF7', '#A9BCF5', '#5882FA', '#0431B4', '#00BFFF', '#A9F5F2', '#088A85', '#9F81F7'],

    register : function () {
        "use strict";

        var from = $('#dpFrom').val();
        var to = $('#dpTo').val();

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllExitCodes',
            parameters: {'from': from, 'to': to}
        }), function () {
            $.otp.clusterJobGeneralGraph.generatePieGraphic('generalGraphExitCode', this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllExitStatuses',
            parameters: {'from': from, 'to': to}
        }), function () {
            $.otp.clusterJobGeneralGraph.generatePieGraphic('generalGraphExitStatus', this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllFailed',
            parameters: {'from': from, 'to': to}
        }), function () {
            $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphFailed', this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllStates',
            parameters: {'from': from, 'to': to}
        }), function () {
            $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphStates', this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllAvgCoreUsage',
            parameters: {'from': from, 'to': to}
        }), function () {
            $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphCores', this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllMemoryUsage',
            parameters: {'from': from, 'to': to}
        }),function () {
            $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphMemory', this);
        });
    },

    update: function (id, data) {
        "use strict";
        RGraph.Clear($('#' + id).get(0));
        $.otp.clusterJobGeneralGraph.register();
    },

    generatePieGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.Reset($('#' + id).get(0));
        var graph = new RGraph.Pie(id, json.data);
        graph.Set('chart.shadow.offsetx', 5);
        graph.Set('chart.shadow.offsety', 5);
        graph.Set('chart.shadow.blur', 15);
        graph.Set('chart.colors', $.otp.clusterJobGeneralGraph.getColors(json.data.length));
        graph.Set('chart.linewidth', 2);
        graph.Set('chart.exploded', 3);
        graph.Set('chart.radius', 80);
        graph.Set('key', json.labels);
        graph.Set('key.colors', $.otp.clusterJobGeneralGraph.getColors(json.data.length));
        graph.Set('key.rounded', false);
        graph.Set('centerx', 120);
        graph.Draw();
    },

    generateLineGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.Reset($('#' + id).get(0));
        var graph = new RGraph.Line(id, json.data);
        graph.Set('labels', $.otp.clusterJobGeneralGraph.normalizeLabels(json.labels));
        graph.Set('text.angle', 45);
        graph.Set('text.size', 8);
        graph.Set('numxticks', json.labels.length - 1);
        graph.Set('background.grid.autofit.align', true);
        graph.Set('chart.gutter.bottom', 100);
        graph.Set('chart.gutter.left', 80);
        graph.Set('key', json.keys);
        graph.Draw();
    },

    getColors : function (elementCount) {
        "use strict";
        return this.colors.slice(0, elementCount);
    },

    normalizeLabels : function (labels) {
        "use strict";
        var step = Math.floor(labels.length / 24)
        var newLabels = labels.filter( function (value_ignored, index) { return index % step === 0 } )
        return newLabels;
    }
}

$.otp.clusterJobGeneralProgress = {
    register : function () {
        "use strict";
        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllStatesTimeDistribution',
            parameters: {'from': $('#dpFrom').val(), 'to': $('#dpTo').val()}
        }), function () {
            $.otp.clusterJobGeneralProgress.generateProgress('generalGraphQueuedStartedEndedProgress', this);
        });
    },

    update : function () {
        "use strict";
        // .multiprogressbar('destroy') does not work properly
        $('#generalGraphQueuedStartedEndedProgress').remove();
        $('<div id="generalGraphQueuedStartedEndedProgress" class="multiProgress"></div>').appendTo('#progressBarContainer_generalGraphQueuedStartedEndedProgress')
        $.otp.clusterJobGeneralProgress.register();
    },

    generateProgress : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        $("#" + id).multiprogressbar ({
            parts: [{value: json.data['queue'][0], text: json.data['queue'][0] + "% (" + json.data['queue'][1] + " hours)", barClass: "progressBarQueue", textClass: "progressTextQueue"},
                    {value: json.data['process'][0], text: json.data['process'][0] + "% (" + json.data['process'][1] + " hours)", barClass: "progressBarProcess", textClass: "progressTextProcess"}]
        });
    }
}
