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
        bServerSide: true,
        bJQueryUI: false,
        bScrollInfinite: true,
        bScrollCollapse: true,
        sScrollY: "600px",
        sPaginationType: "full_numbers",
        sAjaxSource: $.otp.contextPath + '/userAdministration/dataTableSource',
        "fnServerData": function (sSource, aoData, fnCallback) {
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
                        rowData[0] = "<a href=\"" + $.otp.contextPath + "/userAdministration/show/" + id + "\">" + id + "</a>";
                        rowData[4] = createUserChangeMarkup(id, 'enable', rowData[4]);
                        rowData[5] = createUserChangeMarkup(id, 'expireAccount', rowData[5]);
                        rowData[6] = createUserChangeMarkup(id, 'lockAccount', rowData[6]);
                        rowData[7] = createUserChangeMarkup(id, 'expirePassword', rowData[7]);
                    }
                    fnCallback(json);
                }
            });
        }
    });
};

$.otp.userAdministration.editUser = function () {
    "use strict";
    $("#user-role-management table tr a").click(function () {
        var link, id, container, userId, action;
        link = $(this);
        id = link.prev().val();
        container = link.parents("div")[0];
        userId = $($("input", container)[0]).val();
        action = $($("input", container)[1]).val();
        $.ajax({
            type: 'GET',
            url: $.otp.contextPath + "/userAdministration/" + action + "/" + id + "?userId=" + userId,
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
                        linkText = $.i18n.prop("user.administration.userRole.ui.removeRole");
                        divInsertId = "#userRoles";
                    } else if (action === "removeRole") {
                        linkText = $.i18n.prop("user.administration.userRole.ui.addRole");
                        divInsertId = "#availableRoles";
                    }
                    tableRow = link.parents("tr");
                    $("a", tableRow).text(linkText);
                    tableRow.detach();
                    tableRow.appendTo($("table tbody", $(divInsertId)));
                }
            }
        });
    });
    $("#user-group-management table tr a").click(function () {
        var link, id, container, userId, action;
        link = $(this);
        id = link.prev().val();
        container = link.parents("div")[0];
        userId = $($("input", container)[0]).val();
        action = $($("input", container)[1]).val();
        $.ajax({
            type: 'GET',
            url: $.otp.contextPath + "/userAdministration/" + action + "/" + id + "?userId=" + userId,
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
                        linkText = $.i18n.prop("user.administration.userGroup.ui.removeGroup");
                        divInsertId = "#userGroups";
                    } else if (action === "removeGroup") {
                        linkText = $.i18n.prop("user.administration.userGroup.ui.addGroup");
                        divInsertId = "#availableGroups";
                    }
                    tableRow = link.parents("tr");
                    $("a", tableRow).text(linkText);
                    tableRow.detach();
                    tableRow.appendTo($("table tbody", $(divInsertId)));
                }
            }
        });
    });
    $("#edit-user-form").submit(function (event) {
        event.preventDefault();
        $.ajax({
            type: 'GET',
            url: $.otp.contextPath + "/userAdministration/editUser",
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
                        $.otp.warningMessage($.i18n.prop(data.username));
                    }
                    if (data.jabber) {
                        $.otp.warningMessage($.i18n.prop(data.jabber));
                    }
                    if (data.email) {
                        $.otp.warningMessage($.i18n.prop(data.email));
                    }
                } else if (data.success) {
                    $.otp.infoMessage($.i18n.prop("user.administration.edit.success"));
                }
            }
        });
    });
    $("#create-new-group").click(function () {
        var removeErrors = function (parent) {
            var textFields, checkBoxes;
            textFields = $("input[type=text]", parent);
            textFields.parent().removeClass("error");
            textFields.removeAttr("title");

            checkBoxes = $("input[type=checkbox]", parent);
            checkBoxes.parent().removeClass("error");
            checkBoxes.removeAttr("title");
        };
        var resetFields = function (parent) {
            $("input[type=text]", parent).val("");
            $("input[type=checkbox]", parent).attr("checked", false);
            removeErrors(parent);
        };
        resetFields($("#create-group-dialog"));
        $("#create-group-dialog").dialog({
            buttons: [
                {
                    text: $.i18n.prop("default.button.cancel.label"),
                    click: function () {
                        $(this).dialog("close");
                    }
                }, {
                    text: $.i18n.prop("default.button.create.label"),
                    click: function () {
                        $.getJSON($.otp.contextPath + "/userAdministration/createGroup", $("form", $(this)).serialize(), function (data) {
                            removeErrors($("#create-group-dialog"));
                            if (data.success === true) {
                                // create the new available group
                                var row = $("<tr></tr>").appendTo($("#availableGroups table tbody"));
                                $("<td title=\"" + data.group.description + "\">" +  data.group.name + "</td>").appendTo(row);
                                $("<td><input type=\"hidden\" value=\"" + data.group.id + "\"/><a href=\"#\" rel=\"userGroups-" + data.group.id + "\">" + $.i18n.prop("user.administration.userGroup.ui.addGroup") + "</a></td>").appendTo(row);
                                resetFields($("#create-group-dialog"));
                                $("#create-group-dialog").dialog("close");
                            } else if (data.errors) {
                                if (Array.isArray(data.errors)) {
                                    data.errors.forEach(function (error) {
                                        var errorField = $("#create-group-dialog input[name=" + error.field + "]");
                                        errorField.attr("title", error.message);
                                        $("span.error-marker", errorField.parent()).attr("title", error.message);
                                        errorField.parent().addClass("error");
                                    });
                                }
                            }
                        });
                    }
                }]
        });
    });
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
                    $.otp.infoMessage($.i18n.prop("user.administration.register.success", data.user));
                }
            }
        });
    });
};
