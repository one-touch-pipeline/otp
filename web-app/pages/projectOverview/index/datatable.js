/*jslint browser: true */
/*global $ */

$.otp.projectOverviewTable = {
    registerDataTable: function (selector, url) {
        "use strict";
        var oTable = $(selector).dataTable({
            sDom: '<i> T rt<"clear">',
            oTableTools: {
                sSwfPath: $.otp.contextPath + "/js/jquery/tableTools/media/swf/copy_cvs_xls_pdf.swf",
                aButtons: $.otp.tableTools_button_options
            },
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
                    value: $('#project_select').val()
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
            controller : 'projectOverview',
            action : 'individualCountByProject'
        }), {
            projectName : $('#project_select').val()
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
            })
        );
        var oTable2 = $.otp.projectOverviewTable.registerDataTable(
            '#patientsAndSamplesGBCountPerProject',
            $.otp.createLink({
                controller: 'projectOverview',
                action: 'dataTableSourcePatientsAndSamplesGBCountPerProject'
            })
        );
        var oTable4 = $.otp.projectOverviewTable.registerDataTable(
            '#sampleTypeNameCountBySample',
            $.otp.createLink({
                controller : 'projectOverview',
                action : 'dataTableSourceSampleTypeNameCountBySample'
            })
        );
        var oTable5 = $.otp.projectOverviewTable.registerDataTable(
            "#centerNameRunId",
            $.otp.createLink({
                controller : 'projectOverview',
                action : 'dataTableSourceCenterNameRunId'
            })
        );
        $.otp.projectOverviewTable.updatePatientCount();
        $('#project_select').change(function () {
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
