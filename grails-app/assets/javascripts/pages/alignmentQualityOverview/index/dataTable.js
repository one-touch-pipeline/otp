/*jslint browser: true */
/*global $ */

$.otp.alignmentQualityOverviewTable = {
    register : function () {
        "use strict";
        var table = $("#overviewTableProcessedMergedBMF").dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools : $.otp.tableTools,
            bFilter : true,
            bProcessing : true,
            bServerSide : false,
            bSort : true,
            bJQueryUI : false,
            bAutoWidth : true,
            sAjaxSource : $.otp.createLink({
                controller : 'alignmentQualityOverview',
                action : 'dataTableSource'
            }),
            sScrollY: 'auto',
            sScrollX: 'auto',
            bScrollCollapse: false,
            bPaginate: false,
            bDeferRender : true,
            fnServerData : function (sSource, aoData, fnCallback) {
                aoData.push({
                    name : "project",
                    value : $('#project').find('option:selected').text()
                });
                aoData.push({
                    name : "seqType",
                    value : $('#seqType').val()
                });
                $.ajax({
                    "dataType" : 'json',
                    "type" : "POST",
                    "url" : sSource,
                    "data" : aoData,
                    "error" : function () {
                        // clear the table
                        fnCallback({aaData : [], iTotalRecords : 0, iTotalDisplayRecords : 0});
                        table.fnSettings().oFeatures.bServerSide = false;
                    },
                    "success" : function (json) {
                        $("#withdrawn_description").hide();
                        var seqType = $('#seqType').val();
                        for (var i = 0; i < json.aaData.length; i += 1) {
                            var row = json.aaData[i];
                            var identifier, coverage, general;
                            identifier = [
                                $.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    text: row.mockPid,
                                    parameters: {
                                        mockPid: row.mockPid
                                    }
                                }),
                                row.sampleType,
                            ];

                            if (seqType === 'WGS' || seqType === 'WGBS' || seqType === 'WGBS_TAG' || seqType === "ChIP") {
                                coverage = [
                                    row.coverageWithoutN,
                                    row.coverageX,
                                    row.coverageY,
                                ];
                            } else if (seqType === 'EXOME') {
                                coverage = [
                                    "<span class='" + row.onTargetRateWarning + "'>" + row.onTargetRate + " </span>",
                                    row.targetCoverage,
                                ];
                            } else if (seqType === 'RNA') {
                                coverage = [
                                    row.totalReadCounter,
                                    row.duplicates,
                                    row.threePNorm,
                                    row.fivePNorm,
                                    row.chimericPairs,
                                    row.duplicatesRate,
                                    row.end1Sense,
                                    row.end2Sense,
                                    row.estimatedLibrarySize,
                                    row.exonicRate,
                                    row.expressionProfilingEfficiency,
                                    row.genesDetected,
                                    row.intergenicRate,
                                    row.intragenicRate,
                                    row.intronicRate,
                                    row.mapped,
                                    row.mappedUnique,
                                    row.mappedUniqueRateOfTotal,
                                    row.mappingRate,
                                    row.meanCV,
                                    row.uniqueRateofMapped,
                                    row.rRNARate,
                                ]
                            }

                            if (seqType === 'RNA') {
                                general = [
                                    "<span title='" + row.kit.name + "'>" + row.kit.shortName + "</span>",
                                    row.dateFromFileSystem,
                                ];
                            } else {
                                general = [
                                    "<span title='" + row.kit.name + "'>" + row.kit.shortName + "</span>",
                                    row.mappedReads,
                                    "<span class='" + row.duplicateWarning + "'>" + row.duplicates + " </span>",
                                    "<span class='" + row.properlyPpairedWarning + "'>" + row.properlyPaired + " </span>",
                                    row.singletons,
                                    "<span class='" + row.medianWarning + "'>" + row.medianPE_insertsize + " </span>",
                                    "<span class='" + row.diffChrWarning + "'>" + row.diffChr + " </span>",
                                    row.pipeline,
                                    row.dateFromFileSystem,
                                ];
                            }
                            var rowdata = identifier.concat(coverage, general);

                            if (row.withdrawn) {
                                $("#withdrawn_description").show();
                                var withdrawnRow = [];
                                $.each(rowdata, function() {
                                    withdrawnRow.push("<span class='withdrawn'>" + this + " </span>")
                                });
                                rowdata = withdrawnRow;
                            }

                            json.aaData[i] = rowdata;
                        }
                        fnCallback(json);
                        table.fnSettings().oFeatures.bServerSide = false;
                    }
                });
            }
        });
        return table;
    }
};
