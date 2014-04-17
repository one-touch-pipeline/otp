/*jslint browser: true */
/*global $ */

/**
 * Generates the list of Runs
 **/
$.otp.runList = function () {
    "use strict";
    $.otp.run.register();
    $.otp.resizeBodyInit('#runTable', 180);
};

$.otp.run = {
    register: function () {
        "use strict";
        $("#runTable").dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools: {
                sSwfPath : $.otp.contextPath + "/js/jquery/tableTools/media/swf/copy_cvs_xls_pdf.swf",
                aButtons : tableTools_button_options
            },
            bFilter: true,
            bProcessing: true,
            bServerSide: true,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'run',
                action: 'dataTableSource'
            }),
            bPaginate: false,
            bScrollCollapse: true,
            sScrollY: ($(window).height() - 453),
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "filtering",
                    value: JSON.stringify($.otp.run.searchCriteria())
                });
                aoData.push({
                    name: "filter",
                    value: $.otp.run.search
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
                        var i, j, rowData, row, dataQuality;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            rowData = [
                                $.otp.createLinkMarkup({
                                    controller: 'run',
                                    action: 'show',
                                    id: row.id,
                                    text: row.name
                                }),
                                row.seqCenters,
                                row.storageRealm,
                                row.dateCreated,
                                row.dateExecuted,
                                row.blacklisted,
                                row.multipleSource,
                                row.qualityEvaluated
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            }
        });
        // search criteria
        $("#searchCriteriaTable tr td:eq(0) select").change($.otp.run.searchCriteriaChangeHandler);
        $("#searchCriteriaTable tr td:eq(2) input[type=button]").click($.otp.run.searchCriteriaAddRow);
        $("#searchCriteriaTable tr td:eq(1) select").change($.otp.run.updateSearchCriteria);
        $("#searchCriteriaTable tr td:eq(1) input[type=text]").change($.otp.run.updateSearchCriteria);
        $("#searchCriteriaTable tr td:eq(1) input[type=text]").keyup($.otp.run.updateSearchCriteria);
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
            $("td span[name=" + $(this).val() + "]", tr).show();
            $("td span[name=" + $(this).val() + "] select", tr).show();
            $("td span[name=" + $(this).val() + "] select option", tr).show();
            $("td:eq(2) input", tr).show();
        } else {
            // decide whether to delete this element
            if ($("tr", tr.parent()).size() > 1) {
                tr.detach();
            }
        }
        $.otp.run.updateSearchCriteria();
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
        $("td:eq(0) select", cloned).change($.otp.run.searchCriteriaChangeHandler);
        $("td:eq(2) input[type=button]", cloned).click($.otp.run.searchCriteriaAddRow);
        $("td:eq(1) select", cloned).change($.otp.run.updateSearchCriteria);
        $("td:eq(1) input[type=text]", cloned).change($.otp.run.updateSearchCriteria);
        $("td:eq(1) input[type=text]", cloned).keyup($.otp.run.updateSearchCriteria);
    },
    searchCriteria: function () {
        "use strict";
        var result = [];
        $("#searchCriteriaTable tr").each(function (index, element) {
            var selection = $("td:eq(0) select", element).val();
            if (selection !== "none") {
                if (selection=="dateCreatedSelection") {
                    result.push({type: selection, value: {
                        start_day: $("td select[name=dateCreatedSelection_day]", element).val(),
                        start_month: $("td select[name=dateCreatedSelection_month]", element).val(),
                        start_year: $("td select[name=dateCreatedSelection_year]", element).val(),
                        end_day: $("td select[name=dateCreatedSelection2_day]", element).val(),
                        end_month: $("td select[name=dateCreatedSelection2_month]", element).val(),
                        end_year: $("td select[name=dateCreatedSelection2_year]", element).val()
                    }});
                } else if (selection=="dateExecutedSelection") {
                    result.push({type: selection, value: {
                        start_day: $("td select[name=dateExecutedSelection_day]", element).val(),
                        start_month: $("td select[name=dateExecutedSelection_month]", element).val(),
                        start_year: $("td select[name=dateExecutedSelection_year]", element).val(),
                        end_day: $("td select[name=dateExecutedSelection2_day]", element).val(),
                        end_month: $("td select[name=dateExecutedSelection2_month]", element).val(),
                        end_year: $("td select[name=dateExecutedSelection2_year]", element).val()
                    }});
                } else {
                    result.push({type: selection, value: $("td select[name=" + selection + "], td input[name=" + selection + "]", element).val()});
                }
            }
        });
        return result;
    },
    updateSearchCriteria: function () {
        "use strict";
        $("#runTable").dataTable().fnDraw();
    }
};
