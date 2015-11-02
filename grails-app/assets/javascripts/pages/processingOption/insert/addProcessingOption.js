/*jslint browser: true */
/*global $ */

$.otp.addProcessingOption = {
    submitProcessingOption: function (event) {
        "use strict";
        event.preventDefault();
        $.getJSON($.otp.createLink({
            controller: 'processingOption',
            action: 'save'
        }), {
            name: $("#name").val(),
            type: $("#type").val(),
            value: $("#value").val(),
            project: $("#project").val(),
            comment: $("#comment").val()
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
                $.otp.infoMessage($L("processingOption.insert.add.success", data.id));
            }
        }).error(function (jqXHR) {
            $.otp.warningMessage(jqXHR.statusText + jqXHR.status);
        });
    },
    register: function () {
        "use strict";
        $("#add-processingOption-form").submit(this.submitProcessingOption);
    }
};
