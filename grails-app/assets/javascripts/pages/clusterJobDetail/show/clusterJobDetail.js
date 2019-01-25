/*jslint browser: true */
/*global $ */

$.otp.clusterJobDetailProgress = {
    register : function (id) {
        $.ajax($.otp.createLink({
            controller : 'clusterJobDetail',
            action : 'getStatesTimeDistribution',
            parameters: {'id': id}
        })).done(function (data) {
            $.otp.clusterJobDetailProgress.getAllStates(data);
        });
    },

    getAllStates : function (data) {
        "use strict";
        $.otp.clusterJobDetailProgress.generateProgress('jobTypeSpecificGraphProgress', data.data);
    },

    generateProgress : function (id, data) {
        var PERCENT = "percentage";
        var TIME = "time";
        $("#" + id).multiprogressbar ({
            parts: [
                {value: data['queue'][PERCENT],   text: data['queue'][PERCENT] + "% (" + data['queue'][TIME] + ")",     barClass: "progressBarQueue",   textClass: "progressTextQueue"},
                {value: data['process'][PERCENT], text: data['process'][PERCENT] + "% (" + data['process'][TIME] + ")", barClass: "progressBarProcess", textClass: "progressTextProcess"}
            ]
        });
    }
};