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

$.otp.projectOverviewTable = {
    /*
     * The function return only the passed value.
     * It can be used for not modified output.
     */
    returnParameterUnchanged: function (json) {
        return json;
    },

    registerDataTable: function (selector, url, successUpdate) {
        "use strict";
        var oTable = $(selector).dataTable({
            sDom: '<i> B rt<"clear">',
            buttons: $.otp.tableButtons,
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
                aoData.push({
                    name: "project",
                    value: $('#project').find('option:selected').text()
                });
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
        $.getJSON($.otp.createLink({
            controller: 'projectOverview',
            action: 'individualCountByProject'
        }), {
            projectName : $('#project').find('option:selected').text()
        }, function (data) {
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
        }).error(function (jqXHR) {
            $.otp.warningMessage(jqXHR.statusText + jqXHR.status);
        });
    },

    register: function () {
        "use strict";
        var oTable1 = $.otp.projectOverviewTable.registerDataTable(
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
        var oTable2 = $.otp.projectOverviewTable.registerDataTable(
            '#patientsAndSamplesGBCountPerProject',
            $.otp.createLink({
                controller: 'projectOverview',
                action: 'dataTableSourcePatientsAndSamplesGBCountPerProject'
            }),
            $.otp.projectOverviewTable.returnParameterUnchanged
        );
        var oTable4 = $.otp.projectOverviewTable.registerDataTable(
            '#sampleTypeNameCountBySample',
            $.otp.createLink({
                controller: 'projectOverview',
                action: 'dataTableSourceSampleTypeNameCountBySample'
            }),
            $.otp.projectOverviewTable.returnParameterUnchanged
        );
        var oTable5 = $.otp.projectOverviewTable.registerDataTable(
            "#centerNameRunId",
            $.otp.createLink({
                controller: 'projectOverview',
                action: 'dataTableSourceCenterNameRunId'
            }),
            $.otp.projectOverviewTable.returnParameterUnchanged
        );
        $.otp.projectOverviewTable.updatePatientCount();
        $('#project').change(function () {
            var oSettings1 = oTable1.fnSettings();
            oSettings1.oFeatures.bServerSide = true;
            oTable1.fnDraw();
            var oSettings2 = oTable2.fnSettings();
            oSettings2.oFeatures.bServerSide = true;
            oTable2.fnDraw();
            var oSettings4 = oTable4.fnSettings();
            oSettings4.oFeatures.bServerSide = true;
            oTable4.fnDraw();
            var oSettings5 = oTable5.fnSettings();
            oSettings5.oFeatures.bServerSide = true;
            oTable5.fnDraw();
            $.otp.graph.project.init();
            $.otp.projectOverviewTable.updatePatientCount();
        });
    }
};
