/*global $: false, window: false
 */

$(function() {
    $.otp.editorswitch = {};
    $("div.edit-switch p.edit-switch-label button.edit").click(function () {
        "use strict";
        var outerContainer = $(this).parent().parent();
        $("p.edit-switch-editor", outerContainer).show();
        $("p.edit-switch-label", outerContainer).hide();
    });

    $("div.edit-switch-text-area p.edit-switch-text-area-label button.edit-button-left").click(function () {
        "use strict";
        var outerContainer = $(this).parent().parent();
        $("p.edit-switch-text-area-editor", outerContainer).show();
        $("p.edit-switch-text-area-label", outerContainer).hide();
    });


    $("div.edit-switch-url p.edit-switch-url-label button.edit-button-left").click(function () {
        "use strict";
        var outerContainer = $(this).parent().parent();
        $("p.edit-switch-url-editor", outerContainer).show();
        $("p.edit-switch-url-label", outerContainer).hide();
    });

    $("div.edit-switch-drop-down p.edit-switch-drop-down-label button.edit").click(function () {
        "use strict";
        var outerContainer, existingValue, td, options;
        outerContainer = $(this).parent().parent();
        td = $("td.typeDropDown");
        existingValue = $("span.wordBreak", td).text();
        $("p.edit-switch-drop-down-editor", outerContainer).show();
        $("p.edit-switch-drop-down-label", outerContainer).hide();
    });

    $("div.edit-switch-new-value p.edit-switch-new-value-label button.insert").click(function () {
        "use strict";
        var outerContainer, options;
        outerContainer = $(this).parent().parent();
        $("p.edit-switch-new-value-editor", outerContainer).show();
        $("p.edit-switch-new-value-label", outerContainer).hide();
    });

    $("div.edit-switch-new-free-text-value p.edit-switch-new-free-text-value-label button.insert").click(function () {
        "use strict";
        var outerContainer;
        outerContainer = $(this).parent().parent();
        $("p.edit-switch-new-free-text-value-editor", outerContainer).show();
        $("p.edit-switch-new-free-text-value-label", outerContainer).hide();
    });

    $("div.edit-switch-new-free-text-values span.edit-switch-new-free-text-values-label button.add").click(function () {
        "use strict";
        var outerContainer;
        outerContainer = $(this).parent().parent();
        $("span.edit-switch-new-free-text-values-editor", outerContainer).show();
        $("span.edit-switch-new-free-text-values-label", outerContainer).hide();
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
                    $.otp.infoMessage($L("editorswitch.notification.success"));
                    $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
                $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
            }
        });
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
    });

    /*jslint unparam: true */
    $("div.edit-switch-text-area p.edit-switch-text-area-editor button.save").click(function () {
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
                    $.otp.infoMessage($L("editorswitch.notification.success"));
                    $("p.edit-switch-text-area-label span", outerContainer).text($("textarea", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("textarea", container).val($("p.edit-switch-text-area-label span", outerContainer).text());
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
                $("textarea", container).val($("p.edit-switch-text-area-label span", outerContainer).text());
            }
        });
        $("p.edit-switch-text-area-editor", outerContainer).hide();
        $("p.edit-switch-text-area-label", outerContainer).show();
    });

    /*jslint unparam: true */
    $("div.edit-switch-url p.edit-switch-url-editor button.save").click(function () {
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
                    $.otp.infoMessage($L("editorswitch.notification.success"));
                    $("p.edit-switch-url-label span", outerContainer).text($("input:text[name=value]", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("input:text[name=value]", container).val($("p.edit-switch-url-label span", outerContainer).text());
                }
            },
        });
        $("p.edit-switch-url-editor", outerContainer).hide();
        $("p.edit-switch-url-label", outerContainer).show();
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
                    $.otp.infoMessage($L("editorswitch.notification.success"));
                    $("p.edit-switch-drop-down-label span", outerContainer).text($("select option:selected", container).text());
                } else {
                    $.otp.warningMessage(data.error);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
            }
        });
        $("p.edit-switch-drop-down-editor", outerContainer).hide();
        $("p.edit-switch-drop-down-label", outerContainer).show();
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
                    $.otp.infoMessage($L("editorswitch.notification.success"));
                    $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("input:text[name=value]", container).val($("p.edit-switch-new-value-label span", outerContainer).text());
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
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
                    $.otp.infoMessage($L("editorswitch.notification.success"));
                    $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("input:text[name=value]", container).val($("p.edit-switch-new-free-text-value-label span", outerContainer).text());
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
                $("input:text[name=value]", container).val($("p.edit-switch-new-free-text-value-label span", outerContainer).text());
            }
        });
        $("p.edit-switch-new-free-text-value-editor", outerContainer).hide();
        $("p.edit-switch-new-free-text-value-label", outerContainer).show();
        window.setTimeout('location.reload()', 300); //reloads page after 0.3 seconds
    });

    /*jslint unparam: true */
    $("div.edit-switch-new-free-text-values span.edit-switch-new-free-text-values-editor button.save").click(function () {
        "use strict";
        var container, outerContainer, field;
        container = $(this).parent();
        outerContainer = container.parent();
        $.ajax({
            url: $("input:hidden[name=target]", container).val(),
            dataType: 'json',
            data: {
                alias: $("input:text[name=Alias]", container).val(),
                name: $("input:text[name=Name]", container).val(),
                shortDisplayName: $("input:text[name='Short Display Name']", container).val(),
                dirName: $("input:text[name=Directory]", container).val(),
                type: $("input:text[name=Type]", container).val(),
                single: $("input:checked[name=SINGLE]", container).is( ":checked" ),
                paired: $("input:checked[name=PAIRED]", container).is( ":checked" ),
                mate_pair: $("input:checked[name=MATE_PAIR]", container).is( ":checked" ),
                anyLayout: ($("input:checked[name=SINGLE]", container).is( ":checked" )||
                                  $("input:checked[name=PAIRED]", container).is( ":checked" )||
                                  $("input:checked[name=MATE_PAIR]", container).is( ":checked" )),
                platform: $("input:text[name=Platform]", container).val(),
                group: $("input:text[name=Group]", container).val(),
                model: $("input:text[name=Model]", container).val(),
                kit: $("input:text[name=Kit]", container).val(),
                email: $("input:text[name=E-Mail]", container).val(),
                aspera: $("input:text[name='Aspera Account']", container).val(),
                role: $("select[name='Role']", container).val()

            },
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage($L("editorswitch.notification.success"));
                    window.setTimeout('location.reload()', 500);
                } else {
                    $.otp.warningMessage(data.error);
                    }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
            }
        });
        $("span.edit-switch-new-free-text-values-editor", outerContainer).hide();
        $("span.edit-switch-new-free-text-values-label", outerContainer).show();
    });

    $("div.edit-switch-url p.edit-switch-url-editor button.cancel").click(function () {
        "use strict";
        var outerContainer = $(this).parent().parent();
        $("p.edit-switch-url-editor", outerContainer).hide();
        $("p.edit-switch-url-label", outerContainer).show();
        $("input:text[name=value]", $(this).parent()).val($("p.edit-switch-url-label span", outerContainer).text());
    });

    $("div.edit-switch p.edit-switch-editor button.cancel").click(function () {
        "use strict";
        var outerContainer = $(this).parent().parent();
        $("p.edit-switch-editor", outerContainer).hide();
        $("p.edit-switch-label", outerContainer).show();
        $("input:text[name=value]", $(this).parent()).val($("p.edit-switch-label span", outerContainer).text());
    });

    $("div.edit-switch-text-area p.edit-switch-text-area-editor button.cancel").click(function () {
        "use strict";
        var outerContainer = $(this).parent().parent();
        $("p.edit-switch-text-area-editor", outerContainer).hide();
        $("p.edit-switch-text-area-label", outerContainer).show();
        $("textarea", $(this).parent()).val($("p.edit-switch-text-area-label span", outerContainer).text());
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

    $("div.edit-switch-new-free-text-values span.edit-switch-new-free-text-values-editor button.cancel").click(function () {
        "use strict";
        var outerContainer = $(this).parent().parent();
        $("span.edit-switch-new-free-text-values-editor", outerContainer).hide();
        $("span.edit-switch-new-free-text-values-label", outerContainer).show();
    });
});
