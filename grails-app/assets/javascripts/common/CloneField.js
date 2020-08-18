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
    "use strict";
    $(".clone-add").click(function (e) {
        e.preventDefault();
        var target = $(".clone-target");
        target.data("highest-index", target.data("highest-index") + 1);
        var index = target.data("highest-index");

        var clone = $(".clone-template").clone(true, true);

        // replace the template-index with the current index to fulfill the array like indexing
        // prop[0], prop[1], ...
        clone.find("input, select, label").each(function () {
            var component = this;
            $.each(["name", "id", "for", "data-select2-id"], function () {
                var attribute = this;
                $.otp.cloneField.replaceTemplateIndexInProperty(component, attribute, index);
            });
        });

        // Add the cloned and cleaned element to the clone-target container
        target.append(clone);

        $.otp.applySelect2($('select.use-select-2-after-clone', clone));

        // Remove the clone-template class, as only the actual template should have it
        $(".clone-target > .clone-template").removeClass("clone-template").removeClass("hidden");

        return false;
    });

    $(".clone-remove").click(function (e) {
        e.preventDefault();
        $(this).closest('.clone-remove-target').remove();
    });
});

/**
 * Replaces "template-index" in the value of the given attribute of the target with the given index.
 * Guards against unset attribute.
 */
$.otp.cloneField = {
    replaceTemplateIndexInProperty: function (target, attribute, index) {
        "use strict";
        var attr = $(target).attr(attribute);
        // Result can differ based on browser, need to check both options
        if (typeof attr !== typeof undefined && attr !== false) {
            $(target).attr(attribute, $(target).attr(attribute).replace(/template-index/, index));
        }
    }
};
