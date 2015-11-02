/*jslint browser: true */
/*global $ */

$.otp.projectOverviewHome = {
    register: function () {
        "use strict";
        $('#projectGroup_select').change(function () {
            $.otp.graph.overview.init();
        });
    }
};
