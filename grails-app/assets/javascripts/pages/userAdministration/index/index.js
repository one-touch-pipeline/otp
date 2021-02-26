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
            html += 'checked';
        }
        html += '/><input type="button" value="Update" onclick="$.otp.userAdministration.changeUser(' + id + ', \'' + checkboxId + '\', \'' + target + '\')"/>';
        return html;
    };
    $('#userTable').dataTable({
        aoColumns: [
            {
                'mData': function (source, type, data) {
                    return $.otp.createLinkMarkup({
                        controller: 'userAdministration',
                        action: 'show',
                        text: source.username || "[no username]",
                        title: source.id,
                        parameters: {
                            "user.id": source.id
                        }
                    });
                }
            },
            {'mData': 'realname'},
            {'mData': 'email'},
            {'mData': 'deactivationDate'},
            {
                'mData': function (source, type, data) {
                    if (type === "sort") {
                        return source.enabled
                    }
                    return createUserChangeMarkup(source.id, 'enable', source.enabled);
                }
            },
            {
                'mData': function (source, type, data) {
                    if (type === "sort") {
                        return source.acceptedPrivacyPolicy;
                    }
                    var checked = "";
                    if (source.acceptedPrivacyPolicy) {
                        checked = "checked";
                    }
                    return "<input type='checkbox' disabled " +  checked + "/>";
                }
            },
            {
                'mData': function (source, type, data) {
                    var result = "";
                    if (source.username) {
                        result = '<input type="hidden" value="' + source.username + '"/><button class="changeUserButton">Switch to ' + source.username + '</button>';
                    }
                    return result
                }
            }
        ],
        bFilter: false,
        bProcessing: true,
        bServerSide: false,
        bSort: true,
        bJQueryUI: false,
        bPaginate: false,
        bScrollCollapse: true,
        sScrollY: $(window).height(),
        aaSorting: [[1, 'asc']],
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
                    fnCallback({ aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0 });
                },
                "success": function (json) {
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
        },
    });
    $.otp.resizeBodyInit('#userTable', 180);
};
