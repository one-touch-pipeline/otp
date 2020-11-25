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

$(function () {
        "use strict";
        $('.datePicker').datepicker({
                dateFormat: 'yy-mm-dd',
                maxDate: $('#endDate').val()
        });
        var oTableProgress = $("#progressId").dataTable({
            sDom: '<i> B rt<"clear">',
            buttons: $.otp.getDownloadButton(),
            bFilter : true,
            bProcessing : true,
            bServerSide : false,
            bSort : true,
            bJQueryUI : false,
            bAutoWidth : true,
            sAjaxSource : $.otp.createLink({
                controller : 'projectProgress',
                action : 'dataTableSource'
            }),
            bPaginate : false,
            sScrollY:  'auto',
            sScrollX: 'auto',
            bScrollCollapse : false,
            bDeferRender : true,
            fnServerData : function (sSource, aoData, fnCallback) {
                aoData.push({
                    name : "projects",
                    value : $('#projects').val()
                    });
                aoData.push({
                    name : "startDate",
                    value : $('#startDate').val()
                    });
                aoData.push({
                    name : "endDate",
                    value : $('#endDate').val()
                    });
                $.ajax({
                    "dataType" : 'json',
                    "type" : "POST",
                    "url" : sSource,
                    "data" : aoData,
                    "error" : function () {
                        // clear the table
                        fnCallback({aaData : [], iTotalRecords : 0, iTotalDisplayRecords : 0});
                        oTableProgress.fnSettings().oFeatures.bServerSide = false;
                    },
                    "success": function (json) {
                        var i, j, rowData, row;
                        for (i = 0; i < json.aaData.length; i += 1) {
                            row = json.aaData[i];
                            var samples = [];
                            for (j=0; j<row[3].length; j++) {
                                samples.push($.otp.createLinkMarkup({
                                    controller: 'individual',
                                    action: 'show',
                                    id: row[3][j][0],
                                    text: row[3][j][1]
                                }));
                            }
                            rowData = [
                                $.otp.createLinkMarkup({
                                    controller: 'run',
                                    action: 'show',
                                    id: row[0],
                                    title: row[1],
                                    text: row[1]
                                }),
                                row[2],
                                samples.join(", ")
                            ];
                            json.aaData[i] = rowData;
                        }
                        fnCallback(json);
                    }
                });
            }
        });
        $('#display').click(function () {
            var oSettings1 = oTableProgress.fnSettings();
            oSettings1.oFeatures.bServerSide = true;
            oTableProgress.dataTable().fnDraw();
        });
});
