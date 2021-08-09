/*
 * Copyright 2011-2020 The OTP authors
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
$(function () {
    function format(d) {
        var childTable = '<table class="table table-sm table-borderless">'

        for (var i = 0; i < d.clusterJobs.length; i++) {
            childTable += '<tr>' +
                '<td><div title="' + d.clusterJobs[i].state + '" class="small ' + statusToClassName(d.clusterJobs[i].state) + '"></div></td>' +
                '<td>' + $.otp.createLinkMarkup({text: 'Cluster job: ' + d.clusterJobs[i].name, controller: "clusterJobDetail", action: "show", id: d.clusterJobs[i].id, parameters: createLinkParametersForNavigation()}) + '</td>' +
                '<td>' + d.clusterJobs[i].jobId + '</td>'
            if (d.clusterJobs[i].hasLog === true) {
                childTable +='<td>' + $.otp.createLinkMarkup({text: "Log", controller: "clusterJobDetail", action: "showLog", id: d.clusterJobs[i].id, parameters: createLinkParametersForNavigation()}) + '</td>'
            } else {
                childTable +='<td></td>'
            }
            childTable +=
                '<td>' + d.clusterJobs[i].node + '</td>' +
                '<td>' + d.clusterJobs[i].wallTime + '</td>' +
                '<td>' + d.clusterJobs[i].exitCode + '</td>' +
                '</tr>'
        }
        if (d.wes) {
            childTable += '<tr>' +
                '<td></td>' +
                '<td>WES job</td>' +
                '<td>' + d.wes + '</td>' +
                '<td></td>' +
                '<td></td>' +
                '<td></td>' +
                '</tr>'
        }
        if (d.hasLogs) {
            childTable += '<tr>' +
                '<td></td>' +
                '<td>Workflow logs</td>' +
                '<td></td>' +
                '<td>' + $.otp.createLinkMarkup({text: "Log", controller: "workflowRunDetails", action: "showLogs", id: d.id, parameters: createLinkParametersForNavigation()}) + '</td>' +
                '<td></td>' +
                '<td></td>' +
                '</tr>'
        }
        if (d.error) {
            childTable += '<tr>' +
                '<td></td>' +
                '<td>Workflow error: ' + d.error.message + '</td>' +
                '<td></td>' +
                '<td>' + $.otp.createLinkMarkup({text: "Error", controller: "workflowRunDetails", action: "showError", id: d.id, parameters: createLinkParametersForNavigation()}) + '</td>' +
                '<td></td>' +
                '<td></td>' +
                '</tr>'
        }
        childTable += '</table>';
        return childTable;
    }

    var lastStepFailed = false;

    var table = $('#steps').DataTable({
        columns: [
            {
                'data': function (row, type, set, meta) {
                    if (type === "sort") {
                        return row.state;
                    }
                    return '<div title="' + row.state + '" class="' + statusToClassName(row.state) + ' small"></div>';
                }
            },
            {
                'data': function (row, type, set, meta) {
                    if (type === "sort") {
                        return null;
                    }
                    if (!row.error && !row.clusterJobs.length && !row.wes && !row.hasLogs) {
                        return ""
                    }
                    return "<button class='btn btn-sm btn-info details-control'>" +
                        "<i title='Show/hide details' class='bi bi-chevron-bar-expand'></i></button>"
                },
                'orderable': false,
            },

            {'data': 'name', 'orderable': false},
            {'data': 'dateCreated'},
            {'data': 'lastUpdated'},
            {'data': 'duration', 'orderable': false},
            {'data': 'id'},
            {
                'data': function (row, type, set, meta) {
                    if (type === "sort") {
                        return null;
                    }

                    const runState = $("#steps").data("wf-run-state");
                    const buttonsDisabled = !lastStepFailed || runState === "RESTARTED" || runState === "FAILED_FINAL" ? " disabled " : "";

                    return "<form method='POST' class='single'>" +
                        button($.otp.createLink({controller: "workflowRunDetails", action: "restartStep", parameters: {"redirect": $.otp.uriWithParams}}),
                            row.id, "Restart step", buttonsDisabled, "reply") +
                        "</form>"
                },
                'orderable': false,
            },
        ],
        ordering: false,
        paging: false,
        serverSide: true,
        scrollCollapse: true,
        ajax: {
            url: $.otp.createLink({
                controller: 'workflowRunDetails',
                action: 'data',
                parameters: {"workflowRun.id": $("#steps").data("id")}
            }),
            dataSrc: function (json) {
                if (json.data[0].state === "FAILED") {
                    lastStepFailed = true;
                }
                return json.data;
            },
        },
    });

    function createLinkParametersForNavigation() {
        const stepsTable = $("#steps");
        const workflowRunId = stepsTable.data("wf-run-id");
        const workflowId = stepsTable.data("wf-id");
        const state = stepsTable.data("state");
        const name = stepsTable.data("name");

        return {
            "workflowRun.id": workflowRunId,
            "workflow.id": workflowId,
            "state": state,
            "name": name
        }
    }

    table.on('draw', function () {
        $('[title]').tooltip();
        if (lastStepFailed) {
            $('#steps tbody tr:first-child td .details-control').trigger('click');
        }
    });

    $('#steps tbody').on('click', 'td .details-control', function () {
        var tr = $(this).closest('tr');
        var row = table.row(tr);

        if (row.child.isShown()) {
            row.child.hide();
            tr.removeClass('shown');
        } else {
            row.child(format(row.data())).show();
            tr.addClass('shown');
            $('[title]').tooltip();
        }
    });
});
