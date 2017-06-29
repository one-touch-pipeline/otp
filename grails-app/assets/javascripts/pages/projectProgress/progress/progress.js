/*jslint browser: true */
/*global $ */

$.otp.projectProgressTable = {
    registerProjectProgressId : function () {
        "use strict";
        $('.datePicker').datepicker({
                dateFormat: 'yy-mm-dd',
                maxDate: $('#endDate').val()
        });
        var oTableProgress = $("#progressId").dataTable({
            sDom: '<i> B rt<"clear">',
            buttons: $.otp.tableButtons,
            bFilter : true,
            bProcessing : true,
            bServerSide : false,
            bSort : true,
            bJQueryUI : false,
            bAutoWidth : true,
            sAjaxSource : $.otp.createLink({
                controller : 'projectProgress',
                action : 'dataTableSource'
            }),
            bPaginate : false,
            sScrollY:  'auto',
            sScrollX: 'auto',
            bScrollCollapse : false,
            bDeferRender : true,
            fnServerData : function (sSource, aoData, fnCallback) {
                aoData.push({
                    name : "projects",
                    value : $('#projects').val()
                    });
                aoData.push({
                    name : "startDate",
                    value : $('#startDate').val()
                    });
                aoData.push({
                    name : "endDate",
                    value : $('#endDate').val()
                    });
                $.ajax({
                    "dataType" : 'json',
                    "type" : "POST",
                    "url" : sSource,
                    "data" : aoData,
                    "error" : function () {
                        // clear the table
                        fnCallback({aaData : [], iTotalRecords : 0, iTotalDisplayRecords : 0});
                        oTableProgress.fnSettings().oFeatures.bServerSide = false;
                    },
                    "success": function (json) {
                        var i, j, rowData, row;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            var samples = []
                            for (j=0;j<row[4].length; j++) {
                                samples.push($.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    id: row[4][j][0],
                                    text: row[4][j][1]
                                }));
                            }
                            rowData = [
                                $.otp.createLinkMarkup({
                                    controller: 'run',
                                    action: 'show',
                                    id: row[0],
                                    title: row[2],
                                    text: row[2]
                                    }),
                                    row[3],
                                    samples
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            }
        });
        $('#display').click(function () {
            var oSettings1 = oTableProgress.fnSettings();
            oSettings1.oFeatures.bServerSide = true;
            oTableProgress.dataTable().fnDraw();
        });
    }
};
