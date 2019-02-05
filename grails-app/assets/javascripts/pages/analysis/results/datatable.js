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
                    row.sampleTypes,
                    row.tcc,
                    row.ploidy,
                    row.ploidyFactor,
                    row.goodnessOfFit,
                    row.gender,
                    row.solutionPossible,
                    row.version,
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
                    row.sampleTypes,
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
                    row.version,
                    row.dateCreated,
                    row.processingState,];
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
                    row.sampleTypes,
                    row.seqType,
                    row.controlMassiveInvPrefilteringLevel,
                    row.tumorMassiveInvFilteringLevel,
                    row.rnaContaminatedGenesCount,
                    row.rnaDecontaminationApplied,
                    row.version,
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
                    row.sampleTypes,
                    row.seqType,
                    row.libPrepKits,
                    row.version,
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
    },
    registerRunYapsa: function () {
        "use strict";
        $.otp.resultsTable.registerDataTable(
            $('#resultsTable'),
            $.otp.createLink({
                controller: 'runYapsa',
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
                    row.sampleTypes,
                    row.seqType,
                    row.libPrepKits,
                    row.version,
                    row.dateCreated,
                    row.processingState,
                ];
                result.push("")
                return result;
            }
        );
    }
};
