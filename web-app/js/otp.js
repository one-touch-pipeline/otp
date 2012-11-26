/*jslint browser: true, devel: true */
/*global $, Graph */
$.otp = {
    contextPath: $("head meta[name=contextPath]").attr("content"),
    /**
     * Helper method to extend the given link by a further component.
     * Ensures that there is exactly one slash between the link and the further
     * component.
     * @param link The original link
     * @param component The new component to add
     * @returns link + '/' + component
     */
    addLinkComponent: function (link, component) {
        "use strict";
        if (component === undefined || !component) {
            return link;
        }
        if (!isNaN(link)) {
            link = link.toString();
        }
        if (!isNaN(component)) {
            component = component.toString();
        }
        if (link.charAt(link.length - 1) !== "/" && component.charAt(0) !== "/") {
            link += "/";
        } else if (link.charAt(link.length - 1) === "/" && component.charAt(0) === "/") {
            component = component.substring(1);
        }
        link += component;
        return link;
    },
    /**
     * Creates an URL from the passed in options in the same way as the Grails
     * createLink tag. The options is an object with the following attributes:
     * <ul>
     * <li>controller</li>
     * <li>action</li>
     * <li>id</li>
     * <li>parameters</li>
     * </ul>
     *
     * All elements are optional. Parameters is an object which gets serialized
     * into key/value pairs for the query part of the URL.
     *
     * If all elements are provided the following link structure is generated:
     * <strong>/applicationContextPath/controller/action/id?key1=value1&key2=value2</strong>
     * @param options The URL parts to construct the link from
     * @returns an URL to be used in e.g. href element of an a-attribute
     */
    createLink: function (options) {
        "use strict";
        var link, parameter, counter;
        link = $.otp.contextPath;
        if (options === undefined || !options) {
            return link;
        }
        link = $.otp.addLinkComponent(link, options.controller);
        link = $.otp.addLinkComponent(link, options.action);
        link = $.otp.addLinkComponent(link, options.id);
        if (options.parameters !== undefined && options.parameters && Object.keys(options.parameters).length > 0) {
            link += "?";
            counter = 0;
            for (parameter in options.parameters) {
                if (options.parameters.hasOwnProperty(parameter)) {
                    if (counter > 0) {
                        link += "&";
                    }
                    link += parameter + "=" + options.parameters[parameter];
                    counter += 1;
                }
            }
        }
        return link;
    },
    /**
     * Creates the HTML markup for an a element from the passed in options.
     * For the actual link (href) the same attributes in options are supported
     * as in {@link $.otp.createLink}. In addition the following attributes in
     * options are supported:
     * <ul>
     * <li>title</li>
     * <li>text</li>
     * <li>target</li>
     * </ul>
     *
     * Title is used for the title attribute of the a-attribute and text is used
     * for the innerHTML element of the a-attribute
     * @param options The options defining the hyperlink
     * @returns {String} Markup for HTML element a
     */
    createLinkMarkup: function (options) {
        "use strict";
        var link, text, title, target;
        link = '<a href="' + $.otp.createLink(options) + '"';
        text = "";
        if (options !== undefined && options) {
            if (options.text !== undefined && options.text) {
                text = options.text;
            }
            if (options.title !== undefined && options.title) {
                title = options.title;
            }
            if (options.target !== undefined && options.target) {
                target = options.target;
            }
        }
        if (title !== undefined) {
            link += ' title="' + title + '"';
        }
        if (target !== undefined) {
            link += ' target="' + target + '"';
        }
        return link + '>' + text + '</a>';
    }
};

$.i18n.properties({
    name: 'messages',
    path: $.otp.createLink({
        controller: 'js',
        action: 'i18n/'
    }),
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
                        rowData[0] = $.otp.createLinkMarkup({
                            controller: showLink,
                            id: rowData[0].id,
                            text: rowData[0].text
                        });
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
            sAjaxSource: $.otp.createLink({
                controller: 'sequence',
                action: 'dataTableSource'
            }),
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
                        var i, j, rowData, row, fastQC;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            if (row.fastQCFiles !== undefined) {
                                fastQC = "";
                                for (j = 0; j < row.fastQCFiles.length; j += 1) {
                                    fastQC += $.otp.createLinkMarkup({
                                        controller: 'fastqcResults',
                                        action: 'show',
                                        id: row.fastQCFiles[j].id,
                                        text: $.i18n.prop("sequence.list.numberedFastQCFile", (j + 1))
                                    });
                                    fastQC += " ";
                                }
                            } else {
                                fastQC = row.fastqcState.name;
                            }
                            rowData = [
                                '<span title="' + row.projectName + '">' + $.otp.sequence.formatProject(row.projectName) + '</span>',
                                $.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    id: row.individualId,
                                    text: row.mockPid
                                }),
                                row.sampleTypeName,
                                row.seqTypeName,
                                row.libraryLayout,
                                row.seqCenterName,
                                $.otp.createLinkMarkup({
                                    controller: 'run',
                                    action: 'show',
                                    id: row.runId,
                                    title: row.name,
                                    text: $.otp.sequence.formatRun(row.name)
                                }),
                                row.laneId,
                                fastQC,
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
                if ($("a", fastqc).length > 0) {
                    fastqc.addClass("true");
                } else {
                    fastqc.attr("title", fastqc.text());
                    fastqc.addClass("false");
                    fastqc.text("");
                }

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
        $("#searchCriteriaTable tr td:eq(1) select").change($.otp.sequence.updateSearchCriteria);
        $("#searchCriteriaTable tr td:eq(1) input[type=text]").change($.otp.sequence.updateSearchCriteria);
        $("#searchCriteriaTable tr td:eq(1) input[type=text]").keydown($.otp.sequence.updateSearchCriteria);
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
        $.otp.sequence.updateSearchCriteria();
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
        $("td:eq(1) select", cloned).change($.otp.sequence.updateSearchCriteria);
        $("td:eq(1) input[type=text]", cloned).change($.otp.sequence.updateSearchCriteria);
        $("td:eq(1) input[type=text]", cloned).keydown($.otp.sequence.updateSearchCriteria);
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
    },
    updateSearchCriteria: function () {
        "use strict";
        $("#sequenceTable").dataTable().fnDraw();
        $("#export-csv").attr("href", $.otp.createLink({
            controller: 'sequence',
            action: 'exportCsv',
            parameters: {
                filtering: JSON.stringify($.otp.sequence.searchCriteria())
            }
        }));
    }
};

/*jslint unparam: true */
$.otp.option = {
    formatValue: function (value) {
        "use strict";
        var result, MAX_SIZE = 20;
        if (value.length > MAX_SIZE) {
            result = "..." + value.substr(value.length - MAX_SIZE - 1, value.length);
        } else {
            result = value;
        }
        return result;
    },
    register: function () {
        "use strict";
        $("#optionTable").dataTable({
            bFilter: false,
            bProcessing: true,
            bServerSide: true,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'processingOption',
                action: 'datatable'
            }),
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
                        var i, rowData, row, option;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            option = row.option;
                            rowData = [
                                option.name,
                                option.type,
                                '<a href="#" title="' + option.value + '">' + $.otp.option.formatValue(option.value) + '</a>',
                                row.project,
                                option.dateCreated,
                                option.dateObsoleted,
                                option.comment
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            }
        });
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
        sampleBoxes.find("button").click($.otp.addIndividual.addSampleIdentifier);
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
            $(value).next().find("input").each(function (idx, v) {
                sample.id.push($(v).val());
            });
            samplesArray.push(sample);
        });
        samples = JSON.stringify(samplesArray);
        $.getJSON($.otp.createLink({
            controller: 'individual',
            action: 'save'
        }), {
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
 * Shared definition for a datatables view.
 * @param selector The JQuery selector for the table to create the datatable into
 * @param sourcePath The path to the ajax resource
 * @param sortOrder {@code true} for ascending, {@code false} for descending initial sorting of first collumn
 * @param jsonCallback (optional) the callback to invoke when json data has been returned. Get's one argument json
 * @param columnDefs (optional) Array of column definitions, can be used to enable/disable sorting of columns
 * @param postData (optional) Array of additional data to be added to the POST requests
 */
$.otp.createListView = function (selector, sourcePath, sortOrder, jsonCallback, columnDefs, postData) {
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
                for (i = 0; i < postData.length; i += 1) {
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
            sAjaxSource: $.otp.createLink({
                controller: 'notification',
                action: 'dataTableSource'
            }),
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
                    $.getJSON($.otp.createLink({
                        controller: 'notification',
                        action: 'updateType',
                        id: selection.attr("name")
                    }),
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
                    $.getJSON($.otp.createLink({
                        controller: 'notification',
                        action: 'updateMedium',
                        id: selection.attr("name")
                    }),
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
                        $.getJSON($.otp.createLink({
                            controller: 'notification',
                            action: 'jobDefinition',
                            id: $("input:hidden[name=triggerId]", cell).val()
                        }), function (data) {
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
                            $.getJSON($.otp.createLink({
                                controller: 'notification',
                                action: 'jobDefinitions',
                                id: $("select[name=jobExecutionPlan] option:selected", dialog).val()
                            }), function (data) {
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
                            $.getJSON($.otp.createLink({
                                controller: 'notification',
                                action: 'jobDefinitions',
                                id: $("select[name=jobExecutionPlan] option:selected", dialog).val()
                            }), function (data) {
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
                                $.getJSON($.otp.createLink({
                                    controller: 'notification',
                                    action: 'updateTrigger',
                                    id: $("input:hidden", dialog).val()
                                }),
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
                        $.getJSON($.otp.createLink({
                            controller: 'notification',
                            action: 'updateTemplate',
                            id: $("input:hidden", dialog).val()
                        }),
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
        $.getJSON($.otp.createLink({
            controller: 'notification',
            action: 'enableNotification',
            id: checkBox.attr("name")
        }),
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
