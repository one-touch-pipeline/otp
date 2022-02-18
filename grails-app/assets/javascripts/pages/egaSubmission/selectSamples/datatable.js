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

$.otp.selectSamplesTable = {

  /**
     * Gathers all samples of the selected project and displays them with PID, SeqType and SampleType.
     * It also adds a checkbox for selection in the first column.
     *
     * @param header array of additional columns after the selection column
     * @param preSelectedSamples a list of samples to preselect, defined by "<Sample ID><SampleType Name>"
     * @returns datatable object
     */
  selectableSampleList(header, preSelectedSamples) {
    'use strict';

    return $('#selectSamplesTable').dataTable({
      sDom: '<i> T rt<"clear">',
      oTableTools: $.otp.tableTools,
      bFilter: true,
      bProcessing: true,
      bServerSide: false,
      bSort: true,
      bAutoWidth: false,
      sAjaxSource: $.otp.createLink({
        controller: 'egaSubmission',
        action: 'dataTableSelectSamples'
      }),
      bScrollCollapse: true,
      sScrollY: ($(window).height() - 480),
      bPaginate: false,
      bDeferRender: true,
      fnServerData(sSource, aoData, fnCallback) {
        aoData.push({
          name: 'egaProject',
          value: $('#sampleTable').data('project')
        });
        $.ajax({
          dataType: 'json',
          type: 'POST',
          url: sSource,
          data: aoData,
          error() {
            // clear the table
            fnCallback({ aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0 });
          },
          success(json) {
            const result = json;
            for (let i = 0; i < json.aaData.length; i += 1) {
              const entry = json.aaData[i];
              const checked = preSelectedSamples.includes(entry.sampleId + entry.seqType) ? 'checked' : '';
              result.aaData[i][0] = `<input type="checkbox" 
                                          name="sampleAndSeqType" 
                                          value="${entry.identifier}" ${checked}/>`;
              for (let c = 0; c < header.length; c++) {
                result.aaData[i][c + 1] = entry[header[c]];
              }
            }
            fnCallback(result);
          }
        });
      }
    });
  },

  /**
     * Applies a filter on the SeqType column of the datatable.
     *
     * @param table the target datatable
     * @param seqTypeColumnIndex index of the column containing the SeqType
     */
  applySeqTypeFilter(table, seqTypeColumnIndex) {
    $.otp.dataTableFilter.register($('#searchCriteriaTableSeqType'), () => {
      const select = $('#searchCriteriaTableSeqType').find('select')[0];
      if (select.selectedIndex !== 0) {
        table.fnFilter(`^${select.value}$`, seqTypeColumnIndex, true);
      } else {
        table.fnFilter('', seqTypeColumnIndex);
      }
    });
  },

  removeFilterOnColumn(table, seqTypeColumnIndex) {
    table.fnFilter('', seqTypeColumnIndex);
  }
};
