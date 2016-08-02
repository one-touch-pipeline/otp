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
                labels: json.labels,
                colors: ['#025B9A'],
                labelsAbove: true,
                labelsOffsetx: 9,
                strokestyle: 'white',
                linewidth: 3,
                textSize: 10,
                textAccessible: false,
                textAngle: 45,
                labelsAboveAngle: null,
                labelsAboveSize: 9,
                gutterBottom: 80,
                noaxes: true,
                ylabels: false
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
                labels: json.labels,
                textAccessible: false,
                stroke: 'white',
                strokestyle: 'white',
                linewidth: 3,
                labelsSticks: true,
                textSize: 10,
                radius: 80,
                labelsSticksLength: 15,
                colors: ["#4A5964", "#025B9A", "#A4ACB1", "#32AE32", "#E6E6E5", "#FD7202", "#000000"]
            }
        }).draw();
    }
};
