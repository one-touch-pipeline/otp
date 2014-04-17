/*jslint browser: true */
/*global $ */

/**
 * Generates the list of individuals
 **/
$.otp.individualList = function () {
    "use strict";
    $.otp.individual.register();
    $.otp.resizeBodyInit('#individualTable', 180);
};

$.otp.individual = {
    register: function () {
        "use strict";
        $("#individualTable").dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools: {
                sSwfPath: $.otp.contextPath + "/js/jquery/tableTools/media/swf/copy_cvs_xls_pdf.swf",
                aButtons: $.otp.tableTools_button_options
            },
            bFilter: true,
            bProcessing: true,
            bServerSide: true,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'individual',
                action: 'dataTableSource'
            }),
            bPaginate: false,
            bScrollCollapse: true,
            sScrollY: ($(window).height() - 453),
            iDisplayLength: Math.round(($('.body').height() - 200) / 23),
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "filtering",
                    value: JSON.stringify($.otp.individual.searchCriteria())
                });
                aoData.push({
                    name: "filter",
                    value: $.otp.individual.search
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
                        var i, j, rowData, row;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            rowData = [
                                $.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    id: row.id,
                                    text: row.pid
                                }),
                                row.mockFullName,
                                row.mockPid,
                                row.project,
                                row.type
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            }
        });
        // search criteria
        $("#searchCriteriaTable tr td:eq(0) select").change($.otp.individual.searchCriteriaChangeHandler);
        $("#searchCriteriaTable tr td:eq(2) input[type=button]").click($.otp.individual.searchCriteriaAddRow);
        $("#searchCriteriaTable tr td:eq(1) select").change($.otp.individual.updateSearchCriteria);
        $("#searchCriteriaTable tr td:eq(1) input[type=text]").change($.otp.individual.updateSearchCriteria);
        $("#searchCriteriaTable tr td:eq(1) input[type=text]").keyup($.otp.individual.updateSearchCriteria);
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
        $.otp.individual.updateSearchCriteria();
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
        $("td:eq(0) select", cloned).change($.otp.individual.searchCriteriaChangeHandler);
        $("td:eq(2) input[type=button]", cloned).click($.otp.individual.searchCriteriaAddRow);
        $("td:eq(1) select", cloned).change($.otp.individual.updateSearchCriteria);
        $("td:eq(1) input[type=text]", cloned).change($.otp.individual.updateSearchCriteria);
        $("td:eq(1) input[type=text]", cloned).keyup($.otp.individual.updateSearchCriteria);
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
        $("#individualTable").dataTable().fnDraw();
    }
};
