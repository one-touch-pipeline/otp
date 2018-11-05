/*jslint browser: true */
/*global $ */

$.otp.egaTable = {

    makeDataTable: function () {
        "use strict";
        $('#dataTable').dataTable({
            sDom: '<i> T rt<"clear">',
            bSort: false,
            paging: false
        });
    }
};
