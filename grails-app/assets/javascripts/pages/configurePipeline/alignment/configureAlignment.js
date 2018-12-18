/*jslint browser: true */
/*global $ */

$.otp.configureAlignment = {
    register: function () {
        "use strict";
        $("div.addGeneModel button").click(this.addGeneModel);
        $.otp.configureAlignment.setStatSizeFileNames();
        $('.dropDown').empty();
        $.otp.configureAlignment.setGeneModels();
        $.otp.configureAlignment.setToolVersion();
        $('#referenceGenome').change(function () {
            $.otp.configureAlignment.setStatSizeFileNames();
            $('.dropDown').empty();
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
                $.otp.warningMessage($L("editorSwitch.notification.error", textStatus, errorThrown));
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
                        $('.geneModelSelect').append($('<option></option>').val(p.id).html(p.fileName));
                    });
                } else {
                    $('.geneModelSelect').append($('<option></option>'));
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorSwitch.notification.error", textStatus, errorThrown));
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
                $.each(data.data, function(index, value) {
                    var search = (index.indexOf("GENOME_STAR_INDEX") != -1) ? "GENOME_STAR_INDEX" : index;
                    if (value) {
                        $.each(value, function (i, p) {
                            var outputValue = index.toLowerCase().replace("genome_", "").replace("_index", "") + " - " + p.indexToolVersion;
                            if (outputValue == data.defaultGenomeStarIndex) {
                                $('.toolVersionSelect_' + search).append($('<option></option>').val(p.id).html(outputValue).selected());
                            } else {
                                $('.toolVersionSelect_' + search).append($('<option></option>').val(p.id).html(outputValue));
                            }
                        });
                    } else {
                        $('.toolVersionSelect_' + index).append($('<option></option>'));
                    }
                });
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage($L("editorSwitch.notification.error", textStatus, errorThrown));
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
