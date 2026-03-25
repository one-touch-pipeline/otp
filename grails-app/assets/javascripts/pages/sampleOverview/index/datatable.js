/*
 * Copyright 2011-2026 The OTP authors
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

  const parseCoverageFromCellData = (rowData) => {
    // remove html tags
    const rowDataNoHtml = rowData.replace(/(<([^>]+)>)/ig, '');
    // remove parentheses
    const cleanedRowData = rowDataNoHtml.replace(/\s\(.*\)/, '');
    // find first coverage and return
    const result = cleanedRowData.split('|')[1];
    return result ? result.trim() : result;
  };

  const fileName = `Sample_Overview-${$('.selected-project-value strong').text()}`;

  // disable the "none" option in the seqTypeSelection dropdown
  document.querySelector('select[name="seqTypeSelection"]')
    .querySelector('option[value="none"]').disabled = true;

  // Function to initialize fixed columns
  const initializeFixedColumns = () => {
    const table = $('#laneOverviewId');
    const wrapper = table.closest('.dataTables_wrapper');
    const scrollBody = wrapper.find('.dataTables_scrollBody');

    // Function to update second column position based on first column width
    const updateSecondColumnPosition = () => {
      const firstColumnWidth = table.find('td:nth-child(1)').first().outerWidth();
      if (!firstColumnWidth) return;

      table.find('td:nth-child(2)').css('left', `${firstColumnWidth}px`);

      // Handle DataTables scroll header
      const headerTable = wrapper.find('.dataTables_scrollHead table');
      if (headerTable.length) {
        headerTable.addClass('fixed-columns-table');
        headerTable.find('th:nth-child(2)').css('left', `${firstColumnWidth}px`);
      }
    };

    // Initial positioning
    updateSecondColumnPosition();
    // Update positioning when window is resized or table is redrawn
    $(window).off('resize.fixedCols').on('resize.fixedCols', updateSecondColumnPosition);
    // Also update when DataTables draws/adjusts columns
    table.off('draw.fixedCols column-sizing.fixedCols')
      .on('draw.fixedCols column-sizing.fixedCols', updateSecondColumnPosition);

    // Update shadow effect based on scroll position
    if (scrollBody.length) {
      scrollBody.off('scroll.fixedCols').on('scroll.fixedCols', function () {
        const hasScrolled = $(this).scrollLeft() > 0;
        table.find('td:nth-child(2)').toggleClass('custom-shadow', hasScrolled);
      });
    } else {
      // If DataTables hasn't created the scroll body yet, try again after a short delay
      const retryCount = (table.data('fixedColsRetryCount') || 0) + 1;
      table.data('fixedColsRetryCount', retryCount);
      if (retryCount <= 10) {
        setTimeout(initializeFixedColumns, 100);
      } else {
        // eslint-disable-next-line no-console
        console.warn('initializeFixedColumns: giving up after 10 attempts (no scroll body).');
      }
    }

    // Store the update function for later use
    table.data('updateSecondColumnPosition', updateSecondColumnPosition);
  };

  const oTableLaneOverview = $('#laneOverviewId').dataTable({
    dom: '<i>Brt<"clear">',
    buttons: $.otp.getDownloadButton('', fileName),
    searching: true,
    processing: true,
    serverSide: false,
    ordering: true,
    autoWidth: true,
    sAjaxSource: $.otp.createLink({
      controller: 'sampleOverview',
      action: 'dataTableSourceLaneOverview'
    }),
    scrollY: 'auto',
    scrollX: 'auto',
    scrollCollapse: false,
    paging: false,
    deferRender: true,
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
        if (type !== 'sort') return data;
        const api = new $.fn.dataTable.Api(meta.settings);
        const td = api.cell({
          row: meta.row,
          column: meta.col
        }).node();
        const attr = $(td).attr('data-sort');
        return (typeof attr !== 'undefined' && attr !== false) ? attr : Infinity;
      }
    }],
    fnServerData(source, data, fnCallback) {
      $.ajax({
        dataType: 'json',
        type: 'POST',
        url: source,
        data,
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
          json.columnsToHide.forEach((col) => oTableLaneOverview.fnSetColumnVis(col, false));
          if (json.egaUploadInProgress) $('#ega-upload-in-Progress').show();
          if (json.anythingWithdrawn) $('#withdrawn_description').show();
          json.aaData.forEach((row) => {
            const pid = row[0];
            row[0] = $.otp.createLinkMarkup({
              controller: 'individual',
              action: 'show',
              text: pid,
              parameters: { pid }
            });
          });
          fnCallback(json);
          oTableLaneOverview.fnSettings().oFeatures.bServerSide = false;

          // Initialize fixed columns after data is loaded
          initializeFixedColumns();
        }
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

    $('#searchCriteriaTableSeqType').find('tr').each((index, element) => {
      const idx = $('td.attribute select', element)[0].selectedIndex;
      if (idx !== 0) {
        result.push((idx - 1) * step + ignoredColumns);
      }
    });

    for (let i = ignoredColumns; i < tableSize; i += step) {
      oTableLaneOverview.fnFilter(result.includes(i) ? '.+' : '', i, true);
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

// Override the returned function of getDownloadButton of otp.js
// to remove extra spaces in the header names
(function () {
  'use strict';

  // save the original function to a variable
  const proxied = $.otp.getDownloadButton;

  $.otp.getDownloadButton = function () {
    const downloadButton = proxied.apply(this, arguments);

    // Add header formatter to remove HTML tags, entities, and extra spaces
    downloadButton[0].exportOptions.format = downloadButton[0].exportOptions.format || {};
    downloadButton[0].exportOptions.format.header = function (html) {
      // First replace &nbsp; entities with spaces
      const withoutNbsp = html.replace(/&nbsp;/g, ' ');
      // Then remove HTML tags, trim and normalize spaces
      return withoutNbsp.replace(/(<([^>]+)>)/ig, '').trim().replace(/\s+/g, ' ');
    };

    return downloadButton;
  };
}());
