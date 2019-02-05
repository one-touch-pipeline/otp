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

/*global $: false, window: false
 */

$(function() {
    $.otp.editSamples = {};
    $("div.sample-identifiers-field p.inline-element button.edit").click(function () {
        "use strict";
        var outerContainer, existingValues, extractedElements = [], textField, textFieldContainer, saveButton, cancelButton, concreteTextField, concreteCancelButton, concreteSaveButton, lastExistingRow, plusButton;
        outerContainer = $(this).parent().parent();
        existingValues = $("span.wordBreak", outerContainer).text();
        existingValues = existingValues.substring(1, existingValues.length - 1);
        extractedElements = existingValues.split(', ');
        saveButton = $("<button class=\"buttons save\">" + $L("default.button.update.label") + "</button>");
        cancelButton = $("<button class=\"buttons cancel\">" + $L("default.button.cancel.label") + "</button>");
        textFieldContainer = $("p.sample-identifiers-field-editor", outerContainer);
        textField = $("<input type=\"text\" name=\"value\"/>");
        $.each(extractedElements, function () {
            concreteTextField = textField.clone();
            if (this) {
                concreteTextField.val(this).appendTo(textFieldContainer);
                $("<br/>").appendTo(concreteTextField.parent());
            }
        });
        $("table.newSample", outerContainer).show();
        lastExistingRow = $("table.newSample tr", outerContainer).last();
        plusButton = $("div.newSampleIdentifier button.plus", lastExistingRow);
        plusButton.click(function (event) {
            event.preventDefault();
            $.otp.editSamples.identifiers.addRow(outerContainer, lastExistingRow);
        });
        concreteSaveButton = saveButton.clone();
        concreteSaveButton.click(function (event) {
            event.preventDefault();
            $.otp.editSamples.identifiers.save(outerContainer);
        });
        concreteSaveButton.appendTo(lastExistingRow.parent().parent().parent());
        concreteCancelButton = cancelButton.clone();
        concreteCancelButton.click(function (event) {
            event.preventDefault();
            $.otp.editSamples.identifiers.cancel(outerContainer);
        });
        concreteCancelButton.appendTo(concreteSaveButton.parent());
        $("p.sample-identifiers-field-editor", outerContainer).show();
        $("p.sample-identifiers-field-label", outerContainer).hide();
    });

    $.otp.editSamples.identifiers = {
        addRow: function (outerContainer, lastExistingRow) {
            "use strict";
            var newRow, buttonDiv, minusButton;
            newRow = lastExistingRow.clone();
            newRow.appendTo(lastExistingRow.parent());
            buttonDiv = $("div.newSampleIdentifier", outerContainer);
            minusButton = $("button.minus", buttonDiv);
            minusButton.show();
            newRow = $("table.newSample tr", outerContainer).last();
            $("div.newSampleIdentifier", newRow.prev()).hide();
            $("div.newSampleIdentifier button.plus", newRow).click(function (event) {
                event.preventDefault();
                $.otp.editSamples.identifiers.addRow(outerContainer, newRow);
            });
            minusButton.click(function (event) {
                event.preventDefault();
                $("div.newSampleIdentifier", newRow.prev()).show();
                newRow.detach();
            });
            $("div.newSampleIdentifier", newRow).show();
        },
        save: function (outerContainer) {
            "use strict";
            var samplesArray = [], samples = "", container, sampleIdentifiersIds = [], sampleIdentifiersIdsString, identifier, sampleIdentifierFields;
            container = $(this).parent();
            sampleIdentifierFields = $("p.sample-identifiers-field-editor input:text[name=value]");
            sampleIdentifiersIdsString = $("input:hidden[name=sampleIdentifiersIds]", sampleIdentifierFields.parent().parent().parent().parent()).val();
            if (sampleIdentifiersIdsString) {
                sampleIdentifiersIdsString = sampleIdentifiersIdsString.substring(1, sampleIdentifiersIdsString.length - 1);
                sampleIdentifiersIds = sampleIdentifiersIdsString.split(", ");
            }
            $.each(sampleIdentifierFields, function (index, value) {
                var sample = {};
                sample.identifier = [];
                sample.id = [];
                if ($(value).val() === "") {
                    return true; // skip empty entry
                }
                sample.type = outerContainer.parent().prev().text();
                sample.identifier.push($(value).val());
                if (sampleIdentifiersIdsString) {
                    sample.id.push(sampleIdentifiersIds[index]);
                }
                samplesArray.push(sample);
            });
            $.each($("tr.sample"), function (index, value) {
                var sample = {};
                sample.identifier = [];
                if ($("td.value input", value).val() === "") {
                    return true; // skip empty entry
                }
                sample.type = outerContainer.parent().prev().text();
                sample.identifier.push($("td.value input", value).val());
                samplesArray.push(sample);
            });
            samples = JSON.stringify(samplesArray);
            $.ajax({
                url: $("input:hidden[name=target]", outerContainer).val(),
                dataType: 'json',
                data: {samples: samples},
                success: function (data) {
                    if (data.success) {
                        $.otp.infoMessage($L("editorSwitch.notification.success"));
                        $("p.edit-switch-label span", outerContainer).text($("input:text[name=value]", container).val());
                    } else {
                        $.otp.warningMessage(data.error);
                        $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    $.otp.warningMessage($L("editorSwitch.notification.error", textStatus, errorThrown));
                    $("input:text[name=value]", container).val($("p.edit-switch-label span", outerContainer).text());
                }
            });
            window.setTimeout(function() { location.reload() }, 300); //reloads page after 0.3 seconds
        },
        cancel: function (outerContainer) {
            "use strict";
            $.otp.editSamples.identifiers.hideEditFields(outerContainer);
        },
        hideEditFields: function (outerContainer) {
            "use strict";
            $("p.sample-identifiers-field-editor", outerContainer).hide();
            $("table.newSample", outerContainer).hide();
            $("p.sample-identifiers-field-label", outerContainer).show();
            $("input:text[name=value]", outerContainer).detach();
            $("button.save", outerContainer).detach();
            $("button.cancel", outerContainer).detach();
            $("br", outerContainer).detach();
        }
    };

    $("div.newSampleIdentifier button.buttons").click(function () {
        "use strict";
        var outerContainer, existingTable, newTable;
        outerContainer = $(this).parent().parent();
        existingTable = $("table.newSample", outerContainer);
        newTable = existingTable.clone();
        newTable.appendTo(existingTable);
    });
});
