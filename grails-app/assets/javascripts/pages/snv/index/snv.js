/*jslint browser: true */
/*global $ */

/**
 */
$.otp.submitSNV = {
    isNumeric: function(e) {
        try {
            if (window.event) {
                var charCode = window.event.keyCode;
            } else if (e) {
                var charCode = e.which;
            } else {
                return true;
            }
            if ((charCode >= 48 && charCode <= 57)|| charCode == 08 || charCode == 13 || charCode == 9 || charCode == 0 || charCode == 46) {
                return true;
            } else {
                alert("Input fields should contain a numeric value");
                return false;
            }
        } catch (err) {
            alert("Input fields should contain a numeric value");
        }
    },
    submitAlert: function() {
        var r = confirm("Are you sure you want to confirm this registration?");
return r;
    },
}


$.otp.Snv= {
registerIndividualIds: function () {
    "use strict";
    var oTable = $("#individualsPerProject").dataTable({
        bFilter: false,
        bProcessing: true,
        bServerSide: false,
        bSort: true,
        bJQueryUI: false,
        bAutoWidth: false,
        sAjaxSource: $.otp.createLink({
            controller: 'Snv',
            action: 'dataTableSourceForIndividuals'
        }),
        bScrollCollapse: true,
        sScrollY: 350,
        bPaginate:false,
        bDeferRender: true,
        fnServerData: function (sSource, aoData, fnCallback) {
            aoData.push({
                name: "project",
                value : $('#projectName').val()
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
                    for (var i = 0; i < json.aaData.length; i += 1) {
                        var row = json.aaData[i];
                        var mockPid = row[0];
                            row[0] = $.otp.createLinkMarkup({
                                controller: 'samplePair',
                                action: 'index',
                                text: row[0],
                                parameters: {
                                    mockPid: mockPid
                                }
                            });
                    }
                    fnCallback(json);
                }
            });
        }
    });
    return oTable;
},

register: function () {
    "use strict";
    var oTable = $.otp.Snv.registerIndividualIds();

    $('#project_select').change(function () {
        var oSettings1 = oTable.fnSettings();
        oSettings1.oFeatures.bServerSide = true;
        oTable.fnDraw();
    });
}

};
