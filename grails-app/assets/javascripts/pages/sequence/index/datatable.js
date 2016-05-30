/*jslint browser: true */
/*global $ */

//A download button, copied from
//http://www.datatables.net/extensions/tabletools/plug-ins
TableTools.BUTTONS.download = {
    "sAction": "text",
    "sTag": "default",
    "sFieldBoundary": "",
    "sFieldSeperator": "\t",
    "sNewLine": "<br>",
    "sToolTip": "",
    "sButtonClass": "DTTT_button_text",
    "sButtonClassHover": "DTTT_button_text_hover",
    "sButtonText": "Download",
    "mColumns": "all",
    "bHeader": true,
    "bFooter": true,
    "sDiv": "",
    "fnMouseover": null,
    "fnMouseout": null,
    "fnClick": function( nButton, oConfig ) {
        var oParams = this.s.dt.oApi._fnAjaxParameters( this.s.dt );
        var iframe = document.createElement('iframe');
        iframe.style.height = "0px";
        iframe.style.width = "0px";
        iframe.src = oConfig.sUrl+"?" + $.param(oParams);
        document.body.appendChild( iframe );
    },
    "fnSelect": null,
    "fnComplete": null,
    "fnInit": null
};


$.otp.sequence = {

    register: function () {
        "use strict";
        var searchCriteria = $.otp.dataTableFilter.register($("#searchCriteriaTable"), $("#sequenceTable"), true);

        $("#sequenceTable").dataTable({
            sDom: '<i> T rt<"clear">S',
            oTableTools : {
                sSwfPath: $.otp.tableTools.sSwfPath,
                aButtons: [{
                               "sExtends": "download",
                               "sButtonText": "Download CSV",
                               "sToolTip": "Attention: Download can take some minutes",
                               "sUrl": $.otp.createLink({
                                   controller: 'sequence',
                                   action: 'exportAll',
                               }),
                               "fnClick": function( nButton, oConfig ) {
                                   var oParams = this.s.dt.oApi._fnAjaxParameters( this.s.dt );
                                   var iframe = document.createElement('iframe');
                                   iframe.style.height = "0px";
                                   iframe.style.width = "0px";
                                   iframe.src = oConfig.sUrl + "?filtering=" + JSON.stringify(searchCriteria()) +"&" + $.param(oParams);
                                   document.body.appendChild( iframe );
                               },
                           }]
            },
            bFilter: false,
            bProcessing: true,
            bServerSide: true,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'sequence',
                action: 'dataTableSource'
            }),
            bScrollCollapse: true,
            sScrollX: 'auto',
            sScrollXInner: "100%",
            sScrollY: 490,
            iDisplayLength: 100,
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "filtering",
                    value: JSON.stringify(searchCriteria())
                });
                $.ajax({
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": aoData,
                    scroller: {
                        loadingIndicator: true
                    },
                    "error": function () {
                        fnCallback({aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0});
                    },
                    "success": function (json) {
                        var i, j, rowData, row, fastQC;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            if (row.fastQCFiles !== undefined) {
                                fastQC = "";
                                for (j = 0; j < row.fastQCFiles.length; j += 1) {
                                    fastQC += $.otp.createLinkMarkup({
                                        controller: 'fastqcResults',
                                        action: 'show',
                                        id: row.fastQCFiles[j].id,
                                        text: $L("sequence.list.numberedFastQCFile", (j + 1))
                                    });
                                    fastQC += " ";
                                }
                            } else {
                                fastQC = row.fastqcState.name;
                            }
                            rowData = [
                                $.otp.createLinkMarkup({
                                    controller: 'projectOverview',
                                    action: 'index',
                                    parameters: {
                                                projectName: row.projectName
                                    },
                                    text: row.projectName,
                                    title: row.projectName
                                }),

                                $.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    id: row.individualId,
                                    title: row.mockPid,
                                    text: row.mockPid
                                }),
                                row.sampleTypeName,
                                row.seqTypeDisplayName,
                                row.libraryLayout,
                                row.seqCenterName,
                                $.otp.createLinkMarkup({
                                    controller: 'run',
                                    action: 'show',
                                    id: row.runId,
                                    title: row.name,
                                    text: row.name
                                }),
                                row.laneId,
                                row.libraryName,
                                fastQC,
                                row.dateCreated
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            },
            fnRowCallback: function (nRow) {
                var fastqc;
                fastqc = $("td:eq(9)", nRow);
                if ($("a", fastqc).length > 0) {
                    fastqc.addClass("true");
                } else {
                    fastqc.attr("title", fastqc.text());
                    fastqc.addClass("false");
                    fastqc.text("");
                }
            }
        });
    }
};
