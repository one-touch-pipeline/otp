/*jslint browser: true */
/*global $ */

$.otp.resultsTable = {

    registerDataTable: function (tableElement, source, convertRowData) {
        "use strict";
        var oTable = tableElement.dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools : $.otp.tableTools,
            bFilter: true,
            bProcessing: true,
            bServerSide: false,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: source,
            bScrollCollapse: true,
            sScrollY: 600,
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
                    row.normalContamination,
                    row.ploidy,
                    row.ploidyFactor,
                    row.goodnessOfFit,
                    row.gender,
                    row.solutionPossible,

                    row.dateCreated,
                    row.aceseqProcessingState.name,
                ];
                if (row.aceseqInstanceId) {
                    result.push(
                        $.otp.createLinkMarkup({
                            controller: 'aceseq',
                            action: 'plots',
                            parameters: {'aceseqInstance.id': row.aceseqInstanceId},
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
                    row.dateCreated,
                    row.indelProcessingState.name,
                ];
                if (row.indelInstanceId) {
                    result.push(
                        $.otp.createLinkMarkup({
                            controller: 'indel',
                            action: 'plots',
                            parameters: {'indelCallingInstance.id': row.indelInstanceId},
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

    registerSophia: function () {
        "use strict";
        $.otp.resultsTable.registerDataTable(
            $('#resultsTable'),
            $.otp.createLink({
                controller: 'sophia',
                action: 'dataTableResults'
            }),
            function (row) {
                return [
                    $.otp.createLinkMarkup({
                        controller: 'individual',
                        action: 'show',
                        id: row.individualId,
                        text: row.individualPid,
                    }),
                    row.sampleType1 + " \u2013 " + row.sampleType2,
                    row.dateCreated,
                    row.sophiaProcessingState.name,
                ];
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
                    row.dateCreated,
                    row.snvProcessingState.name,
                ];
                if (row.snvInstanceId) {
                    result.push(
                        $.otp.createLinkMarkup({
                            controller: 'snv',
                            action: 'plots',
                            parameters: {'snvCallingInstance.id': row.snvInstanceId},
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
