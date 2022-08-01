/*
 * Copyright 2011-2021 The OTP authors
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

  /**
     * Formats the all cells in a table row and puts them in the correct order
     * @param row map containing the data in one row with column names as the keys
     * @param columnNames name of the columns in the correct order
     * @returns {Array}
     */
  const tableRowsFormatter = function (row, columnNames) {
    let resultTableRow = [];

    columnNames.forEach((name) => {
      resultTableRow[name.data] = tableCellFormatter(row[name.data]);
    });

    if (row.withdrawn) {
      const withdrawnRows = [];
      $.each(resultTableRow, function () {
        withdrawnRows.push(`<span class='withdrawn'>${this} </span>`);
      });
      resultTableRow = withdrawnRows;
    }
    return resultTableRow;
  };

  /**
     * Format a TableCellValue dataset to DataTable cell format.
     *
     * @param tableCellData
     * @returns {string|*|Window.jQuery}
     */
  const tableCellFormatter = function (tableCellData) {
    if (!tableCellData) {
      return '';
    }
    if ((tableCellData.value === undefined) && (tableCellData.icon === undefined)) {
      return $('<div>').text(tableCellData).html(); // escape HTML
    }
    if (tableCellData.status === 'BLOCKED' || tableCellData.status === 'REJECTED') {
      return `<span id="status-cell-${tableCellData.id}" title="${tableCellData.tooltip}">
                <select class="qcDropdown ${tableCellData.status} custom-select custom-select-sm" 
                        data-id="${tableCellData.id}">
                  <option class="ACCEPTED" ${(tableCellData.status === 'ACCEPTED' ? 'selected' : '')}
                          value="ACCEPTED">&#10003; ACCEPTED</option>
                  <option class="BLOCKED" ${(tableCellData.status === 'BLOCKED' ? 'selected' : '')}
                          disabled value="BLOCKED">&#9888; BLOCKED</option>
                  <option class="REJECTED" ${(tableCellData.status === 'REJECTED' ? 'selected' : '')}
                          disabled value="REJECTED">&#10005; REJECTED</option>
                </select>
                ${$('<div>').text(tableCellData.value).html()}
              </span>`;
    }
    if (tableCellData.status === 'WARNING') {
      return `<span id="status-cell-${tableCellData.id}" title="${tableCellData.tooltip}">
                <select class="qcDropdown ${tableCellData.status} custom-select custom-select-sm"
                        data-id="${tableCellData.id}">
                  <option value="ACCEPTED" class="ACCEPTED">&#10003; ACCEPTED</option>
                  <option value="WARNING" class="WARNING" selected>&#9888; WARNING</option>
                </select>
                ${$('<div>').text(tableCellData.value).html()}
              </span>`;
    }

    let result = $('<div>').text(tableCellData.value).html(); // escape HTML
    const cssClass = [];

    if (tableCellData.warnColor) {
      cssClass.push(`text-${tableCellData.warnColor}`);
    }
    if (tableCellData.icon) {
      cssClass.push(`icon-${tableCellData.icon}`);
    }
    if (cssClass.length !== 0) {
      result = `<span class="${cssClass}">${result} </span>`;
    }

    if (tableCellData.tooltip) {
      result = `<span title="${tableCellData.tooltip}">${result}</span>`;
    }
    if (tableCellData.link) {
      const target = tableCellData.linkTarget ? `target="${tableCellData.linkTarget}"` : '';
      const href = `href="${tableCellData.link}"`;

      result = tableCellData.archived ? `&#128451; <a class="archived" ${target} ${href}>${result}</a>` :
        `<a ${target} ${href}>${result}</a>`;
    }
    return result;
  };

  /**
     * Get the list of the column names for the generic DataTable header depending on the selected seqType.
     *
     */
  const getDataTableColumns = function () {
    const workflow = $('#seqType').data('columns');

    let columnNames = [
      { data: 'rowId', visible: false, className: 'no_csv_export' },
      { data: 'pid' },
      { data: 'sampleType' },
      { data: 'qcStatus', className: 'no_csv_export maxWidth230', render: renderQcStatusColumn },
      { data: 'qcStatusOnly', visible: false },
      { data: 'qcComment', visible: false },
      { data: 'qcAuthor', visible: false },
      { data: 'dbVersion', visible: false, className: 'no_csv_export' }
    ];

    // coverage
    if (workflow === 'PANCANCER_AND_WGBS') {
      columnNames = columnNames.concat([
        { data: 'coverageWithoutN' },
        { data: 'coverageX' },
        { data: 'coverageY' }
      ]);
    } else if (workflow === 'PANCANCER_BED') {
      columnNames = columnNames.concat([
        { data: 'onTargetRatio' },
        { data: 'targetCoverage' }
      ]);
    } else if (workflow === 'RNA') {
      columnNames = columnNames.concat([
        { data: 'arribaPlots' },
        { data: 'totalReadCounter' },
        { data: 'percentDuplicates' },
        { data: 'threePNorm' },
        { data: 'fivePNorm' },
        { data: 'chimericPairs' },
        { data: 'duplicatesRate' },
        { data: 'end1Sense' },
        { data: 'end2Sense' },
        { data: 'estimatedLibrarySize' },
        { data: 'exonicRate' },
        { data: 'expressionProfilingEfficiency' },
        { data: 'genesDetected' },
        { data: 'intergenicRate' },
        { data: 'intragenicRate' },
        { data: 'intronicRate' },
        { data: 'mapped' },
        { data: 'mappedUnique' },
        { data: 'mappedUniqueRateOfTotal' },
        { data: 'mappingRate' },
        { data: 'meanCV' },
        { data: 'uniqueRateofMapped' },
        { data: 'rRNARate' }
      ]);
    }

    // general information
    if (workflow === 'RNA') {
      columnNames = columnNames.concat([
        { data: 'kit' },
        { data: 'dateFromFileSystem' }
      ]);
    } else if (workflow === 'CELL_RANGER') {
      columnNames = columnNames.concat([
        { data: 'summary' },
        { data: 'cellRangerVersion' },
        { data: 'referenceGenome' },
        { data: 'expectedCells' },
        { data: 'enforcedCells' },
        { data: 'estimatedNumberOfCells' },
        { data: 'meanReadsPerCell' },
        { data: 'medianGenesPerCell' },
        { data: 'numberOfReads' },
        { data: 'validBarcodes' },
        { data: 'sequencingSaturation' },
        { data: 'q30BasesInBarcode' },
        { data: 'q30BasesInRnaRead' },
        { data: 'q30BasesInUmi' },
        { data: 'readsMappedConfidentlyToIntergenicRegions' },
        { data: 'readsMappedConfidentlyToIntronicRegions' },
        { data: 'readsMappedConfidentlyToExonicRegions' },
        { data: 'readsMappedConfidentlyToTranscriptome' },
        { data: 'fractionReadsInCells' },
        { data: 'totalGenesDetected' },
        { data: 'medianUmiCountsPerCell' },
        { data: 'kit' },
        { data: 'dateFromFileSystem' }
      ]);
    } else {
      columnNames = columnNames.concat([
        { data: 'kit' },
        { data: 'percentMappedReads' },
        { data: 'percentDuplicates' },
        { data: 'percentProperlyPaired' },
        { data: 'percentSingletons' },
        { data: 'insertSizeMedian' },
        { data: 'percentDiffChr' },
        { data: 'pipeline' },
        { data: 'dateFromFileSystem' }
      ]);
    }
    return columnNames;
  };

  /**
     * Initialize the DataTable with it's default settings.
     */
  const initDataTable = function () {
    const table = $('#overviewTableProcessedMergedBMF');
    const fileName = `Alignment_Quality_Control-${$('.selected-project-value strong').text()}`;

    table.DataTable({
      sDom: '<i> B rt<"clear">',
      buttons: $.otp.getDownloadButton(':not(.no_csv_export)', fileName, syncAndRenderDataTable),
      bFilter: true,
      bProcessing: true,
      bServerSide: false,
      bSort: true,
      bAutoWidth: false,
      sScrollY: 'auto',
      sScrollX: 'auto',
      bScrollCollapse: false,
      bPaginate: false,
      bDeferRender: true,
      columns: getDataTableColumns()
    });

    syncAndRenderDataTable();
  };

  /**
     * Sync the qc alignment data with the backend and render the fetched data into the DataTable.
     *
     * @param callback, fired when the sync and rendering is finished
     */
  const syncAndRenderDataTable = function (callback = () => {}) {
    const seqType = $('#seqType');

    if (seqType.val()) {
      const dataTable = $('#overviewTableProcessedMergedBMF').DataTable();
      dataTable.clear().draw();

      enableLoadingSpinner();

      const aoData = [
        {
          name: 'seqType',
          value: seqType.val()
        },
        {
          name: 'sample',
          value: $('#sample').data('sample')
        }
      ];

      $.ajax({
        dataType: 'json',
        type: 'POST',
        url: $.otp.createLink({
          controller: 'alignmentQualityOverview',
          action: 'dataTableSource'
        }),
        data: aoData,
        error() {
          $.otp.toaster.showErrorToast('Data Initialization', 'Loading the alignment quality overview data failed.');
          disableLoadingSpinner();
        },
        success(result) {
          result.aaData.forEach((row) => {
            dataTable.row.add(tableRowsFormatter(row, getDataTableColumns()));
          });

          dataTable.draw();

          disableLoadingSpinner();
          callback();
        }
      });
    }
  };

  const disableLoadingSpinner = function () {
    $('#alignmentQualityOverviewSpinner').hide();
  };

  const enableLoadingSpinner = function () {
    $('#alignmentQualityOverviewSpinner').show();
  };

  /**
     * Fires an update ajax call to change the QC Status to the selected one.
     *
     * @param dropdownMenu, with the selectable status options
     * @param abstractBamFileId
     */
  const changeQcStatus = function (dropdownMenu, abstractBamFileId) {
    const currentDropdownMenu = dropdownMenu;
    const oldValue = $(currentDropdownMenu).find('option[selected]').val();
    const statusCell = $(`#status-cell-${abstractBamFileId}`);
    const statusCellOldContent = statusCell.html();
    const rowData = getTableDataRowById(abstractBamFileId);
    const msgSuffix = `<br><br><b>(${rowData.pid}, ${rowData.sampleType})</b>`;
    const modalTitle = buildModalTitle(currentDropdownMenu.value);

    updateQcChangeDefaultComment(currentDropdownMenu.value);

    openConfirmationModal(modalTitle, (comment) => {
      if (comment == null) {
        currentDropdownMenu.value = oldValue;
      } else {
        statusCell.html(`<div class="spinner-border spinner-border-sm" role="status">
                             <span class="sr-only">Loading...</span>
                         </div>`);

        $.ajax({
          dataType: 'json',
          type: 'POST',
          url: $.otp.createLink({
            controller: 'alignmentQualityOverview',
            action: 'changeQcStatus'
          }),
          data: {
            'abstractBamFile.id': abstractBamFileId,
            newValue: dropdownMenu.value,
            comment,
            dbVersion: rowData.dbVersion
          },
          error(error) {
            if (error && error.responseJSON && error.responseJSON.message) {
              $.otp.toaster.showErrorToast('Status change failed', error.responseJSON.message + msgSuffix);
            } else {
              $.otp.toaster.showErrorToast('Status change failed', `Unknown error.${msgSuffix}`);
            }
            currentDropdownMenu.value = oldValue;
            statusCell.html(statusCellOldContent);
          },
          success(result) {
            statusCell.html(tableCellFormatter(result));
            $.otp.toaster.showSuccessToast(
              'QC Status change',
              `Status successfully changed to ${result.status}.${msgSuffix}`
            );
          }
        });
      }
    }, () => {
      currentDropdownMenu.value = oldValue;
      statusCell.html(statusCellOldContent);
    });
  };

  /**
     * Custom implementation of the DataTables render function for the qc status column.
     * It takes care of the render type. When the type = "sort", the render function will
     * return a sort key and otherwise the display value.
     *
     * @param data of the cell
     * @param type for the rendering mode (sort|display|filter)
     * @param row, data of the row
     * @returns {number|*}
     */
  const renderQcStatusColumn = function (data, type, row) {
    let position = 3;

    if (row.qcStatusOnly) {
      switch (row.qcStatusOnly) {
        case 'ACCEPTED': position = 0; break;
        case 'REJECTED': position = 1; break;
        case 'BLOCKED': position = 2; break;
        default: position = 3; break;
      }
    }

    return type === 'sort' ? position : data;
  };

  /**
     * Generate a dynamic title for the modal headline depending on the new qc status.
     *
     * @param newQcStatus which is ACCEPTED or REJECTED
     * @returns {string} new modal title
     */
  const buildModalTitle = function (newQcStatus) {
    let modalTitle = 'QC status';

    if (newQcStatus === 'ACCEPTED') {
      modalTitle = `Accept ${modalTitle}`;
    } else if (newQcStatus === 'REJECTED') {
      modalTitle = `Reject ${modalTitle}`;
    }

    return modalTitle;
  };

  /**
     * Change the default text for the comment inside the modal.
     * Default is the text of the last comment as long as the state doesn't change.
     * The default comment will be cleared when the state changes.
     *
     * @param newQcStatus which is ACCEPTED or REJECTED
     */
  const updateQcChangeDefaultComment = function (newQcStatus) {
    const modalInput = $('#modalInput');

    if (modalInput.attr('data-mode') !== newQcStatus) {
      modalInput.val('');
      modalInput.attr('data-mode', newQcStatus);
    }
  };

  /**
     * Open the confirmation modal for the qc status change.
     *
     * @param title, shown in the headline
     * @param confirmCallback, performed when confirm button was pressed
     * @param closeCallback, performed when a close button was pressed
     */
  const openConfirmationModal = function (title, confirmCallback, closeCallback) {
    const modal = $('#confirmModal');
    const confirmButton = modal.find('#confirmModal');
    const closeButtons = modal.find('.closeModal');

    modal.find('.modal-title').text(title);

    confirmButton.unbind('click');
    confirmButton.on('click', () => {
      modal.hide();
      const modalInput = modal.find('#modalInput').val();
      confirmCallback(modalInput);
    });

    closeButtons.unbind('click');
    closeButtons.on('click', () => {
      closeCallback();
    });

    modal.modal('toggle').show();
  };

  /**
     * Get the data of a table row as a json object by it's id.
     *
     * @param id of the requested row
     * @returns {*} json object of the row data
     */
  const getTableDataRowById = function (id) {
    const dataTable = $('#overviewTableProcessedMergedBMF').DataTable();

    return dataTable.rows((idx, data) => data.rowId === id.toString()).data()[0];
  };

  /**
     * Bind onchange event to the QC status change dropdown menu.
     */
  $('#overviewTableProcessedMergedBMF').on('change', '.qcDropdown', (event) => {
    changeQcStatus(event.target, $(event.target).data('id'));
  });

  $('#confirmModal').on('shown.bs.modal', () => {
    $('#modalInput').trigger('focus');
  });

  initDataTable();
});
