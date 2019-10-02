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

$.otp.alignmentQualityOverviewTable = {
    /**
     * Formats the all cells in a table row and puts them in the correct order
     * @param row map containing the data in one row with column names as the keys
     * @param columnNames name of the columns in the correct order
     * @returns {Array}
     */
    tableRowsFormatter: function (row, columnNames) {
        var result = [];
        for (var i = 0; i < columnNames.length; i += 1) {
            result.push($.otp.alignmentQualityOverviewTable.tableCellFormatter(row[columnNames[i]]));
        }
        if (row.withdrawn) {
            var withdrawnRows = [];
            $.each(result, function () {
                withdrawnRows.push("<span class='withdrawn'>" + this + " </span>")
            });
            result = withdrawnRows;
        }
        return result;
    },

    tableCellFormatter: function (data) {
        if (!data) {
            return "";
        }
        if ((data.value === undefined) && (data.icon === undefined)) {
            return $('<div>').text(data).html(); // escape HTML
        }
        if (data.status == "BLOCKED" || data.status == "REJECTED") {
            result ="<span title='" + data.tooltip + "'>" + "<select class='" + data.status + "' onchange='$.otp.alignmentQualityOverview.change(this, \"" + data.id + "\", \"" + data.status + "\")'>" +
                 "<option value='ACCEPTED' class='ACCEPTED' " + (data.status == 'ACCEPTED' ? 'selected' : '') + ">&#10003; ACCEPTED</option>" +
                 "<option value='BLOCKED'  class='BLOCKED'" + (data.status == 'BLOCKED' ? 'selected' : '') + ">&#9888; BLOCKED</option>" +
                 "<option  value='REJECTED'  class='REJECTED' " + (data.status == 'REJECTED' ? 'selected' : '') + ">&#10005; REJECTED</option>" +
                 "</select> " + $('<div>').text(data.value).html()+ "</span>";
        } else {
            var result = $('<div>').text(data.value).html(); // escape HTML

            var cssClass = [];
            if (data.warnColor) {
                cssClass.push("text-" + data.warnColor)
            }
            if (data.icon) {
                cssClass.push("icon-" + data.icon)
            }
            if (cssClass.length !== 0) {
                result ="<span class='" + cssClass + "'>" + result + " </span>"
            }

            if (data.tooltip) {
                result ="<span title='" + data.tooltip + "'>" + result + "</span>"
            }
            if (data.link) {
                result ="<a href='" + data.link + "'>" + result + "</a>"
            }
        }
        return result;
    },

    register : function () {
        "use strict";
        if ($('#seqType').val()) {
            var table = $("#overviewTableProcessedMergedBMF").dataTable({
                sDom: '<i> B rt<"clear">',
                buttons: $.otp.tableButtons,
                bFilter: true,
                bProcessing: true,
                bServerSide: false,
                bSort: true,
                bJQueryUI: false,
                bAutoWidth: true,
                sAjaxSource: $.otp.createLink({
                    controller: 'alignmentQualityOverview',
                    action: 'dataTableSource'
                }),
                sScrollY: 'auto',
                sScrollX: 'auto',
                bScrollCollapse: false,
                bPaginate: false,
                bDeferRender: true,
                fnServerData: function (sSource, aoData, fnCallback) {
                    aoData.push({
                        name: "project",
                        value: $('#project').val()
                    });
                    aoData.push({
                        name: "seqType",
                        value: $('#seqType').val()
                    });
                    $.ajax({
                        "dataType": 'json',
                        "type": "POST",
                        "url": sSource,
                        "data": aoData,
                        "error": function () {
                            // clear the table
                            fnCallback({aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0});
                            table.fnSettings().oFeatures.bServerSide = false;
                        },
                        "success": function (json) {
                            var seqType = $('#seqType').find('option:selected').text();
                            var columnNames = [
                                "pid",
                                "sampleType",
                                "qcStatus",
                            ];

                            // coverage
                            if (seqType === 'WGS PAIRED bulk' || seqType === 'WGBS PAIRED bulk' || seqType === 'WGBS_TAG PAIRED bulk' || seqType === "ChIP PAIRED bulk") {
                                columnNames = columnNames.concat([
                                    "coverageWithoutN",
                                    "coverageX",
                                    "coverageY",
                                ]);
                            } else if (seqType === 'EXOME PAIRED bulk') {
                                columnNames = columnNames.concat([
                                    "onTargetRatio",
                                    "targetCoverage",
                                ]);
                            } else if (seqType === 'RNA PAIRED bulk' || seqType === 'RNA SINGLE bulk') {
                                columnNames = columnNames.concat([
                                    "totalReadCounter",
                                    "percentDuplicates",
                                    "threePNorm",
                                    "fivePNorm",
                                    "chimericPairs",
                                    "duplicatesRate",
                                    "end1Sense",
                                    "end2Sense",
                                    "estimatedLibrarySize",
                                    "exonicRate",
                                    "expressionProfilingEfficiency",
                                    "genesDetected",
                                    "intergenicRate",
                                    "intragenicRate",
                                    "intronicRate",
                                    "mapped",
                                    "mappedUnique",
                                    "mappedUniqueRateOfTotal",
                                    "mappingRate",
                                    "meanCV",
                                    "uniqueRateofMapped",
                                    "rRNARate",
                                ]);
                            }

                            // general information
                            if (seqType === 'RNA PAIRED bulk' || seqType === 'RNA SINGLE bulk') {
                                columnNames = columnNames.concat([
                                    "kit",
                                    "dateFromFileSystem",
                                ]);
                            } else if (seqType === '10x_scRNA PAIRED singleCell') {
                                columnNames = columnNames.concat([
                                    'summary',
                                    'expectedCells',
                                    'enforcedCells',
                                    'estimatedNumberOfCells',
                                    'meanReadsPerCell',
                                    'medianGenesPerCell',
                                    'numberOfReads',
                                    'validBarcodes',
                                    'sequencingSaturation',
                                    'q30BasesInBarcode',
                                    'q30BasesInRnaRead',
                                    'q30BasesInUmi',
                                    'readsMappedConfidentlyToIntergenicRegions',
                                    'readsMappedConfidentlyToIntronicRegions',
                                    'readsMappedConfidentlyToExonicRegions',
                                    'readsMappedConfidentlyToTranscriptome',
                                    'fractionReadsInCells',
                                    'totalGenesDetected',
                                    'medianUmiCountsPerCell',
                                    "kit",
                                    "dateFromFileSystem",
                                ]);
                            } else {
                                columnNames = columnNames.concat([
                                    "kit",
                                    "percentMappedReads",
                                    "percentDuplicates",
                                    "percentProperlyPaired",
                                    "percentSingletons",
                                    "insertSizeMedian",
                                    "percentDiffChr",
                                    "pipeline",
                                    "dateFromFileSystem",
                                ]);
                            }
                            for (var i = 0; i < json.aaData.length; i += 1) {
                                var row = json.aaData[i];
                                var rowdata = $.otp.alignmentQualityOverviewTable.tableRowsFormatter(row, columnNames);

                                json.aaData[i] = rowdata;
                            }
                            fnCallback(json);
                            table.fnSettings().oFeatures.bServerSide = false;
                        }
                    });
                }
            });
        }
    }
};
