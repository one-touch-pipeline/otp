/*global $*/

$.otp.crashRecovery = {};
$.otp.crashRecovery.processingStepId = function () {
    "use strict";
    var id = $("#crashRecoveryTable input[name=processingStep]:checked").val();
    if (!id) {
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
    return id;
};

$.otp.crashRecovery.createListView = function () {
    "use strict";
    $.otp.createListView("#crashRecoveryTable", $.otp.contextPath + "/crashRecovery/datatable/", true, function (json) {
        var i, rowData;
        for (i = 0; i < json.aaData.length; i += 1) {
            rowData = json.aaData[i];
            rowData[0] = '<input type="radio" name="processingStep" value="' + rowData[0] + '"/>';
            rowData[1] = '<a href="' + $.otp.contextPath +  '/processes/plan/' + rowData[1].id + '">' + rowData[1].name + '</a>';
            rowData[2] = '<a href="' + $.otp.contextPath + '/processes/process/' + rowData[2] + '">' + rowData[2] + '</a>';
            rowData[3] = '<a href="' + $.otp.contextPath +  '/processes/processingStep/' + rowData[3].id + '">' + rowData[3].name + '</a>';
            rowData[4] = rowData[4].class + '<br/>' + rowData[4].version;
        }
    });
};

$.otp.crashRecovery.showParametersDialog = function (id, target) {
    "use strict";
    $.get($.otp.contextPath + "/crashRecovery/parametersOfJob/" + id, function (data) {
        $(data).dialog({
            buttons: {
                Ok: function () {
                    var parameters = [];
                    $("ul li input", $(this)).each(function () {
                        parameters[parameters.length] = {
                            key: $(this).attr("name"),
                            value: $(this).val()
                        };
                    });
                    $.getJSON($.otp.contextPath + target + id, 'parameters=' + JSON.stringify(parameters), function (data) {
                        $("#crashRecoveryTable").dataTable().fnDraw();
                        alert(data.success);
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

$.otp.crashRecovery.showFailedJobDialog = function (id, target) {
    $("#dialog-error-message-job").dialog({
        modal: true,
        buttons: {
            "Mark Job as Failed": function () {
                var message = $("input", $(this)).val();
                if (!message || message == "") {
                    message = null;
                    return;
                }
                $(this).dialog("close");
                $.getJSON($.otp.contextPath + target + id, {message: message}, function (data) {
                    $("#crashRecoveryTable").dataTable().fnDraw();
                    alert(data.success);
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
    var id = $.otp.crashRecovery.processingStepId();
    if (!id) {
        return;
    }
    $.otp.crashRecovery.showParametersDialog(id, "/crashRecovery/markFinished/");
};

$.otp.crashRecovery.succeededButton = function () {
    "use strict";
    var id = $.otp.crashRecovery.processingStepId();
    if (!id) {
        return;
    }
    $.otp.crashRecovery.showParametersDialog(id, "/crashRecovery/markSucceeded/");
};

$.otp.crashRecovery.failedButton = function () {
    "use strict";
    var id = $.otp.crashRecovery.processingStepId();
    if (!id) {
        return;
    }
    $.otp.crashRecovery.showFailedJobDialog(id, "/crashRecovery/markFailed/");
};

$.otp.crashRecovery.restartButton = function () {
    "use strict";
    var id = $.otp.crashRecovery.processingStepId();
    if (!id) {
        return;
    }
    $.otp.crashRecovery.showFailedJobDialog(id, "/crashRecovery/restart/");
};

$.otp.crashRecovery.startSchedulerButton = function () {
    "use strict";
    $.getJSON($.otp.contextPath + "/crashRecovery/startScheduler/", function (data) {
        if (data.success) {
            alert("Scheduler successfully restarted");
        } else {
            $("#crashRecoveryTable").dataTable().fnDraw();
            alert("Scheduler could not be restarted, check that all Jobs have been triaged");
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
