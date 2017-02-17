/*jslint browser: true */
/*global $ */

$.otp.configureAlignment = {
    register: function () {
        "use strict";
        $("div.addGeneModel button").click(this.addGeneModel);
        $.otp.configureAlignment.setStatSizeFileNames();
        $.otp.configureAlignment.setGeneModels();
        $.otp.configureAlignment.setToolVersion();
        $('#referenceGenome').change(function () {
            $.otp.configureAlignment.setStatSizeFileNames();
            $('.geneModelSelect').not('.hidden').empty();
            $.otp.configureAlignment.setGeneModels();
            $.otp.configureAlignment.setToolVersion();
        });
    },

    setStatSizeFileNames: function () {
        $.ajax({
            type: 'GET',
            url: $.otp.createLink({
                controller: 'configurePipeline',
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
    },

    setGeneModels: function () {
        $.ajax({
            type: 'GET',
            url: $.otp.createLink({
                controller: 'configurePipeline',
                action: 'getGeneModels'
            }),
            dataType: 'json',
            cache: 'false',
            data: {
                referenceGenome: $('#referenceGenome').val()
            },
            success: function (data) {
                if (data.data) {
                    $.each(data.data, function(i, p) {
                        $('.geneModelSelect').not('.hidden').last().append($('<option></option>').val(p.id).html(p.fileName));
                    });
                } else {
                    $('.geneModelSelect').not('.hidden').append($('<option></option>'));
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
            }
        });
    },

    setToolVersion: function () {
        $.ajax({
            type: 'GET',
            url: $.otp.createLink({
                controller: 'configurePipeline',
                action: 'getToolVersions'
            }),
            dataType: 'json',
            cache: 'false',
            data: {
                referenceGenome: $('#referenceGenome').val()
            },
            success: function (data) {
                $.each(data, function(index, value) {
                    $('.toolVersionSelect_' + index).empty();
                    if (value) {
                        $.each(value, function (i, p) {
                            $('.toolVersionSelect_' + index).append($('<option></option>').val(p.id).html(p.indexToolVersion));
                        });
                    } else {
                        $('.toolVersionSelect_' + index).append($('<option></option>'));
                    }
                });
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
            }
        });
    },

    addGeneModel: function (event) {
        "use strict";
        event.preventDefault();
        var existingGeneModels, placeOfLastGeneModelInTable, geneModelBoxes;
        existingGeneModels = $("select.geneModelSelect").not(".hidden");
        placeOfLastGeneModelInTable = existingGeneModels.last();
        geneModelBoxes = $("select.geneModelSelect.hidden").clone().removeClass("hidden").add("<br>");
        geneModelBoxes.appendTo(placeOfLastGeneModelInTable.parent());
        $.otp.configureAlignment.setGeneModels();
    }
};
