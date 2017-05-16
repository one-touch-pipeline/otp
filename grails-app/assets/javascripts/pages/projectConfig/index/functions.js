/*jslint browser: true */
/*global $ */

$.otp.projectConfig = {

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

    referenceGenome: function () {
        "use strict";
        var oTable = $.otp.projectConfig.registerDataTable(
            "#listReferenceGenome",
            $.otp.createLink({
                controller: 'projectConfig',
                action: 'dataTableSourceReferenceGenome'
            }),
            $.otp.projectConfig.returnParameterUnchanged
        );
        $('#project').change(function () {
            var oSettings = oTable.fnSettings();
            oSettings.oFeatures.bServerSide = true;
            oTable.fnDraw();
        });
    },

    deleteUser: function () {
        $('.deletePerson').on('click', function (event) {
            "use strict";
            $.ajax({
                type: 'GET',
                url: $.otp.createLink({
                    controller: 'projectConfig',
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

    /**
     * Asynchronous calls the getAlignmentInfo from the  ProjectOverviewController.
     * While loading displays a Loading text until the data arrived.
     */
    asynchronousCallAlignmentInfo: function () {
        "use strict";
        $.ajax({
            url: $.otp.createLink({
                controller: 'projectConfig',
                action: 'getAlignmentInfo',
                parameters: {project: $('#project').find('option:selected').text()}
            }),
            dataType: 'json',
            success: function (data) {
                $.otp.projectConfig.initialiseAlignmentInfo(data);
            },
            beforeSend: function () {
                $.otp.projectConfig.displayLoading();
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
            $.otp.projectConfig.createAlignmentTable(data);
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
    },

    toggle: function (controlElement, linkElement) {
        var control = document.getElementById(controlElement);
        var link = document.getElementById(linkElement);

        if (control.style.display == "none") {
            control.style.display = "";
            link.innerHTML = "Hide list";
        } else {
            control.style.display = "none";
            link.innerHTML = "Show list";
        }
    }
};
