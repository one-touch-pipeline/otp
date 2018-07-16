/*jslint browser: true */
/*global $ */

// Override jQuery functions to allow rendering of table
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


$.otp.projectUser = {
    autocompletion: function () {
        $('.autocompleted').on({
            input: function () {
                "use strict";
                var searchElement = $("#search");
                $.ajax({
                    type: 'GET',
                    url: $.otp.createLink({
                        controller: 'projectUser',
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
                                'value'      : [result[i].cn, result[i].realName, result[i].mail].join(";"),
                                'username'   : result[i].cn,
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
                        $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown))
                    }
                });
            }
        });
    },

    formSelect: function() {
        $('.inputField.nonLdapUser').prop("disabled", true);
        $('input[type=radio][name=addViaLdap]').change(function() {
            var ldapUserChecked = (this.value === "true");
            $('.inputField.ldapUser').prop("disabled", !ldapUserChecked);
            $('.inputField.nonLdapUser').prop("disabled", ldapUserChecked);
        });
    }
};