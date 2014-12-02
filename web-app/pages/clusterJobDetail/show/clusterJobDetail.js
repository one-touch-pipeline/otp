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
        $("#" + id).multiprogressbar ({
            parts: [{value: data['queue'][0], text: data['queue'][0] + "% (" + data['queue'][1] + ")", barClass: "progressBarQueue", textClass: "progressTextQueue"},
                    {value: data['process'][0], text: data['process'][0] + "% (" + data['process'][1] + ")", barClass: "progressBarProcess", textClass: "progressTextProcess"}]
        });
    },
}

