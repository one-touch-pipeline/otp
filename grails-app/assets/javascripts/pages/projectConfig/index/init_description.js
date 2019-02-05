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

$.otp.initialiseSpecificOverview = {

    /**
     * Boolean shows if descriptionContent is collapsed or not
     */
    descriptionCollapsed: true,

    /**
     *  Tells if textarea is clickable at the moment to collapse/expand the description content
     */
    textareaClickable: true,

    /**
     * If a click on descriptionRow is performed the descriptionContent
     * is collapsed/expanded in the specificOverview HTML.
     * Will only expand/collapse if the click is not on
     * either buttons or edit textareas called.
     */
    toggleDescription: function () {
        $("#descriptionRow").click(function (e) {
            if ($.otp.initialiseSpecificOverview.validateToggle(e)) {
                if ($.otp.initialiseSpecificOverview.descriptionCollapsed == false) {
                    $.otp.initialiseSpecificOverview.collapseDescription();
                } else {
                    $.otp.initialiseSpecificOverview.expandDescription();
                }
            }
        });
    },

    /**
     * Expands the Description and changes the Arrow direction in the description header.
     *
     */
    expandDescription: function () {
        $("#descriptionContent").children().css('height', 'auto');
        $.otp.initialiseSpecificOverview.descriptionCollapsed = false;
        $("#descriptionHeader").html('Description ↑');
    },

    /**
     * Collapse the Description and changes the Arrow direction in the description header.
     */
    collapseDescription: function () {
        $.otp.initialiseSpecificOverview.descriptionCollapsed = true;
        $("#descriptionContent").children().css('height', '3em');
        $("#descriptionHeader").html('Description ↓');
    },

    /**
     * Validates if the edit Button or the editing textarea is clicked,
     * so no collapse will be initiated.
     * @param e
     * @returns {boolean} true or false to tell if a collapse or expand have to be made
     */
    validateToggle: function (e) {
        if (!$(e.target).is("button")) {
            if ($.otp.initialiseSpecificOverview.textareaClickable) {
                return true;
            }
        } else {
            $.otp.initialiseSpecificOverview.toggleTextareaClickDisable();
            $.otp.initialiseSpecificOverview.expandDescription();
            return false;
        }
    },

    /**
     * Toggle the boolean that tells if the textarea is clickable at the moment
     * This is important to ensure that no collapse is made while editing the text.
     */
    toggleTextareaClickDisable: function () {
        $.otp.initialiseSpecificOverview.textareaClickable = !$.otp.initialiseSpecificOverview.textareaClickable;
    }
};
