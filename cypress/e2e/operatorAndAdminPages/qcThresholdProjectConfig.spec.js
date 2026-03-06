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

describe('Check QC Threshold Project Configuration page', () => {
  'use strict';

  const testDisplayPageAndTables = () => {
    it('should display the qcThreshold project configuration page and should display tables for each QC class', () => {
      cy.get('h1').should('contain.text', 'QC Thresholds');
      cy.get('h1').should('contain.text', 'ExampleProject');

      cy.get('.threshold-table').should('exist').should('have.length.greaterThan', 0);
      cy.get('.threshold-table').each((table) => {
        cy.wrap(table).find('thead').should('exist').and('contain.text', 'Property');
        cy.wrap(table).find('tbody').should('exist');
        cy.wrap(table).find('button.add').should('be.visible');
      });
    });
  };

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/qcThreshold/projectConfiguration');
    });

    testDisplayPageAndTables();

    it('should allow adding a new project QC threshold and save it to the table', () => {
      cy.get('.threshold-table').first().within((table) => {
        cy.get('button.add').first().click();

        // Verify the add form row is now visible
        cy.get('tr.add-table-fields').should('not.have.class', 'd-none');

        cy.get('tr.add-table-fields').not('.d-none').within(() => {
          selectFirstAvailableOptionFromDropdowns();

          // Fill in the threshold values
          cy.get('input[name="errorThresholdLower"]').type('0.15');
          cy.get('input[name="warningThresholdLower"]').type('0.25');
          cy.get('input[name="warningThresholdUpper"]').type('0.75');
          cy.get('input[name="errorThresholdUpper"]').type('0.85');

          // Verify only one Save and one Cancel button are visible
          cy.wrap(table).find('.add-buttons-row').within(() => {
            cy.get('.save:not(.d-none)').should('have.length', 1);
            cy.get('.cancel:not(.d-none)').should('have.length', 1);

            cy.get('.save:not(.d-none)').click();
          });
        });

        // Verify the form is hidden after save
        cy.get('tr.add-table-fields').should('have.class', 'd-none');

        // Verify the new threshold appears in the table
        cy.get('tbody').should('contain.text', '0.15');
        cy.get('tbody').should('contain.text', '0.25');
        cy.get('tbody').should('contain.text', '0.75');
        cy.get('tbody').should('contain.text', '0.85');
      });
    });

    it('should show error toast when adding a duplicate project QC threshold and not defining the thresholds', () => {
      cy.get('.threshold-table').first().within((table) => {
        cy.get('button.add').first().click();

        cy.get('tr.add-table-fields').should('not.have.class', 'd-none');

        cy.get('tr.add-table-fields').not('.d-none').within(() => {
          // Select the first available option from each dropdown (which should already exist)
          selectFirstAvailableOptionFromDropdowns();

          // Click Save button
          cy.wrap(table).find('.add-buttons-row').within(() => {
            cy.get('.save:not(.d-none)').click();
          });
        });
      });
      cy.get('#otpToastBox .otpErrorToast').should('be.visible');
      cy.get('#otpToastBox .otpErrorToast').should('contain.text', 'The threshold could not be stored');
      cy.get('#otpToastBox .otpErrorToast').should('contain.text', 'already exists');
      cy.get('#otpToastBox .otpErrorToast').should(
        'contain.text',
        'When leaving the lower thresholds empty, please define BOTH upper warning and upper error thresholds'
      );
    });

    it('should allow modifying an existing project QC threshold', () => {
      cy.get('.threshold-table').first().within(() => {
        cy.get('tbody tr.edit-table-buttons').first().within(() => {
          cy.get('td').eq(3).then(() => {
            // Click the Edit button
            cy.get('button.button-edit').click();

            cy.get('input.edit-fields').should('not.have.class', 'd-none');

            cy.get('.save:not(.d-none)').should('have.length', 1);
            cy.get('.cancel:not(.d-none)').should('have.length', 1);

            // Modify the lower error threshold value
            cy.get('input[name="errorThresholdLower"]').clear().type('0.2');

            cy.get('.save:not(.d-none)').click();

            // Verify the form is hidden after save
            cy.get('input.edit-fields').should('have.class', 'd-none');

            // Verify the new value appears in the table
            cy.get('td').eq(3).should('contain.text', '0.2');
          });
        });
      });
      cy.get('#otpToastBox .otpSuccessToast').should('exist').and('contain.text', 'updated successfully');
    });

    it('should allow deleting an existing project QC threshold', () => {
      cy.get('.threshold-table').first().within(() => {
        cy.get('tbody tr.edit-table-buttons').then(($rows) => {
          const initialRowCount = $rows.length;

          // Get the first existing threshold row and click delete
          cy.get('tbody tr.edit-table-buttons').first().within(() => {
            cy.get('button[type="submit"]').should('contain.text', 'Delete').click();
          });

          // Verify the row was deleted
          cy.get('tbody tr.edit-table-buttons').should('have.length', initialRowCount - 1);
        });
      });
      cy.get('#otpToastBox .otpSuccessToast').should('exist').and('contain.text', 'deleted successfully');
    });

    it('should allow canceling the add project threshold form', () => {
      cy.get('.threshold-table').first().within((table) => {
        cy.get('tbody tr').then(($rows) => {
          const initialRowCount = $rows.length;

          cy.get('button.add').click();
          cy.get('tr.add-table-fields').should('not.have.class', 'd-none');

          cy.get('tr.add-table-fields').not('.d-none').within(() => {
            cy.get('input[name="errorThresholdLower"]').type('0.5');
          });

          cy.wrap(table).find('.add-buttons-row').within(() => {
            cy.get('.save:not(.d-none)').should('have.length', 1);
            cy.get('.cancel:not(.d-none)').should('have.length', 1);

            cy.get('.cancel:not(.d-none)').click();
          });

          cy.get('tr.add-table-fields').should('have.class', 'd-none');
          cy.get('button.add').should('be.visible');

          // Verify no new row was added to the table
          cy.get('tbody tr').should('have.length', initialRowCount);
        });
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    testDisplayPageAndTables();
  });
});

function selectFirstAvailableOptionFromDropdowns() {
  'use strict';

  cy.selectAvailableOptionFromDropdown('property');
  cy.selectAvailableOptionFromDropdown('seqType.id');
  cy.selectAvailableOptionFromDropdown('condition');
}
