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
            result.push($.otp.alignmentQualityOverviewTable.tableCellFormatter(row[columnNames[i]], row[columnNames[i]]));
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
                            $("#withdrawn_description").hide();
                            var seqType = $('#seqType').find('option:selected').text();
                            var columnNames = [
                                "pid",
                                "sampleType",
                                "qcStatus",
                            ];

                            // coverage
                            if (seqType === 'WGS PAIRED' || seqType === 'WGBS PAIRED' || seqType === 'WGBS_TAG PAIRED' || seqType === "ChIP PAIRED") {
                                columnNames = columnNames.concat([
                                    "coverageWithoutN",
                                    "coverageX",
                                    "coverageY",
                                ]);
                            } else if (seqType === 'EXOME PAIRED') {
                                columnNames = columnNames.concat([
                                    "onTargetRatio",
                                    "targetCoverage",
                                ]);
                            } else if (seqType === 'RNA PAIRED' || seqType === 'RNA SINGLE') {
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
                            if (seqType === 'RNA PAIRED' || seqType === 'RNA SINGLE') {
                                columnNames = columnNames.concat([
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
                                if (row.withdrawn) {
                                    $("#withdrawn_description").show();
                                }

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
