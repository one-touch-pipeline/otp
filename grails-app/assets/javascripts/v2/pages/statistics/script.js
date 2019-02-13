/*jslint browser: true */
/*global $ */
OTP = OTP || {};
OTP.pages = OTP.pages || {};
OTP.pages.statistics = OTP.pages.statistics || {};

OTP.pages.statistics = {
    init: function () {
        "use strict";

        var today = new Date().toJSON().slice(0, 10);

        if (startDate && endDate) {
            $('#start').val(startDate);
            $('#end').val(endDate);
        } else {
            $('#end').val(today);
        }

        $('#start').attr("max", today);
        $('#end').attr("max", today);

        $('#start').change(function () {
            var date = $("#start").val();
            $('#end').attr("min", date);
        });

        $('#end').change(function () {
            var date = $("#end").val();
            $('#start').attr("max", date);
        });

    },

};