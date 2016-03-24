/*jslint browser: true */
/*global $ */

$.otp.configureAlignment = {
    register: function () {
        "use strict";
        $.otp.configureAlignment.setDecider();
        $('#decider').change(function () {
            $.otp.configureAlignment.setDecider();
            $.otp.configureAlignment.setStatSizeFileNames();
        });
        $.otp.configureAlignment.setStatSizeFileNames();
        $('#referenceGenome').change(function () {
            $.otp.configureAlignment.setStatSizeFileNames();
        });
    },

    setDecider: function () {
        var decider = $('#decider').val();
        if (decider == "No Alignment") {
            $('.default').hide();
            $('.pancan').hide();
        } else if (decider == "OTP Alignment") {
            $('.default').show();
            $('.pancan').hide();
        } else if (decider == "PanCan Alignment") {
            $('.default').show();
            $('.pancan').show();
        }
    },

    setStatSizeFileNames: function () {
        if ($('#decider').val() == "PanCan Alignment") {
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
    }
};
