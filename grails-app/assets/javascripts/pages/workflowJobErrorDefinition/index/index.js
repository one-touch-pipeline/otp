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

/*global $: false
 */

//no namespace, since method name is given as string and execute using
//window[$method]
workflowJobErrorDefinitionActionChangeSuccessHandler = function (container, additionalData) {
    "use strict";
    var row = container.parent().parent().parent();
    var select = $(".restartJob select", row);
    var editSwitch = $(".restartJob .edit-switch-label", row);
    var display = $("span", editSwitch);
    if (additionalData.restartBean) {
        display.text(additionalData.restartBean);
        select.val(additionalData.restartBean).trigger('change');
        editSwitch.show()
    } else {
        display.text("");
        select.val("").trigger('change');
        editSwitch.hide()
    }
};

$(function () {
    var rows = $('.errorDefinitions tr')
    rows.each(function () {
        var row = $(this);
        var action = $('.restartAction select', row);
        var val = action.val();
        if (val !== "RESTART_JOB") {
            var editSwitch = $(".restartJob .edit-switch-label", row);
            editSwitch.hide();
        }
    });
});
