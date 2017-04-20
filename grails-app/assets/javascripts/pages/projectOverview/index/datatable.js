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
            sDom: '<i> T rt<"clear">',
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

    deleteUser: function () {
        $('.deletePerson').on('click', function (event) {
            "use strict";
            $.ajax({
                type: 'GET',
                url: $.otp.createLink({
                    controller: 'projectOverview',
                    action: 'deleteContactPersonOrRemoveProject'
                }),
                dataType: 'json',
                cache: 'false',
                data: {
                    "projectContactPerson.id": $(event.target).data("id")
                },
                success: function (data) {
                    if (data.success) {
                        $.otp.infoMessage($L("editorswitch.notification.success"));
                        $(event.target).parents("tr").remove();
                    } else {
                        $.otp.warningMessage(data.error);
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    $.otp.warningMessage($L("editorswitch.notification.error", textStatus, errorThrown));
                }
            });
        })
    },

    referenceGenome: function () {
        "use strict";
        var oTable = $.otp.projectOverviewTable.registerDataTable(
            "#listReferenceGenome",
            $.otp.createLink({
                controller: 'projectOverview',
                action: 'dataTableSourceReferenceGenome'
            }),
            $.otp.projectOverviewTable.returnParameterUnchanged
        );
        $('#project').change(function () {
            var oSettings = oTable.fnSettings();
            oSettings.oFeatures.bServerSide = true;
            oTable.fnDraw();
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
            $.otp.projectOverviewTable.updateAlignmentInformation();
        });
    },

    updateAlignmentInformation: function () {
        "use strict";
        $.getJSON($.otp.createLink({
            controller: 'projectOverview',
            action: 'checkForAlignment'
        }), {
            projectName : $('#project').find('option:selected').text()
        }, function (data) {
            $('#projectOverview_alignmentInformation').text(data.alignmentMessage);
        }).error(function (jqXHR) {
            $.otp.warningMessage(jqXHR.statusText + jqXHR.status);
        });
    },

    /**
     * Asynchronous calls the getAlignmentInfo from the  ProjectOverviewController.
     * While loading displays a Loading text until the data arrived.
     */
    asynchronCallAlignmentInfo: function () {
        "use strict";
        $.ajax({
            url: $.otp.createLink({
                controller: 'projectOverview',
                action: 'getAlignmentInfo',
                parameters: {project: $('#project').find('option:selected').text()}
            }),
            dataType: 'json',
            success: function (data) {
                $.otp.projectOverviewTable.initialiseAlignmentInfo(data);
            },
            beforeSend: function () {
                $.otp.projectOverviewTable.displayLoading();
            }
        });
    },

    /**
     * If the loading of the Data is successfull, the Loading animation will be removed.
     * If the loaded Data is empty no Alginment is displayed. If not,
     * the AlginmentInfo table is displayed.
     * @param data holds the data that have been loaded
     */
    initialiseAlignmentInfo: function (data) {
        $('#loadingDots').remove();
        if (data.alignmentInfo != null) {
            $('#alignment_info_table').css('visibility', 'visible');
            $.otp.projectOverviewTable.createAlignmentTable(data);
        } else {
            $('#alignment_info').html($L("projectOverview.alignmentInformation.noAlign"));
        }
    },

    /**
     * Adds a loading animation to the top of the alignment Info.
     */
    displayLoading: function () {
        $('#alignment_info').css(
            'display', 'inline',
            'vertical-alignment', 'top'
        ).prepend('<div class="loadingDots" id="loadingDots">Loading</div>');
    },

    /**
     * Adds rows and columns including content to the Table
     * which is taken from the achieved data.
     * @param data holds the data that have been loaded
     */
    createAlignmentTable: function (data) {
        var aligning = $L("projectOverview.alignmentInformation.aligning");
        var merging = $L("projectOverview.alignmentInformation.merging");
        var samtools = $L("projectOverview.alignmentInformation.samtools");

        $.each(data.alignmentInfo, function (key, value) {
            $('#alignment_info_table tr:last').after(
                '<tr><td colspan="3"><strong>' + key + '</strong></td></tr>' +
                '<tr><td style=\' padding: 5px;\'>' + aligning + '<td>' + value.bwaCommand + '</td><td>' + value.bwaOptions + '</td></tr>' +
                '<tr><td style=\' padding: 5px;\'>' + merging + '</td><td>' + value.mergeCommand + '</td><td>' + value.mergeOptions + '</td></tr>' +
                '<tr><td style=\' padding: 5px;\'>' + samtools + '</td><td>' + value.samToolsCommand + '</td><td></td></tr>');
        });
    }

};
