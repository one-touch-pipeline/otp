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

$(function() {
    $.otp.projectOverviewTable.register();
    $.otp.graph.project.init();
});

$.otp.projectOverviewTable = {
    /*
     * This function returns the passed value without modifying it.
     * It can be used for not modified output.
     */
    returnParameterUnchanged: function (json) {
        return json;
    },

    registerDataTable: function (selector, url, successUpdate) {
        "use strict";
        const fileName = $(selector).parent().attr("data-csv-title") + "-" + $(".selected-project-value strong").text();

        var oTable = $(selector).dataTable({
            sDom: '<i> B rt<"clear">',
            buttons: $.otp.getDownloadButton("", fileName),
            oTableTools: $.otp.tableTools,
            bFilter: true,
            bProcessing: true,
            bServerSide: false,
            bSort: true,
            bJQueryUI: false,
            bAutoWidth: false,
            sAjaxSource: url,
            bScrollCollapse: true,
            sScrollY: 200,
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
                        json = successUpdate(json);
                        fnCallback(json);
                        oTable.fnSettings().oFeatures.bServerSide = false;
                    }
                });
            }
        });
        return oTable;
    },

    updatePatientCount: function () {
        "use strict";
        $.ajax({
            url: $.otp.createLink({
                controller: 'projectOverview',
                action: 'individualCountByProject'
            }),
            dataType: 'json',
            type: 'GET',
            success: function (data) {
                var message, i;
                if (data.individualCount >= 0) {
                    $('#patient-count').html(data.individualCount);
                } else if (data.error) {
                    $.otp.warningMessage(data.error);
                    $('#patient-count').html("");
                } else if (data.errors) {
                    $('#patient-count').html("");
                    message = "<ul>";
                    for (i = 0; i < data.errors.length; i += 1) {
                        message += "<li>" + data.errors[i].message + "</li>";
                    }
                    message += "</ul>";
                    $.otp.warningMessage(message);
                }
            },
            error: function (jqXHR) {
                $.otp.warningMessage(jqXHR.statusText + jqXHR.status);
            }
        });
    },

    register: function () {
        "use strict";
        $.otp.projectOverviewTable.registerDataTable(
            '#projectOverviewTable',
            $.otp.createLink({
                controller: 'projectOverview',
                action: 'dataTableSource'
            }),
            function (json) {
                for (var i = 0; i < json.aaData.length; i += 1) {
                    var row = json.aaData[i];
                    row[0] = $.otp.createLinkMarkup({
                        controller: 'individual',
                        action: 'show',
                        text: row[0],
                        parameters: {
                            mockPid: row[0]
                        }
                    });
                }
                return json;
            }
        );
        $.otp.projectOverviewTable.registerDataTable(
            '#patientsAndSamplesGBCountPerProject',
            $.otp.createLink({
                controller: 'projectOverview',
                action: 'dataTableSourcePatientsAndSamplesGBCountPerProject'
            }),
            $.otp.projectOverviewTable.returnParameterUnchanged
        );
        $.otp.projectOverviewTable.registerDataTable(
            '#sampleTypeNameCountBySample',
            $.otp.createLink({
                controller: 'projectOverview',
                action: 'dataTableSourceSampleTypeNameCountBySample'
            }),
            $.otp.projectOverviewTable.returnParameterUnchanged
        );
        $.otp.projectOverviewTable.registerDataTable(
            "#centerNameRunId",
            $.otp.createLink({
                controller: 'projectOverview',
                action: 'dataTableSourceCenterNameRunId'
            }),
            $.otp.projectOverviewTable.returnParameterUnchanged
        );
        $.otp.projectOverviewTable.updatePatientCount();
    }
};
