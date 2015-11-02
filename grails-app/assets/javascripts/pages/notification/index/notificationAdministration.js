/*jslint browser: true */
/*global $ */

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
            bPaginate: false,
            bScrollCollapse: true,
            sScrollY: ($(window).height() - 410),
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
        $.otp.resizeBodyInit('#notificationsTable', 190);
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
        $('<a href="#">' + $L("notification.administration.template.collapse") + '</a>').appendTo(span).click($.otp.notificationAdministration.collapseTemplate);
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
            $.otp.infoMessage($L("notification.administration.update.success"));
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
            $.otp.warningMessage($L("notification.administration.update.notFound"));
        } else {
            $.otp.warningMessage(jqXHR.statusText + jqXHR.status);
        }
    }
};
