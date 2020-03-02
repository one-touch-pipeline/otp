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

$(function () {
    "use strict";
    var wrapper = $(".multi-input-field");
    wrapper.on("click", ".add-field", function (e) {
        e.preventDefault();

        var parent = $(e.target).parents(".multi-input-field");
        parent.append("<div class=\"field\">");

        var div = parent.children("div.field:last");
        div.append($(e.target).siblings("input:first, select:first, textarea:first").clone().val(""));
        // the whitespace at the start prevents the button from sticking to the input element
        div.append(" <button class=\"remove-field\">-</button>");
    });

    wrapper.on("click", ".remove-field", function (e) {
        e.preventDefault();
        $(e.target).parent("div.field").remove();
    });
});

$.otp.multiInputField = {
    cancel: function (target) {
        "use strict";
        target.find($(".remove-field").click());
    }
};
