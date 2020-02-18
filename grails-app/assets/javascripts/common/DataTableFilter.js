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
                                start: $("td input[name=" + selection + "_start]", element).val(),
                                end:   $("td input[name=" + selection + "_end]", element).val(),
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
            if (tr.siblings("tr").size() > 0) {
                // remove row and determine whether to show an add button
                var lastTr;
                if (tr.is(":last-child")) {
                    lastTr = tr.prev();
                } else {
                    lastTr = tr.nextAll().last();
                }
                tr.detach();
                if ($("td.remove", lastTr).is(":visible")) {
                    $("td.add", lastTr).show();
                }
            } else {
                $("td.attribute select", tr).val("none");
                $("td.add", tr).hide();
                $("td.remove", tr).hide();
            }
        };

        var searchCriteriaChangeHandler = function (event) {
            var tr = $(event.target).parents(".dtf_row");
            $("td.value span.dtf_value_span", tr).hide();
            var attribute = $(event.target).val();
            if (attribute !== "none") {
                $("td span[id='dtf_" + attribute + "']", tr).show();
                $("td.add", tr).show();
                $("td.remove", tr).show();
            } else {
                $("td.add", tr).hide();
                removeRowOrHideInputs(tr);
            }
            updateSearchCriteria();
        };
        var searchCriteriaAddRow = function (event) {
            var tr, cloned;

            tr = $(event.target).parents(".dtf_row");
            $("td.add", tr).hide();
            cloned = tr.clone();
            $("td.value span.dtf_value_span", cloned).hide();
            $("td.add", cloned).hide();
            $("td.remove", cloned).hide();
            $("td.attribute select", cloned).val("none");
            cloned.appendTo(searchCriteriaTable);
        };

        var searchCriteriaRemoveRow = function (event) {
            var tr = $(event.target).parents(".dtf_row");
            $("td.value span.dtf_value_span", tr).hide();
            removeRowOrHideInputs(tr);
            updateSearchCriteria();
        };

        searchCriteriaTable.on("change", "select.dtf_criterium",         searchCriteriaChangeHandler);
        searchCriteriaTable.on("click",  "td.add input[type=button]",    searchCriteriaAddRow);
        searchCriteriaTable.on("click",  "td.remove input[type=button]", searchCriteriaRemoveRow);
        searchCriteriaTable.on("change", "td.value select",              updateSearchCriteria);
        searchCriteriaTable.on("change", "td.value input",               updateSearchCriteria);
        searchCriteriaTable.on("keyup",  "td.value input[type=text]",    updateSearchCriteria);

        return searchCriteria;
    }
};
