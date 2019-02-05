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

$("div.showChangeLog button").click(function () {
    "use strict";
    var container = $(this).parent();
    $.ajax({
        url: $("input:hidden", container).val(),
        dataType: 'json',
        success: function (data) {
            var tbody, tr, i, dialog, date;
            tbody = $("table tbody", container);
            dialog = $("div", container).clone();
            for (i = 0; i < data.length; i += 1) {
                tr = tbody.append("<tr>");
                date = new Date(data[i].timestamp);
                tr.append("<td title=\"" + date.toLocaleString() + "\">" + $.datepicker.formatDate("yy-mm-dd", date) + "</td>");
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
