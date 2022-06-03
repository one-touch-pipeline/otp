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

$.otp.projectConfig = {

  returnParameterUnchanged(json) {
    'use strict';

    return json;
  },

  registerDataTable(selector, url, successUpdate) {
    'use strict';

    const oTable = $(selector).dataTable({
      sDom: '<i> T rt<"clear">',
      oTableTools: $.otp.tableTools,
      bFilter: true,
      bProcessing: true,
      bServerSide: false,
      bSort: true,
      bAutoWidth: false,
      sAjaxSource: url,
      bScrollCollapse: true,
      bPaginate: false,
      bDeferRender: true,
      fnServerData(sSource, aoData, fnCallback) {
        $.ajax({
          dataType: 'json',
          url: sSource,
          data: aoData,
          error() {
            // clear the table
            fnCallback({ aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0 });
            oTable.fnSettings().oFeatures.bServerSide = false;
          },
          success(json) {
            const result = successUpdate(json);
            fnCallback(result);
            oTable.fnSettings().oFeatures.bServerSide = false;
          }
        });
      }
    });
    return oTable;
  },

  referenceGenome() {
    'use strict';

    $.otp.projectConfig.registerDataTable(
      '#listReferenceGenome',
      $.otp.createLink({
        controller: 'alignmentConfigurationOverview',
        action: 'dataTableSourceReferenceGenome'
      }),
      $.otp.projectConfig.returnParameterUnchanged
    );
  },

  /**
     * Asynchronous calls the getAlignmentInfo from the  ProjectOverviewController.
     * While loading displays a Loading text until the data arrived.
     * @deprecated method is part of the old workflow system
     */
  asynchronousCallAlignmentInfo() {
    'use strict';

    $.ajax({
      url: $.otp.createLink({
        controller: 'alignmentConfigurationOverview',
        action: 'getAlignmentInfo'
      }),
      dataType: 'json',
      success(data) {
        $.otp.projectConfig.initialiseAlignmentInfo(data);
      },
      beforeSend() {
        $.otp.projectConfig.displayLoading();
      }
    });
  },

  /**
     * If the loading of the Data is successfull, the Loading animation will be removed.
     * If the loaded Data is empty no Alginment is displayed. If not,
     * the AlginmentInfo table is displayed.
     * @param data holds the data that have been loaded
     * @deprecated method is part of the old workflow system
     */
  initialiseAlignmentInfo(data) {
    'use strict';

    $('#loader').remove();
    if (data.alignmentInfo ? (data.alignmentInfo.length > 0) : false) {
      $('#alignment_info_table').css('visibility', 'visible');
      $.otp.projectConfig.createAlignmentTable(data);
    } else {
      $('#alignment_info').html('No Alignment');
    }
  },

  /**
     * Adds a loading animation to the top of the alignment Info.
     * @deprecated method is part of the old workflow system
     */
  displayLoading() {
    'use strict';

    $('#alignment_info').css(
      'display',
      'inline',
      'vertical-alignment',
      'top'
    ).prepend('<div id="loader" class="loader"></div>');
  },

  /**
     * Adds rows and columns including content to the Table
     * which is taken from the achieved data.
     * @param data holds the data that have been loaded
     * @deprecated method is part of the old workflow system
     */
  createAlignmentTable(data) {
    'use strict';

    $.each(data.alignmentInfo, (key, value) => {
      $('#alignment_info_table tr:last').after(
        `<tr>
           <td colspan="3"><strong> ${key} </strong></td>
           <td></td>
           <td></td>
         </tr>
         <tr>
           <td style="padding: 5px; white-space: nowrap;">Aligning</td>
           <td>${value.alignmentProgram}</td>
           <td>${value.alignmentParameter}</td>
         </tr>
         <tr>
           <td style="padding: 5px; white-space: nowrap;">Merging</td>
           <td>${value.mergeCommand}</td>
           <td>${value.mergeOptions}</td>           
         </tr>
         <tr>
           <td style="padding: 5px; white-space: nowrap;">Samtools</td>
           <td>${value.samToolsCommand}</td>
           <td></td>
         </tr>
         <tr>
           <td style="padding: 5px; white-space: nowrap;">Roddy Pipeline Version</td>
           <td>${value.programVersion}</td>
           <td></td>
         </tr>`
      );
    });
  }
};

$(() => {
  'use strict';

  $.otp.projectConfig.referenceGenome();
  $.otp.projectConfig.asynchronousCallAlignmentInfo();
});
