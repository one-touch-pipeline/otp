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