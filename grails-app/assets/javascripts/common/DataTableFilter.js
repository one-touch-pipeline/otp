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
     * @param onUpdateFunction function that is executed after changes
     * @returns searchCriteria function returning an object containing the currently set filters
     */
  register(searchCriteriaTable, onUpdateFunction) {
    'use strict';

    const searchCriteria = function () {
      const result = [];
      searchCriteriaTable.find('tr').each((index, element) => {
        const selection = $('td.attribute select', element).val();
        if (selection !== 'none') {
          if ($(`td.value span#${selection}`, element).hasClass('dateSelection')) {
            result.push({
              type: selection,
              value: {
                start: $(`td input[name=${selection}_start`, element).val(),
                end: $(`td input[name=${selection}_end]`, element).val()
              }
            });
          } else {
            result.push({
              type: selection,
              value: $(`td select[name=${selection}], td input[name=${selection}]`, element).val()
            });
          }
        }
      });
      return result;
    };

    const updateSearchCriteria = function () {
      if (onUpdateFunction !== undefined) {
        onUpdateFunction(searchCriteria);
      }
    };

    const removeRowOrHideInputs = function (tr) {
      // if there's more than one filter row, remove it, otherwise reset it
      if (tr.siblings('tr').length > 0) {
        // remove row and determine whether to show an add button
        let lastTr;
        if (tr.is(':last-child')) {
          lastTr = tr.prev();
        } else {
          lastTr = tr.nextAll().last();
        }
        tr.detach();
        if ($('td.remove', lastTr).is(':visible')) {
          $('td.add', lastTr).show();
        }
      } else {
        /* 'change.select2' is a limited 'changed'-event, that triggers only a sync between the
                 * 'raw' select-tag and the select2 visuals.
                 * We can't use a normal 'trigger(change)' because this function is an event handler, so
                 * would trigger a recursive stack overflow. (Ask me how I know this ;-) )
                 *
                 * see also: https://select2.org/programmatic-control/events#limiting-the-scope-of-the-change-event
                 *     and:  https://github.com/select2/select2/issues/3620 */
        $('td.attribute select', tr).val('none').trigger('change.select2');
        $('td.add', tr).hide();
        $('td.remove', tr).hide();
      }
    };

    const searchCriteriaChangeHandler = function (event) {
      const tr = $(event.target).parents('.dtf_row');
      $('td.value span.dtf_value_span', tr).hide();
      const attribute = $(event.target).val();
      if (attribute !== 'none') {
        $(`td span[id='dtf_${attribute}']`, tr).show();
        $('td.add', tr).show();
        $('td.remove', tr).show();
      } else {
        $('td.add', tr).hide();
        removeRowOrHideInputs(tr);
      }
      updateSearchCriteria();
    };
    const searchCriteriaAddRow = function (event) {
      const tr = $(event.target).parents('.dtf_row');
      $('td.add', tr).hide();

      // select2 doesn't take well to cloning, do the required voodoo dance.
      $('select.select2-hidden-accessible', tr).select2('destroy');
      const cloned = tr.clone();
      $.otp.applySelect2($('select.use-select-2', tr));

      $('td.value span.dtf_value_span', cloned).hide();
      $('td.add', cloned).hide();
      $('td.remove', cloned).hide();

      /* 'change.select2' is a limited 'changed'-event, that triggers only a sync between the
             * 'raw' select-tag and the select2 visuals.
             * We can't use a normal 'trigger(change)' because this function is an event handler, so
             * would trigger a recursive stack overflow. (Ask me how I know this ;-) )
             *
             * see also: https://select2.org/programmatic-control/events#limiting-the-scope-of-the-change-event
             *     and:  https://github.com/select2/select2/issues/3620 */
      $('td.attribute select', cloned).val('none').trigger('change.select2');
      cloned.appendTo(searchCriteriaTable);

      /*
             * Add select2 to new element only _after_ all other DOM-objects have reached their final state.
             * Doing it earlier results in unstable width-estimations, and jumping elements on reflow/adding new lines.
             */
      $.otp.applySelect2($('select.use-select-2', cloned));
    };

    const searchCriteriaRemoveRow = function (event) {
      const tr = $(event.target).parents('.dtf_row');
      $('td.value span.dtf_value_span', tr).hide();
      removeRowOrHideInputs(tr);
      updateSearchCriteria();
    };

    searchCriteriaTable.on('change', 'select.dtf_criterium', searchCriteriaChangeHandler);
    searchCriteriaTable.on('click', 'td.add input[type=button]', searchCriteriaAddRow);
    searchCriteriaTable.on('click', 'td.remove input[type=button]', searchCriteriaRemoveRow);
    searchCriteriaTable.on('change', 'td.value select', updateSearchCriteria);
    searchCriteriaTable.on('change', 'td.value input', updateSearchCriteria);
    searchCriteriaTable.on('keyup', 'td.value input[type=text]', updateSearchCriteria);

    return searchCriteria;
  }
};
