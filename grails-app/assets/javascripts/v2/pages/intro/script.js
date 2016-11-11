/*jslint browser: true */
/*global $ */
//= require v2/rgraph

OTP = OTP || {};
OTP.pages = OTP.pages || {};

OTP.pages.intro = {
    init: function () {
        "use strict";

        $('.js-account').focus();

        $.ajax({
            url: OTP.createLink({
                controller : 'intro',
                action : 'patientsBySeqType'
            }),
            success: OTP.pages.intro.patientsCountPerSequenceType
        });

        $.ajax({
            url: OTP.createLink({
                controller : 'intro',
                action : 'samplesBySeqType'
            }),
            success: OTP.pages.intro.sampleCountPerSequenceType
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
