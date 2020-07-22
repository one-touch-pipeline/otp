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

/*jslint browser: true */
/*global $ */

// Override jQuery-UI functions to allow rendering of table
$.ui.autocomplete.prototype._renderMenu = function (ul, items) {
    var self = this;
    ul.append("<table style='margin-right: 20px'></table>");
    $.each(items, function (index, item) {
        self._renderItemData(ul, ul.find("table"), item);
    });
};
$.ui.autocomplete.prototype._renderItemData = function (ul, table, item) {
    return this._renderItem(table, item).data("ui-autocomplete-item", item);
};
$.ui.autocomplete.prototype._renderItem = function (table, item) {
    return $("<tr class='ui-menu-item'></tr>")
    // the a-tag allows the select callback without destroying the table and serves as a search-column
        .append("<td style='display: none'><a>"+item.value+"</a></td>"+
            "<td>"+(item.realName || '-')+"</td>"+
            "<td>"+(item.username || '-')+"</td>"+
            "<td>"+(item.email || '-')+"</td>"+
            "<td>"+(item.department || '-')+"</td>")
        .appendTo(table);
};

$(function () {
    $(".user-auto-complete").on("input", "input", function (e) {
        "use strict";
        var searchElement = $(e.target);
        $.ajax({
            type: 'GET',
            url: $.otp.createLink({
                controller: 'ldap',
                action: 'getUserSearchSuggestions',
                parameters: {
                    "searchString": searchElement.val()
                }
            }),
            dataType: 'json',
            success: function (data) {
                var suggestionMap = [];
                var focused = "";
                var result = data.content;
                for (var i = 0; i < result.length; i++) {
                    suggestionMap.push({
                        // autocomplete completes with the 'value' column
                        'value'      : [result[i].username, result[i].realName, result[i].mail].join(";"),
                        'username'   : result[i].username,
                        'realName'   : result[i].realName,
                        'email'      : result[i].mail,
                        'department' : result[i].department
                    });
                }
                searchElement.autocomplete({
                    minLength: 3,
                    delay: 0,
                    source: suggestionMap,

                    select: function (event, ui) {
                        event.stopPropagation();
                        if (ui.item !== undefined) {
                            $(this).val(ui.item.username);
                        } else {
                            $(this).val(focused); // needed to prevent missing focus on first selected item
                        }
                        return false;
                    },
                    focus: function (event, ui) {
                        if (ui.item !== undefined) {
                            focused = ui.item.username;
                        }
                    }
                });
                searchElement.data("uiAutocomplete").search(searchElement.val());
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown)
            }
        });
    });
});
