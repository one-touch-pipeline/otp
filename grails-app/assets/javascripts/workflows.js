/*jslint continue: true */
/*global $, Graph */
$.otp.workflows = {
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
    statusImageHtml: function (status) {
        "use strict";
        return '<img src="' + $.otp.createLink({
            controller: 'assets',
            action: 'status',
            id: $.otp.workflows.statusToImage(status)
        }) + '" alt="' + status + '" title="' + status + '"/>';
    },

    /**
     * Converts the status name into the image name which is used to represent the status.
     * @param status The name as listed in documentation for statusImageHtml
     * @returns Name of image or {@code null} if incorrect status
     * @see statusImageHtml
     */
    statusToImage: function (status) {
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
            return "red_anime.gif";
        case "FINISHED":
            return "blue.png";
        case "SUCCESS":
            return "green.png";
        case "FAILURE":
        case "RESTARTED":
            return "red.png";
        }
        return null;
    },
    /**
     * Helper method to render a date in a common way.
     * @param value Date in JSON representation
     * @returns Formatted date
     */
    renderDate: function (value) {
        "use strict";
        if (value) {
            var date, isInLast24Hours, text;
            date = new Date(value);
            isInLast24Hours = function (d) {
                return ((Date.now() - d.getTime()) <= 24 * 60 * 60 * 1000);
            };
            text = '<span title="' + date.toLocaleString() + '">';
            if (isInLast24Hours(date)) {
                text += date.toLocaleTimeString();
            } else {
                text += date.toDateString();
            }
            text += '</span>';
            return text;
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
     **/
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
     **/
    createRestartProcessingStepLink: function (id, dataTable) {
        "use strict";
        var imageLink = $.otp.createLink({
            controller: 'assets',
            action: 'redo.png'
        });
        return '<a onclick="$.otp.workflows.restartProcessingStep(' + id + ', \'' + dataTable + '\');" href="#" title="Restart" ><img src="' + imageLink + '"/></a>';
    },
    planGraph: {
        render: function (r, n) {
            "use strict";
            var element, textElement, i, param, addNewline, attr, parameters;
            parameters = "";
            addNewline = false;
            if (n.constantParameters.length > 0) {
                parameters = "Constant Parameters:\n";
            }
            for (i = 0; i < n.constantParameters.length; i += 1) {
                if (addNewline) {
                    parameters += "\n";
                }
                param = n.constantParameters[i];
                parameters += param.type.name + ": " + param.value;
                addNewline = true;
            }
            if (n.inputParameters.length > 0) {
                if (addNewline) {
                    parameters += "\n";
                }
                parameters = "Input Parameters:\n";
                addNewline = false;
            }
            for (i = 0; i < n.inputParameters.length; i += 1) {
                if (addNewline) {
                    parameters += "\n";
                }
                param = n.inputParameters[i];
                parameters += param.type.name + ": ";
                if (param.value) {
                    parameters += param.value;
                }
                addNewline = true;
            }
            if (n.outputParameters.length > 0) {
                if (addNewline) {
                    parameters += "\n";
                }
                parameters = "Output Parameters:\n";
                addNewline = false;
            }
            for (i = 0; i < n.outputParameters.length; i += 1) {
                if (addNewline) {
                    parameters += "\n";
                }
                param = n.outputParameters[i];
                parameters += param.type.name + ": ";
                if (param.value) {
                    parameters += param.value;
                }
                addNewline = true;
            }
            attr = {
                'stroke-width': '1px',
                'fill': '#feb',
                'title': parameters
            };
            if (n.startJob) {
                // start jobs are green ellipse
                element = r.ellipse(n.point[0], n.point[1], 75, 25);
                attr.fill = 'green';
            } else if (n.endStateAware) {
                // end state aware jobs are ellipse
                element = r.ellipse(n.point[0], n.point[1], 75, 25);
            } else {
                // normal jobs are rects
                element = r.rect(n.point[0] - 75, n.point[1] - 25, 150, 50);
            }
            if (n.pbsJob) {
                // pbsJobs are orange
                attr.fill = 'orange';
            }
            textElement = r.text(n.point[0], n.point[1], (n.label || n.id));
            if (n.processingStep !== undefined && n.processingStep !== null) {
                if (n.failed) {
                    attr.fill = 'red';
                } else if (n.succeeded) {
                    attr.fill = 'lightgreen';
                } else if (n.finished) {
                    attr.fill = 'lightblue';
                }
                textElement.attr({
                    href: $.otp.createLink({
                        controller: 'processes',
                        action: 'processingStep',
                        id: n.processingStep
                    })
                });
            }
            element.attr(attr);
            return r.set().push(element, textElement);
        },
        testEdgeValid: function (data, c) {
            "use strict";
            var j, fromValid, toValid;
            fromValid = false;
            toValid = false;
            for (j = 0; j < data.jobs.length; j += 1) {
                if (j === 0 && data.jobs[j].id === c.from) {
                    fromValid = true;
                    continue;
                }
                if (data.jobs[j].id === c.from) {
                    fromValid = data.jobs[j].processingStep !== null;
                }
                if (data.jobs[j].id === c.to) {
                    toValid = data.jobs[j].processingStep !== null;
                }
            }
            return fromValid && toValid;
        },
        /**
         * Renders a graph representing a JobExecutionPlan or Process.
         * @param idName Name of the element id where to render the graph to
         * @param data JSON data structure containing jobs and connections
         **/
        setup: function (idName, data) {
            "use strict";
            var i, g, job, c, layouter, renderer;
            g = new Graph();
            g.edgeFactory.template.style.directed = true;
            for (i = 0; i < data.jobs.length; i += 1) {
                job = data.jobs[i];
                job.render = $.otp.workflows.planGraph.render;
                job.label = job.name;
                g.addNode(job.id, job);
            }
            for (i = 0; i < data.connections.length; i += 1) {
                c = data.connections[i];
                g.addEdge(c.from, c.to, {
                    stroke: $.otp.workflows.planGraph.testEdgeValid(data, c) ? "red" : "black"
                });
            }
            layouter = new Graph.Layout.Spring(g);
            layouter.layout();
            renderer = new Graph.Renderer.Raphael(idName, g, $("#" + idName).parent().width() - 20, 600);
            renderer.draw();
            $("#" + idName).data("graph", {layouter: layouter, renderer: renderer});
        }
    },
    /**
     * Generates the JobExecutionPlan DSL for the given plan.
     * Writes the DSL into a textarea of a dialog.
     * @param plan JSON structure of the JobExecutionPlan
     **/
    generatePlanDSL: function (plan) {
        "use strict";
        var startJob, job, dsl, findStartJob, nextJob, renderConstantParameters, findOutputParameter, jobForParameter, renderInputParameters, renderOutputParameters;
        dsl = "";
        findStartJob = function () {
            var i;
            for (i = 0; i < plan.jobs.length; i += 1) {
                if (plan.jobs[i].startJob) {
                    return plan.jobs[i];
                }
            }
            return null;
        };
        nextJob = function (currentJob) {
            var i, j;
            for (i = 0; i < plan.connections.length; i += 1) {
                if (plan.connections[i].from === currentJob.id) {
                    for (j = 0; j < plan.jobs.length; j += 1) {
                        if (plan.jobs[j].id === plan.connections[i].to) {
                            return plan.jobs[j];
                        }
                    }
                }
            }
            return null;
        };
        renderConstantParameters = function (job) {
            var i, param;
            for (i = 0; i < job.constantParameters.length; i += 1) {
                param = job.constantParameters[i];
                dsl += "        constantParameter(\"";
                dsl += param.type.name;
                dsl += "\", \"";
                dsl += param.value;
                dsl += "\")\n";
            }
        };
        findOutputParameter = function (id) {
            var i, j, job;
            for (i = 0; i < plan.jobs.length; i += 1) {
                job = plan.jobs[i];
                for (j = 0; j < job.outputParameters.length; j += 1) {
                    if (id === job.outputParameters[j].type.id) {
                        return job.outputParameters[j];
                    }
                }
                for (j = 0; j < job.passthroughParameters.length; j += 1) {
                    if (id === job.passthroughParameters[j].type.id) {
                        return findOutputParameter(job.passthroughParameters[j].mapping);
                    }
                }
            }
            return null;
        };
        jobForParameter = function (parameter) {
            var i, j, job;
            for (i = 0; i < plan.jobs.length; i += 1) {
                job = plan.jobs[i];
                for (j = 0; j < job.inputParameters.length; j += 1) {
                    if (parameter === job.inputParameters[j]) {
                        return job;
                    }
                }
                for (j = 0; j < job.outputParameters.length; j += 1) {
                    if (parameter === job.outputParameters[j]) {
                        return job;
                    }
                }
                for (j = 0; j < job.passthroughParameters.length; j += 1) {
                    if (parameter === job.passthroughParameters[j]) {
                        return job;
                    }
                }
            }
            return null;
        };
        renderInputParameters = function (job) {
            var i, outputParameter;
            for (i = 0; i < job.inputParameters.length; i += 1) {
                outputParameter = findOutputParameter(job.inputParameters[i].mapping);
                if (!outputParameter) {
                    continue;
                }
                dsl += "        inputParameter(\"";
                dsl += job.inputParameters[i].type.name;
                dsl += "\", \"";
                dsl += jobForParameter(outputParameter).name;
                dsl += "\", \"";
                dsl += outputParameter.type.name;
                dsl += "\")\n";
            }
        };
        renderOutputParameters = function (job) {
            var i, param;
            for (i = 0; i < job.outputParameters.length; i += 1) {
                param = job.outputParameters[i];
                dsl += "        outputParameter(\"";
                dsl += param.type.name;
                dsl += "\")\n";
            }
        };
        dsl += "plan(\"" + plan.name + "\") {\n";
        startJob = findStartJob();
        dsl += "    start(\"" + startJob.name + "\", \"" + startJob.bean + "\") {\n";
        renderConstantParameters(startJob);
        renderOutputParameters(startJob);
        dsl += "    }\n";
        job = nextJob(startJob);
        while (job) {
            dsl += "    job(\"" + job.name + "\", \"" + job.bean + "\") {\n";
            renderConstantParameters(job);
            renderInputParameters(job);
            renderOutputParameters(job);
            dsl += "    }\n";
            job = nextJob(job);
        }
        dsl += "}\n";
        $("#plan-dsl-dialog textarea").text(dsl);
        $("#plan-dsl-dialog").dialog();
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
                        return row.allProcessesCount;
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
                                failed: true
                            },
                            text: row.failedProcessesCount
                        });
                    }
                    return "-";
                }, "aTargets": [2] },
                { "mRender": function (data, type, row) {
                    if (row.runningProcessesCount) {
                        return row.runningProcessesCount;
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
     * @param failed Whether to limit to failed processes (true) or include all processes (false)
     */
    registerProcesses: function (selector, planId, failed) {
        "use strict";
        $.otp.createListView(selector, $.otp.createLink({
            controller: 'processes',
            action: 'planData',
            id: planId,
            parameters: {
                failed: failed
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
                rowData[1] = $.otp.workflows.statusImageHtml(rowData[1].name);
                if (rowData[2]) {
                    if (rowData[2].controller) {
                        rowData[2] = $.otp.createLinkMarkup(rowData[2]);
                    } else {
                        rowData[2] = rowData[2].text;
                    }
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
                        id: rowData[6].id,
                        title: rowData[6].error,
                        text: rowData[6].state.name
                    });
                } else {
                    rowData[6] = rowData[6].state.name;
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
                    switch (actions[j]) {
                    case "restart":
                        rowData[8] += $.otp.workflows.createRestartProcessingStepLink(stepId, selector);
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
            { "bSortable": true, "aTargets": [3] },
            { "bSortable": false, "aTargets": [4] },
            { "bSortable": false, "aTargets": [5] },
            { "bSortable": false, "aTargets": [6] },
            { "bSortable": false, "aTargets": [7] },
            { "bSortable": false, "aTargets": [8] }
        ], undefined, 240, {
            iDisplayLength: 25,
            bPaginate: true
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
                var image = $("img", $("#disable-workflow-button").parent());
                image.attr("src", image.attr("src").replace("grey", "green"));
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
                var image = $("img", $("#disable-workflow-button").parent());
                image.attr("src", image.attr("src").replace("green", "grey"));
            });
        });
        $("#show-visualization").click(function () {
            $("#plan-visualization").show();
            $("#hide-visualization").show();
            $(this).hide();
            if ($("#plan-visualization").data("graph")) {
                $("#plan-visualization").data("graph").layouter.layout();
                $("#plan-visualization").data("graph").renderer.draw();
                return;
            }
            $.getJSON($.otp.createLink({
                controller: 'processes',
                action: 'planVisualization',
                id: planId
            }), function (data) {
                $.otp.workflows.planGraph.setup("plan-visualization", data);
            });
        });
        $("#hide-visualization").click(function () {
            $("#plan-visualization").hide();
            $("#show-visualization").show();
            $(this).hide();
        });
        $("#generate-dsl").click(function () {
            $.getJSON($.otp.createLink({
                controller: 'processes',
                action: 'planVisualization',
                id: planId
            }), $.otp.workflows.generatePlanDSL);
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
            var i, j, rowData, stepId, actions;
            for (i = 0; i < json.aaData.length; i += 1) {
                rowData = json.aaData[i];
                stepId = rowData[0];
                rowData[0] = $.otp.createLinkMarkup({
                    controller: 'processes',
                    action: 'processingStep',
                    id: stepId,
                    text: stepId
                });
                rowData[1] = $.otp.workflows.statusImageHtml(rowData[1].name);
                if (rowData[3]) {
                    rowData[3] = '<span title="' + rowData[3].name + '">' + rowData[3].name.substr(rowData[3].name.lastIndexOf('.') + 1) + "</span><br/>" +
                        '<span title="' + rowData[3].version + '">' + rowData[3].version.substring(0, 7) + '</span>';
                } else {
                    rowData[3] = "-";
                }
                rowData[4] = $.otp.workflows.renderDate(rowData[4]);
                rowData[5] = $.otp.workflows.renderDate(rowData[5]);
                if (rowData[6]) {
                    rowData[6] = $.otp.workflows.formatTimespan(rowData[6]);
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
                        rowData[8] += $.otp.workflows.createRestartProcessingStepLink(stepId, selector);
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
        ],
            undefined,
            140);
        $("#show-visualization").click(function () {
            $("#process-visualization").show();
            $("#hide-visualization").show();
            $(this).hide();
            if ($("#process-visualization").data("graph")) {
                $("#process-visualization").data("graph").layouter.layout();
                $("#process-visualization").data("graph").renderer.draw();
                return;
            }
            $.getJSON($.otp.createLink({
                controller: 'processes',
                action: 'processVisualization',
                id: processId
            }), function (data) {
                $.otp.workflows.planGraph.setup("process-visualization", data);
            });
        });
        $("#hide-visualization").click(function () {
            $("#process-visualization").hide();
            $("#show-visualization").show();
            $(this).hide();
        });
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
                    rowData[2] = rowData[2].name;
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
                            rowData[3] += ">" + rowData[4] + ".e" +ids[j] + "</li>";
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
        }
    }
};
