/*global $: false, window: false
 */

$("div.edit-switch p.edit-switch-label button.edit").click(function () {
    "use strict";
    var outerContainer = $(this).parent().parent();
    $("p.edit-switch-editor", outerContainer).show();
    $("p.edit-switch-label", outerContainer).hide();
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
                $.otp.infoMessage($.i18n.prop("editorswitch.notifiation.success"));
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
$("div.edit-switch p.edit-switch-editor button.cancel").click(function () {
    "use strict";
    var outerContainer = $(this).parent().parent();
    $("p.edit-switch-editor", outerContainer).hide();
    $("p.edit-switch-label", outerContainer).show();
    $("input:text[name=value]", $(this).parent()).val($("p.edit-switch-label span", outerContainer).text());
});
