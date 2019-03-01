/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
