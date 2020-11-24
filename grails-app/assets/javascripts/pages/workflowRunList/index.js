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
    var statusToClassName = function (status) {
        "use strict";
        switch (status) {
            case "WAITING_ON_USER":
                return "dot orange";
            case "FAILED":
                return "dot red";
            case "PENDING":
                return "dot grey";
            case "WAITING_ON_SYSTEM":
                return "sm-loader grey";
            case "RUNNING":
                return "sm-loader green";
            case "SUCCESS":
                return "dot green";
            case "SKIPPED":
                return "dot black";
            case "FAILED_FINAL":
                return "dot blue";
            case "KILLED":
                return "dot purple";
        }
        return "";
    }

    var table = $("#runs").DataTable({
        columns: [
            {
                'data': function (row, type, set, meta) {
                    if (type === "sort") {
                        return null;
                    }
                    var buttonsDisabled = (row.state !== "FAILED" || row.stepId === null) ? " disabled " : ""
                    return "<input type='checkbox' name='step' value='" + row.stepId + "' form='bulk' class='tableCheckbox' " + buttonsDisabled + ">"
                },
                'orderable': false,
            },
            {
                'data': function (row, type, set, meta) {
                    if (type === "sort") {
                        return row.state;
                    }
                    return '<div title="' + row.state + '" class="' + statusToClassName(row.state) + '"></div>';
                }
            },
            {
                'data': function (row, type, set, meta) {
                    if (type === "sort") {
                        return row.comment;
                    }
                    if (row.comment) {
                        return '<div title="' + row.comment + '"><img src="' + $.otp.createLink({controller: "assets", action: "info.png"}) + '"></div>';
                    }
                    return "";
                }
            },
            {'data': 'workflow', 'orderable': false},
            {'data': 'name'},
            {'data': 'step', 'orderable': false},
            {'data': 'dateCreated'},
            {'data': 'lastUpdated'},
            {'data': 'duration', 'orderable': false},
            {'data': 'id'},
            {
                'data': function (row, type, set, meta) {
                    if (type === "sort") {
                        return null;
                    }

                    var buttonsDisabled = (row.state !== "FAILED" || row.stepId === null) ? " disabled " : ""
                    var stepsDisabled = (row.steps.length === 0) ? " disabled " : ""

                    var result = "<form method='POST' class='single'>" +
                        "          <div class='btn-group'>" +
                                     button($.otp.createLink({controller: "workflowRunList", action: "setFailedFinal"}), row.stepId, "Set failed final",
                                          buttonsDisabled, $.otp.createLink({controller: "assets", action: "set-failed-final.svg"})) +
                                     button( $.otp.createLink({controller: "workflowRunList", action: "restartStep"}), row.stepId , "Restart \"" + row.step + "\" step",
                                          buttonsDisabled ,$.otp.createLink({controller: "assets", action: "restart-step.svg"})) +
                        "            <div class='btn-group' role='group'>" +
                        "              <button type='button' class='btn btn-primary dropdown-toggle dropdown-toggle-split' data-toggle='dropdown' " +
                        "                      aria-haspopup='true' aria-expanded='false' title='Restart previous steps'" + buttonsDisabled + stepsDisabled + ">" +
                        "                <span class='sr-only'>Toggle Dropdown</span>" +
                        "              </button>" +
                        "              <div class='dropdown-menu dropdown-menu-right'>"
                    for (var i = 0; i < row.steps.length; i++) {
                        result += "      <button class='dropdown-item' formaction='" + $.otp.createLink({controller: "workflowRunList", action: "restartStep"}) + "' " +
                            "                    name='step' value='" + row.stepIds[i] + "'" + buttonsDisabled + ">Restart \"" + row.steps[i] + "\" step</button>"
                    }
                    result += "        </div>" +
                        "            </div>" +
                                     button( $.otp.createLink({controller: "workflowRunList", action: "restartRun"}), row.stepId, "Restart run",
                                         buttonsDisabled, $.otp.createLink({controller: "assets", action: "restart-run.svg"})) +
                        "          </div>" +
                        "        </form>"
                    return result
                },
                'orderable': false,
            },
        ],
        processing: true,
        serverSide: true,
        ordering: true,
        order: [[7, 'desc']],
        paging: true,
        pageLength: 100,
        scrollCollapse: true,
        ajax: {
            url: $.otp.createLink({
                controller: 'workflowRunList',
                action: 'data'
            }),
            data: function (data) {
                data["workflow.id"] = $('#workflow').val();
                data["state"] = $('#state').val();
                data["name"] = $('#name').val();
            },
            dataSrc: function (json) {
                setCount(json.count);
                return json.data;
            },
        },
    });

    var button = function(action, value, title, buttonsDisabled, imgSrc) {
        return "<button class='btn btn-primary' formaction='" + action + "' name='step' value='" + value + "' title='" + title + "' " + buttonsDisabled + ">" +
            "<img src='" + imgSrc + "' style='height:1em; width:1em'/>" +
            "</button>"
    }

    table.on('draw', function () {
        $('[title]').tooltip({trigger: 'hover focus click'});
    });

    var setCount = function (values) {
        $("#allRuns").text(values[0]);
        $("#runningRuns").text(values[1]);
        $("#failedRuns").text(values[2]);
    }

    $("#workflow, #state, #name").on("change", function () {
        table.draw();
    })

    $("#selectAll").on("click", function (e) {
        var checkboxes = $("input.tableCheckbox:not([disabled])");
        var checked = e.target.checked;
        checkboxes.each(function (i, el) {
            el.checked = checked;
        })
    });

    $("#bulk").on("submit", function (e) {
        var checkboxes = $("input.tableCheckbox");
        var anySelected = false;
        checkboxes.each(function (i, el) {
            if (el.checked) {
                anySelected = true;
            }
        })
        if (!anySelected) {
            e.preventDefault();
            alert("Select at least one row");
        }

        addSelection(e.target);
    });

    $("#runs").on("submit", ".single", function (e) {
        addSelection(e.target);
    });

    var addSelection = function (form) {
        addHiddenField(form, "workflow.id", $('#workflow').val());
        addHiddenField(form, "state", $('#state').val());
        addHiddenField(form, "name", $('#name').val());
    }

    var addHiddenField = function (form, name, value) {
        $("<input>", {type: "hidden", name: name, value: value}).appendTo(form);
    }
});
