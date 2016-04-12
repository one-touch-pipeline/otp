/*global $*/

$.otp.crashRecovery = {};
$.otp.crashRecovery.processingStepIds = function () {
    "use strict";
    var ids = [];
    $("#crashRecoveryTable input[name=processingStep]:checked").each (function(index, element) {
        ids.push($(element).val());
    });
    if (!ids) {
        $("#dialog-select-job").dialog({
            modal: true,
            buttons: {
                Ok: function () {
                    $(this).dialog("close");
                }
            }
        });
        return null;
    }
    return ids;
};

$.otp.crashRecovery.createListView = function () {
    "use strict";
    $.otp.createListView("#crashRecoveryTable", $.otp.createLink({
        controller: 'crashRecovery',
        action: 'datatable'
    }), true, function (json) {
        var i, rowData;
        for (i = 0; i < json.aaData.length; i += 1) {
            rowData = json.aaData[i];
            rowData[0] = '<input type="checkbox" name="processingStep" value="' + rowData[0] + '" ' + (rowData[5] ? "checked" : "") +' />';
            rowData[1] = $.otp.createLinkMarkup({
                controller: 'processes',
                action: 'plan',
                id: rowData[1].id,
                text: rowData[1].name
            });
            rowData[2] = $.otp.createLinkMarkup({
                controller: 'processes',
                action: 'process',
                id: rowData[2],
                text: rowData[2]
            });
            rowData[3] = $.otp.createLinkMarkup({
                controller: 'processes',
                action: 'processingStep',
                id: rowData[3].id,
                text: rowData[3].name
            });
            rowData[4] = rowData[4]['class'] + '<br/>' + rowData[4].version;
        }
    },
        undefined,
        undefined,
        140,
        { bSort: false });
};

$.otp.crashRecovery.showParametersDialog = function (ids, target) {
    "use strict";
    $.get($.otp.createLink({
        controller: 'crashRecovery',
        action: 'parametersOfJob',
        parameters: {ids: ids}
    }), function (data) {
        $(data).dialog({
            width: '500px',
            buttons: {
                Ok: function () {
                    var parameters = [];
                    $("ul li input", $(this)).each(function () {
                        parameters[parameters.length] = {
                            key: $(this).attr("name"),
                            value: $(this).val()
                        };
                    });
                    $.getJSON($.otp.createLink({
                        controller: target.controller,
                        action: target.action,
                        parameters: {ids: ids}
                    }), 'parameters=' + JSON.stringify(parameters), function (data) {
                        $("#crashRecoveryTable").dataTable().fnDraw();
                        $.otp.infoMessage(data.success);
                    });
                    $(this).dialog("close");
                },
                Cancel: function () {
                    $(this).dialog("close");
                }
            }
        });
    });
};

$.otp.crashRecovery.showFailedJobDialog = function (ids, target) {
    "use strict";
    $("#dialog-error-message-job").dialog({
        modal: true,
        buttons: {
            "Mark Job as Failed": function () {
                var message = $("input", $(this)).val();
                if (!message || message === "") {
                    message = null;
                    return;
                }
                $(this).dialog("close");
                $.getJSON($.otp.createLink({
                    controller: target.controller,
                    action: target.action,
                    parameters: {ids: ids}
                }), {message: message}, function (data) {
                    $("#crashRecoveryTable").dataTable().fnDraw();
                    $.otp.infoMessage(data.success);
                });
            },
            Cancel: function () {
                $(this).dialog("close");
            }
        }
    });
};

$.otp.crashRecovery.finishedButton = function () {
    "use strict";
    var ids = $.otp.crashRecovery.processingStepIds();
    if (!ids) {
        return;
    }
    $.otp.crashRecovery.showParametersDialog(ids, {
        controller: 'crashRecovery',
        action: 'markFinished'
    });
};

$.otp.crashRecovery.succeededButton = function () {
    "use strict";
    var ids = $.otp.crashRecovery.processingStepIds();
    if (!ids) {
        return;
    }
    $.otp.crashRecovery.showParametersDialog(ids, {
        controller: 'crashRecovery',
        action: 'markSucceeded'
    });
};

$.otp.crashRecovery.failedButton = function () {
    "use strict";
    var ids = $.otp.crashRecovery.processingStepIds();
    if (!ids) {
        return;
    }
    $.otp.crashRecovery.showFailedJobDialog(ids, {
        controller: 'crashRecovery',
        action: 'markFailed'
    });
};

$.otp.crashRecovery.restartButton = function () {
    "use strict";
    var ids = $.otp.crashRecovery.processingStepIds();
    if (!ids) {
        return;
    }
    $.otp.crashRecovery.showFailedJobDialog(ids, {
        controller: 'crashRecovery',
        action: 'restart'
    });
};

$.otp.crashRecovery.startSchedulerButton = function () {
    "use strict";
    $.getJSON($.otp.createLink({
        controller: 'crashRecovery',
        action: 'startScheduler'
    }), function (data) {
        if (data.success) {
            $.otp.infoMessage("Scheduler successfully restarted");
        } else {
            $("#crashRecoveryTable").dataTable().fnDraw();
            $.otp.warningMessage("Scheduler could not be restarted, check that all Jobs have been triaged");
        }
    });
};

$.otp.crashRecovery.setupView = function () {
    "use strict";
    $.otp.crashRecovery.createListView();
    $("#markFinished").click($.otp.crashRecovery.finishedButton);
    $("#markSucceeded").click($.otp.crashRecovery.succeededButton);
    $("#markFailed").click($.otp.crashRecovery.failedButton);
    $("#restart").click($.otp.crashRecovery.restartButton);
    $("#startScheduler").click($.otp.crashRecovery.startSchedulerButton);
};
