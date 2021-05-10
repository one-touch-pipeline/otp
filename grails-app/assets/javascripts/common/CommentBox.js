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

/*jslint browser: true */
/*global $ */

$.setDisableForControls = function (value) {
    $("#button-save").prop("disabled", value);
    $("#button-cancel").prop("disabled", value);
};

$(function () {
    var container = $("#comment-box-container");
    var commentContent = container.find("#comment-content");
    var lastSavedComment = commentContent.val();

    commentContent.on("input", function() {
        "use strict";
        $.setDisableForControls(false);
    });
    $("#button-save").on("click", function() {
        "use strict";
        var controller = $(this).data('controller');
        var action = $(this).data('action');

        var id = container.find("#entity-id").val();
        var comment = commentContent.val();

        $.setDisableForControls(true);
        $.ajax({
            url: $.otp.createLink({ controller: controller, action: action }),
            dataType: "json",
            data: { id: id, comment: comment },
            type: "POST",
            encoding: "UTF-8",
            success: function (data) {
                lastSavedComment = commentContent.val();
                $("#authorSpan").text(data.date + " " + data.author);
            }
        });
    });
    $("#button-cancel").on("click", function() {
        "use strict";
        $.setDisableForControls(true);
        commentContent.val(lastSavedComment);
    });
});
