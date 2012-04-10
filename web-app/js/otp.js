/**
 * Constructor for OTP object.
 * @param contextPath The contextPath which is used to create links.
 * @returns
 */
function OTP() {
}

$.otp = new OTP();

// TODO: create proper links
$.i18n.properties({
    name: 'messages',
    path: '/otp/js/i18n/',
    mode: "map"
});

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
 * Creates the HTML markup for the health image.
 * @param succeeded The number of succeeded processes
 * @param finished The number of finished processes
 * @return HTML string for health image
 */
OTP.prototype.healthImageHtml = function (succeeded, finished) {
    "use strict";
    var percent, image, title;
    percent = (finished > 0) ? succeeded / finished * 100 : 0;
    if (percent <= 19) {
        image = "health-00to19.png";
    } else if (percent <= 39) {
        image = "health-20to39.png";
    } else if (percent <= 59) {
        image = "health-40to59.png";
    } else if (percent <= 79) {
        image = "health-60to79.png";
    } else {
        image = "health-80plus.png";
    }
    title = succeeded + ' of ' + finished + ' Processes finished successfully';
    return '<img src="' + this.contextPath + '/images/status/' + image + '" alt="' + title + '" title="' + title + '"/>';
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
        return '<span title="' + date.toLocaleString() + '">' + $.timeago(date) + '</span>';
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
 * Shared definition for a datatables view.
 * @param selector The JQuery selector for the table to create the datatable into
 * @param sourcePath The path to the ajax resource
 * @param sortOrder {@code true} for ascending, {@code false} for descending initial sorting of first collumn
 * @param jsonCallback (optional) the callback to invoke when json data has been returned. Get's one argument json
 * @param columnDefs (optional) Array of column definitions, can be used to enable/disable sorting of columns
 * @param postData (optional) Array of additional data to be added to the POST requests
 */
OTP.prototype.createListView = function (selector, sourcePath, sortOrder, jsonCallback, columnDefs, postData) {
    "use strict";
    $(selector).dataTable({
        sPaginationType: "full_numbers",
        bFilter: false,
        bJQueryUI: true,
        bProcessing: true,
        bServerSide: true,
        sAjaxSource: sourcePath,
        fnServerData: function (sSource, aoData, fnCallback) {
            var i;
            if (postData) {
                for (i = 0; i < postData.length; i++) {
                    aoData.push(postData[i]);
                }
            }
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "success": function (json) {
                    if (jsonCallback) {
                        jsonCallback(json);
                    }
                    fnCallback(json);
                }
            });
        },
        aoColumnDefs: columnDefs,
        aaSorting: [[0, sortOrder ? "asc" : "desc"]]
    });
};

/**
 * Creates the datatables view for the list of all JobExecutionPlans
 * @param selector The JQuery selector for the table to create the datatable into
 */
OTP.prototype.createJobExecutionPlanListView = function (selector) {
    "use strict";
    this.createListView(selector, this.contextPath + '/processes/listData', true, function (json) {
        var i, processId, rowData;
        for (i = 0; i < json.aaData.length; i++) {
            rowData = json.aaData[i];
            processId = rowData[2].id;
            rowData[0] = $.otp.statusImageHtml(rowData[0].name);
            if (rowData[1]) {
                rowData[1] = $.otp.healthImageHtml(rowData[1].succeeded, rowData[1].finished);
            } else {
                rowData[1] = "-";
            }
            rowData[2] = '<a href="' + $.otp.contextPath +  '/processes/plan/' + processId + '">' + rowData[2].name + '</a>';
            rowData[4] = '<a href="' + $.otp.contextPath + '/processes/plan/' + processId + '?failed=true">' + rowData[4] + '</a>';
            rowData[5] = $.otp.renderDate(rowData[5]);
            rowData[6] = $.otp.renderDate(rowData[6]);
            if (rowData[7]) {
                rowData[7] = $.otp.formatTimespan(rowData[7]);
            } else {
                rowData[7] = "-";
            }
        }
    });
};

/**
 * Callback for restart ProcessingStep.
 * Performs the AJAX call to restart the step and reloads the given datatable.
 * @param id The id of the ProcessingStep to restart.
 * @param dataTable Selector for the datatable
 **/
OTP.prototype.restartProcessingStep = function (id, selector) {
    "use strict";
    $.getJSON($.otp.contextPath + "/processes/restartStep/" + id, function (data) {
        // TODO: proper notification.
        alert(data.success);
        $(selector).dataTable().fnDraw();
    });
};

/**
 * Creates HTML markup for link to restart a ProcessingStep.
 * @param id The id of the ProcessingStep to restart.
 * @param dataTable Selector for the datatable
 **/
OTP.prototype.createRestartProcessingStepLink = function (id, dataTable) {
    "use strict";
    var imageLink = $.otp.contextPath + "/images/redo.png";
    return '<a onclick="$.otp.restartProcessingStep(' + id + ', \'' + dataTable + '\');" href="#" title="Restart" ><img src="' + imageLink + '"/></a>';
};

/**
 * Creates the datatables view for the list of all Processes for a given JobExecutionPlan
 * @param selector The JQuery selector for the table to create the datatable into
 * @param planId The id of the JobExecutionPlan for which the list of Processes should be retrieved.
 * @param failed Whether to limit to failed processes (true) or include all processes (false)
 */
OTP.prototype.createProcessListView = function (selector, planId, failed) {
    "use strict";
    this.createListView(selector, this.contextPath + '/processes/planData/' +  planId + '/?failed=' + failed, false, function (json) {
        var i, j, rowData, stepId, actions;
        for (i = 0; i < json.aaData.length; i++) {
            rowData = json.aaData[i];
            rowData[0] = '<a href="' + $.otp.contextPath + '/processes/process/' + rowData[0] + '">' + rowData[0] + '</a>';
            rowData[1] = $.otp.statusImageHtml(rowData[1].name);
            rowData[2] = rowData[2] ? rowData[2] : "-";
            rowData[3] = $.otp.renderDate(rowData[3]);
            rowData[4] = $.otp.renderDate(rowData[4]);
            stepId = rowData[6].id;
            if (rowData[6].error) {
                rowData[6] = '<a href="' + $.otp.contextPath + '/processes/processingStep/' + rowData[6].id + '" title="' + rowData[6].error + '">' + rowData[6].state.name + '</a>';
            } else {
                rowData[6] = rowData[6].state.name;
            }
            actions = rowData[7].actions;
            rowData[7] = "";
            for (j = 0; j < actions.length; j += 1) {
                switch (actions[j]) {
                case "restart":
                    rowData[7] += $.otp.createRestartProcessingStepLink(stepId, selector);
                    break;
                default:
                    // nothing
                    break;
                }
            }
        }
    }, [
        { "bSortable": true,  "aTargets": [0] },
        { "bSortable": false, "aTargets": [1] },
        { "bSortable": false, "aTargets": [2] },
        { "bSortable": true,  "aTargets": [3] },
        { "bSortable": false, "aTargets": [4] },
        { "bSortable": false, "aTargets": [5] },
        { "bSortable": false, "aTargets": [6] },
        { "bSortable": false, "aTargets": [7] }
    ]);
};

/**
 * Creates the datatables view for the list of all ProcessingSteps for a given Process.
 * @param selector The JQuery selector for the table to create the datatable into
 * @param processId The id of the Process for which the list of ProcessingSteps should be retrieved.
 */
OTP.prototype.createProcessingStepListView = function (selector, processId) {
    "use strict";
    this.createListView(selector, this.contextPath + '/processes/processData/' +  processId + '/', false, function (json) {
        var i, j, rowData, stepId, actions;
        for (i = 0; i < json.aaData.length; i++) {
            rowData = json.aaData[i];
            stepId = rowData[0];
            rowData[0] = '<a href="' + $.otp.contextPath + '/processes/processingStep/' + stepId + '">' + stepId + '</a>';
            rowData[1] = $.otp.statusImageHtml(rowData[1].name);
            if (rowData[3]) {
                rowData[3] = '<span title="' + rowData[3].name + '">' + rowData[3].name.substr(rowData[3].name.lastIndexOf('.') + 1) + "</span><br/>" +
                    '<span title="' + rowData[3].version + '">' + rowData[3].version.substring(0, 7) + '</span>';
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
            if (rowData[7].error) {
                rowData[7] = '<span title="' + rowData[7].error + '">' + rowData[7].state.name + '</span>';
            } else {
                rowData[7] = rowData[7].state.name;
            }
            actions = rowData[8].actions;
            rowData[8] = "";
            for (j = 0; j < actions.length; j += 1) {
                switch (actions[j]) {
                case "restart":
                    rowData[8] += $.otp.createRestartProcessingStepLink(stepId, selector);
                    break;
                default:
                    // nothing
                    break;
                }
            }
        }
    }, [
        { "bSortable": true,  "aTargets": [0] },
        { "bSortable": false, "aTargets": [1] },
        { "bSortable": false, "aTargets": [2] },
        { "bSortable": false, "aTargets": [3] },
        { "bSortable": false, "aTargets": [4] },
        { "bSortable": false, "aTargets": [5] },
        { "bSortable": false, "aTargets": [6] },
        { "bSortable": false, "aTargets": [7] },
        { "bSortable": false, "aTargets": [8] }
    ]);
};

/**
 * Creates the datatables view for the list of all ProcessingStepUpdates for a given ProcessingStep.
 * @param selector The JQuery selector for the table to create the datatable into
 * @param processId The id of the ProcessingStep for which the list of Updates should be retrieved.
 */
OTP.prototype.createProcessingStepUpdatesListView = function (selector, stepId) {
    "use strict";
    this.createListView(selector, this.contextPath + '/processes/processingStepDate/' +  stepId + '/', false, function (json) {
        var i, rowData;
        for (i = 0; i < json.aaData.length; i++) {
            rowData = json.aaData[i];
            rowData[1] = $.otp.renderDate(rowData[1]);
            rowData[2] = rowData[2].name;
            if (!rowData[3]) {
                rowData[3] = "-";
            }
        }
    }, [
        { "bSortable": true,  "aTargets": [0] },
        { "bSortable": false, "aTargets": [1] },
        { "bSortable": false, "aTargets": [2] },
        { "bSortable": false, "aTargets": [3] }
    ]);
};

/**
 * Creates the datatables view for the list of Parameters (either input or output) for a given ProcessingStep.
 * @param selector The JQuery selector for the table to create the datatable into
 * @param processId The id of the ProcessingStep for which the list of Updates should be retrieved.
 * @param inputOrOutput If {@code true} the input parameters are retrieved, if {@code false} the output
 */
OTP.prototype.createParameterListView = function (selector, stepId, inputOrOutput) {
    "use strict";
    this.createListView(selector, this.contextPath + '/processes/parameterData/' +  stepId + '/', true, null, [
        { "bSortable": true,  "aTargets": [0] },
        { "bSortable": false, "aTargets": [1] },
        { "bSortable": false, "aTargets": [2] },
        { "bSortable": false, "aTargets": [3] }
    ], [{"name": "input", "value": inputOrOutput}]);
};
