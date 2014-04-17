/*jslint browser: true */
/*global $ */

$.otp.option = {
    formatValue: function (value) {
        "use strict";
        var result, MAX_SIZE = 20;
        if (value.length > MAX_SIZE) {
            result = "..." + value.substr(value.length - MAX_SIZE - 1, value.length);
        } else {
            result = value;
        }
        return result;
    },
    newProcessingOption: function (event) {
        "use strict";
        event.preventDefault();
        window.location = 'insert';
    },
    register: function () {
        "use strict";
        $("#optionTable").dataTable({
            bFilter: false,
            bProcessing: true,
            bServerSide: true,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'processingOption',
                action: 'datatable'
            }),
            bScrollInfinite: false,
            bPaginate: false,
            bScrollCollapse: true,
            sScrollY: ($(window).height() - 450),
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                $.ajax({
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": aoData,
                    "error": function () {
                        // clear the table
                        fnCallback({aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0});
                    },
                    "success": function (json) {
                        var i, rowData, row, option;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            option = row.option;
                            rowData = [
                                option.name,
                                option.type,
                                '<a href="#" title="' + option.value + '">' + $.otp.option.formatValue(option.value) + '</a>',
                                row.project,
                                option.dateCreated,
                                option.dateObsoleted,
                                option.comment
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            }
        });
        $.otp.resizeBodyInit('#optionTable', 190);
        $(".linkButton").click(this.newProcessingOption);
    }
};
