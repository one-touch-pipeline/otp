/*jslint browser: true */
/*global $ */

$.otp.samplePair = {
        /*
         * The function return only the passed value.
         * It can be used for not modified output.
         */
    registerSamplePairForSnvProcessingSNV : function (selector, url, successUpdate) {
        "use strict";
        var oTableSamplePairForSnvProcessingSNV = $(selector).dataTable({
            sDom: '<i> B rt<"clear">',
            buttons: $.otp.tableButtons,
            bFilter : true,
            bProcessing : true,
            bServerSide : false,
            bSort : true,
            bJQueryUI : false,
            bAutoWidth : false,
            sAjaxSource : url,
            bPaginate : false,
            sScrollY:  'auto',
            bScrollCollapse : false,
            bDeferRender : true,
            fnServerData : function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "mockPid",
                    value: $("#mockPid")[0].value
                });
                $.ajax({
                    "dataType" : 'json',
                    "type" : "POST",
                    "url" : sSource,
                    "data" : aoData,
                    "error" : function () {
                        // clear the table
                        fnCallback({aaData : [], iTotalRecords : 0, iTotalDisplayRecords : 0});
                    },
                    "success": function (json) {
                        json = successUpdate(json)
                        fnCallback(json);
                    }
                });
            }
        });
    },
    register: function () {
         "use strict";
         $.otp.samplePair.registerSamplePairForSnvProcessingSNV(
                 '#samplePairForSnvProcessingSnvFinished',
                 $.otp.createLink({
                     controller: 'samplePair',
                     action: 'dataTableSNVFinishedSamplePairs'
                 }),
                 function (json) {
                     var i, rowData, row;
                     for (i = 0; i < json.aaData.length; i += 1) {
                         row = json.aaData[i];
                         rowData = [
                             row.sampleType1,
                             row.sampleType2,
                             row.seqType,
                             row.lastUpdated,
                             row.samplePairPath,
                         ];
                         json.aaData[i] = rowData;
                     }
                     return json
                 }
             );
         $.otp.samplePair.registerSamplePairForSnvProcessingSNV(
                 '#samplePairForSnvProcessingSNVInProgress',
                 $.otp.createLink({
                     controller: 'samplePair',
                     action: 'dataTableSNVInprogressSamplePairs'
                 }),
                 function (json) {
                     var i, rowData, row;
                     for (i = 0; i < json.aaData.length; i += 1) {
                         row = json.aaData[i];
                         rowData = [
                             row.sampleType1,
                             row.sampleType2,
                             row.seqType,
                             row.dateCreated,
                         ];
                         json.aaData[i] = rowData;
                     }
                     return json
                 }
             );
         $.otp.samplePair.registerSamplePairForSnvProcessingSNV(
                 '#notStartedSamplePairs',
                 $.otp.createLink({
                     controller: 'samplePair',
                     action: 'dataTableSNVNotStartedSamplePairs'
                 }),
                 function (json) {
                     var i, rowData, row;
                     for (i = 0; i < json.aaData.length; i += 1) {
                         row = json.aaData[i];
                         rowData = [
                             row.sampleType1,
                             row.laneCount1,
                             row.coverage1,
                             row.sampleType2,
                             row.laneCount2,
                             row.coverage2,
                             row.seqType,
                         ];
                         json.aaData[i] = rowData;
                     }
                     return json
                 }
             );
         $.otp.samplePair.registerSamplePairForSnvProcessingSNV(
                 '#disabledSamplePairs',
                 $.otp.createLink({
                     controller: 'samplePair',
                     action: 'dataTableSNVProcessingDisabledSamplePairs'
                 }),
                 function (json) {
                     var i, rowData, row;
                     for (i = 0; i < json.aaData.length; i += 1) {
                         row = json.aaData[i];
                         rowData = [
                             row.sampleType1,
                             row.sampleType2,
                             row.seqType,
                         ];
                         json.aaData[i] = rowData;
                     }
                     return json
                 }
             );
         }
};