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

/*jslint unparam: true */
$.otp.sequence = {
    formatProject: function (project) {
        "use strict";
        var result;
        if (project === "PROJECT_NAME") {
            result = "mb";
        } else if (project === "astrocytome") {
            result = "pa";
        } else {
            result = project;
        }
        return result;
    },
    formatRun: function (run) {
        "use strict";
        var result;
        if (run.length > 7) {
            result = run.substr(0, 6) + "…";
        } else {
            result = run;
        }
        return result;
    },
    register: function () {
        "use strict";
        $("#sequenceTable").dataTable({
            bFilter: false,
            bProcessing: true,
            bServerSide: true,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.contextPath + '/sequence/dataTableSource',
            bScrollInfinite: true,
            bScrollCollapse: true,
            sScrollY: "600px",
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "filtering",
                    value: JSON.stringify($.otp.sequence.searchCriteria())
                });
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
                        var i, rowData, row;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            rowData = [
                                '<span title="' + row.projectName + '">' + $.otp.sequence.formatProject(row.projectName) + '</span>',
                                '<a href="' + $.otp.contextPath + '/individual/show/' + row.individualId + '">' + row.mockPid + '</a>',
                                row.sampleTypeName,
                                row.seqTypeName,
                                row.libraryLayout,
                                row.seqCenterName,
                                '<a href="' + $.otp.contextPath + '/run/show/' + row.runId + '" title="' + row.name + '">' + $.otp.sequence.formatRun(row.name) + '</a>',
                                row.laneId,
                                row.fastqcState.name,
                                row.alignmentState.name,
                                row.hasOriginalBam,
                                (new Date(row.dateCreated)).toDateString()
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            },
            fnRowCallback: function (nRow) {
                var fastqc, alignment, origAlignment;
                fastqc = $("td:eq(8)", nRow);
                fastqc.attr("title", fastqc.text());
                if (fastqc.text() === "FINISHED") {
                    fastqc.addClass("true");
                } else {
                    fastqc.addClass("false");
                }
                fastqc.text("");

                alignment = $("td:eq(9)", nRow);
                alignment.attr("title", alignment.text());
                if (alignment.text() === "FINISHED") {
                    alignment.addClass("true");
                } else {
                    alignment.addClass("false");
                }
                alignment.text("");

                origAlignment = $("td:eq(10)", nRow);
                origAlignment.addClass(origAlignment.text());
                origAlignment.text("");
            }
        });
        // search criteria
        $("#searchCriteriaTable tr td:eq(0) select").change($.otp.sequence.searchCriteriaChangeHandler);
        $("#searchCriteriaTable tr td:eq(2) input[type=button]").click($.otp.sequence.searchCriteriaAddRow);
        $("#searchCriteriaTable tr td:eq(1) select").change(function () {
            $("#sequenceTable").dataTable().fnDraw();
        });
        $("#searchCriteriaTable tr td:eq(1) input[type=text]").change(function () {
            $("#sequenceTable").dataTable().fnDraw();
        });
        $("#searchCriteriaTable tr td:eq(1) input[type=text]").keydown(function () {
            $("#sequenceTable").dataTable().fnDraw();
        });
    },
    searchCriteriaChangeHandler: function () {
        "use strict";
        var tr = $(this).parent().parent();
        $("td:eq(1) *", tr).hide();
        $("td:eq(2) input", tr).hide();
        if ($(this).val() !== "none") {
            $("td select[name=" + $(this).val() + "]", tr).show();
            $("td select[name=" + $(this).val() + "] option", tr).show();
            $("td input[name=" + $(this).val() + "]", tr).show();
            $("td:eq(2) input", tr).show();
        } else {
            // decide whether to delete this element
            if ($("tr", tr.parent()).size() > 1) {
                tr.detach();
            }
        }
        $("#sequenceTable").dataTable().fnDraw();
    },
    searchCriteriaAddRow: function () {
        "use strict";
        var tr, cloned;
        tr = $(this).parent().parent();
        cloned = tr.clone();
        $("td:eq(1) *", cloned).hide();
        $("td:eq(2) input", cloned).hide();
        $("td:eq(0) select", cloned).val("none");
        cloned = cloned.appendTo($("#searchCriteriaTable"));
        $("td:eq(0) select", cloned).change($.otp.sequence.searchCriteriaChangeHandler);
        $("td:eq(2) input[type=button]", cloned).click($.otp.sequence.searchCriteriaAddRow);
        $("td:eq(1) select", cloned).change(function () {
            $("#sequenceTable").dataTable().fnDraw();
        });
        $("td:eq(1) input[type=text]", cloned).change(function () {
            $("#sequenceTable").dataTable().fnDraw();
        });
        $("td:eq(1) input[type=text]", cloned).keydown(function () {
            $("#sequenceTable").dataTable().fnDraw();
        });
    },
    searchCriteria: function () {
        "use strict";
        var result = [];
        $("#searchCriteriaTable tr").each(function (index, element) {
            var selection = $("td:eq(0) select", element).val();
            if (selection !== "none") {
                result.push({type: selection, value: $("td select[name=" + selection + "], td input[name=" + selection + "]", element).val()});
            }
        });
        return result;
    }
};

$.otp.addIndividual = {
    addSample: function (event) {
        "use strict";
        event.preventDefault();
        var existingSamples, placeOfLastSampleInTable, sampleBoxes, newSampleRow;
        // Subtract the hidden element
        existingSamples = $("tr.sample").not(".hidden");
        placeOfLastSampleInTable = existingSamples.last();
        sampleBoxes = $("tr.hidden").clone().removeClass("hidden");
        sampleBoxes.find("button").click($.otp.plusButtonClickHandler);
        sampleBoxes.appendTo(placeOfLastSampleInTable.parent());
        newSampleRow = $("tr.newSample");
        newSampleRow.appendTo(sampleBoxes.last().parent());
    },
    addSampleIdentifier: function (event) {
        "use strict";
        event.preventDefault();
        var clickedButton, td, row, inputs, newInput, minusButton;
        clickedButton = $(this);
        td = clickedButton.parent().parent().get(0);
        row = $(td).parent().get(0);
        inputs = $(row).find("input");
        newInput = inputs.last().clone().val('');
        minusButton = clickedButton.clone();
        minusButton.text("-");
        newInput.appendTo($(td));
        newInput.wrap('<div class="removeSampleIdentifier"></div>');
        minusButton.click(function (event) {
            event.preventDefault();
            $(this).parent().remove();
        });
        minusButton.appendTo(newInput.parent());
    },
    submitIndividual: function (event) {
        "use strict";
        event.preventDefault();
        var samplesArray = [], samples = "";
        $.each($("tr.sample").not("tr.sample:hidden"), function (index, value) {
            var sample = { };
            sample.id = [];
            sample.type = ($("select option:selected", value).val());
            $(value).next().find("input").each( function(idx, v) {
                sample.id.push($(v).val());
            });
            samplesArray.push(sample);
        });
        samples = JSON.stringify(samplesArray);
        $.getJSON($.otp.contextPath + "/individual/save", {
            pid: $("#pid").val(),
            project: $("#project").val(),
            mockPid: $("#mockPid").val(),
            mockFullName: $("#mockFullName").val(),
            individualType: $("#individualType").val(),
            samples: samples
        }, function (data) {
            var message, i;
            if (data.error) {
                $.otp.warningMessage(data.error);
            } else if (data.errors) {
                message = "<ul>";
                for (i = 0; i < data.errors.length; i += 1) {
                    message += "<li>" + data.errors[i].message + "</li>";
                }
                message += "</ul>";
                $.otp.warningMessage(message);
            } else if (data.success) {
                console.log(("juhu"));
                $.otp.infoMessage($.i18n.prop("individual.insert.add.success", data.individual));
            }
        }).error(function (jqXHR) {
            $.otp.warningMessage(jqXHR.statusText + jqXHR.status);
        });
    },
    register: function () {
        "use strict";
        $("div.addSample button").click(this.addSample);
        $("div.newSampleIdentifier button").click(this.addSampleIdentifier);
        $("#add-individual-form").submit(this.submitIndividual);
    }
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
};

/**
 * Helper method to render a date in a common way.
 * @param value Date in JSON representation
 * @returns Formatted date
 */
OTP.prototype.renderDate = function (value) {
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
        return  text;
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
        bScrollInfinite: true,
        bScrollCollapse: true,
        sScrollY: "600px",
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
    window.setInterval(function () {
        if ($.otp.autorefresh.enabled) {
            $(selector).dataTable().fnDraw();
        }
    }, 10000);
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

/**
 * Handles the enable/disable auto-refresh functionality.
 */
$.otp.autorefresh = {
    /**
     * Whether auto-refresh is currently enabled.
     */
    enabled: false,
    /**
     * Registers the click handler on the links
     */
    setup: function (enabled) {
        "use strict";
        $("#refreshBox a").click($.otp.autorefresh.handleClick);
        $.otp.autorefresh.enabled = enabled;
    },
    /**
     * The click handler for the links. Performs an AJAX request to enable/disable auto-refresh.
     * @param event
     */
    handleClick: function (event) {
        "use strict";
        event.preventDefault();
        $.getJSON($(this).attr("href"), $.otp.autorefresh.ajaxHandler);
    },
    /**
     * Callback for AJAX request to enable/diasble auto-refresh
     * @param data
     */
    ajaxHandler: function (data) {
        "use strict";
        if (data.enabled === true) {
            $("#refreshBox span.enable").hide();
            $("#refreshBox span.disable").show();
            $.otp.autorefresh.enabled = true;
        } else if (data.enabled === false) {
            $("#refreshBox span.enable").show();
            $("#refreshBox span.disable").hide();
            $.otp.autorefresh.enabled = false;
        }
    }
};

$.otp.notificationAdministration = {
    setup: function () {
        "use strict";
        $("#notificationsTable").dataTable({
            bFilter: true,
            bProcessing: true,
            bServerSide: true,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.contextPath + '/notification/dataTableSource',
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
                        var i, rowData, row, trigger;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            trigger = row.trigger.value;
                            if (row.trigger.link !== null) {
                                trigger = '<a href="' + row.trigger.link + '">' + row.trigger.value + '</a>';
                            }
                            trigger += '<input type="hidden" name="id" value="' + row.trigger.id + '"/>';
                            trigger += '<input type="hidden" name="className" value="' + row.trigger.clazz + '"/>';
                            trigger += '<input type="hidden" name="triggerId" value="' + row.trigger.triggerId + '"/>';
                            rowData = [
                                '<input type="checkbox" name="' + row.notification.id + '" ' + (row.notification.enabled ? 'checked="checked"' : '') + '/>',
                                row.notification.type.name,
                                row.notification.medium.name,
                                trigger,
                                '<input type="hidden" value="' + row.subject.id + '"/><span>' + row.subject.template + '</span>',
                                '<input type="hidden" value="' + row.template.id + '"/><span>' + row.template.template + '</span>'
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            },
            fnRowCallback: function (nRow) {
                var cell, selection, notificationId;
                // install click callback for the checkbox
                notificationId = $("td:eq(0) input[type=checkbox]", nRow).attr("name");
                $("td:eq(0) input[type=checkbox]", nRow).click($.otp.notificationAdministration.enableNotification);
                // set the Type select
                cell = $('td:eq(1)', nRow);
                selection = $("#typeSelection-Prototype").clone();
                $("option[value=" + cell.text() + "]", selection).attr("selected", true);
                selection.removeAttr("id");
                selection.attr("name", notificationId);
                selection.show();
                cell.empty();
                selection.appendTo(cell).change(function () {
                    var selection = $(this);
                    $.getJSON($.otp.contextPath + "/notification/updateType/" + selection.attr("name"),
                            {type: $("option:selected", selection).val()},
                            $.otp.notificationAdministration.notificationUpdate)
                            .error($.otp.notificationAdministration.errorHandler);
                });

                // set the medium select
                cell = $('td:eq(2)', nRow);
                selection = $("#mediumSelection-Protoype").clone();
                $("option[value=" + cell.text() + "]", selection).attr("selected", true);
                selection.removeAttr("id");
                selection.attr("name", notificationId);
                selection.show();
                cell.empty();
                selection.appendTo(cell).change(function () {
                    var selection = $(this);
                    $.getJSON($.otp.contextPath + "/notification/updateMedium/" + selection.attr("name"),
                            {medium: $("option:selected", selection).val()},
                            $.otp.notificationAdministration.notificationUpdate)
                            .error($.otp.notificationAdministration.errorHandler);
                });

                // add the trigger edit button
                $('<button class="edit"></button>').appendTo($("td:eq(3)", nRow)).click(function () {
                    var cell, dialog, triggerId;
                    cell = $(this).parent();
                    dialog = $("#trigger-dialog").clone();
                    triggerId = $("input:hidden[name=triggerId]", cell).val();
                    dialog.removeAttr("id");
                    $("input:hidden", dialog).val($("input:hidden[name=id]").val());
                    if ($("input:hidden[name=className]", cell).val() === "de.dkfz.tbi.otp.job.plan.JobExecutionPlan") {
                        // it's a job execution plan
                        $("select[name=jobExecutionPlan] option[value=" + triggerId + "]", dialog).attr("selected", true);
                    } else if ($("input:hidden[name=className]", cell).val() === "de.dkfz.tbi.otp.job.plan.JobDefinition") {
                        $.getJSON($.otp.contextPath + "/notification/jobDefinition/" + $("input:hidden[name=triggerId]", cell).val(), function (data) {
                            $("select[name=jobExecutionPlan] option[value=" + data.jobExecutionPlan + "]", dialog).attr("selected", true);
                            $.otp.notificationAdministration.createJobOptions($("select[name=jobDefinition]", dialog), data.jobs);
                            $("option[value=" + triggerId + "]", dialog).attr("selected", true);
                            $("select[name=jobDefinition]", dialog).show();
                            $("input:checkbox", dialog).attr("checked", true);
                        });
                    }
                    // when the checkbox get checked load the JobDefinitions
                    $("input:checkbox", dialog).click(function () {
                        var jobSelection = $("select[name=jobDefinition]", dialog);
                        if ($(this).attr("checked")) {
                            jobSelection.show();
                            $.getJSON($.otp.contextPath + "/notification/jobDefinitions/" + $("select[name=jobExecutionPlan] option:selected", dialog).val(), function (data) {
                                $.otp.notificationAdministration.createJobOptions(jobSelection, data.jobs);
                            }).error($.otp.notificationAdministration.errorHandler);
                        } else {
                            jobSelection.empty();
                            jobSelection.hide();
                        }
                    });
                    // when the workflow selection changes, reload the job selection (if shown)
                    $("select[name=jobExecutionPlan]", dialog).change(function () {
                        if ($("input:checkbox", dialog).attr("checked") === "checked") {
                            var jobSelection = $("select[name=jobDefinition]", dialog);
                            jobSelection.empty();
                            $.getJSON($.otp.contextPath + "/notification/jobDefinitions/" + $("select[name=jobExecutionPlan] option:selected", dialog).val(), function (data) {
                                $.otp.notificationAdministration.createJobOptions(jobSelection, data.jobs);
                            }).error($.otp.notificationAdministration.errorHandler);
                        }
                    });
                    // and show the dialog
                    dialog.dialog({
                        buttons: {
                            "Save": function () {
                                var dialog;
                                dialog = $(this);
                                $.getJSON($.otp.contextPath + "/notification/updateTrigger/" + $("input:hidden", dialog).val(),
                                        {
                                        jobExecutionPlan: $("select[name=jobExecutionPlan] option:selected", dialog).val(),
                                        jobDefinition: $("select[name=jobDefinition] option:selected", dialog).val()
                                    }, $.otp.notificationAdministration.notificationUpdate
                                    ).error($.otp.notificationAdministration.errorHandler);
                                dialog.dialog("close");
                            },
                            "Cancel": function () {
                                $(this).dialog("close");
                            }
                        }
                    });
                });

                // adjust the subject and message
                $.otp.notificationAdministration.template($('td:eq(4)', nRow));
                $.otp.notificationAdministration.template($('td:eq(5)', nRow));
                return nRow;
            }
        });
    },
    /**
     * Creates options out of the jobs and appends them to the jobSelection.
     * Basically a method to build up the list of JobDefinitions
     * @param jobSelection jQuery object pointing to a selection list
     * @param jobs List of objects with at least elments id and name
     */
    createJobOptions: function (jobSelection, jobs) {
        "use strict";
        var i, job;
        for (i = 0; i < jobs.length; i += 1) {
            job = jobs[i];
            $('<option value="' + job.id + '">' + job.name + '</option>').appendTo(jobSelection);
        }
    },
    /**
     * Creates the cell markup for a template. The text is shortened with an expand link
     * and an edit button is added which shows a dialog to update the template.
     * @param cell The table cell the template is in
     */
    template: function (cell) {
        "use strict";
        var span, editButton;
        span = $("span", cell);
        span.attr("title", span.text());
        if (span.text().length > 20) {
            $.otp.notificationAdministration.expandTemplateMarkup(span);
        }
        editButton = $('<button class="edit"></button>');
        editButton.click(function () {
            var cell, dialog;
            cell = $(this).parent();
            dialog = $("#template-dialog").clone();
            dialog.removeAttr("id");
            $("textarea", dialog).val($("span", cell).attr("title"));
            $("input:hidden", dialog).val($("input:hidden", cell).val());
            dialog.dialog({
                buttons: {
                    "Save": function () {
                        $.getJSON($.otp.contextPath + "/notification/updateTemplate/" + $("input:hidden", dialog).val(),
                                {text: $("textarea", dialog).val()}, $.otp.notificationAdministration.notificationUpdate)
                                .error($.otp.notificationAdministration.errorHandler);
                        $(this).dialog("close");
                    },
                    "Cancel": function () {
                        $(this).dialog("close");
                    }
                }
            });
        });
        editButton.appendTo(cell);
    },
    /**
     * Adjusts the template for too long text by using only the first twenty characters
     * and adding a link to expand the text.
     * @param span The text element to shorten
     */
    expandTemplateMarkup: function (span) {
        "use strict";
        span.text(span.text().substring(0, 19));
        $('<a href="#">…</a>').appendTo(span).click($.otp.notificationAdministration.expandTemplate);
    },
    /**
     * Callback for expanding the text of a template. Reads the actual text from the title
     * element of the parent of the clicked element and sets it on this element. In addition
     * a link to collapse the text again is added.
     * @param event The click event which triggered the expand
     */
    expandTemplate: function (event) {
        "use strict";
        var span;
        span = $(this).parent();
        event.preventDefault();
        span.text(span.attr("title") + " ");
        $('<a href="#">' + $.i18n.prop("notification.administration.template.collapse") + '</a>').appendTo(span).click($.otp.notificationAdministration.collapseTemplate);
    },
    /**
     * Callback for collapsing the text of a template. Shortens in the same way as in the initial
     * rendering of the table.
     * @param event The click event which triggered the collapse
     */
    collapseTemplate: function (event) {
        "use strict";
        event.preventDefault();
        $.otp.notificationAdministration.expandTemplateMarkup($(this).parent());
    },
    /**
     * Callback for the enable/disable notification checkbox. Performs an AJAX request to update
     * the enable state of the notification.
     */
    enableNotification: function () {
        "use strict";
        var checkBox = $(this);
        $.getJSON($.otp.contextPath + "/notification/enableNotification/" + checkBox.attr("name"),
                {enabled: checkBox.attr("checked") === "checked"}, $.otp.notificationAdministration.notificationUpdate)
                .error($.otp.notificationAdministration.errorHandler);
    },
    /**
     * Reloads the notification datatable.
     */
    update: function () {
        "use strict";
        $("#notificationsTable").dataTable().fnDraw();
    },
    /**
     * Callback for any AJAX request which alters the state of a notification.
     * It expects a JSON structure containing either a boolean success field or an error with the error message.
     * In success case a notification is shown informing about the update, in error case the error message is shown.
     * In both cases the datatable is reloaded to show the current state.
     * @param data The JSON data fetched through AJAX
     */
    notificationUpdate: function (data) {
        "use strict";
        var i, message;
        if (data.success) {
            $.otp.infoMessage($.i18n.prop("notification.administration.update.success"));
        } else if (data.error) {
            $.otp.warningMessage(data.error);
        } else if (data.errors) {
            message = "<ul>";
            for (i = 0; i < data.errors.length; i += 1) {
                message += "<li>" + data.errors[i].message + "</li>";
            }
            message += "</ul>";
            $.otp.warningMessage(message);
        }
        $.otp.notificationAdministration.update();
    },
    /**
     * Callaback for any failing AJAX request.
     * @param jqXHR
     */
    errorHandler: function (jqXHR) {
        "use strict";
        if (jqXHR.status === 404) {
            $.otp.warningMessage($.i18n.prop("notification.administration.update.notFound"));
        } else {
            $.otp.warningMessage(jqXHR.statusText + jqXHR.status);
        }
    }
};
