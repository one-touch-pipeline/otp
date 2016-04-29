/*jslint browser: true */
/*global $ */

$.otp.snvResultsTable = {

    registerDataTable: function (successUpdate) {
        "use strict";
        var oTable = $('#snvResultsTable').dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools : $.otp.tableTools,
            bFilter: true,
            bProcessing: true,
            bServerSide: false,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'snv',
                action: 'dataTableSnvResults'
            }),
            bScrollCollapse: true,
            sScrollY: 600,
            bPaginate: false,
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "project",
                    value : $('#projectName').val()
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
                        json = successUpdate(json)
                        fnCallback(json);
                        oTable.fnSettings().oFeatures.bServerSide = false;
                    }
                });
            }
        });
        return oTable;
    },

    register: function () {
        "use strict";
        var oTable = $.otp.snvResultsTable.registerDataTable(
            function (json) {
                var i, rowData, row;
                for (i = 0; i < json.aaData.length; i += 1) {
                    row = json.aaData[i];
                    rowData = [
                        $.otp.createLinkMarkup({
                            controller: 'individual',
                            action: 'show',
                            id: row.individualId,
                            text: row.individualPid,
                        }),
                        row.sampleType1 + " - " + row.sampleType2,
                        row.seqType,
                        row.libPrepKits,
                        row.snvInstanceName,
                        row.snvProcessingState.name,
                        $.otp.createLinkMarkup({
                            controller: 'snv',
                            action: 'plots',
                            id: row.snvInstanceId,
                            text: 'Plots'
                        }),
                    ];
                    json.aaData[i] = rowData;
                }
                return json;
            }
        );
    },
};
