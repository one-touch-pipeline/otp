/*jslint browser: true */
/*global $ */

$.otp.projectOverviewTable = {
    registerLaneOverviewId : function () {
        "use strict";
        var oTableLaneOverview = $("#laneOverviewId").dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools : $.otp.tableTools,
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
                        for (var i = 0; i < json.aaData.length; i += 1) {
                            var row = json.aaData[i];
                            var mockPid = row[0];
                                row[0] = $.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    text: row[0],
                                    parameters: {
                                        mockPid: mockPid
                                    }
                                });
                            if ($('#project').val()!= "MMML") {
                                row[2] = $.otp.createLinkMarkup({
                                        controller: 'individual',
                                        action: 'show',
                                        text: row[2],
                                        parameters: {
                                            mockPid: mockPid
                                        }
                                    });
                            }
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

        $.otp.dataTableFilter.register($('.searchCriteriaTableSampleTypes'), oTableLaneOverview, false, function () {
            var select = $('.searchCriteriaTableSampleTypes').find('select')[0],
                column = 1;
            if (select.selectedIndex !== 0) {
                oTableLaneOverview.fnFilter('^' + select.value + '$', column, true);
            } else {
                oTableLaneOverview.fnFilter('', column);
            }
        });
        return oTableLaneOverview;
    }
};