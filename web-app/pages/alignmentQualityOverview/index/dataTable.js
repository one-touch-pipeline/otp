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
                    value : $('#project').val()
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
                        $("#withdrawn_description").hide()
                        var seqType = $('#seqType').val()
                        for (var i = 0; i < json.aaData.length; i += 1) {
                            var row = json.aaData[i];
                            var identifier, coverage, general
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

                            if (seqType == 'WGS') {
                                coverage = [
                                        row.coverageWithoutN,
                                        row.coverageX,
                                        row.coverageY,
                                ];
                            } else if (seqType == 'EXOME') {
                                coverage = [
                                        "<span class='" + row.onTargetRateWarning+"'>" + row.onTargetRate + " </span>",
                                        row.targetCoverage,
                                ];
                            }

                            general = [
                                row.mappedReads,
                                "<span class='" + row.duplicateWarning+"'>" + row.duplicates + " </span>",
                                row.totalReadCount,
                                "<span class='" +row.properlyPpairedWarning+"'>" + row.properlyPaired + " </span>",
                                row.singletons,
                                row.standardDeviationPE_Insertsize,
                                "<span class='" + row.medianWarning+"'>" + row.medianPE_insertsize + " </span>",
                                row.meanPE_Insertsize,
                                row.workflow.replace("DEFAULT_OTP", "bwa aln").replace("PANCAN_ALIGNMENT", "PanCan"),
                                row.dateFromFileSystem,
                            ];
                            var rowdata = identifier.concat(coverage, general);

                            if (row.withdrawn) {
                                $("#withdrawn_description").show()
                                var withdrawnRow = []
                                $.each(rowdata, function() {
                                    withdrawnRow.push("<span class='withdrawn'>" + this + " </span>")
                                })
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
