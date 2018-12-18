/*global $: false
 */
$.otp.userAdministration = {};
$.otp.userAdministration.changeUser = function (userId, field, target) {
    "use strict";
    $.ajax({
        url: target + "/?user.id=" + userId,
        dataType: 'json',
        data: { flag: $("#" + field).is(':checked') },
        cache: 'false',
        success: function (data) {
            // redraw the dataTable to reset all changes
            $('#userTable').dataTable().fnDraw();
            if (data.success) {
                $.otp.infoMessage($L("editorSwitch.notification.success"));
            } else {
                $.otp.warningMessage(data.error);
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            $.otp.warningMessage($L("editorSwitch.notification.error", textStatus, errorThrown));
        }
    });
};

$.otp.userAdministration.loadUserList = function () {
    "use strict";
    var createUserChangeMarkup = function (id, target, enabled) {
        var html, checkboxId;
        checkboxId = "user-change-" + id + "-" + target;
        html = '<input type="checkbox" id="' + checkboxId + '" ';
        if (enabled) {
            html += 'checked="checked"';
        }
        html += '/><input type="button" value="update" onclick="$.otp.userAdministration.changeUser(' + id + ', \'' + checkboxId + '\', \'' + target + '\')"/>';
        return html;
    };
    $('#userTable').dataTable({
        // TODO: in future it might be interesting to allow filtering
        bFilter: false,
        bProcessing: true,
        bServerSide: false,
        bSort: true,
        bJQueryUI: false,
        bPaginate: false,
        bScrollCollapse: true,
        sScrollY: ($(window).height() - 440),
        sAjaxSource: $.otp.createLink({
            controller: 'userAdministration',
            action: 'dataTableSource'
        }),
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
                    var rowData, userId, i;
                    for (i = 0; i < json.aaData.length; i += 1) {
                        rowData = json.aaData[i];
                        userId = rowData[0];
                        rowData[0] = $.otp.createLinkMarkup({
                            controller: 'userAdministration',
                            action: 'show',
                            text: userId,
                            parameters: {
                                "user.id": userId
                            }
                        });
                        rowData[3] = createUserChangeMarkup(userId, 'enable', rowData[3]);
                        rowData[4] = createUserChangeMarkup(userId, 'expireAccount', rowData[4]);
                        rowData[5] = createUserChangeMarkup(userId, 'lockAccount', rowData[5]);
                        rowData[6] = '<input type="hidden" value="' + rowData[1] + '"/><button class="changeUserButton">Switch to ' + rowData[1] + '</button>';
                    }
                    fnCallback(json);
                }
            });
        },
        fnRowCallback: function (nRow) {
            $("button.changeUserButton", nRow).click(function (event) {
                var form;
                event.preventDefault();
                form = $("#switch-user-form");
                $("input:hidden", form).val($("input:hidden", $(this).parent()).val());
                form.submit();
            });
            return nRow;
        }
    });
    $.otp.resizeBodyInit('#userTable', 180);
};

$.otp.userAdministration.editUser = {
    submitUserForm: function (event) {
        "use strict";
        event.preventDefault();
        $.ajax({
            type: 'GET',
            url: $.otp.createLink({
                controller: 'userAdministration',
                action: 'editUser'
            }),
            dataType: 'json',
            cache: 'false',
            data: {
                user: $("#edit-user-userid").val(),
                username: $("#edit-user-username").val(),
                email: $("#edit-user-email").val(),
                realName: $("#edit-user-realname").val(),
                asperaAccount: $("#edit-user-asperaaccount").val()
            },
            success: function (data) {
                if (data.error) {
                    $.otp.warningMessage($L(data.error));
                } else if (data.success) {
                    $.otp.infoMessage($L("user.administration.edit.success"));
                }
            }
        });
    },
    /*
     The Role Domain is split in "ROLE_*" and "GROUP_*" Authorities. These are
     kept in different places in the GUI, named after their respective authority.
     To select the right values the parameter "type" is used.
      */
    addOrRemoveUserRole: function (origin, userId, roleId, type) {
        "use strict";
        if (!(['Group', 'Role'].includes(type))) { return; }
        var action = $(origin).attr('class');
        if (!(['add', 'remove'].includes(action))) { return; }

        var labels = {
            "add"    : {opposite: "remove", appendToSection: "user"},
            "remove" : {opposite: "add",    appendToSection: "available"}
        };

        $.ajax({
            type: 'GET',
            url: $.otp.createLink({
                controller: 'userAdministration',
                action: action+'Role',
                parameters: {
                    user: userId,
                    role: roleId
                }
            }),
            dataType: 'json',
            cache: 'false',
            success: function (data) {
                if (data.success) {
                    var tableRow = $(origin).parents("tr");
                    var opposite = labels[action].opposite;
                    $("a", tableRow).text($L("user.administration.role."+opposite+""+type));
                    $("a", tableRow).attr('class', opposite);
                    tableRow.detach();
                    tableRow.appendTo($("table tbody", $("#"+labels[action].appendToSection+""+type)));
                } else {
                    $.otp.warningMessage(data.error);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorSwitch.notification.error", textStatus, errorThrown));
            }
        });
    },
    register: function () {
        "use strict";
        $("#edit-user-form").submit(this.submitUserForm);
        $("#create-new-group").click(function () {
            var object = $.otp.userAdministration.editUser.newGroupDialog;
            object.showDialog.call(object);
        });
    }
};

$.otp.userAdministration.create = {
    register: function () {
        "use strict";
        $("#create-user-form").validate({
            rules: {
                username: {
                    required: true
                },
                email: {
                    required: true,
                    email: true
                }
            },
            highlight: function (element, errorClass, validClass) {
                $(element).addClass(errorClass);
                $(element).parent().next().addClass(errorClass);
                $(element).parent().next().removeClass(validClass);
            },
            unhighlight: function (element, errorClass, validClass) {
                $(element).removeClass(errorClass);
                $(element).parent().next().addClass(validClass);
                $(element).parent().next().removeClass(errorClass);
                $(element).removeAttr("title");
            },
            submitHandler: function (form) {
                $(form).ajaxSubmit({
                    success: function (data) {
                        var validator, i;
                        if (data.success === true) {
                            $.otp.infoMessage($L("user.administration.register.success", data.user.id));
                        } else if (data.errors) {
                            for (i = 0; i < data.errors.errors.length; i += 1) {
                                validator = $("#create-user-form").validate().settings;
                                validator.highlight($("#create-user-form input[name=" + data.errors.errors[i].field + "]"), validator.errorClass, validator.validClass);
                                $.otp.warningMessage(data.errors.errors[i].message);
                            }
                        }
                    }
                });
            },
            errorPlacement: function (error, element) {
                $(element).parent().next().attr("title", error.text());
                $(element).attr("title", error.text());
            }
        });
        $("#create-user-form").valid();
    }
};