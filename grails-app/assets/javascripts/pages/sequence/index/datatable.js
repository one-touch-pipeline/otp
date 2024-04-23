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

$.otp.sequence = {

  register() {
    'use strict';

    const searchCriteria = $.otp.dataTableFilter.register($('#searchCriteriaTable'), () => {
      $('#sequenceTable').dataTable().fnDraw();
    });
    const showRunLinks = document.getElementById('showRunLinks').value;

    $('#sequenceTable').DataTable({
      dom: '<"row align-items-end" <"col" Bfr>><"row" t>Si',
      buttons: [
        $.otp.showOrHideColumn(() => $.otp.showOrHideColumn()),
        $.otp.getDownloadButtonServerSide(() => $.otp.createLink({
          controller: 'sequence',
          action: 'exportAll',
          parameters: { filtering: JSON.stringify(searchCriteria()) }
        }), 'Download CSV'),
        $.otp.getDownloadButtonServerSide(() => $.otp.createLink({
          controller: 'sequence',
          action: 'sampleSwapTemplate',
          parameters: { filtering: JSON.stringify(searchCriteria()) }
        }), 'Download Sample Swap Template')
      ],
      bFilter: false,
      bProcessing: true,
      bServerSide: true,
      bSort: true,
      bAutoWidth: true,
      sAjaxSource: $.otp.createLink({
        controller: 'sequence',
        action: 'dataTableSource'
      }),
      sScrollX: 'auto',
      sScrollXInner: '100%',
      sScrollY: 500,
      iDisplayLength: 150,
      bDeferRender: true,
      scroller: true,
      columnDefs: [{
        targets: 23,
        visible: false
      }],
      fnServerData(sSource, aoData, fnCallback) {
        aoData.push({
          name: 'filtering',
          value: JSON.stringify(searchCriteria())
        });
        $.ajax({
          dataType: 'json',
          type: 'POST',
          url: sSource,
          data: aoData,
          scroller: {
            boundaryScale: 1,
            loadingIndicator: true
          },
          error() {
            fnCallback({ aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0 });
          },
          success(json) {
            const result = json;
            $('#withdrawn_description').hide();
            let i; let j; let rowData; let row; let fastQC;
            for (i = 0; i < json.aaData.length; i += 1) {
              row = json.aaData[i];
              if (row.fastQCFiles !== undefined) {
                fastQC = row.fileArchived ? '<span>&#128451;</span>' : '';
                for (j = 0; j < row.fastQCFiles.length; j += 1) {
                  fastQC += $.otp.createLinkMarkup({
                    controller: 'fastqcResults',
                    action: 'show',
                    id: row.fastQCFiles[j].fastqId,
                    text: row.fastQCFiles[j].readName
                  });
                  fastQC += ' ';
                }
              } else {
                fastQC = row.fastqcState;
              }
              let runLink = row.name ? `<span title="${row.name}">${row.name}</span>` : '';
              if (showRunLinks === 'true') {
                runLink = $.otp.createLinkMarkup({
                  controller: 'run',
                  action: 'show',
                  id: row.runId,
                  title: row.name,
                  text: row.name
                });
              }
              // changes here may require changes in fnRowCallback, where for some column additional values are set
              rowData = [
                $.otp.createLinkMarkup({
                  controller: 'projectOverview',
                  action: 'index',
                  parameters: {
                    [$.otp.projectParameter]: row.projectName
                  },
                  text: row.projectName,
                  title: row.projectName
                }),
                `<div class="trim-text-with-ellipsis-left-based"><bdi>${$.otp.createLinkMarkup({
                  controller: 'individual',
                  action: 'show',
                  id: row.individualId,
                  title: row.pid,
                  text: row.pid
                })}</bdi>
                 </div>`,
                row.sampleTypeName,
                row.seqTypeDisplayName,
                row.libraryLayout,
                row.singleCell,
                row.sampleName,
                row.seqCenterName,
                row.libraryPreparationKit ?
                  `<span title="${row.libraryPreparationKit}">${row.libraryPreparationKit}</span>` : '',
                row.antibodyTarget,
                runLink,
                row.laneId ?
                  `<span title="${row.laneId}">${row.laneId}</span>` : '',
                row.libraryName,
                row.singleCellWellLabel,
                fastQC,
                row.ilseId,
                row.problem ?
                  `<span title="${row.problemDescription}">${row.problem}</span>` : '',
                row.fileExists,
                row.fileArchived,
                row.dateCreated,
                row.speciesCommonName,
                row.scientificName,
                row.strain,
                row.mixedInSpecies,
                row.dataFormat
              ];

              if (row.withdrawn) {
                $('#withdrawn_description').show();
                const withdrawnRow = [];
                rowData.forEach((rowEntry) => {
                  withdrawnRow.push(rowEntry != null ? `<span class='withdrawn'>${rowEntry}</span>` : '');
                });
                rowData = withdrawnRow;
              }

              result.aaData[i] = rowData;
            }
            fnCallback(result);
          }
        });
      },
      fnRowCallback(nRow) {
        const fastqc = $('td:eq(14)', nRow);
        if ($('a', fastqc).length > 0) {
          fastqc.addClass('true');
          if ($('span', fastqc).length > 0) {
            fastqc.addClass('archived');
          }
        } else {
          fastqc.attr('title', fastqc.text());
          fastqc.addClass('false');
          fastqc.text('');
        }

        const contamination = $('td:eq(16)', nRow);
        if (contamination.text() === '') {
          contamination.attr('title', 'No known problems');
          contamination.addClass('VALID');
        } else {
          contamination.addClass('warning');
        }

        const fileExists = $('td:eq(17)', nRow);
        const fileExistsWithdrawn = $('td:eq(16) span', nRow);
        if (fileExists.text() === 'true' || fileExistsWithdrawn.text() === 'true') {
          fileExists.addClass('VALID');
        } else {
          fileExists.addClass('false');
        }
        fileExists.attr('title', fileExists.text());
        fileExists.text('');

        const fileArchived = $('td:eq(18)', nRow);
        if (fileArchived.text() === 'true') {
          fileArchived.addClass('VALID');
        } else {
          fileArchived.addClass('false');
        }
        fileArchived.attr('title', fileArchived.text());
        fileArchived.text('');
      }
    });
  }
};

$(() => {
  'use strict';

  $.otp.sequence.register();
});
