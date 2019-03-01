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

$.otp.selectSamplesTable = {

    selectSamples: function (preSelectedSamples) {
        "use strict";
        var oTable = $('#selectSamplesTable').dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools: $.otp.tableTools,
            bFilter: true,
            bProcessing: true,
            bServerSide: false,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: $.otp.createLink({
                controller: 'egaSubmission',
                action: 'dataTableSelectSamples'
            }),
            bScrollCollapse: true,
            sScrollY: ($(window).height() - 480),
            bPaginate: false,
            bDeferRender: true,
            fnServerData: function (sSource, aoData, fnCallback) {
                aoData.push({
                    name: "project",
                    value: $('#projectName').text()
                });
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
                        for (var i = 0; i < json.aaData.length; i += 1) {
                            var column = json.aaData[i];
                            var currentSampleAndSeqType = parseInt(column[0].split("-")[0]) + column[3];
                            var checked = preSelectedSamples.includes(currentSampleAndSeqType) ? 'checked' : '';
                            column[0] = '<input type="checkbox" name="sampleAndSeqType" value="'+ column[0] + '" ' + checked + '/>';
                        }
                        fnCallback(json);
                    }
                });
            }
        });

        $.otp.dataTableFilter.register($('#searchCriteriaTableSeqType'), oTable, false, function () {
            var select = $('#searchCriteriaTableSeqType').find('select')[0],
                column = 3;
            if (select.selectedIndex !== 0) {
                oTable.fnFilter('^' + select.value + '$', column, true);
            } else {
                oTable.fnFilter('', column);
            }
        });

        return oTable;
    }
};
