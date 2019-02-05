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

$.otp.bamMetadataImport = {


    addValues: function() {
        var wrapper         = $(".input-fields-wrap"); //Fields wrapper
        var add_button      = $(".add-field-button"); //Add button ID
        add_button.on("click",function(e){ //on add input button click
           e.preventDefault();
            $.otp.bamMetadataImport.fillInputFields()
         });
        $(wrapper).on("click",".remove_field", function(e){ //user click on remove text
            e.preventDefault();
            $(this).parent('div').remove();
        });
    },

    fillInputFields: function() {
        $(".input-fields-wrap").append('<div><input type="text" style="width:600px" name="furtherFilePaths"/>' +
            '<button class="remove_field">-</button></div>');
    }
};
