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

$(function() {
    "use strict";

    var setStatSizeFileNames = function() {
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
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
            }
        });
    };

    var setGeneModels = function() {
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
                $('.geneModelSelect').empty();
                if (data.data) {
                    $.each(data.data, function(i, p) {
                        $('.geneModelSelect').append($('<option></option>').val(p.id).html(p.fileName));
                    });
                } else {
                    $('.geneModelSelect').append($('<option></option>'));
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
            }
        });
    };

    var setToolVersion = function() {
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
                $('.toolVersionSelect').empty();
                $.each(data.data, function(index, value) {
                    var search = (index.indexOf("GENOME_STAR_INDEX") !== -1) ? "GENOME_STAR_INDEX" : index;
                    if (value) {
                        $.each(value, function (i, p) {
                            var outputValue = index.toLowerCase().replace("genome_", "").replace("_index", "") + " - " + p.indexToolVersion;
                            if (outputValue === data.defaultGenomeStarIndex) {
                                $('#toolVersionSelect_' + search).append($('<option></option>').prop("selected", true).val(p.id).html(outputValue));
                            } else {
                                $('#toolVersionSelect_' + search).append($('<option></option>').val(p.id).html(outputValue));
                            }
                        });
                    } else {
                        $('#toolVersionSelect_' + index).append($('<option></option>'));
                    }
                });
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $.otp.warningMessage(textStatus + " occurred while processing the data. Reason: " + errorThrown);
            }
        });
    };

    $('#referenceGenome').on("change", function() {
        setStatSizeFileNames();
        setGeneModels();
        setToolVersion();
    }).trigger("change");
});
