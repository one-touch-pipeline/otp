/*jslint browser: true */
/*global $ */

$.otp.sequence = {
    formatProject: function (project) {
        "use strict";
        var result;
        if (project === "PROJECT_NAME") {
            result = "mb";
        } else if (project === "astrocytome") {
            result = "pa";
        } else {
            result = project;
        }
        return result;
    },
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
        $("#sequenceTable").dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools: {
                sSwfPath: $.otp.contextPath + "/js/jquery/tableTools/media/swf/copy_cvs_xls_pdf.swf",
                aButtons: $.otp.tableTools_button_options
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
            bScrollInfinite: true,
            sScrollY: ($(window).height() - 415),
            iDisplayLength: Math.round(($('.body').height() - 180) / 23),
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "filtering",
                    value: JSON.stringify($.otp.sequence.searchCriteria())
                });
                $.ajax({
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": aoData,
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
                                '<span title="' + row.projectName + '">' + $.otp.sequence.formatProject(row.projectName) + '</span>',
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
        // search criteria
        $("#searchCriteriaTable tr td:eq(0) select").change($.otp.sequence.searchCriteriaChangeHandler);
        $("#searchCriteriaTable tr td:eq(2) input[type=button]").click($.otp.sequence.searchCriteriaAddRow);
        $("#searchCriteriaTable tr td:eq(1) select").change($.otp.sequence.updateSearchCriteria);
        $("#searchCriteriaTable tr td:eq(1) input[type=text]").change($.otp.sequence.updateSearchCriteria);
        $("#searchCriteriaTable tr td:eq(1) input[type=text]").keyup($.otp.sequence.updateSearchCriteria);
    },
    searchCriteriaChangeHandler: function () {
        "use strict";
        var tr = $(this).parent().parent();
        $("td:eq(1) *", tr).hide();
        $("td:eq(2) input", tr).hide();
        if ($(this).val() !== "none") {
            $("td select[name=" + $(this).val() + "]", tr).show();
            $("td select[name=" + $(this).val() + "] option", tr).show();
            $("td input[name=" + $(this).val() + "]", tr).show();
            $("td:eq(2) input", tr).show();
        } else {
            // decide whether to delete this element
            if ($("tr", tr.parent()).size() > 1) {
                tr.detach();
            }
        }
        $.otp.sequence.updateSearchCriteria();
    },
    searchCriteriaAddRow: function () {
        "use strict";
        var tr, cloned;
        tr = $(this).parent().parent();
        cloned = tr.clone();
        $("td:eq(1) *", cloned).hide();
        $("td:eq(2) input", cloned).hide();
        $("td:eq(0) select", cloned).val("none");
        cloned = cloned.appendTo($("#searchCriteriaTable"));
        $("td:eq(0) select", cloned).change($.otp.sequence.searchCriteriaChangeHandler);
        $("td:eq(2) input[type=button]", cloned).click($.otp.sequence.searchCriteriaAddRow);
        $("td:eq(1) select", cloned).change($.otp.sequence.updateSearchCriteria);
        $("td:eq(1) input[type=text]", cloned).change($.otp.sequence.updateSearchCriteria);
        $("td:eq(1) input[type=text]", cloned).keyup($.otp.sequence.updateSearchCriteria);
    },
    searchCriteria: function () {
        "use strict";
        var result = [];
        $("#searchCriteriaTable tr").each(function (index, element) {
            var selection = $("td:eq(0) select", element).val();
            if (selection !== "none") {
                result.push({type: selection, value: $("td select[name=" + selection + "], td input[name=" + selection + "]", element).val()});
            }
        });
        return result;
    },
    updateSearchCriteria: function () {
        "use strict";
        $("#sequenceTable").dataTable().fnDraw();
        $("#export-csv").attr("href", $.otp.createLink({
            controller: 'sequence',
            action: 'exportCsv',
            parameters: {
                filtering: JSON.stringify($.otp.sequence.searchCriteria())
            }
        }));
    }
};
