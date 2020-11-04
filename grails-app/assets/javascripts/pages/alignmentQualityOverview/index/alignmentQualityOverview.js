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

$.otp.alignmentQualityOverview = {
    change: function (dropdownMenu, id, oldValue) {
        var comment = prompt("Please provide a comment for this change:");

        if (comment == null) {
            dropdownMenu.value = oldValue;
        } else {
            $("#wait").css("display", "block")
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": $.otp.createLink({
                    controller: 'alignmentQualityOverview',
                    action: 'changeQcStatus',
                }),
                "data": {
                    "abstractBamFile.id": id,
                    "newValue": dropdownMenu.value,
                    "comment": comment,
                },
                "success": function (json) {
                    if (!json.success) {
                        alert("Failed to edit value.\n" + json.error);
                        dropdownMenu.value = oldValue;
                    } else {
                        $("#overviewTableProcessedMergedBMF").DataTable().destroy();
                        $.otp.alignmentQualityOverviewTable.register();
                    }
                },
                "complete": function () {
                    $("#wait").css("display", "none")
                }
            });
        }
    }
}
