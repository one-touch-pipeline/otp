/*jslint browser: true */
/*global $ */

$.otp.sequence = {
    formatSeqType: function (seqTypeName) {
        "use strict";
        var result;
        if (seqTypeName === "EXON") {
            result = "EXOME";
        } else {
            result = seqTypeName;
        }
        return result;
    },

    register: function () {
        "use strict";
        var searchCriteria = $.otp.dataTableFilter.register($("#searchCriteriaTable"), $("#sequenceTable"), true, function (searchCriteria) {
            $("#export-csv").attr("href", $.otp.createLink({
                controller: 'sequence',
                action: 'exportCsv',
                parameters: {
                    filtering: JSON.stringify(searchCriteria())
                }
            }));
        });

        $("#sequenceTable").dataTable({
            sDom: '<i> rt<"clear">S',
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
                                        text: $.i18n.prop("sequence.list.numberedFastQCFile", (j + 1))
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
                                $.otp.sequence.formatSeqType(row.seqTypeName),
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
                                fastQC,
                                row.alignmentState.name,
                                (new Date(row.dateCreated)).toDateString()
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            },
            fnRowCallback: function (nRow) {
                var fastqc, alignment;
                fastqc = $("td:eq(8)", nRow);
                if ($("a", fastqc).length > 0) {
                    fastqc.addClass("true");
                } else {
                    fastqc.attr("title", fastqc.text());
                    fastqc.addClass("false");
                    fastqc.text("");
                }
                alignment = $("td:eq(9)", nRow);
                alignment.attr("title", alignment.text());
                if (alignment.text() === "FINISHED") {
                    alignment.addClass("true");
                } else {
                    alignment.addClass("false");
                }
                alignment.text("");
            }
        });
    }
};
