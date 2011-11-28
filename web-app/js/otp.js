/**
 * Constructor for OTP object.
 * @param contextPath The contextPath which is used to create links.
 * @returns
 */
function OTP(contextPath) {
    "use strict";
    this.contextPath = contextPath;
}

/**
 * Creates the HTML markup for the status image.
 * The status is referenced by name:
 * <ul>
 * <li>NEW</li>
 * <li>DISABLED</li>
 * <li>RUNNING</li>
 * <li>RUNNINGFAILEDBEFORE</li>
 * <li>SUCCESS</li>
 * <li>FAILURE</li>
 * </ul>
 * @param status The name of the status
 * @returns HTML string for the status image
 */
OTP.prototype.statusImageHtml = function (status) {
    "use strict";
    return '<img src="' + this.contextPath + '/images/status/' + this.statusToImage(status) + '" alt="' + status + '" title="' + status + '"/>';
};

/**
 * Converts the status name into the image name which is used to represent the status.
 * @param status The name as listed in documentation for statusImageHtml
 * @returns Name of image or {@code null} if incorrect status
 * @see statusImageHtml
 */
OTP.prototype.statusToImage = function (status) {
    "use strict";
    switch (status) {
    case "NEW":
    case "CREATED":
        return "empty.png";
    case "DISABLED":
    case "SUSPENDED":
        return "grey.png";
    case "RUNNING":
    case "STARTED":
    case "RESUMED":
        return "green_anime.gif";
    case "RUNNINGFAILEDBEFORE":
    case "RESTARTED":
        return "red_anime.gif";
    case "FINISHED":
        return "blue.png";
    case "SUCCESS":
        return "green.png";
    case "FAILURE":
        return "red.png";
    }
    return null;
};

/**
 * Helper method to render a date in a common way.
 * @param value Date in JSON representation
 * @returns Formatted date
 */
OTP.prototype.renderDate = function (value) {
    "use strict";
    if (value) {
        var date = new Date(value);
        return '<span title="' + date.toLocaleString() +'">' + $.timeago(date) + '</span>';
    } else {
        return "-";
    }
};

/**
 * Formats a given time span in msec into a human readable text split by the most
 * convenient unit.
 * @param msec The time span in milliseconds
 * @returns Nicely formatted text to represent the time span
 */
OTP.prototype.formatTimespan = function (msec) {
    "use strict";
    var sec, min, hour, day;
    if (msec < 1000) {
        return msec + " msec";
    }
    sec = Math.round(msec / 1000);
    msec = msec % 1000;
    if (sec < 60) {
        return sec + " sec " + msec + " msec";
    }
    min = msec / 60;
    sec = Math.round(sec % 60);
    if (min < 60) {
        return min + " min " + sec + " sec";
    }
    hour = min / 60;
    min = Math.round(min % 60);
    if (hour < 24) {
        return hour + " h " + min + " min";
    }
    day = Math.round(hour / 24);
    hour = hour % 24;
    return day + " day(s) " + hour + " h";
};

/**
 * Creates the datatables view for the list of all JobExecutionPlans
 * @param selector The JQuery selector for the table to create the datatable into
 */
OTP.prototype.createJobExecutionPlanListView = function (selector) {
    "use strict";
    $(selector).dataTable({
        sPaginationType: "full_numbers",
        bJQueryUI: true,
        bProcessing: true,
        bServerSide: true,
        sAjaxSource: this.contextPath + '/processes/listData',
        fnServerData: function (sSource, aoData, fnCallback) {
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "success": function (json) {
                    var i, rowData;
                    for (i = 0; i < json.aaData.length; i++) {
                        rowData = json.aaData[i];
                        rowData[0] = $.otp.statusImageHtml(rowData[0].name);
                        rowData[2] = '<a href="' + $.otp.contextPath +  '/processes/plan/' + rowData[2].id + '">' + rowData[2].name + '</a>';
                        rowData[4] = $.otp.renderDate(rowData[4]);
                        rowData[5] = $.otp.renderDate(rowData[5]);
                        if (rowData[6]) {
                            rowData[6] = $.otp.formatTimespan(rowData[6]);
                        } else {
                            rowData[6] = "-";
                        }
                    }
                    fnCallback(json);
                }
            });
        }
    });
};

/**
 * Creates the datatables view for the list of all Processes for a given JobExecutionPlan
 * @param selector The JQuery selector for the table to create the datatable into
 * @param planId The id of the JobExecutionPlan for which the list of Processes should be retrieved.
 */
OTP.prototype.createProcessListView = function (selector, planId) {
    "use strict";
    $(selector).dataTable({
        sPaginationType: "full_numbers",
        bJQueryUI: true,
        bProcessing: true,
        bServerSide: true,
        sAjaxSource: $.otp.contextPath + '/processes/planData/' +  planId + '/',
        fnServerData: function (sSource, aoData, fnCallback) {
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "success": function (json) {
                    var i, rowData;
                    for (i = 0; i < json.aaData.length; i++) {
                        rowData = json.aaData[i];
                        rowData[0] = '<a href="' + $.otp.contextPath + '/processes/process/' + rowData[0] + '">' + rowData[0] + '</a>';
                        rowData[1] = $.otp.statusImageHtml(rowData[1].name);
                        rowData[2] = $.otp.renderDate(rowData[2]);
                        rowData[3] = $.otp.renderDate(rowData[3]);
                        rowData[5] = rowData[5].name;
                    }
                    fnCallback(json);
                }
            });
        },
        aaSorting: [[0, "desc"]],
        aoColumnDefs: [
            { "bSortable": true,  "aTargets": [0] },
            { "bSortable": false, "aTargets": [1] },
            { "bSortable": true,  "aTargets": [2] },
            { "bSortable": false, "aTargets": [3] },
            { "bSortable": false, "aTargets": [4] },
            { "bSortable": false, "aTargets": [5] }
        ]
    });
};

/**
 * Creates the datatables view for the list of all ProcessingSteps for a given Process.
 * @param selector The JQuery selector for the table to create the datatable into
 * @param processId The id of the Process for which the list of ProcessingSteps should be retrieved.
 */
OTP.prototype.createProcessingStepListView = function (selector, processId) {
    "use strict";
    $(selector).dataTable({
        sPaginationType: "full_numbers",
        bJQueryUI: true,
        bProcessing: true,
        bServerSide: true,
        sAjaxSource: $.otp.contextPath + '/processes/processData/' +  processId + '/',
        fnServerData: function (sSource, aoData, fnCallback) {
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "success": function (json) {
                    var i, rowData;
                    for (i = 0; i < json.aaData.length; i++) {
                        rowData = json.aaData[i];
                        rowData[0] = '<a href="' + $.otp.contextPath + '/processes/processingStep/' + rowData[0] + '">' + rowData[0] + '</a>';
                        rowData[1] = $.otp.statusImageHtml(rowData[1].name);
                        if (rowData[3]) {
                            rowData[3] = rowData[3].name + "<br/>" + rowData[3].version.substring(0, 7);
                        } else {
                            rowData[3] = "-";
                        }
                        rowData[4] = $.otp.renderDate(rowData[4]);
                        rowData[5] = $.otp.renderDate(rowData[5]);
                        if (rowData[6]) {
                            rowData[6] = $.otp.formatTimespan(rowData[6]);
                        } else {
                            rowData[6] = "-";
                        }
                        rowData[7] = rowData[7].name;
                    }
                    fnCallback(json);
                }
            });
        },
        aaSorting: [[0, "desc"]],
        aoColumnDefs: [
            { "bSortable": true,  "aTargets": [0] },
            { "bSortable": false, "aTargets": [1] },
            { "bSortable": false, "aTargets": [2] },
            { "bSortable": false, "aTargets": [3] },
            { "bSortable": false, "aTargets": [4] },
            { "bSortable": false, "aTargets": [5] },
            { "bSortable": false, "aTargets": [6] },
            { "bSortable": false, "aTargets": [7] }
        ]
    });
};

/**
 * Creates the datatables view for the list of all ProcessingStepUpdates for a given ProcessingStep.
 * @param selector The JQuery selector for the table to create the datatable into
 * @param processId The id of the ProcessingStep for which the list of Updates should be retrieved.
 */
OTP.prototype.createProcessingStepUpdatesListView = function (selector, stepId) {
    "use strict";
    $(selector).dataTable({
        sPaginationType: "full_numbers",
        bJQueryUI: true,
        bProcessing: true,
        bServerSide: true,
        sAjaxSource: $.otp.contextPath + '/processes/processingStepDate/' +  stepId + '/',
        fnServerData: function (sSource, aoData, fnCallback) {
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "success": function (json) {
                    var i, rowData;
                    for (i = 0; i < json.aaData.length; i++) {
                        rowData = json.aaData[i];
                        rowData[1] = $.otp.renderDate(rowData[1]);
                        rowData[2] = rowData[2].name;
                        if (!rowData[3]) {
                            rowData[3] = "-";
                        }
                    }
                    fnCallback(json);
                }
            });
        },
        aaSorting: [[0, "desc"]],
        aoColumnDefs: [
            { "bSortable": true,  "aTargets": [0] },
            { "bSortable": false, "aTargets": [1] },
            { "bSortable": false, "aTargets": [2] },
            { "bSortable": false, "aTargets": [3] }
        ]
    });
};
