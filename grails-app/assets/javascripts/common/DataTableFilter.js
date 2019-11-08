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

$.otp.dataTableFilter = {
    /**
     * Filter for datatables.
     * @param searchCriteriaTable jQuery object containing the inputs/selectors to filter
     * @param dataTable jQuery object containing the datatable
     * @param updateTable boolean deciding whether the datatable should be updated after changes
     * @param onUpdateFunction function that is executed after changes
     * @returns searchCriteria function returning an object containing the currently set filters
     */
    register : function (searchCriteriaTable, dataTable, updateTable, onUpdateFunction) {
        "use strict";

        var searchCriteria = function () {
            var result = [];
            searchCriteriaTable.find("tr").each(function (index, element) {
                var selection = $("td.attribute select", element).val();
                if (selection !== "none") {
                    if ($("td.value span#" + selection, element).hasClass('dateSelection')) {
                        result.push({type: selection, value: {
                                start_day: $("td select[name=" + selection + "_start_day]", element).val(),
                                start_month: $("td select[name=" + selection + "_start_month]", element).val(),
                                start_year: $("td select[name=" + selection + "_start_year]", element).val(),
                                end_day: $("td select[name=" + selection + "_end_day]", element).val(),
                                end_month: $("td select[name=" + selection + "_end_month]", element).val(),
                                end_year: $("td select[name=" + selection + "_end_year]", element).val()
                            }});
                    } else {
                        result.push({type: selection, value: $("td select[name=" + selection + "], td input[name=" + selection + "]", element).val()});
                    }
                }
            });
            return result;
        };

        var updateSearchCriteria = function () {
            if (updateTable) {
                dataTable.dataTable().fnDraw();
            }
            if (onUpdateFunction !== undefined) {
                onUpdateFunction(searchCriteria);
            }
        };

        var removeRowOrHideInputs = function (tr) {
            // if there's more than one filter row, remove it, otherwise reset it
            if ($("tr", tr.parent()).size() > 1) {
                // remove row and determine whether to show an add button
                var lastTr;
                if (tr.is(":last-child")) {
                    lastTr = tr.prev();
                } else {
                    lastTr = tr.nextAll().last();
                }
                tr.detach();
                if ($("td.remove input", lastTr).is(":visible")) {
                    $("td.add input", lastTr).show();
                }
            } else {
                $("td.attribute select", tr).val("none");
                $("td.add input", tr).hide();
                $("td.remove input", tr).hide();
            }
        };

        var searchCriteriaChangeHandler = function (event) {
            var tr = $(event.target).parent().parent();
            $("td.value *", tr).hide();
            $("td.add input", tr).hide();
            if ($(event.target).val() !== "none") {
                $("td select[name='" + $(this).val() + "']", tr).show();
                $("td select[name='" + $(this).val() + "'] option", tr).show();
                $("td input[name='" + $(this).val() + "']", tr).show();
                $("td span[id='" + $(this).val() + "']", tr).show();
                $("td span[id='" + $(this).val() + "'] select", tr).show();
                $("td span[id='" + $(this).val() + "'] select option", tr).show();
                $("td.add input", tr).show();
                $("td.remove input", tr).show();
            } else {
                removeRowOrHideInputs(tr);
            }
            updateSearchCriteria();
        };

        var searchCriteriaAddRow = function (event) {
            var tr, cloned;
            $(event.target).hide();
            tr = $(event.target).parent().parent();
            cloned = tr.clone();
            $("td.value *", cloned).hide();
            $("td.add input", cloned).hide();
            $("td.remove input", cloned).hide();
            $("td.attribute select", cloned).val("none");
            cloned.appendTo($("#searchCriteriaTable"));
        };

        var searchCriteriaRemoveRow = function () {
            var tr = $(this).parent().parent();
            $("td.value *", tr).hide();
            removeRowOrHideInputs(tr);
            updateSearchCriteria();
        };

        searchCriteriaTable.on("change", "tr td.attribute select", searchCriteriaChangeHandler);
        searchCriteriaTable.on("click", "tr td.add input[type=button]", searchCriteriaAddRow);
        searchCriteriaTable.on("click", "tr td.remove input[type=button]", searchCriteriaRemoveRow);
        searchCriteriaTable.on("change", "tr td.value select", updateSearchCriteria);
        searchCriteriaTable.on("change", "tr td.value input[type=text]", updateSearchCriteria);
        searchCriteriaTable.on("keyup", "tr td.value input[type=text]", updateSearchCriteria);

        return searchCriteria;
    }
};
