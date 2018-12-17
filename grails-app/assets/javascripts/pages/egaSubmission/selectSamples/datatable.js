/*jslint browser: true */
/*global $ */

$.otp.selectSamplesTable = {

    selectSamples: function () {
        "use strict";
        var oTable = $('#selectSamplesTable').dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools: $.otp.tableTools,
            bFilter: true,
            bProcessing: true,
            bServerSide: false,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'egaSubmission',
                action: 'dataTableSelectSamples'
            }),
            bScrollCollapse: true,
            sScrollY: ($(window).height() - 480),
            bPaginate: false,
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "project",
                    value: $('#projectName').text()
                });
                $.ajax({
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": aoData,
                    "error": function () {
                        // clear the table
                        fnCallback({aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0});
                    },
                    "success": function (json) {
                        for (var i = 0; i < json.aaData.length; i += 1) {
                            var column = json.aaData[i];
                            column[0] = '<input type="checkbox" name="sampleAndSeqType" value="'+ column[0] +'"/>';
                        }
                        fnCallback(json);
                    }
                });
            }
        });

        $.otp.dataTableFilter.register($('#searchCriteriaTableSeqType'), oTable, false, function () {
            var select = $('#searchCriteriaTableSeqType').find('select')[0],
                column = 3;
            if (select.selectedIndex !== 0) {
                oTable.fnFilter('^' + select.value + '$', column, true);
            } else {
                oTable.fnFilter('', column);
            }
        });

        return oTable;
    }
};
