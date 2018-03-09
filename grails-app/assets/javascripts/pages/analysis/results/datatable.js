/*jslint browser: true */
/*global $ */

$.otp.resultsTable = {

    registerDataTable: function (tableElement, source, convertRowData) {
        "use strict";
        var oTable = tableElement.dataTable({
            sDom: '<i> B rt<"clear">',
            buttons: $.otp.tableButtons,
            bFilter: true,
            bProcessing: true,
            bServerSide: false,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: source,
            bScrollCollapse: true,
            sScrollY: 'auto',
            sScrollX: 'auto',
            bPaginate: false,
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "project.id",
                    value : $('#project').val()
                });
                $.ajax({
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": aoData,
                    "error": function () {
                        // clear the table
                        fnCallback({aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0});
                        oTable.fnSettings().oFeatures.bServerSide = false;
                    },
                    "success": function (json) {
                        var i;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            json.aaData[i] = convertRowData(json.aaData[i]);
                        }
                        fnCallback(json);
                        oTable.fnSettings().oFeatures.bServerSide = false;
                    }
                });
            },
            fnInitComplete: function () {
                new $.fn.dataTable.FixedColumns( this, {
                    "leftColumns": 2
                } );
            }
        });
        return oTable;
    },

    registerAceseq: function () {
        "use strict";
        $.otp.resultsTable.registerDataTable(
            $('#resultsTable'),
            $.otp.createLink({
                controller: 'aceseq',
                action: 'dataTableResults'
            }),
            function (row) {
                var result = [
                    $.otp.createLinkMarkup({
                        controller: 'individual',
                        action: 'show',
                        id: row.individualId,
                        text: row.individualPid,
                    }),
                    row.sampleType1 + " \u2013 " + row.sampleType2,
                    row.tcc,
                    row.ploidy,
                    row.ploidyFactor,
                    row.goodnessOfFit,
                    row.gender,
                    row.solutionPossible,
                    row.pluginVersion,
                    row.dateCreated,
                    row.processingState,
                ];
                if (row.instanceId) {
                    result.push(
                        $.otp.createLinkMarkup({
                            controller: 'aceseq',
                            action: 'plots',
                            parameters: {'bamFilePairAnalysis.id': row.instanceId, 'plotType': 'ACESEQ_ALL'},
                            text: 'Plots'
                        })
                    );
                } else {
                    result.push("")
                }
                return result;
            }
        );
    },

    registerIndel: function () {
        "use strict";
        $.otp.resultsTable.registerDataTable(
            $('#resultsTable'),
            $.otp.createLink({
                controller: 'indel',
                action: 'dataTableResults'
            }),
            function (row) {
                var plotIndel, plotTinda;
                if (row.instanceId) {
                    plotIndel = $.otp.createLinkMarkup({
                        controller: 'indel',
                        action: 'plots',
                        parameters: {'bamFilePairAnalysis.id': row.instanceId, 'plotType': 'INDEL'},
                        text: 'Plots'
                    });
                    plotTinda = $.otp.createLinkMarkup({
                        controller: 'indel',
                        action: 'plots',
                        parameters: {'bamFilePairAnalysis.id': row.instanceId, 'plotType': 'INDEL_TINDA'},
                        text: 'Plot'
                    });
                } else {
                    plotIndel = "";
                    plotTinda = "";
                }

                var result = [
                    $.otp.createLinkMarkup({
                        controller: 'individual',
                        action: 'show',
                        id: row.individualId,
                        text: row.individualPid,
                    }),
                    row.sampleType1 + " \u2013 " + row.sampleType2,
                    row.seqType,
                    row.libPrepKits,
                    row.numIndels,
                    row.numIns,
                    row.numDels,
                    row.numSize1_3,
                    row.numSize4_10,
                    plotIndel,
                    row.somaticSmallVarsInTumor,
                    row.somaticSmallVarsInControl,
                    row.somaticSmallVarsInTumorCommonInGnomad,
                    row.somaticSmallVarsInControlCommonInGnomad,
                    row.somaticSmallVarsInTumorPass,
                    row.somaticSmallVarsInControlPass,
                    row.tindaSomaticAfterRescue,
                    row.tindaSomaticAfterRescueMedianAlleleFreqInControl,
                    plotTinda,
                    row.pluginVersion,
                    row.dateCreated,
                    row.processingState.name,];
                return result;
            }
        );
    },

    registerSophia: function () {
        "use strict";
        $.otp.resultsTable.registerDataTable(
            $('#resultsTable'),
            $.otp.createLink({
                controller: 'sophia',
                action: 'dataTableResults'
            }),
            function (row) {
                var result =  [
                    $.otp.createLinkMarkup({
                        controller: 'individual',
                        action: 'show',
                        id: row.individualId,
                        text: row.individualPid,
                    }),
                    row.sampleType1 + " \u2013 " + row.sampleType2,

                    row.controlMassiveInvPrefilteringLevel,
                    row.tumorMassiveInvFilteringLevel,
                    row.rnaContaminatedGenesCount,
                    row.rnaDecontaminationApplied,
                    row.pluginVersion,
                    row.dateCreated,
                    row.processingState,
                ];
                if (row.instanceId) {
                    result.push(
                        $.otp.createLinkMarkup({
                            controller: 'sophia',
                            action: 'plots',
                            parameters: {'bamFilePairAnalysis.id': row.instanceId, 'plotType': 'SOPHIA'},
                            text: 'Plots'
                        })
                    );
                } else {
                    result.push("")
                }
                result.push(row.rnaContaminatedGenesMoreThanTwoIntron);
                return result;

            }
        );
    },

    registerSnv: function () {
        "use strict";
        $.otp.resultsTable.registerDataTable(
            $('#resultsTable'),
            $.otp.createLink({
                controller: 'snv',
                action: 'dataTableResults'
            }),
            function (row) {
                var result = [
                    $.otp.createLinkMarkup({
                        controller: 'individual',
                        action: 'show',
                        id: row.individualId,
                        text: row.individualPid,
                    }),
                    row.sampleType1 + " \u2013 " + row.sampleType2,
                    row.seqType,
                    row.libPrepKits,
                    row.pluginVersion,
                    row.dateCreated,
                    row.processingState,
                ];
                if (row.instanceId) {
                    result.push(
                        $.otp.createLinkMarkup({
                            controller: 'snv',
                            action: 'plots',
                            parameters: {'bamFilePairAnalysis.id': row.instanceId, 'plotType': 'SNV'},
                            text: 'Plots'
                        })
                    );
                } else {
                    result.push("")
                }
                return result;
            }
        );
    }
};
