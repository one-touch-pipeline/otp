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

$.otp.metaDataImport = {

    buttonAction: function() {
        $(".add-field-button").on("click", function(e){
            e.preventDefault();
            $.otp.metaDataImport.addNewFieldAndButton();
        });
        $($(".input-fields-wrap")).on("click", ".remove_field", function(e){
            e.preventDefault();
            $(this).parent('div').remove();
        });
    },

    addNewFieldAndButton: function() {
        $(".input-fields-wrap").append('<div><input name="paths" style="width:1000px" value="" type="text"> ' +
            '<button class="remove_field">-</button></div>');
    },

    addNewField: function() {
        $(".add-button").on("click", function (e) {
            e.preventDefault();
            if (e.currentTarget.classList.contains("keywords-button")) {
                $("#input-fields-keywords").append('<input type="text" list="keywordList" name="keywordNames" size="130"/>');
            } else if (e.currentTarget.classList.contains("connected-projects-button")) {
                $("#input-fields-connected-projects").append('<input type="text" list="projectList" name="connectedProjectNames" size="130"/>');
            }
        });
    }
};
