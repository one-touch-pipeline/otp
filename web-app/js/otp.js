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

$.otp.message = function (message, warning) {
    "use strict";
    if (!message) {
        return;
    }
    var classes, button, divCode;
    classes = "message";
    if (warning) {
        classes += " errors";
    }
    button = $("<div class=\"close\"><button></button></div>");
    $("button", button).click(function () {
        $(this).parent().parent().remove();
    });
    divCode = $("<div class=\"" + classes + "\"><p>" + message + "</p></div>");
    button.appendTo(divCode);
    divCode.append($("<div style=\"clear: both;\"></div>"));
    $("#infoBox").append(divCode);
};

$.otp.infoMessage = function (message) {
    "use strict";
    this.message(message, false);
};

$.otp.warningMessage = function (message) {
    "use strict";
    this.message(message, true);
};

/**
 * Generates a generic datatable.
 * The datatable expects an ajax source at "dataTableSource" which provides a JSON response.
 * The first column of the data source is expected to be a complex datatabe consisting of <em>id</em>
 * and a descriptive <em>text</em>.
 * @param selector The jQuery selector of the table
 * @param showLink The link to the controller/action to show more details of an element
 */
$.otp.genericList = function (selector, showLink) {
    "use strict";
    $(selector).dataTable({
        bFilter: true,
        bProcessing: true,
        bServerSide: true,
        bSort: true,
        bJQueryUI: false,
        bAutoWidth: false,
        sAjaxSource: 'dataTableSource',
        bScrollInfinite: true,
        bScrollCollapse: true,
        sScrollY: "600px",
        bDeferRender: true,
        fnServerData: function (sSource, aoData, fnCallback) {
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "error": function () {
                    // clear the table
                    fnCallback({aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0});
                },
                "success": function (json) {
                    var i, rowData;
                    for (i = 0; i < json.aaData.length; i += 1) {
                        rowData = json.aaData[i];
                        rowData[0] = "<a href=\"" + $.otp.contextPath + showLink + rowData[0].id + "\">" + rowData[0].text + "</a>";
                    }
                    fnCallback(json);
                }
            });
        }
    });
};

/**
 * Generates the list of individuals
 **/
$.otp.individualList = function () {
    "use strict";
    $.otp.genericList('#individualTable', "/individual/show/");
};

/**
 * Generates the list of Runs
 **/
$.otp.runList = function () {
    "use strict";
    $.otp.genericList('#runTable', "/run/show/");
};

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
        bJQueryUI: false,
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
 * @param selector Selector for the datatable
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
 * Renders a graph representing a JobExecutionPlan or Process.
 * @param idName Name of the element id where to render the graph to
 * @param data JSON data structure containing jobs and connections
 **/
OTP.prototype.renderJobExecutionPlanGraph = function (idName, data) {
    "use strict";
    var i, g, job, c, layouter, renderer;
    var render = function (r, n) {
        var element, textElement, i, param, addNewline, attr;
        var parameters = "";
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
                href: $.otp.contextPath + "/processes/processingStep/" + n.processingStep
            });
        }
        element.attr(attr);
        return r.set().push(element, textElement);
    };
    g = new Graph();
    g.edgeFactory.template.style.directed = true;
    for (i = 0; i < data.jobs.length; i += 1) {
        job = data.jobs[i];
        job.render = render;
        job.label = job.name;
        g.addNode(job.id, job);
    }
    for (i = 0; i < data.connections.length; i += 1) {
        c = data.connections[i];
        var test = function () {
            var j;
            var fromValid = false;
            var toValid = false;
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
        };
        g.addEdge(c.from, c.to, {
            stroke: test() ? "red" : "black"
        });
    }
    layouter = new Graph.Layout.Spring(g);
    layouter.layout();
    renderer = new Graph.Renderer.Raphael(idName, g, $("#" + idName).parent().width() - 20, 600);
    renderer.draw();
    $("#" + idName).data("graph", {layouter: layouter, renderer: renderer});
};

/**
 * Generates the JobExecutionPlan DSL for the given plan.
 * Writes the DSL into a textarea of a dialog.
 * @param plan JSON structure of the JobExecutionPlan
 **/
OTP.prototype.generatePlanDSL = function (plan) {
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
    $("#enable-workflow-button").click(function () {
        $.get($.otp.contextPath + "/processes/enablePlan/" + planId, function (data) {
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
        $.get($.otp.contextPath + "/processes/disablePlan/" + planId, function (data) {
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
        $.getJSON($.otp.contextPath + "/processes/planVisualization/" + planId, function (data) {
            $.otp.renderJobExecutionPlanGraph("plan-visualization", data);
        });
    });
    $("#hide-visualization").click(function () {
        $("#plan-visualization").hide();
        $("#show-visualization").show();
        $(this).hide();
    });
    $("#generate-dsl").click(function () {
        $.getJSON($.otp.contextPath + "/processes/planVisualization/" + planId, $.otp.generatePlanDSL);
    });
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
    $("#show-visualization").click(function () {
        $("#process-visualization").show();
        $("#hide-visualization").show();
        $(this).hide();
        if ($("#process-visualization").data("graph")) {
            $("#process-visualization").data("graph").layouter.layout();
            $("#process-visualization").data("graph").renderer.draw();
            return;
        }
        $.getJSON($.otp.contextPath + "/processes/processVisualization/" + processId, function (data) {
            $.otp.renderJobExecutionPlanGraph("process-visualization", data);
        });
    });
    $("#hide-visualization").click(function () {
        $("#plan-visualization").hide();
        $("#show-visualization").show();
        $(this).hide();
    });
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
            } else {
                if (rowData[3].stackTraceIdentifier) {
                    rowData[3] = '<a href="' + $.otp.contextPath + "/processes/getProcessingErrorStackTrace/" + rowData[3].id + '" target="_blank">' + rowData[3].errorMessage + '</a>';
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
