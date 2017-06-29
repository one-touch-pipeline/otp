/*jslint browser: true */
/*global $ */

$.otp.projectOverviewTable = {
    registerMMMLMappingId : function () {
        "use strict";
        var oTableMMMLMapping = $("#overviewTableMMMLMappingId").dataTable({
            sDom: '<i> B rt<"clear">',
            buttons: $.otp.tableButtons,
            bFilter : true,
            bProcessing : true,
            bServerSide : false,
            bSort : true,
            bJQueryUI : false,
            bAutoWidth : false,
            sAjaxSource : $.otp.createLink({
                controller : 'projectOverview',
                action : 'dataTableMMMLMapping'
            }),
            bPaginate : false,
            sScrollY:  540,
            sScrollX: 'auto',
            bScrollCollapse : false,
            bDeferRender : true,
            fnServerData : function (sSource, aoData, fnCallback) {
                $.ajax({
                    "dataType" : 'json',
                    "type" : "POST",
                    "url" : sSource,
                    "data" : aoData,
                    "error" : function () {
                        // clear the table
                        fnCallback({aaData : [], iTotalRecords : 0, iTotalDisplayRecords : 0});
                    },
                    "success" : function (json) {
                        for (var i = 0; i < json.aaData.length; i += 1) {
                            var row = json.aaData[i];
                            var mockPid = row[1];
                            row[0] = $.otp.createLinkMarkup({
                                controller: 'individual',
                                action: 'show',
                                text: row[0],
                                parameters: {
                                    mockPid: mockPid
                                }
                            });
                            row[1] = $.otp.createLinkMarkup({
                                controller: 'individual',
                                action: 'show',
                                text: row[1],
                                parameters: {
                                    mockPid: mockPid
                                }
                            });
                            row[2] = $.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    text: row[2],
                                    parameters: {
                                        mockPid: mockPid
                                    }
                                });
                        }
                        fnCallback(json);
                    }
                });
            }
        });
        return oTableMMMLMapping;
    }
};
