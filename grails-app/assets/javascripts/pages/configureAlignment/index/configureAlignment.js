/*jslint browser: true */
/*global $ */

$.otp.configureAlignment = {
    register: function () {
        "use strict";
        $.otp.configureAlignment.setStatSizeFileNames();
        $('#referenceGenome').change(function () {
            $.otp.configureAlignment.setStatSizeFileNames();
        });
    },

    setStatSizeFileNames: function () {
        $.ajax({
            type: 'GET',
            url: $.otp.createLink({
                controller: 'configureAlignment',
                action: 'getStatSizeFileNames'
            }),
            dataType: 'json',
            cache: 'false',
            data: {
                referenceGenome: $('#referenceGenome').val()
            },
            success: function (data) {
                $('#statSizeFileNames').empty();
                if (data.data) {
                    $.each(data.data, function(i, p) {
                        $('#statSizeFileNames').append($('<option></option>').val(p).html(p));
                    });
                } else {
                    $('#statSizeFileNames').append($('<option></option>'));
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
            }
        });
    }
};
