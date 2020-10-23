/*
 * Copyright 2011-2020 The OTP authors
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

$(function () {
    var update = function (e) {
        $(e.target).parents("form").submit();
    }
    var form = $("form.selector");
    form.on("change", "select", update);
    form.on("change", "input", update);
    form.on("keyup", "input[type=text]", update);


    $(".format").on("click", function (e) {
        e.preventDefault();
        var value = $("#configValue");
        value.val(JSON.stringify(JSON.parse(value.val()), null, 2));
    });


    $("#relatedSelectorType").on("change", function (e) {
        var types = $(e.target).val();
        $("#relatedSelectors").children("div").each(function () {
            if (types.indexOf($(this).data("type")) === -1) {
                $(this).hide();
            } else {
                $(this).show();
            }
        });
    });
});
