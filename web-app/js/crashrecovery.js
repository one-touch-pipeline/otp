/*global $*/

$.otp.crashRecovery = {};
$.otp.crashRecovery.processingStepId = function () {
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
    $.otp.createListView("#crashRecoveryTable", $.otp.contextPath + "/crashRecovery/datatable/", true, function (json) {
        var i, rowData;
        for (i = 0; i < json.aaData.length; i++) {
            rowData = json.aaData[i];
            rowData[0] = '<input type="radio" name="processingStep" value="' + rowData[0] + '"/>';
            rowData[1] = '<a href="' + $.otp.contextPath +  '/processes/plan/' + rowData[1].id + '">' + rowData[1].name + '</a>';
            rowData[2] = '<a href="' + $.otp.contextPath + '/processes/process/' + rowData[2] + '">' + rowData[2] + '</a>';
            rowData[3] = '<a href="' + $.otp.contextPath +  '/processes/processingStep/' + rowData[3].id + '">' + rowData[3].name + '</a>';
            rowData[4] = rowData[4].class + '<br/>' + rowData[4].version;
        }
    });
};

$.otp.crashRecovery.finishedButton = function () {
    var id = $.otp.crashRecovery.processingStepId();
    if (!id) {
        return;
    }
    console.log(id);
    // TODO: query for Process Parameters to set.
    alert("Marking a Job as finished is not yet implemented");
};

$.otp.crashRecovery.succeededButton = function () {
    var id = $.otp.crashRecovery.processingStepId();
    if (!id) {
        return;
    }
    console.log(id);
    // TODO: query for Process Parameters to set.
    alert("Marking a Job as succeeded is not yet implemented");
};

$.otp.crashRecovery.failedButton = function () {
    var id = $.otp.crashRecovery.processingStepId();
    if (!id) {
        return;
    }
    $("#dialog-error-message-job").dialog({
        modal: true,
        buttons: {
            "Mark Job as Failed": function () {
                var message = $("input", $(this)).val();
                console.log(message);
                if (!message || message == "") {
                    message = null;
                    return;
                }
                $(this).dialog("close");
                $.getJSON($.otp.contextPath + "/crashRecovery/markFailed/" + id, {message: message}, function (data) {
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

$.otp.crashRecovery.restartButton = function () {
    var id = $.otp.crashRecovery.processingStepId();
    if (!id) {
        return;
    }
    console.log(id);
    // TODO: show dialog
    alert("Restarting a Job is not yet implemented");
};

$.otp.crashRecovery.startSchedulerButton = function () {
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
    $.otp.crashRecovery.createListView();
    $("#markFinished").click($.otp.crashRecovery.finishedButton);
    $("#markSucceeded").click($.otp.crashRecovery.succeededButton);
    $("#markFailed").click($.otp.crashRecovery.failedButton);
    $("#restart").click($.otp.crashRecovery.restartButton);
    $("#startScheduler").click($.otp.crashRecovery.startSchedulerButton);
};
