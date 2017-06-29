$.otp.sampleIdentifierOverviewTable = {

    registerDataTable: function (selector, url) {
        "use strict";
        var oTable = $(selector).dataTable({
            sDom: '<i> B rt<"clear">',
            buttons: $.otp.tableButtons,
            bFilter: true,
            bProcessing: true,
            bServerSide: false,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: url,
            bScrollCollapse: true,
            sScrollY: 500,
            bPaginate: false,
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "project",
                    value: $('#project').find('option:selected').text()
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
        var oTable = $.otp.sampleIdentifierOverviewTable.registerDataTable(
            '#sampleIdentifierOverviewTable',
            $.otp.createLink({
                controller: 'sampleIdentifierOverview',
                action: 'dataTableSourceListSampleIdentifierByProject'
            })
        );
        $('#project_select').change(function () {
            var oSettings1 = oTable.fnSettings();
            oSettings1.oFeatures.bServerSide = true;
            oTable.fnDraw();
        });
    },
};
