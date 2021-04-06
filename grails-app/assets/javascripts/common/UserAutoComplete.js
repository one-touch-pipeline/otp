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
    let autocompleteContent = "<td class='ui-state-disabled'>" + item.placeholder + "</td>";

    if (item.user) {
        autocompleteContent = "<td style='display: none'><a>"+item.value+"</a></td>"+   // the a-tag allows the select callback without destroying the table and serves as a search-column
            "<td>"+(item.user.realName || '-')+"</td>"+
            "<td>"+(item.user.username || '-')+"</td>"+
            "<td>"+(item.user.email || '-')+"</td>"+
            "<td>"+(item.user.department || '-')+"</td>"
    }

    return $("<tr class='ui-menu-item'></tr>")
        .append(autocompleteContent)
        .appendTo(table);
};

const renderPlaceholder = function (searchElement, placeholderContent) {
    searchElement.autocomplete({
        minLength: 1,
        delay: 0,
        source: [{
            value: searchElement.val(),
            placeholder: placeholderContent
        }],
    });
    searchElement.data("uiAutocomplete").search(searchElement.val());
}

$(function () {
    var failure = function(title, message) {
        if ($.otp.toaster) {
            $.otp.toaster.showErrorToast(title, message);
        } else {
            $.otp.warningMessage(title + message);
        }
    }

    $(".user-auto-complete").on("input", "input", function (e) {
        "use strict";
        const searchElement = $(e.target);
        const searchLength = searchElement.val().length;

        if (searchLength < 3) {
            renderPlaceholder(searchElement, "Please enter " + (3 - searchLength) + " more letter" + (3 - searchLength > 1 ? "s" : "") + "...");
        } else {
            renderPlaceholder(searchElement, "Loading...");

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
                success: function (result) {
                    var suggestionMap = [];
                    var focused = "";

                    for (var i = 0; i < result.length; i++) {
                        suggestionMap.push({
                            // autocomplete completes with the 'value' column
                            value: [result[i].username, result[i].realName, result[i].mail].join(";"),
                            user: {
                                username: result[i].username,
                                realName: result[i].realName,
                                email: result[i].mail,
                                department: result[i].department
                            }
                        });
                    }
                    searchElement.autocomplete({
                        minLength: 3,
                        delay: 0,
                        source: suggestionMap,

                        select: function (event, ui) {
                            event.stopPropagation();
                            if (ui.item && ui.item.user) {
                                $(this).val(ui.item.user.username);
                            } else {
                                $(this).val(focused); // needed to prevent missing focus on first selected item
                            }
                            return false;
                        },
                        focus: function (event, ui) {
                            if (ui.item && ui.item.user) {
                                focused = ui.item.user.username;
                            }
                        }
                    });
                    searchElement.data("uiAutocomplete").search(searchElement.val());
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    failure(textStatus + " occurred while processing the data.", "Reason: " + errorThrown)
                }
            });
        }
    });
});
