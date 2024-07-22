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

$.otp.resultsTable = {

  // Global flag for archived project
  projectArchived: false,
  projectDeleted: false,

  registerDataTable(tableElement, source, columnDefs, convertRowData) {
    'use strict';

    const fileName = `${document.title.replaceAll(' ', '_')}-${$('.selected-project-value strong').text()}`;
    const table = tableElement.DataTable({
      dom: '<i> B rt<"clear">',
      buttons: $.otp.getDownloadButton('', fileName),
      columnDefs,
      filter: true,
      processing: true,
      serverSide: false,
      sort: true,
      autoWidth: false,
      scrollCollapse: true,
      scrollY: 'auto',
      scrollX: 'auto',
      paginate: false,
      deferRender: true,
      ajax: (data, callback) => {
        $.ajax({
          dataType: 'json',
          type: 'POST',
          url: source,
          data,
          error() {
            // clear the table
            callback({
              aaData: [],
              iTotalRecords: 0,
              iTotalDisplayRecords: 0
            });
          },
          success(json) {
            $.otp.resultsTable.projectArchived = json.archived;
            $.otp.resultsTable.projectDeleted = json.deleted;
            callback({
              ...json,
              aaData: json.aaData.map(convertRowData)
            });
          }
        });
      }
    });
    return table;
  },

  /**
   * Get a render function which wraps the data in a div shortened with ellipsis.
   *
   * The max width is given in 'em' and the unshortened data is provided in the title.
   */
  getEllipsisRenderer(nCharacters) {
    'use strict';

    return function (data) {
      return `<div class="trim-text-with-ellipsis" \
                   style="max-width: ${nCharacters}em" \
                   title="${data}">
                     ${data}
              </div>`;
    };
  },

  /**
   * Helper function to return a link to the plots depending on if an instance exists.
   *
   * Returns the link if there is an instance, otherwise an empty string.
   */
  plotLinkHelper(instanceId, controller, plotType) {
    'use strict';

    if ($.otp.resultsTable.projectArchived) {
      return 'Plots archived';
    }
    if ($.otp.resultsTable.projectDeleted) {
      return 'Plots deleted';
    }

    return instanceId ? $.otp.createLinkMarkup({
      controller,
      action: 'plots',
      parameters: {
        'bamFilePairAnalysis.id': instanceId,
        plotType
      },
      text: 'Plots'
    }) : '';
  },

  registerAceseq() {
    'use strict';

    $.otp.resultsTable.registerDataTable(
      $('#resultsTable'),
      $.otp.createLink({
        controller: 'aceseq',
        action: 'dataTableResults'
      }),
      [],
      (row) => [
        $.otp.createLinkMarkup({
          controller: 'individual',
          action: 'show',
          id: row.individualId,
          text: row.individualPid
        }),
        row.sampleTypes,
        row.tcc,
        row.ploidy,
        row.ploidyFactor,
        row.goodnessOfFit,
        row.gender,
        row.solutionPossible,
        $.otp.resultsTable.plotLinkHelper(row.instanceId, 'aceseq', 'ACESEQ_ALL'),
        row.version,
        row.dateCreated,
        row.processingState,
        this.prepareLinkArray(row.configFile)
      ]
    );
  },

  registerIndel() {
    'use strict';

    $.otp.resultsTable.registerDataTable(
      $('#resultsTable'),
      $.otp.createLink({
        controller: 'indel',
        action: 'dataTableResults'
      }),
      [],
      (row) => [
        $.otp.createLinkMarkup({
          controller: 'individual',
          action: 'show',
          id: row.individualId,
          text: row.individualPid
        }),
        row.sampleTypes,
        row.seqType,
        row.libPrepKits,
        // Indels
        row.numIndels,
        row.numIns,
        row.numDels,
        row.numSize1_3,
        row.numSize4_10,
        $.otp.resultsTable.plotLinkHelper(row.instanceId, 'indel', 'INDEL'),
        // Swap Checker
        row.somaticSmallVarsInTumor,
        row.somaticSmallVarsInControl,
        row.somaticSmallVarsInTumorCommonInGnomad,
        row.somaticSmallVarsInControlCommonInGnomad,
        row.somaticSmallVarsInTumorPass,
        row.somaticSmallVarsInControlPass,
        row.germlineSmallVarsInBothRare,
        // TINDA
        row.tindaSomaticAfterRescue,
        row.tindaSomaticAfterRescueMedianAlleleFreqInControl,
        $.otp.resultsTable.plotLinkHelper(row.instanceId, 'indel', 'INDEL_TINDA'),
        // Remaining
        row.version,
        row.dateCreated,
        row.processingState,
        this.prepareLinkArray(row.configFile)
      ]
    );
  },

  registerSophia() {
    'use strict';

    $.otp.resultsTable.registerDataTable(
      $('#resultsTable'),
      $.otp.createLink({
        controller: 'sophia',
        action: 'dataTableResults'
      }),
      [{
        targets: 7,
        render: $.otp.resultsTable.getEllipsisRenderer(20)
      }],
      (row) => [
        $.otp.createLinkMarkup({
          controller: 'individual',
          action: 'show',
          id: row.individualId,
          text: row.individualPid
        }),
        row.sampleTypes,
        row.seqType,
        row.controlMassiveInvPrefilteringLevel,
        row.tumorMassiveInvFilteringLevel,
        row.rnaContaminatedGenesCount,
        row.rnaDecontaminationApplied,
        row.rnaContaminatedGenesMoreThanTwoIntron,
        $.otp.resultsTable.plotLinkHelper(row.instanceId, 'sophia', 'SOPHIA'),
        row.version,
        row.dateCreated,
        row.processingState,
        this.prepareLinkArray(row.configFile)
      ]
    );
  },

  registerSnv() {
    'use strict';

    $.otp.resultsTable.registerDataTable(
      $('#resultsTable'),
      $.otp.createLink({
        controller: 'snv',
        action: 'dataTableResults'
      }),
      [],
      (row) => [
        $.otp.createLinkMarkup({
          controller: 'individual',
          action: 'show',
          id: row.individualId,
          text: row.individualPid
        }),
        row.sampleTypes,
        row.seqType,
        row.libPrepKits,
        $.otp.resultsTable.plotLinkHelper(row.instanceId, 'snv', 'SNV'),
        row.version,
        row.dateCreated,
        row.processingState,
        this.prepareLinkArray(row.configFile)
      ]
    );
  },
  registerRunYapsa() {
    'use strict';

    $.otp.resultsTable.registerDataTable(
      $('#resultsTable'),
      $.otp.createLink({
        controller: 'runYapsa',
        action: 'dataTableResults'
      }),
      [],
      (row) => [
        $.otp.createLinkMarkup({
          controller: 'individual',
          action: 'show',
          id: row.individualId,
          text: row.individualPid
        }),
        row.sampleTypes,
        row.seqType,
        row.libPrepKits,
        row.version,
        row.dateCreated,
        row.processingState,
        this.prepareLinkArray(row.configFile)
      ]
    );
  },
  prepareLinkArray(data) {
    'use strict';

    return data.map((el) => this.prepareLink(el)).join(' | ');
  },
  prepareLink(data) {
    'use strict';

    if (!data.link) {
      return data.value;
    }

    const target = data.linkTarget ? `target="${data.linkTarget}"` : '';
    const href = `href="${data.link}"`;

    return `<a ${target} ${href}>${data.value}</a>`;
  }
};
