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

/*global $: false, window: false
 */

$(function() {
    $("td.add-table-buttons button.add").click(function (event) {
        "use strict";
        event.preventDefault();
        var p = $(this).parent();
        $(".save", p).show();
        $(".cancel", p).show();
        $(".add", p).hide();
        $(".add-table-fields", p.parent().parent()).show();
    });

    $("td.add-table-buttons button.cancel").click(function (event) {
        "use strict";
        event.preventDefault();
        var p = $(this).parent();
        $(".save", p).hide();
        $(".cancel", p).hide();
        $(".add", p).show();
        $(".add-table-fields", p.parent().parent()).hide();
    });

    $("tr.edit-table-buttons button.button-edit").click(function (event) {
        "use strict";
        event.preventDefault();
        var p = $(this).parent();
        $(".save", p).show();
        $(".cancel", p).show();
        $(".button-edit", p).hide();
        $(".edit-fields", p.parent()).show();
        $(".show-fields", p.parent()).hide();
    });

    $("tr.edit-table-buttons button.cancel").click(function (event) {
        "use strict";
        event.preventDefault();
        var p = $(this).parent();
        $(".save", p).hide();
        $(".cancel", p).hide();
        $(".button-edit", p).show();
        $(".edit-fields", p.parent()).hide();
        $(".show-fields", p.parent()).show();
    });

    $.otp.editorswitch = {};
    $("div.edit-switch .edit-switch-label button.js-edit").click(function () {
        "use strict";
        var outerContainer = $(this).parent().parent();
        $(".edit-switch-editor", outerContainer).show();
        $(".edit-switch-label", outerContainer).hide();
    });

    /*jslint unparam: true */
    $("div.edit-switch-text p.edit-switch-editor button.save").click(function () {
        "use strict";
        var container, outerContainer;
        container = $(this).parent();
        outerContainer = container.parent();
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            data: {value: $("input:text[name=value]", container).val()},
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage("Data stored successfully");
                    $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
                $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
            }
        });
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
    });

    /*jslint unparam: true */
    $("div.edit-switch-text-area p.edit-switch-editor button.save").click(function () {
        "use strict";
        var container, outerContainer;
        container = $(this).parent();
        outerContainer = container.parent();
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            data: {value: $("textarea", container).val()},
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage("Data stored successfully");
                    $("p.edit-switch-label span", outerContainer).text($("textarea", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("textarea", container).val($("p.edit-switch-label span", outerContainer).text());
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
                $("textarea", container).val($("p.edit-switch-label span", outerContainer).text());
            }
        });
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
    });

    /*jslint unparam: true */
    $("div.edit-switch-url p.edit-switch-editor button.save").click(function () {
        "use strict";
        var container, outerContainer;
        container = $(this).parent();
        outerContainer = container.parent();
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            data: {value: $("input:text[name=value]", container).val()},
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage("Data stored successfully");
                    $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
                }
            },
        });
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
    });

    $("div.edit-switch-drop-down p.edit-switch-editor button.save").click(function () {
        "use strict";
        var container, outerContainer;
        container = $(this).parent();
        outerContainer = container.parent();
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            data: { value: $("select option:selected", container).attr("value") },
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage("Data stored successfully");
                    $("p.edit-switch-label span", outerContainer).text($("select option:selected", container).text());
                } else {
                    $.otp.warningMessage(data.error);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
            }
        });
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
    });

    $("div.edit-switch-date p.edit-switch-editor button.save").click(function () {
        "use strict";
        var container, outerContainer;
        container = $(this).parent();
        outerContainer = container.parent();
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            data: { value: $('input[type="date"]', container).val() },
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage("Data stored successfully");
                    $("p.edit-switch-label span", outerContainer).text($('input[type="date"]', container).val());
                } else {
                    $.otp.warningMessage(data.error);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
            }
        });
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
    });

    /*jslint unparam: true */
    $("div.edit-switch-checkboxes p.edit-switch-editor button.save").click(function () {
        "use strict";
        var container, outerContainer, value;
        container = $(this).parent();
        outerContainer = container.parent();
        value = {};
        $("input:checked", container).each(function (index) {
            value["value["+index+"]"] = this.name
        });
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            data: value,
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage("Data stored successfully");
                    var names = [];
                    $("input:checkbox", container).each(function () {
                        if (this.checked) {
                            names.push(this.name);
                            $(this).data("checked", true)
                        } else {
                            $(this).data("checked", false)
                        }
                    });
                    $("p.edit-switch-label span", outerContainer).text(names.join(", ") || "(None selected)");
                } else {
                    $.otp.warningMessage(data.error);
                    resetCheckboxes(container);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
                resetCheckboxes(container);
            }
        });
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
    });

    function resetCheckboxes(container) {
        $("input:checkbox", container).each(function () {
            this.checked = $(this).data("checked");
        });
    }

    /*jslint unparam: true */
    $("div.edit-switch-new-value p.edit-switch-editor button.save").click(function () {
        "use strict";
        var container, outerContainer;
        container = $(this).parent();
        outerContainer = container.parent();
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            data: {value: $("select option:selected", container).text()},
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage("Data stored successfully");
                    $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
                $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
            }
        });
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
        window.setTimeout(function() { location.reload() }, 300); //reloads page after 0.3 seconds
    });

    /*jslint unparam: true */
    $("div.edit-switch-new-free-text-value p.edit-switch-editor button.save").click(function () {
        "use strict";
        var container, outerContainer;
        container = $(this).parent();
        outerContainer = container.parent();
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            data: {value: $("input:text[name=value]", container).val()},
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage("Data stored successfully");
                    $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
                $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
            }
        });
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
        window.setTimeout(function() { location.reload() }, 300); //reloads page after 0.3 seconds
    });

    /*jslint unparam: true */
    $("div.edit-switch-new-free-text-values span.edit-switch-editor button.save").click(function () {
        "use strict";
        var container, outerContainer, data = {};
        container = $(this).parent();
        outerContainer = container.parent();
        $("input:text", container).each(function() {
            data[this.name] = $(this).val();
        });
        $("input:checked", container).each(function() {
            data[this.name] = $(this).is(":checked");
        });
        $("select", container).each(function() {
            data[this.name] = $(this).val();
        });
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            data: data,
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage("Data stored successfully");
                    window.setTimeout(function() { location.reload() }, 300);
                } else {
                    $.otp.warningMessage(data.error);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
            }
        });
        $("span.edit-switch-editor", outerContainer).hide();
        $("span.edit-switch-label", outerContainer).show();
    });

    $("div.edit-switch-toggle p.edit-switch-editor button.toggle").click(function () {
        "use strict";
        var container, outerContainer, orgVal, invVal;
        container = $(this).parent();
        outerContainer = container.parent();
        orgVal = $("input:hidden[name=value]", container).val();
        invVal = (orgVal === "true" ? "false" : "true");
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage("Data stored successfully");
                    $.each(["label", "editor"], function () {
                        $("p.edit-switch-"+this+" span", outerContainer).removeClass("icon-"+orgVal).addClass("icon-"+invVal);
                    });
                    if (data.tooltip) {
                        $("p.edit-switch-label", outerContainer)[0].title = data.tooltip;
                    }
                    $("input:hidden[name=value]", container).attr("value", invVal);
                } else {
                    $.otp.warningMessage(data.error);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
            }
        });
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
    });

    $("div.edit-switch .edit-switch-editor button.cancel").click(function () {
        "use strict";
        var outerContainer = $(this).parent().parent();
        $(".edit-switch-editor", outerContainer).hide();
        $(".edit-switch-label", outerContainer).show();
        $(".edit-switch-input", $(this).parent()).val($("p.edit-switch-label span", outerContainer).text());
        if (outerContainer.hasClass("edit-switch-checkboxes")) {
            resetCheckboxes(outerContainer);
        }
    });
});
