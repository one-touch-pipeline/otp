/*jslint browser: true */
/*global $ */

$.otp.projectOverviewTable = {
    registerLaneOverviewId : function () {
        "use strict";
        var oTableLaneOverview = $("#laneOverviewId").dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools : {
                sSwfPath: $.otp.contextPath + "/js/jquery/tableTools/media/swf/copy_cvs_xls_pdf.swf",
                aButtons: $.otp.tableTools_button_options
            },
            bFilter : true,
            bProcessing : true,
            bServerSide : false,
            bSort : true,
            bJQueryUI : false,
            bAutoWidth : false,
            sAjaxSource : $.otp.createLink({
                controller : 'projectOverview',
                action : 'dataTableSourceLaneOverview'
            }),
            bPaginate : false,
            sScrollY:  410,
            sScrollX: 'auto',
            bScrollCollapse : false,
            bDeferRender : true,
            fnServerData : function (sSource, aoData, fnCallback) {
                aoData.push({
                    name : "project",
                    value : $('#project').val()
                });
                $.ajax({
                    "dataType" : 'json',
                    "type" : "POST",
                    "url" : sSource,
                    "data" : aoData,
                    "error" : function () {
                        // clear the table
                        fnCallback({aaData : [], iTotalRecords : 0, iTotalDisplayRecords : 0});
                        oTableLaneOverview.fnSettings().oFeatures.bServerSide = false;
                    },
                    "success" : function (json) {
                        var i, j, rowData, row;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            if ($('#project').val()!= "MMML") {
                                row[2] = $.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    text: row[2],
                                    parameters: {
                                        mockPid: row[0]
                                    }
                                });
                            }
                            row[0] = $.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    text: row[0],
                                    parameters: {
                                        mockPid: row[0]
                                    }
                                });
                        }
                        fnCallback(json);
                        oTableLaneOverview.fnSettings().oFeatures.bServerSide = false;
                    }
                });
            }
        });

        $.otp.dataTableFilter.register($('#searchCriteriaTable'), oTableLaneOverview, false, function () {
            var tableSize = $('#laneOverviewId').find('thead > tr > th').size(),
                ignoredColumns = $('#laneOverviewId').data('ignore-filter-columns'),
                result = [],
                i;

            $('#searchCriteriaTable').find('tr').each(function (index, element) {
                var idx = $('td.attribute select', element)[0].selectedIndex;
                if (idx !== 0) {
                    result.push(idx + ignoredColumns - 1);
                }
            });

            for (i = 0; i < tableSize; i += 1) {
                if (result.indexOf(i) !== -1) {
                    oTableLaneOverview.fnFilter('.+', i, true);
                } else {
                    oTableLaneOverview.fnFilter('', i);
                }
            }
        });
        return oTableLaneOverview;
    }
};
