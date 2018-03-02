/*jslint browser: true */
/*global $ */

$.otp.sequence = {

    register: function () {
        "use strict";
        var searchCriteria = $.otp.dataTableFilter.register($("#searchCriteriaTable"), $("#sequenceTable"), true);

        var link = $.otp.createLink({
            controller: 'sequence',
            action: 'exportAll',
        });

        $("#sequenceTable").dataTable({
            dom: '<i> B rt<"clear">S',
            buttons: [ {
                extend: 'csv',
                titleAttr: 'Attention: Download can take a while',
                action: function ( e, dt, node, config ) {
                    var iframe = document.createElement('iframe');
                    iframe.style.height = "0px";
                    iframe.style.width = "0px";
                    iframe.src = link + "?filtering=" + encodeURIComponent(JSON.stringify(searchCriteria()));
                    document.body.appendChild( iframe );
                }
            }],
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
                        $("#withdrawn_description").hide();
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
                                fastQC = row.fastqcState;
                            }
                            rowData = [
                                $.otp.createLinkMarkup({
                                    controller: 'projectOverview',
                                    action: 'index',
                                    parameters: {
                                                project: row.projectName
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
                                row.libraryPreparationKit ?
                                    "<span title='" + row.libraryPreparationKit + "'>" + row.libraryPreparationKit + "</span>" : "",
                                $.otp.createLinkMarkup({
                                    controller: 'run',
                                    action: 'show',
                                    id: row.runId,
                                    title: row.name,
                                    text: row.name
                                }),
                                row.laneId ?
                                    "<span title='" + row.laneId + "'>" + row.laneId + "</span>" : "",
                                row.libraryName,
                                fastQC,
                                row.ilseId,
                                row.problem ?
                                   "<span title='" + row.problemDescription + "'>" + row.problem + "</span>" : "",
                                row.dateCreated
                            ];

                            if (row.withdrawn) {
                                $("#withdrawn_description").show();
                                var withdrawnRow = [];
                                rowData.forEach(function(rowEntry) {
                                    rowEntry = rowEntry ? "<span class='withdrawn'>" + rowEntry + " </span>" : ""
                                    withdrawnRow.push(rowEntry)
                                });
                                rowData = withdrawnRow;
                            }

                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            },
            fnRowCallback: function (nRow) {
                var fastqc;
                fastqc = $("td:eq(10)", nRow);
                if ($("a", fastqc).length > 0) {
                    fastqc.addClass("true");
                } else {
                    fastqc.attr("title", fastqc.text());
                    fastqc.addClass("false");
                    fastqc.text("");
                }
                var contamination = $("td:eq(12)", nRow);
                if (contamination.text() === "") {
                    contamination.attr("title", "No known problems");
                    contamination.addClass("VALID");
                } else {
                    contamination.addClass("warning");
                }
            }
        });
    }
};
