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
                    value: $('#project').find('option:selected').val()
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
                parameters: { "project.id": $('#project').find('option:selected').val() }
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
        $('#loader').remove();
        if (data.alignmentInfo != null) {
            $('#alignment_info_table').css('visibility', 'visible');
            $.otp.projectConfig.createAlignmentTable(data);
        } else {
            $('#alignment_info').html("No Alignment");
        }
    },

    /**
     * Adds a loading animation to the top of the alignment Info.
     */
    displayLoading: function () {
        $('#alignment_info').css(
            'display', 'inline',
            'vertical-alignment', 'top'
        ).prepend('<div id="loader" class="loader"></div>');
    },

    /**
     * Adds rows and columns including content to the Table
     * which is taken from the achieved data.
     * @param data holds the data that have been loaded
     */
    createAlignmentTable: function (data) {
        $.each(data.alignmentInfo, function (key, value) {
            $('#alignment_info_table tr:last').after(
                '<tr><td colspan="3"><strong>' + key + '</strong></td></tr>' +
                '<tr><td style=\' padding: 5px; white-space: nowrap;\'>Aligning<td>' + value.alignmentProgram + '</td><td>' + value.alignmentParameter + '</td></tr>' +
                '<tr><td style=\' padding: 5px; white-space: nowrap;\'>Merging</td><td>' + value.mergeCommand + '</td><td>' + value.mergeOptions + '</td></tr>' +
                '<tr><td style=\' padding: 5px; white-space: nowrap;\'>Samtools</td><td>' + value.samToolsCommand + '</td><td></td></tr>' +
                '<tr><td style=\' padding: 5px; white-space: nowrap;\'>Roddy Pipeline Version</td><td>' + value.programVersion + '</td><td></td></tr>');
        });
    }
};
