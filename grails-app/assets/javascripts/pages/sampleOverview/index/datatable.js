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

$(() => {
  'use strict';

  const parseCoverageFromCellData = function (rowData) {
    // remove html tags
    const htmlRe = /(<([^>]+)>)/ig;
    const rowDataNoHtml = rowData.replace(htmlRe, '');
    // remove parentheses
    const parenthesesRe = /\s\(.*\)/;
    const cleanedRowData = rowDataNoHtml.replace(parenthesesRe, '');
    // find first coverage and return
    const result = cleanedRowData.split('|')[1];
    if (result) {
      return result.trim();
    }
    return result;
  };

  const fileName = `Sample_Overview-${$('.selected-project-value strong').text()}`;

  const oTableLaneOverview = $('#laneOverviewId').dataTable({
    sDom: '<i> B rt<"clear">',
    buttons: $.otp.getDownloadButton('', fileName),
    bFilter: true,
    bProcessing: true,
    bServerSide: false,
    bSort: true,
    bAutoWidth: false,
    sAjaxSource: $.otp.createLink({
      controller: 'sampleOverview',
      action: 'dataTableSourceLaneOverview'
    }),
    sScrollY: 'auto',
    sScrollX: 'auto',
    bScrollCollapse: false,
    bPaginate: false,
    bDeferRender: true,
    createdRow(row) {
      $.each($('td', row), function () {
        const coverage = parseCoverageFromCellData($(this).html());
        if (coverage) {
          $(this).attr('data-sort', coverage);
        }
      });
    },
    columnDefs: [{
      targets: 'custom-sort',
      type: 'num',
      render(data, type, full, meta) {
        let result = data;
        if (type === 'sort') {
          const api = new $.fn.dataTable.Api(meta.settings);
          const td = api.cell({
            row: meta.row,
            column: meta.col
          }).node();
          const attr = $(td).attr('data-sort');
          if (typeof attr !== 'undefined' && attr !== false) {
            result = attr;
          } else {
            result = Infinity;
          }
        }
        return result;
      }
    }],
    fnServerData(sSource, aoData, fnCallback) {
      $.ajax({
        dataType: 'json',
        type: 'POST',
        url: sSource,
        data: aoData,
        error() {
          // clear the table
          fnCallback({
            aaData: [],
            iTotalRecords: 0,
            iTotalDisplayRecords: 0
          });
          oTableLaneOverview.fnSettings().oFeatures.bServerSide = false;
        },
        success(json) {
          for (let j = 0; j < json.columnsToHide.length; j++) {
            oTableLaneOverview.fnSetColumnVis(json.columnsToHide[j], false);
          }
          if (json.anythingWithdrawn) {
            $('#withdrawn_description').show();
          }
          for (let i = 0; i < json.aaData.length; i += 1) {
            const row = json.aaData[i];
            const pid = row[0];
            row[0] = $.otp.createLinkMarkup({
              controller: 'individual',
              action: 'show',
              text: pid,
              parameters: {
                pid
              }
            });
          }
          fnCallback(json);
          oTableLaneOverview.fnSettings().oFeatures.bServerSide = false;
        }
      });
    },
    fnInitComplete() {
      // eslint-disable-next-line no-new
      new $.fn.dataTable.FixedColumns(this, {
        leftColumns: 2
      });
    }
  });

  $.otp.dataTableFilter.register($('#searchCriteriaTableSeqType'), () => {
    const ignoredColumns = parseInt($('#laneOverviewId').data('ignore-filter-columns'), 10);
    const workflowSize = parseInt($('#laneOverviewId').data('workflow-size'), 10);
    const seqTypeSize = parseInt($('#laneOverviewId').data('seq-type-size'), 10);
    const step = workflowSize + 1;
    const tableSize = seqTypeSize * step + ignoredColumns;
    const result = [];
    let i;

    $('#searchCriteriaTableSeqType').find('tr').each((index, element) => {
      const idx = $('td.attribute select', element)[0].selectedIndex;
      if (idx !== 0) {
        result.push((idx - 1) * step + ignoredColumns);
      }
    });

    for (i = ignoredColumns; i < tableSize; i += step) {
      if (result.indexOf(i) !== -1) {
        oTableLaneOverview.fnFilter('.+', i, true);
      } else {
        oTableLaneOverview.fnFilter('', i);
      }
    }
  });

  $.otp.dataTableFilter.register($('#searchCriteriaTableSampleType'), () => {
    const select = $('#searchCriteriaTableSampleType').find('select')[0];
    const column = 1;
    if (select.selectedIndex !== 0) {
      oTableLaneOverview.fnFilter(`^${select.value}$`, column, true);
    } else {
      oTableLaneOverview.fnFilter('', column);
    }
  });
});
