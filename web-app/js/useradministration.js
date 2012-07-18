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
