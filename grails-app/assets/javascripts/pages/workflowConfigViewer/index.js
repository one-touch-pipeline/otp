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

//= require ../shared/workflowConfigBase

$(function () {
    $('select').select2({
        allowClear: true,
        theme: 'bootstrap4',
    });

    const form = $("form.selector");
    form.on("change", "select", build);
    form.on("change", "input", build);
    form.on("keyup", "input[type=text]", build);

    function build() {
        const workflowId = $("#workflowSelector").val();
        const workflowVersionId = $("#workflowVersionSelector").val();
        const projectId = $("#projectSelector").val();
        const seqTypeId = $("#seqTypeSelector").val();
        const refGenId = $("#refGenSelector").val();
        const libPrepKitId = $("#libPrepKitSelector").val();

        const configValue = $("#configValue");
        const relatedSelectors = $("#relatedSelectors");

        if (workflowId || workflowVersionId || projectId || seqTypeId || refGenId || libPrepKitId) {
            $.ajax({
                url: $.otp.createLink({
                    controller: 'workflowConfigViewer',
                    action: 'build',
                }),
                dataType: "json",
                type: "POST",
                data: {
                    workflow: workflowId,
                    workflowVersion: workflowVersionId,
                    project: projectId,
                    seqType: seqTypeId,
                    referenceGenome: refGenId,
                    libraryPreparationKit: libPrepKitId,
                },
                success: function (response) {

                    const selectorHtml = (response.selectors);
                    relatedSelectors.html(selectorHtml);

                    $(".expandable-button").click(function () {
                        $(this).siblings(".expandable-container").toggleClass("collapsed");
                        $(this).siblings(".expandable-container").toggleClass("expanded");
                    });

                    if (/<\/?[a-z][\s\S]*>/i.test(selectorHtml)) {
                        configValue.val(JSON.stringify(JSON.parse(response.config), null, 2));
                    } else {
                        configValue.val("");
                        $.otp.toaster.showInfoToast('No selectors found', 'There are no related selectors to this selection');
                    }

                },
                error: function (error) {
                    $.otp.toaster.showErrorToast('Error while processing', error.responseJSON?.message);
                }
            });
        } else {
            configValue.val("");
            relatedSelectors.html("");
        }
    }
});

