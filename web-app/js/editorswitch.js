/*global $: false, window: false
 */

$.otp.editorswitch = {};
$("div.edit-switch p.edit-switch-label button.edit").click(function () {
    "use strict";
    var outerContainer = $(this).parent().parent();
    $("p.edit-switch-editor", outerContainer).show();
    $("p.edit-switch-label", outerContainer).hide();
});
$("div.edit-switch-drop-down p.edit-switch-drop-down-label button.edit").click(function () {
    "use strict";
    var outerContainer, existingValue, td, options;
    outerContainer = $(this).parent().parent();
    td = $("td.typeDropDown");
    existingValue = $("span.wordBreak", td).text();
    $("p.edit-switch-drop-down-editor", outerContainer).show();
    $("p.edit-switch-drop-down-label", outerContainer).hide();
    options = $(".dropDown").empty();
    options.append($("<option />").val(existingValue).text(existingValue));
    $.getJSON($.otp.contextPath + "/individual/typeDropDown", function (result) {
        var options = $(".dropDown");
        $.each(result, function () {
            if (this.name !== existingValue) {
                options.append($("<option />").val(this.name).text(this.name));
            }
        });
    });
});

$("div.edit-switch-new-value p.edit-switch-new-value-label button.insert").click(function () {
    "use strict";
    var outerContainer, options;
    outerContainer = $(this).parent().parent();
    $("p.edit-switch-new-value-editor", outerContainer).show();
    $("p.edit-switch-new-value-label", outerContainer).hide();
    options = $(".dropDown").empty();
    $.getJSON($.otp.contextPath + "/individual/sampleTypeDropDown", function (result) {
        var options = $(".dropDown");
        $.each(result, function () {
            options.append($("<option />").val(this).text(this));
        });
    });
});

$("div.edit-switch-new-free-text-value p.edit-switch-new-free-text-value-label button.insert").click(function () {
    "use strict";
    var outerContainer;
    outerContainer = $(this).parent().parent();
    $("p.edit-switch-new-free-text-value-editor", outerContainer).show();
    $("p.edit-switch-new-free-text-value-label", outerContainer).hide();
});

/*jslint unparam: true */
$("div.edit-switch p.edit-switch-editor button.save").click(function () {
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
                $.otp.infoMessage($.i18n.prop("editorswitch.notification.success"));
                $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
            } else {
                $.otp.warningMessage(data.error);
                $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            $.otp.warningMessage($.i18n.prop("editorswitch.notification.error", textStatus, errorThrown));
            $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
        }
    });
    $("p.edit-switch-editor", outerContainer).hide();
    $("p.edit-switch-label", outerContainer).show();
});
$("div.edit-switch-drop-down p.edit-switch-drop-down-editor button.save").click(function () {
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
                $.otp.infoMessage($.i18n.prop("editorswitch.notification.success"));
                $("p.edit-switch-drop-down-label span", outerContainer).text($("select option:selected", container).text());
            } else {
                $.otp.warningMessage(data.error);
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            $.otp.warningMessage($.i18n.prop("editorswitch.notification.error", textStatus, errorThrown));
        }
    });
    $("p.edit-switch-drop-down-editor", outerContainer).hide();
    $("p.edit-switch-drop-down-label", outerContainer).show();
    window.setTimeout('location.reload()', 300); //reloads page after 0.3 seconds
});
/*jslint unparam: true */
$("div.edit-switch-new-value p.edit-switch-new-value-editor button.save").click(function () {
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
                $.otp.infoMessage($.i18n.prop("editorswitch.notification.success"));
                $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
            } else {
                $.otp.warningMessage(data.error);
                $("input:text[name=value]", container).val($("p.edit-switch-new-value-label span", outerContainer).text());
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            $.otp.warningMessage($.i18n.prop("editorswitch.notification.error", textStatus, errorThrown));
            $("input:text[name=value]", container).val($("p.edit-switch-new-value-label span", outerContainer).text());
        }
    });
    $("p.edit-switch-new-value-editor", outerContainer).hide();
    $("p.edit-switch-new-value-label", outerContainer).show();
    window.setTimeout('location.reload()', 300); //reloads page after 0.3 seconds
});

/*jslint unparam: true */
$("div.edit-switch-new-free-text-value p.edit-switch-new-free-text-value-editor button.save").click(function () {
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
                $.otp.infoMessage($.i18n.prop("editorswitch.notification.success"));
                $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
            } else {
                $.otp.warningMessage(data.error);
                $("input:text[name=value]", container).val($("p.edit-switch-new-free-text-value-label span", outerContainer).text());
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            $.otp.warningMessage($.i18n.prop("editorswitch.notification.error", textStatus, errorThrown));
            $("input:text[name=value]", container).val($("p.edit-switch-new-free-text-value-label span", outerContainer).text());
        }
    });
    $("p.edit-switch-new-free-text-value-editor", outerContainer).hide();
    $("p.edit-switch-new-free-text-value-label", outerContainer).show();
    window.setTimeout('location.reload()', 300); //reloads page after 0.3 seconds
});

$("div.edit-switch p.edit-switch-editor button.cancel").click(function () {
    "use strict";
    var outerContainer = $(this).parent().parent();
    $("p.edit-switch-editor", outerContainer).hide();
    $("p.edit-switch-label", outerContainer).show();
    $("input:text[name=value]", $(this).parent()).val($("p.edit-switch-label span", outerContainer).text());
});
$("div.edit-switch-drop-down p.edit-switch-drop-down-editor button.cancel").click(function () {
    "use strict";
    var outerContainer = $(this).parent().parent();
    $("p.edit-switch-drop-down-editor", outerContainer).hide();
    $("p.edit-switch-drop-down-label", outerContainer).show();
});
$("div.edit-switch-new-value p.edit-switch-new-value-editor button.cancel").click(function () {
    "use strict";
    var outerContainer = $(this).parent().parent();
    $("p.edit-switch-new-value-editor", outerContainer).hide();
    $("p.edit-switch-new-value-label", outerContainer).show();
});
$("div.edit-switch-new-free-text-value p.edit-switch-new-free-text-value-editor button.cancel").click(function () {
    "use strict";
    var outerContainer = $(this).parent().parent();
    $("p.edit-switch-new-free-text-value-editor", outerContainer).hide();
    $("p.edit-switch-new-free-text-value-label", outerContainer).show();
});
