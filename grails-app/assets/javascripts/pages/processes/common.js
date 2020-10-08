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

/*jslint continue: true */
/*global $ */
$.otp.workflows = {
    /**
     * Creates the HTML markup for the status.
     * The status is referenced by name:
     * @param status The name of the status
     * @returns HTML string for the status div
     */
    statusDivHtml: function (status) {
        "use strict";
        return '<div class="' + $.otp.workflows.statusToClassName(status) + '"></div>';
    },

    /**
     * Converts the status name into the image name which is used to represent the status.
     * @param status The name as listed in documentation for statusDivHtml
     * @returns string of image or {@code null} if incorrect status
     * @see statusDivHtml
     */
    statusToClassName: function (status) {
        "use strict";
        switch (status) {
            case "NEW":
            case "CREATED":
                return "";
            case "DISABLED":
            case "SUSPENDED":
                return "dot grey";
            case "RUNNING":
            case "STARTED":
            case "RESUMED":
                return "sm-loader green";
            case "RUNNINGFAILEDBEFORE":
                return "sm-loader red";
            case "FINISHED":
                return "dot blue";
            case "SUCCESS":
                return "dot green";
            case "FAILURE":
                return "dot red";
            case "RESTARTED":
                return "dot purple";
        }
        return "";
    },
    /**
     * Helper method to render a date in a common way.
     *
     * @param value Map with keys 'full' and 'shortest'
     * @returns Span element displaying the shortest time and full time as tooltip
     */
    renderDate: function (value) {
        "use strict";
        if (value) {
            return '<span title="' + value.full + '">' + value.shortest + '</span>';
        }
        return "-";
    },
    /**
     * Formats a given time span in msec into a human readable text split by the most
     * convenient unit.
     * @param msec The time span in milliseconds
     * @returns Nicely formatted text to represent the time span
     */
    formatTimespan: function (msec) {
        "use strict";
        var sec, min, hour, day;
        if (msec < 1000) {
            return msec + " msec";
        }
        sec = msec / 1000;
        msec = msec % 1000;
        if (sec < 60) {
            return Math.floor(sec) + " sec " + msec + " msec";
        }
        min = sec / 60;
        sec = Math.floor(sec % 60);
        if (min < 60) {
            return Math.floor(min) + " min " + sec + " sec";
        }
        hour = min / 60;
        min = Math.floor(min % 60);
        if (hour < 24) {
            return Math.floor(hour) + " h " + min + " min";
        }
        day = Math.floor(hour / 24);
        hour = Math.floor(hour % 24);
        return day + " day(s) " + hour + " h";
    },
    /**
     * Callback for restart ProcessingStep.
     * Performs the AJAX call to restart the step and reloads the given datatable.
     * @param id The id of the ProcessingStep to restart.
     * @param selector Selector for the datatable
     */
    restartProcessingStep: function (id, selector) {
        "use strict";
        $.getJSON($.otp.createLink({
            controller: 'processes',
            action: 'restartStep',
            id: id
        }), function (data) {
            $.otp.infoMessage(data.success);
            $(selector).dataTable().fnDraw();
        });
    },
    /**
     * Creates HTML markup for link to restart a ProcessingStep.
     * @param id The id of the ProcessingStep to restart.
     * @param dataTable Selector for the datatable
     */
    createRestartProcessingStepLink: function (id, dataTable) {
        "use strict";
        var imageLink = $.otp.createLink({
            controller: 'assets',
            action: 'redo.png'
        });
        return '<a id="restartProcessingStepLink" onclick="$.otp.workflows.restartProcessingStep(' + id + ', \'' + dataTable + '\');" href="#" title="Restart" ><img src="' + imageLink + '"/></a>';
    },
};
