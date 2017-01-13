/*jslint browser: true */
/*global $ */

$.otp.indelResultsTable = {

    registerDataTable: function (successUpdate) {
        "use strict";
        var oTable = $('#indelResultsTable').dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools : $.otp.tableTools,
            bFilter: true,
            bProcessing: true,
            bServerSide: false,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'indel',
                action: 'dataTableIndelResults'
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
        var oTable = $.otp.indelResultsTable.registerDataTable(
            function (json) {
                var i, rowData, row;
                for (i = 0; i < json.aaData.length; i += 1) {
                    row = json.aaData[i];
                    rowData = [
                        $.otp.createLinkMarkup({
                            controller: 'individual',
                            action: 'show',
                            parameters: {'indelCallingInstance.id': row.individualId},
                            text: row.individualPid,
                        }),
                        row.sampleType1 + " \u2013 " + row.sampleType2,
                        row.seqType,
                        row.libPrepKits,
                        row.dateCreated,
                        row.indelProcessingState.name,
                    ];
                    if (row.indelInstanceId) {
                        rowData.push(
                            $.otp.createLinkMarkup({
                                controller: 'indel',
                                action: 'plots',
                                parameters: {'indelCallingInstance.id': row.indelInstanceId},
                                text: 'Plots'
                            })
                        );
                    } else {
                        rowData.push("")
                    }
                    json.aaData[i] = rowData;
                }
                return json;
            }
        );
    },
};
