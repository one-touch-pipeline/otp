/*jslint browser: true */
/*global $ */

$.otp.projectOverviewTable = {
    registerMMMLMappingId : function () {
        "use strict";
        var oTableMMMLMapping = $("#overviewTableMMMLMappingId").dataTable({
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
                        fnCallback(json);
                    }
                });
            }
        });
        return oTableMMMLMapping;
    }
};
