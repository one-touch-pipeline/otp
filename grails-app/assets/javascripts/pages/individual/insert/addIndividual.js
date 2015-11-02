/*jslint browser: true */
/*global $ */

/**
 */
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
                $.otp.infoMessage($L("individual.insert.add.success", data.id));
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
    }
};
