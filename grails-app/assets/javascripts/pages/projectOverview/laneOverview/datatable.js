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
            bAutoWidth : true,
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
                        if (json.anythingWithdrawn) {
                            $("#withdrawn_description").show();
                        }
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
                            if ($('#project').val() != "MMML" && $('#project').val() != "MMML_XP" && $('#project').val() != "MMML_RARE_LYMPHOMA_XP" && $('#project').val() != "MMML_RARE_LYMPHOMA_EXOMES") {
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
            },

            fnInitComplete: function () {
                $.otp.projectOverviewTable.hideEmptyColumns(this);
                new $.fn.dataTable.FixedColumns( this, {
                    "leftColumns": 2
                } );
            }
        });

        $.otp.dataTableFilter.register($('#searchCriteriaTable'), oTableLaneOverview, false, function () {
            var tableSize = $('#laneOverviewId').find('thead > tr > th').size(),
                ignoredColumns = parseInt($('#laneOverviewId').data('ignore-filter-columns')),
                workflowSize = parseInt($('#laneOverviewId').data('workflow-size')),
                result = [],
                i;

            $('#searchCriteriaTable').find('tr').each(function (index, element) {
                var idx = $('td.attribute select', element)[0].selectedIndex;
                if (idx !== 0) {
                    result.push((idx-1) * (workflowSize + 1) + ignoredColumns);
                }
            });

            for (i = ignoredColumns - 1; i < tableSize; i += 1) {
                if (result.indexOf(i) !== -1) {
                    oTableLaneOverview.fnFilter('.+', i, true);
                } else {
                    oTableLaneOverview.fnFilter('', i);
                }
            }
        });

        $.otp.dataTableFilter.register($('#searchCriteriaTable3'), oTableLaneOverview, false, function () {
            var select = $('#searchCriteriaTable3').find('select')[0],
                column = 1;
            if (select.selectedIndex !== 0) {
                oTableLaneOverview.fnFilter('^' + select.value + '$', column, true);
            } else {
                oTableLaneOverview.fnFilter('', column);

            }
        });
        return oTableLaneOverview;
    },

    hideEmptyColumns : function (tableObject) {
        "use strict";
        var selector = tableObject.selector;
        var columnsToHide = [];

        $(selector).find('tr:nth-child(2)').find('th').each(function(i) {
            var columnIndex = $(this).index();
            var rows = $(this).parents('table').find('tr td:nth-child(' + (i + 1) + ')');
            var rowsLength = $(rows).length;
            var emptyRows = true;

            rows.each(function(r) {
                if (this.innerHTML != '')
                    emptyRows = false;
            });

            if(emptyRows) {
                columnsToHide.push(columnIndex);
            }
        });

        for(var i=0; i< columnsToHide.length; i++) {
            tableObject.fnSetColumnVis( columnsToHide[i], false );
        }

        tableObject.fnAdjustColumnSizing();


    },
};
