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

describe('Check reference genome overview page', () => {
  'use strict';

  const BUTTON_TEXTS = {
    SHOW_EDIT_SWITCH: 'Show Edit Switch',
    HIDE_EDIT_SWITCH: 'Hide Edit Switch',
    SHOW_AUX_COLUMNS: 'Show Auxiliary Columns',
    HIDE_AUX_COLUMNS: 'Hide Auxiliary Columns',
    SHOW_LEGACY_DATA: 'Show Legacy Data',
    HIDE_LEGACY_DATA: 'Hide Legacy Data'
  };

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.fixture('referenceGenomeOverview/ui.json').as('referenceGenomeOverview');
      cy.visit('/referenceGenome/index');
    });

    // Initial page with all editorSwitches turn off, legacy data hidden, and auxiliary columns hidden
    it('should verify initial page state and buttons', () => {
      cy.get('div#referenceGenome-datatable_wrapper').should('exist').within(() => {
        // We have 11 columns in total, but only 5 are shown by default
        cy.get('th').should('have.length', 5);
        cy.get('tr:has(td)').first().find('td').should('have.length', 5);
        // Check all the shown UI elements exist
        cy.get('@referenceGenomeOverview').then((fixture) => {
          cy.get('th').should('have.length', 5).each((item, index) => {
            cy.wrap(item).should('contain.text', fixture.defaultColumns[index]);
          });

          // Date column shows only date w/o time
          cy.get('td').eq(3).then(($date) => {
            cy.logDebug('Date column', $date.text());
            cy.wrap($date).contains(/\d{4}-\d{2}-\d{2}/);
          });

          // Check if all buttons shown
          fixture.buttons.forEach((buttonText) => {
            cy.get(`button:contains("${buttonText}")`).should('exist');
          });
        });

        // No EditorSwitches are shown
        cy.get('button.js-edit').should('not.be.visible');

        // No legacy data is shown
        cy.get('tr:has(td).text-muted').should('not.be.visible');

        // Show the auxiliary columns and check if they are 11 intotal
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_AUX_COLUMNS}")`).click();
        cy.get('th').should('have.length', 11);
        cy.get('tr:has(td)').first().find('td').should('have.length', 11);

        // Check the data for the test are available.
        cy.get(`button:contains(${BUTTON_TEXTS.SHOW_LEGACY_DATA})`).click();
        cy.get('table#referenceGenome-datatable>tbody').within(() => {
          cy.get('tr:has(td)').its('length').should('be.at.least', 2);
          // Check there are reference genomes with legacy data and not legacy exist
          cy.get('tr:has(td).text-muted').its('length').should('be.at.least', 1);
          cy.get('tr:has(td)').not('.text-muted').its('length').should('be.at.least', 1);
        });
      });

      // All tooltips should be available
      cy.get('table#referenceGenome-datatable thead th').each(($th) => {
        cy.wrap($th).find('i.helper-icon').trigger('mouseover');
        cy.wrap($th).find('i.helper-icon').should('have.attr', 'title').and('not.be.empty');
      });
    });

    // Checks if all buttons work as expected
    it('should toggle buttons and verify their effects', () => {
      cy.get('div#referenceGenome-datatable_wrapper').within(() => {
        cy.toggleButton(BUTTON_TEXTS.SHOW_EDIT_SWITCH, 'p.edit-switch-label>button', 'be.visible');
        cy.toggleButton(BUTTON_TEXTS.HIDE_EDIT_SWITCH, 'p.edit-switch-label>button', 'not.be.visible');

        // Toggle button: Show/Hide Edit Switch
        // On the default page they are hidden
        cy.get('div form input[type="checkbox"]').each((item) => {
          cy.wrap(item).should('not.be.visible').and('have.attr', 'disabled');
        });

        // After clicking the show button, all edit switches should be shown
        cy.toggleButton(BUTTON_TEXTS.SHOW_EDIT_SWITCH, 'p.edit-switch-label>button', 'be.visible');
        cy.get('div form input[type="checkbox"]').each((item) => {
          cy.wrap(item).should('not.be.visible').and('not.have.attr', 'disabled');
        });

        // Hide them again
        cy.toggleButton(BUTTON_TEXTS.HIDE_EDIT_SWITCH, 'p.edit-switch-label>button', 'not.be.visible');
        cy.get('div form input[type="checkbox"]').each((item) => {
          cy.wrap(item).should('not.be.visible').and('have.attr', 'disabled');
        });

        // Toggle button: Show/Hide Auxiliary Columns
        // Only default columns are shown initially
        cy.get('@referenceGenomeOverview').then((fixture) => {
          cy.get('th').should('have.length', fixture.defaultColumns.length);
        });

        // After clicking, all columns should be shown
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_AUX_COLUMNS}")`).click();
        cy.get('@referenceGenomeOverview').then((fixture) => {
          cy.get('th').should('have.length', fixture.allColumns.length);
        });

        // Hide them again
        cy.get(`button:contains("${BUTTON_TEXTS.HIDE_AUX_COLUMNS}")`).click();
        cy.get('@referenceGenomeOverview').then((fixture) => {
          cy.get('th').should('have.length', fixture.defaultColumns.length);
        });

        // Toggle button: Show/Hide Legacy Data
        // No legacy rows are shown initially
        cy.get('tr.text-muted').should('not.be.visible');

        // After clicking the show button, legacy rows should be shown
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_LEGACY_DATA}")`).click();
        cy.get('tr.text-muted').should('exist').and('be.visible');

        // Hide them again
        cy.get(`button:contains("${BUTTON_TEXTS.HIDE_LEGACY_DATA}")`).click();
        cy.get('tr.text-muted').should('not.be.visible');
      });
    });

    // Checks if all editorSwitches work after switching on
    it('should check the toggling of editSwitch button', () => {
      cy.get('div#referenceGenome-datatable_wrapper').within(() => {
        // Case 1: Show Edit Switch -> Show Auxiliary Columns
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_AUX_COLUMNS}")`).click();

        cy.get('th').should('contain.text', 'File name prefix');
        cy.get('td').eq(1).find('button.js-edit').should('be.visible');

        // To save time let's check only the first row
        cy.get('tr:has(td)').not('.text-muted').first().within(() => {
          cy.get('td').each(($col, $idx) => {
            if ($idx < 9) {
              cy.wrap($col).find('button.js-edit').should('be.visible').click({ multiple: true });
              cy.wrap($col).find('input').should('exist').and('be.visible');
              cy.wrap($col).find('button.save').should('be.visible').and('be.enabled');
              cy.wrap($col).find('button.cancel').should('be.visible').and('be.enabled');
              cy.wrap($col).find('button.cancel').click({ multiple: true });
            }
          });
        });

        cy.get(`button:contains("${BUTTON_TEXTS.HIDE_EDIT_SWITCH}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.HIDE_AUX_COLUMNS}")`).click();

        // Case 2: Show Auxiliary Columns -> Show Edit Switch
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_AUX_COLUMNS}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();

        cy.get('th').should('contain.text', 'File name prefix');
        cy.get('td').eq(1).find('button.js-edit').should('be.visible');

        cy.get(`button:contains("${BUTTON_TEXTS.HIDE_EDIT_SWITCH}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.HIDE_AUX_COLUMNS}")`).click();

        // Case 3: Show Edit Switch -> Show Legacy Data
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_LEGACY_DATA}")`).click();

        cy.get('tr').last().find('td').last()
          .find('input.slider')
          .should('be.checked');
        cy.get('td').eq(1).find('button.js-edit').should('be.visible');

        cy.get('tr.text-muted').should('exist').and('be.visible');
        cy.get('p.edit-switch-label>button').should('exist').and('be.visible');

        // just check one row
        cy.get('tr').eq(3).find('p.edit-switch-label>button').should('be.visible')
          .click({ multiple: true });
        cy.get('tr').eq(3).within(() => {
          cy.get('input').should('be.visible');
          cy.get('button.save').should('be.visible').and('be.enabled');
          cy.get('button.cancel').should('be.visible').and('be.enabled');
          cy.get('button.cancel').click({ multiple: true });
        });
      });
    });

    it('should handle auxiliary columns editing in different button sequences', () => {
      cy.get('div#referenceGenome-datatable_wrapper').within(() => {
        // Sequence 1: Show Edit -> Show Auxiliary
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_AUX_COLUMNS}")`).click();
        cy.get('p.edit-switch-label>button').should('be.visible').and('be.enabled');

        // Reset
        cy.get(`button:contains("${BUTTON_TEXTS.HIDE_AUX_COLUMNS}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.HIDE_EDIT_SWITCH}")`).click();

        // Sequence 2: Show Auxiliary -> Show Edit
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_AUX_COLUMNS}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();
        cy.get('p.edit-switch-label>button').should('be.visible').and('be.enabled');
      });
    });

    // Check if the legacy state toggling works
    it('should toggle legacy state when slider is checked or unchecked', () => {
      cy.get('table#referenceGenome-datatable tbody tr').first().within(() => {
        cy.get('td').eq(4).find('input.slider').as('slider');
      });
      cy.get('@slider').should('not.be.checked');

      // Enable the edit switch
      cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();
      // Mark it as legacy by checking the slider
      cy.get('@slider').check({ force: true });
      // This row will be hidden from the table, since it becomes legacy
      cy.get('@slider').should('not.be.visible');

      // Show the legacy data in the table
      cy.get(`button:contains("${BUTTON_TEXTS.SHOW_LEGACY_DATA}")`).click();

      // it is shown as the last row
      cy.get('table#referenceGenome-datatable tbody tr').last().within(() => {
        cy.get('td').eq(4).find('input.slider').as('slider');
      });
      // and is checked
      cy.get('@slider').should('be.checked');

      // Enable the edit switch again so that we can uncheck it
      cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();
      cy.get('@slider').uncheck({ force: true });

      // It is back to the first row and not checked
      cy.get('table#referenceGenome-datatable tbody tr').first().within(() => {
        cy.get('td').eq(4).find('input.slider').should('not.be.checked');
      });
    });

    it('should validate edit functionality for reference genome fields', () => {
      const NAME_ORI = 'GRCm38mm10_PhiX';
      const NAME_NEW = 'New Value';
      const NAME_BWA = 'bwa';
      const NAME_TMP = 'Temporary Value';

      cy.intercept('/referenceGenome/updateName/*').as('updateName');

      cy.get('div#referenceGenome-datatable_wrapper').within(() => {
        // Setup edit mode
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();

        cy.get(`div.edit-switch>p.edit-switch-label>span:contains("${NAME_ORI}"):not(:contains("${NAME_BWA}"))`).first()
          .parent().parent()
          .within(() => {
            cy.get('p.edit-switch-label>button').click();
            cy.get('input.edit-switch-input').clear().type(NAME_NEW);
            cy.get('button.save').click();
          });
      });

      // Check if the name was updated successfully
      cy.wait('@updateName').its('response.statusCode').should('eq', 200);
      cy.contains(NAME_NEW).should('exist');
      cy.get('#otpToastBox .otpSuccessToast').should('exist')
        .and('contain.text', 'Data stored successfully');

      // Change it back to the original value
      cy.get('div#referenceGenome-datatable_wrapper').within(() => {
        cy.get(`div.edit-switch>p.edit-switch-label>span:contains("${NAME_NEW}")`).first()
          .parent().parent()
          .within(() => {
            cy.get('p.edit-switch-label>button').click();
            cy.get('input.edit-switch-input').clear().type(NAME_ORI);
            cy.get('button.save').click();
          });
      });

      // Verify the name is back to the previous value
      cy.wait('@updateName').its('response.statusCode').should('eq', 200);
      cy.contains(NAME_ORI).should('exist');
      cy.get('#otpToastBox .otpSuccessToast').should('exist')
        .and('contain.text', 'Data stored successfully');

      // Test cancel operation
      cy.get('div#referenceGenome-datatable_wrapper').within(() => {
        cy.get('p.edit-switch-label>button').first().click();
        cy.get('input.edit-switch-input').first().clear().type(NAME_TMP);
        cy.get('button.cancel').first().click();
        cy.contains(NAME_TMP).should('not.exist');
      });
    });

    it('should show error toast if trying to assign empty value to name field', () => {
      cy.intercept('POST', '/referenceGenome/updateName/*').as('updateName');

      cy.get('#otpToastBox .otpErrorToast').should('not.exist');

      cy.get('div#referenceGenome-datatable_wrapper').within(() => {
        // Setup edit mode
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();

        // Edit using empty value should trigger error
        cy.get('p.edit-switch-label>button').first().click({ force: true });
        cy.get('input.edit-switch-input').first().clear(); // Empty input
        cy.get('button.save').first().click({ force: true });
      });

      // check the toast messages
      cy.wait('@updateName').then((interception) => {
        expect(interception.response.statusCode).to.equal(400);
        cy.logDebug('Real backend response:', JSON.stringify(interception.response.body));
      });
      // Activate the code below after the issue (otp-2703) has resolved
      // https://one-touch-pipeline.myjetbrains.com/youtrack/agiles/106-5/current?issue=otp-2703
      /* ```
      cy.get('#otpToastBox .otpErrorToast').should('have.length', 1)
        .and('be.visible')
        .and('contain.text', 'Request failed')
        .and('contain.text', 'There is one error')
        .and('contain.text', 'Field "name" cannot be empty');
      ``` */
    });

    it('should show error toast if trying to remove all species', () => {
      cy.intercept('POST', '/referenceGenome/updateSpecies/*').as('updateSpeciesWithStrain');

      cy.get('#otpToastBox .otpErrorToast').should('not.exist');

      cy.get('div#referenceGenome-datatable_wrapper').within(() => {
        // Setup edit mode
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_AUX_COLUMNS}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_LEGACY_DATA}")`).click();
      });

      // Remove all Species should trigger error
      cy.get('table tr:has(td)').eq(2).find('td').eq(2)
        .within(() => {
          cy.get('div.edit-switch-multi-drop-down').then(($switch) => {
            if ($switch.find('span.select2-selection__choice__remove').length > 0) {
              cy.wrap($switch).find('span.select2-selection__choice__remove').each(($x) => {
                const parentElms = $x.parentsUntil('p.edit-switch-editor');
                const select2Elm = parentElms[parentElms.length - 1];
                cy.wrap(select2Elm).should('have.attr', 'data-select2-id');
                cy.logDebug('Found select2 id in dataset:', select2Elm.dataset.select2Id);
                cy.wrap(select2Elm).parent().find('button.save').as('saveButton');
                cy.wrap($x).click({ force: true });
              });
              cy.get('@saveButton').click({ force: true });
            }
          });
        });

      // check the toast messages
      cy.wait('@updateSpeciesWithStrain').its('response.statusCode').should('eq', 400);
      // Activate the code below after the issue (otp-2703) has resolved
      // https://one-touch-pipeline.myjetbrains.com/youtrack/agiles/106-5/current?issue=otp-2703
      /* ```
      cy.get('#otpToastBox .otpErrorToast').should('have.length', 1)
        .and('be.visible')
        .and('contain.text', 'Request failed')
        .and('contain.text', 'There is one error')
        .and('contain.text', 'Reference genome needs at least one species');
      ``` */
    });

    it('should show error toast if trying to set wrong number to genome length', () => {
      cy.intercept('POST', '/referenceGenome/updateLength/*').as('updateLength');

      cy.get('#otpToastBox .otpErrorToast').should('not.exist');
      cy.get('div#referenceGenome-datatable_wrapper').within(() => {
        // Setup edit mode
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_AUX_COLUMNS}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_EDIT_SWITCH}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_LEGACY_DATA}")`).click();
      });

      cy.get('table tr:has(td)').eq(2).find('td').eq(3)
        .within(() => {
          // Edit using empty value should trigger error
          cy.get('p.edit-switch-label>button').first().click();
          cy.get('input.edit-switch-input').first().clear().type('-100'); // negative number
          cy.get('button.save').first().click();
        });

      // check the toast messages
      cy.wait('@updateLength').then((interception) => {
        expect(interception.response.statusCode).to.equal(400);
        cy.logDebug('Real backend response:', JSON.stringify(interception.response));
      });

      // Activate the code below after the issue (otp-2703) has resolved
      // https://one-touch-pipeline.myjetbrains.com/youtrack/agiles/106-5/current?issue=otp-2703
      /* ```
      cy.get('#otpToastBox .otpErrorToast').should('have.length', 1)
        .and('be.visible')
        .and('contain.text', 'Request failed')
        .and('contain.text', 'There is one error')
        .and('contain.text', 'Field "length" with value "-100" must be greater than zero');
      ``` */
    });

    // Check for downloading files
    it('should download all csv files by clicking the buttons and verify the downloads', () => {
      cy.fixture('referenceGenomeOverview/content').then((config) => {
        // No legacy and without auxiliary columns
        cy.logDebug('No legacy and without auxiliary columns');
        cy.get('div#referenceGenome-datatable_wrapper button:has(span:contains("Download CSV"))').click();
        cy.checkDownloadByContent(
          config.filename,
          config.extension,
          config.compact.header,
          config.compact.data
        );

        // With legacy and without auxiliary columns
        cy.logDebug('With legacy and without auxiliary columns');
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_LEGACY_DATA}")`).click();
        cy.get('div#referenceGenome-datatable_wrapper button:has(span:contains("Download CSV"))').click();
        cy.checkDownloadByContent(
          config.filename,
          config.extension,
          config.compact.header,
          config.compact.data.concat(config.compact.data_legacy)
        );
        cy.get(`button:contains("${BUTTON_TEXTS.HIDE_LEGACY_DATA}")`).click();

        // No legacy and with auxiliary columns
        cy.logDebug('No legacy and with auxiliary columns');
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_AUX_COLUMNS}")`).click();
        cy.get('div#referenceGenome-datatable_wrapper button:has(span:contains("Download CSV"))').click();
        cy.checkDownloadByContent(
          config.filename,
          config.extension,
          config.full.header,
          config.full.data
        );
        cy.get(`button:contains("${BUTTON_TEXTS.HIDE_AUX_COLUMNS}")`).click();

        // With legacy and with auxiliary columns
        cy.logDebug('With legacy and with auxiliary columns');
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_AUX_COLUMNS}")`).click();
        cy.get(`button:contains("${BUTTON_TEXTS.SHOW_LEGACY_DATA}")`).click();
        cy.get('div#referenceGenome-datatable_wrapper button:has(span:contains("Download CSV"))').click();
        cy.checkDownloadByContent(
          config.filename,
          config.extension,
          config.full.header,
          config.full.data.concat(config.full.data_legacy)
        );
      });
    });
  });
});
