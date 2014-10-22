/*jslint browser: true */
/*global $ */

$.otp.sampleTypeCombinationPerIndividual = {
        /*
         * The function return only the passed value.
         * It can be used for not modified output.
         */
    registersamplePairForSnvProcessingSNV : function (selector, url, successUpdate) {
        "use strict";
        var oTablesamplePairForSnvProcessingSNV = $(selector).dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools : $.otp.tableTools,
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
         $.otp.sampleTypeCombinationPerIndividual.registersamplePairForSnvProcessingSNV(
                 '#samplePairForSnvProcessingSnvFinished',
                 $.otp.createLink({
                     controller: 'sampleTypeCombinationPerIndividual',
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
                             row.sampleTypeCombinationPath,
                         ];
                         json.aaData[i] = rowData;
                     }
                     return json
                 }
             );
         $.otp.sampleTypeCombinationPerIndividual.registersamplePairForSnvProcessingSNV(
                 '#samplePairForSnvProcessingSNVInProgress',
                 $.otp.createLink({
                     controller: 'sampleTypeCombinationPerIndividual',
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
         $.otp.sampleTypeCombinationPerIndividual.registersamplePairForSnvProcessingSNV(
                 '#notStartedsamplePairs',
                 $.otp.createLink({
                     controller: 'sampleTypeCombinationPerIndividual',
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
         $.otp.sampleTypeCombinationPerIndividual.registersamplePairForSnvProcessingSNV(
                 '#disabledSamplePairs',
                 $.otp.createLink({
                     controller: 'sampleTypeCombinationPerIndividual',
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