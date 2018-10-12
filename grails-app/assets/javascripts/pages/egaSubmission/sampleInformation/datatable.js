/*jslint browser: true */
/*global $ */

$.otp.sampleInformationTable = {

    makeDataTable: function () {
        "use strict";
        $('#dataTable').dataTable({
            sDom: '<i> T rt<"clear">',
            bSort: false,
            paging: false
        });
    }
};
