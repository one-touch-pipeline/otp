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

$.confirmation = function (text) {
    if (text) {
        return confirm(text);
    }
    return true
};

$(function() {
    $('input[type=radio][name=addViaLdap]').change(function() {
        var ldapUserChecked = (this.value === "true");
        $('.inputField.ldapUser').prop("disabled", !ldapUserChecked);
        $('.inputField.nonLdapUser').prop("disabled", ldapUserChecked);
    }).filter(":checked").trigger("change");

    $('#listEmails').click(function () {
        var project = $(this).data('project');
        var emails = $(this).data('emails');
        prompt("Emails for " + project, emails);
    });

    $('#add-button, #deactivateButton, #reactivateButton').click(function () {
        var text = $(this).data('text');
        //This is needed to prevent multiple submits
        this.disabled = true;

        if (!$.confirmation(text)) {
            this.disabled = false;
            return false;
        }

        //This is needed since Chrome does not submit when the button is disabled, where as Firefox and Edge do
        $(this).parents('form').submit();
        return true
    });
});

$(document).ready(function(){
    $('.loaded-content').show();
    $('.loader').hide();
});
