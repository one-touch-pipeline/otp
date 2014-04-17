/*jslint browser: true */
/*global $ */

$.otp.projectOverviewTable = {
    registerLaneOverviewId : function () {
        "use strict";
        var oTableLaneOverview = $("#laneOverviewId").dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools : {
                sSwfPath : $.otp.contextPath
                    + "/js/jquery/tableTools/media/swf/copy_cvs_xls_pdf.swf",
                aButtons : tableTools_button_options
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
                        fnCallback(json);
                        oTableLaneOverview.fnSettings().oFeatures.bServerSide = false;
                    }
                });
            }
        });
        return oTableLaneOverview;
    }
};
