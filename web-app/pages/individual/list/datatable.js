/*jslint browser: true */
/*global $ */

/**
 * Generates the list of individuals
 **/
$.otp.individualList = function () {
    "use strict";
    $.otp.individual.register();
    $.otp.resizeBodyInit('#individualTable', 180);
};

$.otp.individual = {
    register: function () {
        "use strict";
        var searchCriteria = $.otp.dataTableFilter.register($("#searchCriteriaTable"), $("#individualTable"), true);

        $("#individualTable").dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools: {
                sSwfPath: $.otp.contextPath + "/js/jquery/tableTools/media/swf/copy_cvs_xls_pdf.swf",
                aButtons: $.otp.tableTools_button_options
            },
            bFilter: true,
            bProcessing: true,
            bServerSide: true,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'individual',
                action: 'dataTableSource'
            }),
            bPaginate: false,
            bScrollCollapse: true,
            sScrollY: ($(window).height() - 453),
            iDisplayLength: Math.round(($('.body').height() - 200) / 23),
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "filtering",
                    value: JSON.stringify(searchCriteria())
                });
                aoData.push({
                    name: "filter",
                    value: $.otp.individual.search
                });
                $.ajax({
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": aoData,
                    "error": function () {
                        fnCallback({aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0});
                    },
                    "success": function (json) {
                        var i, j, rowData, row;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            rowData = [
                                $.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    id: row.id,
                                    text: row.pid
                                }),
                                row.mockFullName,
                                row.mockPid,
                                row.project,
                                row.type
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            }
        });
    }
};
