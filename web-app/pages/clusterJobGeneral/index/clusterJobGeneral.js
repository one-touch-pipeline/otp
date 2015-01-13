/*jslint browser: true */
/*global $ */

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
                action : 'getAllClusterJobs'
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
    }
}

$.otp.clusterJobGeneralGraph = {

    colors : ['#81BEF7', '#A9BCF5', '#5882FA', '#0431B4', '#00BFFF', '#A9F5F2', '#088A85', '#9F81F7'],

    register : function () {
        "use strict";
        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllExitCodes'
        }), function () {
            $.otp.clusterJobGeneralGraph.generatePieGraphic('generalGraphExitCode', this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllExitStatuses'
        }), function () {
            $.otp.clusterJobGeneralGraph.generatePieGraphic('generalGraphExitStatus', this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllFailed',
            parameters: {'from': $('.graphTimeSpanContainer#queuedStartedEnded [name=dpFrom]').val(), 'to': $('.graphTimeSpanContainer#queuedStartedEnded [name=dpTo]').val()}
        }), function () {
            $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphFailed', this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllStates',
            parameters: {'from': $('.graphTimeSpanContainer#queuedStartedEnded [name=dpFrom]').val(), 'to': $('.graphTimeSpanContainer#queuedStartedEnded [name=dpTo]').val()}
        }), function () {
            $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphStates', this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllAvgCoreUsage',
            parameters: {'from': $('.graphTimeSpanContainer#coreUsage [name=dpFrom]').val(), 'to': $('.graphTimeSpanContainer#coreUsage [name=dpTo]').val()}
        }), function () {
            $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphCores', this);
        });

        RGraph.AJAX($.otp.createLink({
            controller : 'clusterJobGeneral',
            action : 'getAllMemoryUsage',
            parameters: {'from': $('.graphTimeSpanContainer#memoryUsage [name=dpFrom]').val(), 'to': $('.graphTimeSpanContainer#memoryUsage [name=dpTo]').val()}
        }),function () {
            $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphMemory', this);
        });

        $('.datePicker').each(function(){
            $(this).datepicker(
                {
                    dateFormat: 'yy-mm-dd',
                    onSelect: function () {
                        var labels = ['from', 'to'];
                        var dates = {};
                        $(this).parents('.graphTimeSpanContainer').find('.datePicker').each(function (index) {
                            dates[labels[index]] = $(this).val()
                        });
                        $.otp.clusterJobGeneralGraph.update($(this).parents('.lineGraphContainer').find('canvas').attr('id'), dates);
                    }
                }
            );
        })
    },

    update: function (id, data) {
        "use strict";
        RGraph.Clear($('#' + id).get(0));
        switch (id) {
            case 'generalGraphFailed':
                RGraph.AJAX($.otp.createLink({
                    controller : 'clusterJobGeneral',
                    action : 'getAllFailed',
                    parameters : data
                }), function () {
                    $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphFailed', this);
                });
                break;
            case 'generalGraphStates':
                RGraph.AJAX($.otp.createLink({
                    controller : 'clusterJobGeneral',
                    action : 'getAllStates',
                    parameters : data
                }), function () {
                    $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphStates', this);
                });
                break;

            case 'generalGraphCores':
                RGraph.AJAX($.otp.createLink({
                    controller : 'clusterJobGeneral',
                    action : 'getAllAvgCoreUsage',
                    parameters : data
                }), function () {
                    $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphCores', this);
                });
                break;

            case 'generalGraphMemory':
                RGraph.AJAX($.otp.createLink({
                    controller : 'clusterJobGeneral',
                    action : 'getAllMemoryUsage',
                    parameters : data
                }), function () {
                    $.otp.clusterJobGeneralGraph.generateLineGraphic('generalGraphMemory', this);
                });
                break;
        }
    },

    generatePieGraphic : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        RGraph.Reset($('#' + id).get(0));
        var graph = new RGraph.Pie(id, json.data);
        graph.Set('chart.labels', json.labels);
        graph.Set('chart.labels.sticks', true);
        graph.Set('chart.shadow.offsetx', 5);
        graph.Set('chart.shadow.offsety', 5);
        graph.Set('chart.shadow.blur', 15);
        graph.Set('chart.colors', $.otp.clusterJobGeneralGraph.getColors(json.data.length));
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
        }), function () {
            $.otp.clusterJobGeneralProgress.generateProgress('generalGraphQueuedStartedEndedProgress', this);
        });
    },

    update : function () {
        "use strict";
        $('.multiProgress').multiprogressbar('destroy');
        $.otp.clusterJobGeneralProgress.register();
    },

    generateProgress : function (id, data) {
        "use strict";
        var json = JSON.parse(data.response);
        $("#" + id).multiprogressbar ({
            parts: [{value: json.data['queue'][0], text: json.data['queue'][0] + "% (" + json.data['queue'][1] + ")", barClass: "progressBarQueue", textClass: "progressTextQueue"},
                    {value: json.data['process'][0], text: json.data['process'][0] + "% (" + json.data['process'][1] + ")", barClass: "progressBarProcess", textClass: "progressTextProcess"}]
        });
    }
}
