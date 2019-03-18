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

$.otp.addIndividual = {
    addSample: function (event) {
        "use strict";
        event.preventDefault();
        var existingSamples, placeOfLastSampleInTable, sampleBoxes, newSampleRow;
        // Subtract the hidden element
        existingSamples = $("tr.sample").not(".hidden");
        placeOfLastSampleInTable = existingSamples.last();
        sampleBoxes = $("tr.hidden").clone().removeClass("hidden");
        sampleBoxes.find("button").click($.otp.addIndividual.addSampleIdentifier);
        sampleBoxes.appendTo(placeOfLastSampleInTable.parent());
        newSampleRow = $("tr.newSample");
        newSampleRow.appendTo(sampleBoxes.last().parent());
    },
    addSampleIdentifier: function (event) {
        "use strict";
        event.preventDefault();
        var clickedButton, td, row, inputs, newInput, minusButton;
        clickedButton = $(this);
        td = clickedButton.parent().parent().get(0);
        row = $(td).parent().get(0);
        inputs = $(row).find("input");
        newInput = inputs.last().clone().val('');
        minusButton = clickedButton.clone();
        minusButton.text("-");
        newInput.appendTo($(td));
        newInput.wrap('<div class="removeSampleIdentifier"></div>');
        minusButton.click(function (event) {
            event.preventDefault();
            $(this).parent().remove();
        });
        minusButton.appendTo(newInput.parent());
    },
    submitIndividual: function (event) {
        "use strict";
        event.preventDefault();
        var samplesArray = [], samples = "";
        $.each($("tr.sample").not("tr.sample:hidden"), function (index, value) {
            var sample = { };
            sample.identifier = [];
            sample.type = ($("select option:selected", value).val());
            $(value).next().find("input").each(function (idx, v) {
                sample.identifier.push($(v).val());
            });
            samplesArray.push(sample);
        });
        samples = JSON.stringify(samplesArray);
        $.getJSON($.otp.createLink({
            controller: 'individual',
            action: 'save'
        }), {
            pid: $("#pid").val(),
            project: $("#project").val(),
            mockPid: $("#mockPid").val(),
            mockFullName: $("#mockFullName").val(),
            internIdentifier: $("#internIdentifier").val(),
            individualType: $("#individualType").val(),
            samples: samples
        }, function (data) {
            var message, i;
            if (data.error) {
                $.otp.warningMessage(data.error);
            } else if (data.errors) {
                message = "<ul>";
                for (i = 0; i < data.errors.length; i += 1) {
                    message += "<li>" + data.errors[i].message + "</li>";
                }
                message += "</ul>";
                $.otp.warningMessage(message);
            } else if (data.success) {
                $.otp.infoMessage("New Individual with Id " + data.id + " created.");
            }
        }).error(function (jqXHR) {
            $.otp.warningMessage(jqXHR.statusText + jqXHR.status);
        });
    },
    register: function () {
        "use strict";
        $("div.addSample button").click(this.addSample);
        $("div.newSampleIdentifier button").click(this.addSampleIdentifier);
        $("#add-individual-form").submit(this.submitIndividual);
        var lastPid = $("#pid").val();
        $("#pid").keyup(function() {
            var pid = $("#pid").val();
            var mockPid = $("#mockPid").val();
            var mockFullName = $("#mockFullName").val();
            if (lastPid == mockPid) {
                $("#mockPid").val(pid);
            }
            if (lastPid == mockFullName) {
                $("#mockFullName").val(pid);
            }
            lastPid = $("#pid").val();
        });
    }
};
