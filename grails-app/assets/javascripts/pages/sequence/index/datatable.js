/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*jslint browser: true */
/*global $ */

$.otp.sequence = {

    register: function () {
        "use strict";
        var searchCriteria = $.otp.dataTableFilter.register($("#searchCriteriaTable"), $("#sequenceTable"), true);
console.log("sssssssssssssssssssssssssss")
        var link = $.otp.createLink({
            controller: 'sequence',
            action: 'exportAll',
        });
console.log("dddddd")
console.log($("#sequenceTable"))
        $("#sequenceTable").dataTable({
            dom: '<i> B rt<"clear">S',
            buttons: [ {
                extend: 'csv',
                titleAttr: 'Attention: Download can take a while',
                action: function ( e, dt, node, config ) {
                    console.log("jjjjjjjjjjjjjj")

                    var iframe = document.createElement('iframe');
                    iframe.style.height = "0px";
                    iframe.style.width = "0px";
                    iframe.style.border = "0px";
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
              console.log("hhhhhhh")

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
                                console.log("444444444444")
  },
                    "success": function (json) {
                                      console.log("666666666666")

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
                                row.singleCell,
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
                                row.fileExists,
                                row.dateCreated,
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
                 console.log("ccccccccccccccc")
       var fastqc;
                fastqc = $("td:eq(11)", nRow);
                if ($("a", fastqc).length > 0) {
                    fastqc.addClass("true");
                } else {
                    fastqc.attr("title", fastqc.text());
                    fastqc.addClass("false");
                    fastqc.text("");
                }
                var contamination = $("td:eq(13)", nRow);
                if (contamination.text() === "") {
                    contamination.attr("title", "No known problems");
                    contamination.addClass("VALID");
                } else {
                    contamination.addClass("warning");
                }
                var fileExists = $("td:eq(14)", nRow);
                if (fileExists.text() === "true") {
                    fileExists.addClass("VALID");
                } else {
                    fileExists.addClass("false");
                }
                fileExists.attr("title", fileExists.text());
                fileExists.text('');
            }
        });
        console.log("dddddddddddddddddddddddddddddddddddd")
    }
};
