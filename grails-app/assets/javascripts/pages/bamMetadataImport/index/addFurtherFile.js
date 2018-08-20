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
