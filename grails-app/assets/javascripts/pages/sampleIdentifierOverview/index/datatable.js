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

$(function() {
    "use strict";
    $.otp.sampleIdentifierOverviewTable.registerDataTable(
        '#sampleIdentifierOverviewTable',
        $.otp.createLink({
            controller: 'sampleIdentifierOverview',
            action    : 'dataTableSourceSampleIdentifierOverview'
        })
    );
});

$.otp.sampleIdentifierOverviewTable = {

    registerDataTable: function (selector, url) {
        "use strict";
        var oTable = $(selector).dataTable({
            sDom: '<i> B rt<"clear">',
            buttons: $.otp.getDownloadButton(),
            bFilter: true,
            bProcessing: true,
            bServerSide: false,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: url,
            bScrollCollapse: true,
            sScrollY: 500,
            bPaginate: false,
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
                        oTable.fnSettings().oFeatures.bServerSide = false;
                    },
                    "success": function (json) {
                        oTable.fnSettings().oFeatures.bServerSide = false;
                        var i, rowData, row;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            rowData = [
                                $.otp.createLinkMarkup({
                                    controller: "individual",
                                    action    : "show",
                                    id        : row.individual.id,
                                    text      : row.individual.name
                                }),
                                $.otp.createLinkMarkup({
                                    controller: "seqTrack",
                                    action    : "seqTrackSet",
                                    parameters: {
                                        "individual": row.individual.id,
                                        "sampleType": row.sampleType.id,
                                        "seqType"   : row.seqType.id
                                    },
                                    text      : row.sampleType.name
                                }),
                                row.seqType.displayText,
                                $.otp.sampleIdentifierOverviewTable.formatSampleIdentifier(row.sampleIdentifier)
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            }
        });
        return oTable;
    },

    formatSampleIdentifier: function (data) {
        return $.map(data, function (it) {
            return "<span class=\"" + (it.withdrawn ? "withdrawn" : "") + "\" title=\"" + it.comments + "\">" + it.text + "</span>";
        }).join(", ");
    },
};
