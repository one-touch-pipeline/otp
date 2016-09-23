/*global $: false
 */
$.otp.userAdministration = {};
$.otp.userAdministration.changeUser = function (userId, field, target) {
    "use strict";
    $.ajax({
        url: target + "/" + userId,
        dataType: 'json',
        data: {value: $("#" + field).attr("checked") === "checked" ? true : false},
        cache: 'false',
        success: function () {
            // redraw the dataTable to reset all changes
            $('#userTable').dataTable().fnDraw();
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
                    var rowData, id, i;
                    for (i = 0; i < json.aaData.length; i += 1) {
                        rowData = json.aaData[i];
                        id = rowData[0];
                        rowData[0] = $.otp.createLinkMarkup({
                            controller: 'userAdministration',
                            action: 'show',
                            id: id,
                            text: id
                        });
                        rowData[4] = createUserChangeMarkup(id, 'enable', rowData[4]);
                        rowData[5] = createUserChangeMarkup(id, 'expireAccount', rowData[5]);
                        rowData[6] = createUserChangeMarkup(id, 'lockAccount', rowData[6]);
                        rowData[7] = createUserChangeMarkup(id, 'expirePassword', rowData[7]);
                        rowData[8] = '<input type="hidden" value="' + rowData[1] + '"/><button>Switch to ' + rowData[1] + '</button>';
                    }
                    fnCallback(json);
                }
            });
        },
        fnRowCallback: function (nRow) {
            $("td:eq(8) button", nRow).click(function (event) {
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
    addRemoveRoles: function () {
        "use strict";
        var link, id, container, userId, action;
        link = $(this);
        id = $("input:hidden", link.parent()).val();
        container = link.parents("div")[0];
        userId = $($("input", container)[0]).val();
        action = $($("input", container)[1]).val();
        $.ajax({
            type: 'GET',
            url: $.otp.createLink({
                controller: 'userAdministration',
                action: action,
                id: id,
                parameters: {
                    userId: userId
                }
            }),
            dataType: 'json',
            cache: 'false',
            success: function (data) {
                if (data.error) {
                    $.otp.errorMessage(data.error);
                } else if (data.success) {
                    var linkText, divInsertId, tableRow;
                    linkText = "";
                    divInsertId = "";
                    if (action === "addRole") {
                        linkText = $L("user.administration.userRole.ui.removeRole");
                        divInsertId = "#userRoles";
                    } else if (action === "removeRole") {
                        linkText = $L("user.administration.userRole.ui.addRole");
                        divInsertId = "#availableRoles";
                    }
                    tableRow = link.parents("tr");
                    $("a", tableRow).text(linkText);
                    tableRow.detach();
                    tableRow.appendTo($("table tbody", $(divInsertId)));
                }
            }
        });
    },
    addRemoveGroups: function () {
        "use strict";
        var link, id, container, userId, action;
        link = $(this);
        id = link.prev().val();
        container = link.parents("div")[0];
        userId = $($("input", container)[0]).val();
        action = $($("input", container)[1]).val();
        $.ajax({
            type: 'GET',
            url: $.otp.createLink({
                controller: 'userAdministration',
                action: action,
                id: id,
                parameters: {
                    userId: userId
                }
            }),
            dataType: 'json',
            cache: 'false',
            success: function (data) {
                if (data.error) {
                    $.otp.errorMessage(data.error);
                } else if (data.success) {
                    var linkText, divInsertId, tableRow;
                    linkText = "";
                    divInsertId = "";
                    if (action === "addGroup") {
                        linkText = $L("user.administration.userGroup.ui.removeGroup");
                        divInsertId = "#userGroups";
                    } else if (action === "removeGroup") {
                        linkText = $L("user.administration.userGroup.ui.addGroup");
                        divInsertId = "#availableGroups";
                    }
                    tableRow = link.parents("tr");
                    $("a", tableRow).text(linkText);
                    tableRow.detach();
                    tableRow.appendTo($("table tbody", $(divInsertId)));
                }
            }
        });
    },
    /**
     * Module for handling the new Group dialog.
     */
    newGroupDialog: {
        /**
         * jQuery object pointing to the div containing the markup for the new Group dialog.
         */
        getDiv: function() {
            return $("#create-group-dialog");
        },
        /**
         * Removes the error class from the direct parent of all input elements
         * of type checkbox and text.
         * In addition it removes the set title on these elements.
         */
        removeErrors: function () {
            "use strict";
            var textFields;
            textFields = $("input[type=text], input[type=checkbox]", this.getDiv());
            textFields.parent().removeClass("error");
            textFields.parent().removeClass("error-checkbox");
            textFields.parent().removeAttr("title");
            textFields.removeAttr("title");
        },
        /**
         * Resets the value of all input fields of the dialog.
         * That is clears the value of all text input and
         * unchecks the checkboxes.
         */
        resetFields: function () {
            "use strict";
            $("input[type=text]", this.getDiv()).val("");
            $("input[type=checkbox]", this.getDiv()).attr("checked", false);
            this.removeErrors();
        },
        /**
         * Callback to process the result JSON returned by the server upon
         * creating a new Group.
         * In case of success the new Group is added to the list of available Groups,
         * the dialog fields are reset and the dialog is closed.
         *
         * In case of error the table cells containing errors are highlighted.
         * In addition the title attribute is set on the error field.
         * Before setting the new errors, the previous ones are removed.
         *
         * The passed in param data structure is expected to be valid JSON in the following
         * format:
         * @code{
         * {
         *     success: boolean,
         *     group: {
         *         id: Number,
         *         name: string,
         *         description: string,
         *     },
         *     errors: [{
         *         field: string,
         *         message: string
         *     }]
         * }
         * }
         */
        processResults: function (data) {
            "use strict";
            if (data.success === true) {
                // create the new available group
                var row = $("<tr></tr>").appendTo($("#availableGroups table tbody"));
                $("<td title=\"" + data.group.description + "\">" +  data.group.name + "</td>").appendTo(row);
                $("<td><input type=\"hidden\" value=\"" + data.group.id + "\"/><a href=\"#\" rel=\"userGroups-" + data.group.id + "\">" + $L("user.administration.userGroup.ui.addGroup") + "</a></td>")
                    .appendTo(row)
                    .click($.otp.userAdministration.editUser.addRemoveGroups);
                this.getDiv().dialog("close");
            } else if (data.errors) {
                this.removeErrors();
                if (Array.isArray(data.errors)) {
                    data.errors.forEach(function (error) {
                        var errorField = $("input[name=" + error.field + "]");
                        if (error.field == 'name') {
                            errorField.parent().addClass("error");
                        } else {
                            errorField.parent().addClass("error-checkbox");
                            errorField.parent().attr("title", error.message);
                        }
                        errorField.attr("title", error.message);
                    });
                }
            }
        },
        showDialog: function () {
            "use strict";
            this.resetFields();
            this.getDiv().dialog({
                buttons: [
                    {
                        text: $L("default.button.cancel.label"),
                        click: function () {
                            $(this).dialog("close");
                        }
                    }, {
                        text: $L("default.button.create.label"),
                        click: function () {
                            $.getJSON($.otp.createLink({
                                controller: 'userAdministration',
                                action: 'createGroup'
                            }), $("form", $(this)).serialize(), function (data) {
                                var object = $.otp.userAdministration.editUser.newGroupDialog;
                                object.processResults.call(object, data);
                            });
                        }
                    }]
            });
        }
    },
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
                username: $("#edit-user-username").val(),
                jabber: $("#edit-user-jabber").val(),
                email: $("#edit-user-email").val()
            },
            success: function (data) {
                if (data.error) {
                    if (data.username) {
                        $.otp.warningMessage($L(data.username));
                    }
                    if (data.jabber) {
                        $.otp.warningMessage($L(data.jabber));
                    }
                    if (data.email) {
                        $.otp.warningMessage($L(data.email));
                    }
                } else if (data.success) {
                    $.otp.infoMessage($L("user.administration.edit.success"));
                }
            }
        });
    },
    register: function () {
        "use strict";
        $("#user-role-management table tr a").click(this.addRemoveRoles);
        $("#user-group-management table tr a").click(this.addRemoveGroups);
        $("#edit-user-form").submit(this.submitUserForm);
        $("#create-new-group").click(function () {
            var object = $.otp.userAdministration.editUser.newGroupDialog;
            object.showDialog.call(object);
        });
    }
};

$.otp.userAdministration.register = function () {
    "use strict";
    $("#registerForm").submit(function (event) {
        event.preventDefault();
        $.ajax({
            type: 'GET',
            url: "performRegistration",
            dataType: 'json',
            cache: 'false',
            data: {
                username: $("#register-form-username").val(),
                userRealName: $("#register-form-name").val(),
                email: $("#register-form-email").val()
            },
            success: function (data) {
                if (data.error) {
                    $.otp.warningMessage(data.username);
                    $.otp.warningMessage(data.userRealName);
                    $.otp.warningMessage(data.email);
                } else if (data.success) {
                    $.otp.infoMessage($L("user.administration.register.success", data.user));
                }
            }
        });
    });
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
                },
                jabber: {
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