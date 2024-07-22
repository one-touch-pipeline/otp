/*
 * Copyright 2011-2024 The OTP authors
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

    return $('#selectSamplesTable').DataTable({
      dom: '<i> T rt<"clear">',
      tableTools: $.otp.tableTools,
      filter: true,
      processing: true,
      serverSide: false,
      columnDefs: [{
        targets: 'no-sort',
        orderable: false
      }],
      order: [[1, 'asc']],
      sort: true,
      autoWidth: false,
      scrollCollapse: true,
      scrollY: ($(window).height() - 480),
      paginate: false,
      deferRender: true,
      ajax: (data, callback) => {
        $.ajax({
          dataType: 'json',
          type: 'POST',
          url: $.otp.createLink({
            controller: 'egaSubmission',
            action: 'dataTableSelectSamples'
          }),
          data: {
            ...data,
            egaProject: $('#sampleTable').data('project')
          },
          error() {
            // clear the table
            callback({
              aaData: [],
              iTotalRecords: 0,
              iTotalDisplayRecords: 0
            });
          },
          success(json) {
            const result = json;
            for (let i = 0; i < json.aaData.length; i += 1) {
              const entry = json.aaData[i];
              const checked = preSelectedSamples.includes(`${entry.identifier}`) ? 'checked' : '';
              let firstColumn = `<input type="checkbox" name="sampleAndSeqType" 
                                            value="${entry.identifier}" ${checked}/>`;

              if (entry.fileExists !== 'true') {
                const samplesWithMissingFile = `${entry.identifier},${entry.individual},${entry.seqTypeDisplayName},` +
                  `${entry.sequencingReadType},${entry.singleCellDisplayName},${entry.sampleType}`;
                firstColumn =
                  `${firstColumn} 
                   <img src="${$.otp.createAssetLink('warning.png')}" alt="warning" title="No fastq file found!">
                   <input type="hidden" name="samplesWithMissingFile" value="${samplesWithMissingFile}">
                   `;
              }

              result.aaData[i][0] = firstColumn;
              for (let c = 0; c < header.length; c++) {
                result.aaData[i][c + 1] = entry[header[c]];
              }
            }
            callback(result);
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
    'use strict';

    $.otp.dataTableFilter.register($('#searchCriteriaTableSeqType'), () => {
      const select = $('#searchCriteriaTableSeqType').find('select')[0];
      if (select.selectedIndex !== 0) {
        table.column(seqTypeColumnIndex).search(select.value).draw();
      } else {
        this.removeFilterOnColumn(table, seqTypeColumnIndex);
      }
    });
  },

  removeFilterOnColumn(table, seqTypeColumnIndex) {
    'use strict';

    table.column(seqTypeColumnIndex).search('').draw();
  }
};
