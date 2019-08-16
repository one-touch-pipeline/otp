/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
                $.otp.infoMessage("Data stored successfully");
            } else {
                $.otp.warningMessage(data.error);
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
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
                realName: $("#edit-user-realname").val()
            },
            success: function (data) {
                if (data.error) {
                    $.otp.warningMessage(data.error);
                } else if (data.success) {
                    $.otp.infoMessage("User updated successfully.");
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

        var messages = {
            "user.administration.role.addRole": "Add Role to User",
            "user.administration.role.removeRole": "Remove Role from User",
            "user.administration.role.addGroup": "Add User to this Group",
            "user.administration.role.removeGroup": "Remove User from this Group"
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
                    $("a", tableRow).text(messages["user.administration.role."+opposite+""+type]);
                    $("a", tableRow).attr('class', opposite);
                    tableRow.detach();
                    tableRow.appendTo($("table tbody", $("#"+labels[action].appendToSection+""+type)));
                } else {
                    $.otp.warningMessage(data.error);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
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
                            $.otp.infoMessage("New User with Id " + data.user.id + " created.");
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
