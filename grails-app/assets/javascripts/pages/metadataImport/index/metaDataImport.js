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
    }
};
