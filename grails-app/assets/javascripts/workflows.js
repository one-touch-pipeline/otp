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
    /**
     * Creates the datatables view for the list of all JobExecutionPlans
     * @param selector The JQuery selector for the table to create the datatable into
     */
    registerJobExecutionPlan: function (selector) {
        "use strict";
        $.otp.createListView(selector, $.otp.createLink({
            controller: 'processes',
            action: 'listData'
        }), true, undefined,
            [
                { "mRender": function (data, type, row) {
                    return $.otp.createLinkMarkup({
                        controller: 'processes',
                        action: 'plan',
                        id: row.id,
                        text: row.name
                    });
                }, "aTargets": [0] },
                { "mRender": function (data, type, row) {
                    if (row.allProcessesCount) {
                        return $.otp.createLinkMarkup({
                            controller: 'processes',
                            action: 'plan',
                            id: row.id,
                            text: row.allProcessesCount
                        });
                    }
                    return "-";
                }, "aTargets": [1] },
                { "mRender": function (data, type, row) {
                    if (row.failedProcessesCount) {
                        return $.otp.createLinkMarkup({
                            controller: 'processes',
                            action: 'plan',
                            id: row.id,
                            parameters: {
                                state: 'FAILURE'
                            },
                            text: row.failedProcessesCount
                        });
                    }
                    return "-";
                }, "aTargets": [2] },
                { "mRender": function (data, type, row) {
                    if (row.runningProcessesCount) {
                        return $.otp.createLinkMarkup({
                            controller: 'processes',
                            action: 'plan',
                            id: row.id,
                            parameters: {
                                state: 'RUNNING'
                            },
                            text: row.runningProcessesCount
                        });
                    }
                    return "-";
                }, "aTargets": [3] },
                { "mRender": function (data, type, row) {
                    return $.otp.workflows.renderDate(row.lastSuccessfulDate);
                }, "aTargets": [4] },
                { "mRender": function (data, type, row) {
                    return $.otp.workflows.renderDate(row.lastFailureDate);
                }, "aTargets": [5] }
            ],
            undefined,
            140, {
                "aaSorting": [[5, 'desc']],
                "fnRowCallback": function (nRow, aData, iDisplayIndex, iDisplayIndexFull) {
                    if (aData.enabled === false) {
                        $(nRow).addClass("withdrawn");
                    }
                }
            });
    },
    /**
     * Creates the datatables view for the list of all Processes for a given JobExecutionPlan
     * @param selector The JQuery selector for the table to create the datatable into
     * @param planId The id of the JobExecutionPlan for which the list of Processes should be retrieved.
     * @param state Whether to limit to specific processes (failed, running) or include all processes
     */
    registerProcesses: function (selector, planId, state) {
        "use strict";
        $.otp.createListView(selector, $.otp.createLink({
            controller: 'processes',
            action: 'planData',
            id: planId,
            parameters: {
                state: state
            }
        }), false, function (json) {
            var i, j, rowData, stepId, actions;
            for (i = 0; i < json.aaData.length; i += 1) {
                rowData = json.aaData[i];
                rowData[0] = $.otp.createLinkMarkup({
                    controller: 'processes',
                    action: 'process',
                    id: rowData[0],
                    text: rowData[0]
                });
                rowData[1] = $.otp.workflows.statusDivHtml(rowData[1]);
                if (rowData[2]) {
                    rowData[2] = rowData[2].text;
                } else {
                    rowData[2] = "-";
                }
                rowData[3] = $.otp.workflows.renderDate(rowData[3]);
                rowData[4] = $.otp.workflows.renderDate(rowData[4]);
                stepId = rowData[6].id;
                if (rowData[6].error) {
                    rowData[6] = $.otp.createLinkMarkup({
                        controller: 'processes',
                        action: 'processingStep',
                        id: stepId,
                        text: rowData[6].state
                    });
                } else {
                    rowData[6] = rowData[6].state;
                }
                if(rowData[7] != null && rowData[7].length > 0) {
                    rowData[7] = '<div class="commentSet" title="' + rowData[7] + '">&nbsp;</div>';
                }
                else {
                    rowData[7] = "";
                }
                actions = rowData[8].actions;
                rowData[8] = "";
                for (j = 0; j < actions.length; j += 1) {
                    if (actions[j] === "restart") {
                        rowData[8] += $.otp.workflows.createRestartProcessingStepLink(stepId, selector);
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
            { "bSortable": false, "aTargets": [7] },
            { "bSortable": false, "aTargets": [8] }
        ], undefined, undefined, {
            iDisplayLength: 25,
            bPaginate: true,
            sPaginationType: "listbox",
            bScrollCollapse: false,
            sDom:'<i>prtp<"clear">',
        });
        $("#enable-workflow-button").click(function () {
            $.get($.otp.createLink({
                controller: 'processes',
                action: 'enablePlan',
                id: planId
            }), function (data) {
                if (data !== "true") {
                    $.otp.warningMessage("Could not enable Workflow");
                    return;
                }
                $("#enable-workflow-button").hide();
                $("#disable-workflow-button").show();
                $("strong", $("#disable-workflow-button").parent()).text("enabled");
                $("#workflow-status").removeClass("grey").addClass("green");
            });
        });
        $("#disable-workflow-button").click(function () {
            $.get($.otp.createLink({
                controller: 'processes',
                action: 'disablePlan',
                id: planId
            }), function (data) {
                if (data !== "false") {
                    $.otp.warningMessage("Could not disable Workflow");
                    return;
                }
                $("#disable-workflow-button").hide();
                $("#enable-workflow-button").show();
                $("strong", $("#disable-workflow-button").parent()).text("disabled");
                $("#workflow-status").removeClass("green").addClass("grey");
            });
        });
    },
    /**
     * Creates the datatables view for the list of all ProcessingSteps for a given Process.
     * @param selector The JQuery selector for the table to create the datatable into
     * @param processId The id of the Process for which the list of ProcessingSteps should be retrieved.
     */
    registerProcessingStep: function (selector, processId) {
        "use strict";
        $.otp.createListView(selector, $.otp.createLink({
            controller: 'processes',
            action: 'processData',
            id: processId + '/'
        }), false, function (json) {
            json.aaData.forEach(function (data, index) {
                var row = [];
                row[0] = $.otp.createLinkMarkup({
                    controller: 'processes',
                    action    : 'processingStep',
                    id        : data.processingStep.id,
                    text      : "<div style='padding: 17px 0px 17px 10px;'>" + data.processingStep.id + "</div>"
                });
                row[1] = $.otp.workflows.statusDivHtml(data.lastUpdate.state);
                row[2] = data.processingStep.jobName;

                var jobClass = "-";
                if (data.processingStep) {
                    jobClass = data.processingStep.jobClass
                }
                row[3] = '<span title="' + jobClass + '">' + jobClass.split("\.").pop() + "</span><br/>";

                row[4] = $.otp.workflows.renderDate(data.times.creation);
                row[5] = $.otp.workflows.renderDate(data.times.lastUpdate);

                row[6] = "-";
                if (data.times.duration) {
                    row[6] = $.otp.workflows.formatTimespan(data.times.duration);
                }

                row[7] = data.lastUpdate.state;

                row[8] = "";
                for (var action of data.actions) {
                    switch (action) {
                        case "restart":
                            row[8] += $.otp.workflows.createRestartProcessingStepLink(data.processingStep.id, selector);
                            break;
                        default:
                            // nothing
                            break;
                    }
                }
                json.aaData[index] = row
            });
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
        ], undefined, 100);
    },
    processingStep: {
        processingStepId: null,
        /**
         * Creates the datatables view for the list of all ProcessingStepUpdates for a given ProcessingStep.
         * @param selector The JQuery selector for the table to create the datatable into
         */
        processingStepUpdates: function (selector) {
            "use strict";
            $.otp.createListView(selector, $.otp.createLink({
                controller: 'processes',
                action: 'processingStepDate',
                id: $.otp.workflows.processingStep.processingStepId + '/'
            }), false, function (json) {
                var i, rowData;
                for (i = 0; i < json.aaData.length; i += 1) {
                    rowData = json.aaData[i];
                    rowData[1] = $.otp.workflows.renderDate(rowData[1]);
                    rowData[2] = rowData[2];
                    if (!rowData[3]) {
                        rowData[3] = "-";
                    } else {
                        if (rowData[3].stackTraceIdentifier) {
                            rowData[3] = $.otp.createLinkMarkup({
                                controller: 'processes',
                                action: 'getProcessingErrorStackTrace',
                                id: rowData[3].id,
                                target: "_blank",
                                text: rowData[3].errorMessage
                            });
                        } else {
                            rowData[3] = rowData[3].errorMessage;
                        }
                    }
                }
            }, [
                { "bSortable": true,  "aTargets": [0] },
                { "bSortable": false, "aTargets": [1] },
                { "bSortable": false, "aTargets": [2] },
                { "bSortable": false, "aTargets": [3] }
            ]);
        },
        /**
         * Creates the datatables view for the list of Parameters (either input or output) for a given ProcessingStep.
         * @param selector The JQuery selector for the table to create the datatable into
         * @param inputOrOutput If {@code true} the input parameters are retrieved, if {@code false} the output
         */
        parameters: function (selector, inputOrOutput) {
            "use strict";
            $.otp.createListView(selector, $.otp.createLink({
                controller: 'processes',
                action: 'parameterData',
                id: $.otp.workflows.processingStep.processingStepId + '/'
            }), true, function (data) {
                var i, j, rowData, ids;
                for (i = 0; i < data.aaData.length; i += 1) {
                    rowData = data.aaData[i];
                    if (rowData[1] === "__pbsIds") {
                        ids = rowData[3].split(',');
                        rowData[3] = "<ul>";
                        for (j = 0; j < ids.length; j += 1) {
                            rowData[3] += "<li";
                            if (j > 4) {
                                rowData[3] += ' style="display: none;"';
                            }
                            rowData[3] += ">" + ids[j] + "</li>";
                        }
                        if (ids.length >= 5) {
                            rowData[3] += '<li><a href="#">...</a></li>';
                        }
                        rowData[3] += "</ul>";
                    }
                }
            }, [
                { "bSortable": true,  "aTargets": [0] },
                { "bSortable": false, "aTargets": [1] },
                { "bSortable": false, "aTargets": [2] },
                { "bSortable": false, "aTargets": [3] }
            ], [{"name": "input", "value": inputOrOutput}]);
            $(selector).on("click", "tbody tr td ul li a", function () {
                $("li", $(this).parent().parent()).show();
                $(this).parent().hide();
            });
        },
        register: function (updatesSelector, inputParamSelector, outputParamSelector, stepId) {
            "use strict";
            $.otp.workflows.processingStep.processingStepId = stepId;
            $.otp.workflows.processingStep.processingStepUpdates(updatesSelector);
            $.otp.workflows.processingStep.parameters(inputParamSelector, true);
            $.otp.workflows.processingStep.parameters(outputParamSelector, false);
        },
        promptClusterJobIds: function (clusterJobIds) {
            prompt("All cluster job IDs", clusterJobIds);
        }
    }
};
