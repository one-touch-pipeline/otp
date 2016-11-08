/*global $: false, window: false
 */

$(function() {
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
                    $.otp.infoMessage($L("editorswitch.notification.success"));
                    $("p.edit-switch-label span", outerContainer).text($("textarea", container).val());
                } else {
                    $.otp.warningMessage(data.error);
                    $("textarea", container).val($("p.edit-switch-label span", outerContainer).text());
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
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
                    $.otp.infoMessage($L("editorswitch.notification.success"));
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
            data: {value: $("select option:selected", container).text()},
            success: function (data) {
                if (data.success) {
                    $.otp.infoMessage($L("editorswitch.notification.success"));
                    $("p.edit-switch-label span", outerContainer).text($("select option:selected", container).text());
                } else {
                    $.otp.warningMessage(data.error);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
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
                    $.otp.infoMessage($L("editorswitch.notification.success"));
                    var names = [];
                    $("input:checkbox", container).each(function () {
                        if (this.checked) {
                            names.push(this.name);
                            $(this).data("checked", true)
                        } else {
                            $(this).data("checked", false)
                        }
                    });
                    $("p.edit-switch-label span", outerContainer).text(names.join(", ") || $L("editorswitch.noneSelected"));
                } else {
                    $.otp.warningMessage(data.error);
                    resetCheckboxes(container);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
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
        window.setTimeout('location.reload()', 300); //reloads page after 0.3 seconds
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
        window.setTimeout('location.reload()', 300); //reloads page after 0.3 seconds
    });

    /*jslint unparam: true */
    $("div.edit-switch-new-free-text-values span.edit-switch-editor button.save").click(function () {
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
                role: $("select[name='Role']", container).val(),
                typeSelect: $("select[name='Type']", container).val(),
                actionSelect: $("select[name='Action']", container).val(),
                errorExpression: $("input:text[name='Error Expression']", container).val()
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
        $("span.edit-switch-editor", outerContainer).hide();
        $("span.edit-switch-label", outerContainer).show();
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
