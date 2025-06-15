/*
 * Copyright 2011-2025 The OTP authors
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

$.otp.refgen = {
  /**
   * Constants
   */
  SPECIES_INDEX: 2,
  DATE_INDEX: 9,
  LEGACY_INDEX: 10,
  AUXILIARY_COLUMNS_INDEX: [1, 3, 4, 6, 7, 8],

  /**
   * Definition of the datatable for reference genome list
   *
   * Note: Showing/hiding columns and rows is implemented with a class called hide_column.
   * The default implementation of datatables doesn't work with OTP EditSwitch control,
   * which associated js event handler gets removed when the column is hidden.
   *
   * @returns a newly created DataTable instance
   */
  exportableReferenceGenomeTable: () => {
    'use strict';

    return new DataTable('#referenceGenome-datatable', {
      dom: '<"row justify-content-end" <"col" B>> t',
      paginate: false,
      ordering: true,
      // legacy data is sorted to the end, which is fixed
      orderFixed: [[$.otp.refgen.LEGACY_INDEX, 'asc']],
      // sort the species column (not fixed)
      order: [[$.otp.refgen.SPECIES_INDEX, 'asc']],
      columnDefs: [
        // trim the date to show only the date part
        {
          targets: $.otp.refgen.DATE_INDEX,
          render: (data) => data.substring(0, 10)
        },
        // disable the sorting of legacy column
        {
          targets: $.otp.refgen.LEGACY_INDEX,
          orderable: false
        }
      ],
      // add the four buttons
      buttons: [
        $.otp.refgen.getToggleEditSwitchButton(),
        $.otp.refgen.getToggleAuxiliaryColumnsButton(),
        $.otp.refgen.getToggleLegacyButton(),
        $.otp.getDownloadButton()
      ],
      // render the legacy data rows, to grey out the text (css: text-muted)
      rowCallback: (row, data) => {
        if (data[$.otp.refgen.LEGACY_INDEX].indexOf('checked') >= 0) {
          $('td', row).addClass('text-muted');
        }
      },

      // eslint-disable-next-line object-shorthand
      initComplete: function () {
        const dt = this.api();

        $.otp.refgen.toggleEditSwitch(false);
        $.otp.refgen.toggleLegacyData(false, dt);
      }
    });
  },

  /**
   * Setup the editSwitch button
   */
  getToggleEditSwitchButton: () => {
    'use strict';

    return [{
      text: 'Show Edit Switch',
      name: 'toggleEditSwitch',
      action: (e, dt) => {
        const btn = $(e.currentTarget);
        const state = btn.data('state') !== true;
        $.otp.refgen.toggleEditSwitch(state, dt);
        btn.data('state', state).text(`${state ? 'Hide' : 'Show'} Edit Switch`);
      }
    }];
  },

  /**
   * Setup the toggle button for auxiliary columns
   */
  getToggleAuxiliaryColumnsButton: () => {
    'use strict';

    return [{
      text: 'Show Auxiliary Columns',
      name: 'toggleAuxiliaryColumns',
      action: (e, dt) => {
        const btn = $(e.currentTarget);
        const state = btn.data('state') !== true;
        $.otp.refgen.toggleColumnVisibility(state, dt);
        btn.data('state', state).text(`${state ? 'Hide' : 'Show'} Auxiliary Columns`);
      }
    }];
  },

  /**
   * Setup the toggle button for legacy data
   */
  getToggleLegacyButton: () => {
    'use strict';

    return [{
      text: 'Show Legacy Data',
      name: 'toggleLegacyData',
      action: (e, dt) => {
        const btn = $(e.currentTarget);
        const state = btn.data('state') !== true;
        $.otp.refgen.toggleLegacyData(state, dt);
        btn.data('state', state).text(`${state ? 'Hide' : 'Show'} Legacy Data`);
      }
    }];
  },

  /**
   * Show or hide the edit switch
   * @param state true to show; false to hide
   */
  toggleEditSwitch: (state) => {
    'use strict';

    const dt = $('#referenceGenome-datatable').DataTable();
    const nodes = dt.columns().nodes().flatten().to$();
    if (state) {
      nodes.find('button.js-edit').show();
      nodes.find('input.slider').prop('disabled', '');
    } else {
      nodes.find('button.js-edit').hide();
      nodes.find('input.slider').prop('disabled', 'disabled');
    }
  },

  /**
   * Show or hide the auxiliary columns
   * @param state true to show; false to hide
   */
  toggleColumnVisibility: (state) => {
    'use strict';

    $('#referenceGenome-datatable').DataTable().columns($.otp.refgen.AUXILIARY_COLUMNS_INDEX).visible(state);
  },

  /**
   * Show or hide the legacy data
   * @param state true to show; false to hide
   * @param dt is the datatables api object
   */
  toggleLegacyData: (state, dt) => {
    'use strict';

    // eslint-disable-next-line array-callback-return
    dt.rows().every(function () {
      const row = this.node();
      if (this.data()[$.otp.refgen.LEGACY_INDEX].indexOf('checked') >= 0) {
        $(row)[state ? 'show' : 'hide']();
      }
    });
  }
};

$(() => {
  'use strict';

  $.otp.refgen.exportableReferenceGenomeTable();

  // hide the auxiliary columns after the table is rendered completely
  // unfortunately not working if called in the initComplete() function
  $(() => $.otp.refgen.toggleColumnVisibility(false));
});

// Override the returned function of getDownloadButton of otp.js
// to make multiple Species in different lines by downloading
(function () {
  'use strict';

  // save the original function to a variable
  const proxied = $.otp.getDownloadButton;

  $.otp.getDownloadButton = function () {
    // eslint-disable-next-line prefer-rest-params
    const downloadButton = proxied.apply(this, arguments);
    const proxiedInner = downloadButton[0].exportOptions.format.body;
    downloadButton[0].exportOptions.format.body = function (html, row, col, node) {
      // hidden rows are not downloaded
      // eslint-disable-next-line prefer-rest-params
      return $(node).is(':visible') ? proxiedInner.apply(this, arguments)
        .trim()
        .replace(/(\s){2,}/g, '; ') : ''; // separate multiple rows in one cell with semicolons
    };
    return downloadButton;
  };
}());
