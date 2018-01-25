/*jslint browser: true */
/*global $ */

$.otp.projectMember = {

    toggle: function (controlElement, linkElement) {
        var control = document.getElementById(controlElement);
        var link = document.getElementById(linkElement);

        if (control.style.display == "none") {
            control.style.display = "";
            link.innerHTML = "Hide list";
        } else {
            control.style.display = "none";
            link.innerHTML = "Show list";
        }
    },

    deleteUser: function () {
        $('.deletePerson').on('click', function (event) {
            "use strict";
            $.ajax({
                type: 'GET',
                url: $.otp.createLink({
                    controller: 'projectMember',
                    action: 'deleteContactPersonOrRemoveProject'
                }),
                dataType: 'json',
                cache: 'false',
                data: {
                    "projectContactPerson.id": $(event.target).data("id")
                },
                success: function (data) {
                    if (data.success) {
                        $.otp.infoMessage($L("editorswitch.notification.success"));
                        $(event.target).parents("tr").remove();
                    } else {
                        $.otp.warningMessage(data.error);
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
                }
            });
        })
    }
};
