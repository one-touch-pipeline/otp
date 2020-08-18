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
    // hide and show the date picker for storageUntil when defined by a user
    var storageUntil = $("input#storageUntil");
    $("select#storagePeriod").on("change", function (e) {
        if ($(e.target).val() === "USER_DEFINED") {
            storageUntil.show()
        } else {
            storageUntil.hide()
        }
    }).trigger("change");

    // hide and show the textbox to provide a custom SpeciesWithStrain
    var customSpecies = $("input#customSpeciesWithStrain");
    $("select#speciesWithStrain").on("change", function (e) {
        if ($(e.target).val() === "other") {
            customSpecies.show();
            customSpecies.prop("disabled", false);
        } else {
            customSpecies.hide();
            customSpecies.prop("disabled", true);
        }
    }).trigger("change");

    // if the PI role is selected, manage users should be checked
    $(".project-role-select").on("change", function () {
        $(".project-role-select option:selected").each(function () {
            var manageUsersBox = $(this).closest(".project-request-user-table").find(".set-for-authority");
            if (this.text === "PI") {
                manageUsersBox.prop('checked', true);
                manageUsersBox.prop('disabled', true);
            } else {
                manageUsersBox.prop('disabled', false);
            }
        })
    }).trigger("change");
});
