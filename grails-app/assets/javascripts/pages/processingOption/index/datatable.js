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
    htmlEscape: function (value) {
        return value
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    },
    register: function () {
        "use strict";
        $("#optionTable").dataTable({
            bFilter: false,
            bProcessing: true,
            bServerSide: false,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'processingOption',
                action: 'datatable'
            }),
            bPaginate: false,
            bScrollCollapse: true,
            sScrollY: 'auto',
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
                                option.name.name.toLowerCase(),
                                option.type,
                                '<span title="' + $.otp.option.htmlEscape(option.value) + '">' + $.otp.option.formatValue(option.value) + '</span>',
                                row.project,
                                option.dateCreated
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
