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

$(() => {
  'use strict';

  $.otp.processingTimeStatistics.registerDataTable();
  $.otp.processingTimeStatistics.registerDatePicker();
});

$.otp.processingTimeStatistics = {

  error(msg) {
    'use strict';

    const errorContainer = $('.error-container');
    const errorList = $('.error-list');
    errorList.append(`<p>${msg}</p>`);
    errorContainer.show();

    $('.close-error').on('click', () => {
      errorList.children('p').remove();
      errorContainer.hide();
    });
  },

  edit(element) {
    'use strict';

    const type = $(element).attr('id');
    const input = prompt(`Enter new ${type}`, $(element).parent().find('#edit-value')[0].innerHTML);

    $.ajax({
      dataType: 'json',
      type: 'POST',
      url: $.otp.createLink({
        controller: 'processingTimeStatistics',
        action: 'editValue'
      }),
      data: {
        id: $(element).parent()[0].id,
        value: input,
        type
      },
      error() {
        let errorMsg = `Failed to edit value '${type}'.`;
        if (type === 'ticketCreated' || type === 'SubmissionReceivedNotice') {
          errorMsg += " Check for date format ('yyyy-MM-dd HH:mm').";
        }
        $.otp.processingTimeStatistics.error(errorMsg);
      },
      success(json) {
        $(element).parent().find('#edit-value')[0].innerHTML = json.value;
        $('#processingTimeStatisticsTable').DataTable().draw();
      }
    });
  },

  registerDatePicker() {
    'use strict';

    $('.datePicker').on('change', () => {
      $('#processingTimeStatisticsTable').DataTable().destroy();
      $.otp.processingTimeStatistics.registerDataTable();
      $('#dpTo').attr('min', $('#dpFrom').val());
      $('#dpFrom').attr('max', $('#dpTo').val());
    });
  },

  registerDataTable() {
    'use strict';

    const table = $('#processingTimeStatisticsTable').dataTable({
      bProcessing: true,
      bServerSide: true,
      bSort: false,
      bAutoWidth: false,
      sAjaxSource: $.otp.createLink({
        controller: 'processingTimeStatistics',
        action: 'dataTableSource',
        parameters: { from: $('#dpFrom').val(), to: $('#dpTo').val() }
      }),
      bScrollCollapse: true,
      bPaginate: false,
      sScrollX: 'auto',
      sScrollXInner: '100%',
      sScrollY: 800,
      bDeferRender: true,
      fnServerData(sSource, aoData, fnCallback) {
        const input = $('.dataTables_filter input');
        if (input.val().length > 0 && input.val().length < 3) {
          return;
        }
        $.ajax({
          dataType: 'json',
          type: 'POST',
          url: sSource,
          data: aoData,
          error() {
            $.otp.processingTimeStatistics.error('Failed to load table. Check the database connection.');
          },
          success(json) {
            for (let i = 0; i < json.aaData.length; i += 1) {
              const row = json.aaData[i];

              // link OTRS ticket url
              row[0] = $.otp.processingTimeStatistics.createIsFinished(
                row[27],
                `<a href="${row[0]}">${row[29]}</a>`
              );

              row[1] = row[1].join('<br>');

              // shorten project names to max 10 character and add tooltip
              for (let x = 0; x < row[2].length; x += 1) {
                if (row[2][x].length > 10) {
                  row[2][x] = `<p title="${row[2][x]}">${row[2][x].substring(0, 10)}...</p>`;
                }
              }

              // add new line between projects
              row[2] = row[2].join('<br>');

              // display size, list of objects in title
              row[3] = `<p title="${row[3].join('\n')}">${row[3].length}</p>`;
              row[4] = `<p title="${row[4].join('\n')}">${row[4].length}</p>`;
              row[5] = `<p title="${row[5].join('\n')}">${row[5].length}</p>`;

              // add edit-button to fields
              row[6] = `<div class="edit-container" id="${row[28]}">
                            <div id="edit-value">
                                ${row[6]}
                            </div>
                            <button class="edit" 
                                    id="submissionReceivedNotice" 
                                    onclick="$.otp.processingTimeStatistics.edit(this)">    
                                &nbsp;
                            </button>
                        </div>`;
              row[8] = `<div class="edit-container" id="${row[28]}">
                            <div id="edit-value">
                                ${row[8]}
                            </div>
                            <button class="edit" 
                                    id="ticketCreated" 
                                    onclick="$.otp.processingTimeStatistics.edit(this)">
                                &nbsp;
                            </button>
                        </div>`;
              row[25] = $.otp.processingTimeStatistics.createIsFinished(row[27], row[25]);

              row[26] = `<div class="edit-container" id="${row[28]}">
                             <div id="edit-value">
                                 ${row[26]}
                             </div>
                             <button class="edit" 
                                     id="comment" 
                                     onclick="$.otp.processingTimeStatistics.edit(this)">
                                 &nbsp;
                             </button>
                         </div>`;

              row.pop(); // finalNotificationSent
              row.pop(); // ticketId
              row.pop(); // ticketNumber
            }
            fnCallback(json);
          }
        });
      }
    });

    table.on('search.dt', () => {
      const input = $('.dataTables_filter input');
      if (input.val().length < 3) {
        input.css('color', 'red');
      } else {
        input.css('color', 'black');
      }
    });
  },

  createIsFinished(finished, row) {
    'use strict';

    return `<span class="${(finished ? 'finalProcessingStatusSent' : 'finalProcessingStatusNotSent')}">
                ${row}
            </span>`;
  }
};
