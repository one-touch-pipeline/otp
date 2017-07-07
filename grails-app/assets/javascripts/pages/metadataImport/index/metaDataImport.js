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

    fillFields: function (list) {
        if (list !== null || !list.isEmpty()) {
            for (var i = 0; i < list.length; i++) {
                $(".input-fields-wrap").find('input').last().val(list[i]);
                if (i !== list.length - 1) {
                    $.otp.metaDataImport.addNewFieldAndButton();
                } else {
                    break;
                }
            }
        }
    },

    addNewFieldAndButton: function() {
        $(".input-fields-wrap").append('<div><input name="paths" style="width:1000px" value="" type="text"> ' +
            '<button class="remove_field">-</button></div>');
    }
};
