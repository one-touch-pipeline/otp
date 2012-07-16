/*global $: false, window: false
 */

$("div.showChangeLog button").click(function () {
    "use strict";
    var container = $(this).parent();
    $.ajax({
        url: $("input:hidden", container).val(),
        dataType: 'json',
        success: function (data) {
            var tbody, tr, i, dialog;
            tbody = $("table tbody", container);
            dialog = $("div", container).clone();
            for (i = 0; i < data.length; i += 1) {
                tr = tbody.append("<tr>");
                tr.append("<td>" + data[i].from + "</td>");
                tr.append("<td>" + data[i].to + "</td>");
                tr.append("<td>" + data[i].source + "</td>");
                tr.append("<td>" + data[i].comment + "</td>");
            }
            $("div", container).dialog({
                width: 800,
                close: function () {
                    $(this).remove();
                }
            });
            container.append(dialog);
        }
    });
});
