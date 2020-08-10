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
        var searchCriteria = $.otp.dataTableFilter.register($("#searchCriteriaTable"), function (searchCriteria) {
            $("#sequenceTable").dataTable().fnDraw()
        });
        var showRunLinks = document.getElementById("showRunLinks").value;

        $("#sequenceTable").dataTable({
            dom: '<i> B rt<"clear">S',
            buttons: [ {
                extend: 'csv',
                text: 'Download CSV',
                titleAttr: 'Attention: Download can take a while',
                action: function ( e, dt, node, config ) {
                    var iframe = document.createElement('iframe');
                    iframe.style.height = "0px";
                    iframe.style.width = "0px";
                    iframe.style.border = "0px";
                    iframe.src = $.otp.createLink({
                        controller: 'sequence',
                        action: 'exportAll',
                        parameters: {
                            filtering: JSON.stringify(searchCriteria()),
                        }
                    });
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
                                        id: row.fastQCFiles[j].fastqId,
                                        text: row.fastQCFiles[j].readName
                                    });
                                    fastQC += " ";
                                }
                            } else {
                                fastQC = row.fastqcState;
                            }
                            var runLink = row.name ?
                                "<span title='" + row.name + "'>" + row.name + "</span>" : "";
                            if (showRunLinks === "true") {
                                runLink = $.otp.createLinkMarkup({
                                    controller: 'run',
                                    action: 'show',
                                    id: row.runId,
                                    title: row.name,
                                    text: row.name
                                })
                            }
                            //changes here may require changes in fnRowCallback, where for some column additional values are set
                            rowData = [
                                $.otp.createLinkMarkup({
                                    controller: 'projectOverview',
                                    action: 'index',
                                    parameters: {
                                        [$.otp.projectParameter]: row.projectName
                                    },
                                    text: row.projectName,
                                    title: row.projectName
                                }),
                                "<div class='trim-text-with-ellipsis-left-based'><bdi>" + $.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    id: row.individualId,
                                    title: row.mockPid,
                                    text: row.mockPid
                                }) + "</bdi></div>",
                                row.sampleTypeName,
                                row.seqTypeDisplayName,
                                row.libraryLayout,
                                row.singleCell,
                                row.seqCenterName,
                                row.libraryPreparationKit ?
                                    "<span title='" + row.libraryPreparationKit + "'>" + row.libraryPreparationKit + "</span>" : "",
                                row.antibodyTarget,
                                runLink,
                                row.laneId ?
                                    "<span title='" + row.laneId + "'>" + row.laneId + "</span>" : "",
                                row.libraryName,
                                row.singleCellWellLabel,
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
                                    rowEntry = rowEntry != null ? "<span class='withdrawn'>" + rowEntry + "</span>" : ""
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
                fastqc = $("td:eq(13)", nRow);
                if ($("a", fastqc).length > 0) {
                    fastqc.addClass("true");
                } else {
                    fastqc.attr("title", fastqc.text());
                    fastqc.addClass("false");
                    fastqc.text("");
                }
                var contamination = $("td:eq(15)", nRow);
                if (contamination.text() === "") {
                    contamination.attr("title", "No known problems");
                    contamination.addClass("VALID");
                } else {
                    contamination.addClass("warning");
                }
                var fileExists = $("td:eq(16)", nRow);
                var fileExistsWithdrawn = $("td:eq(16) span", nRow);
                if (fileExists.text() === "true" || fileExistsWithdrawn.text() === "true") {
                    fileExists.addClass("VALID");
                } else {
                    fileExists.addClass("false");
                }
                fileExists.attr("title", fileExists.text());
                fileExists.text('');
            }
        });
    }
};

$(function() {
    $.otp.sequence.register();
});
