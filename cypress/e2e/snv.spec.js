/*
 * Copyright 2011-2022 The OTP authors
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

describe('Check snv pages', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.fixture('bigProject.json').then((config) => {
        cy.visit(`/snv/results?project=${config[0].projectNameUsedForTables}`);
      });
    });

    it('should visit the plots page when clicking on Plots and check if plot exists', () => {
      cy.log('Waiting until loading is done.');
      cy.get('table tbody tr').contains('Loading...').should('not.exist');

      cy.get('table tbody tr').eq(2).find('a').contains('Plots')
        .click();

      cy.get('object[data]:not([data=""])').should('exist');
      cy.checkPage('/snv/plots');
    });

    it('should download csv, when button is clicked', () => {
      cy.get('table tbody tr').contains('Loading...').should('not.exist');
      cy.get('div#resultsTable_wrapper button').contains('Download').click();

      cy.fixture('bigProject.json').then((config) => {
        cy.checkDownloadByContent(`SNV_Results-${config[0].projectNameUsedForTables}`, '.csv', [
          'Patient ID', 'Sample Types', 'Seq. Type', 'Library Prep. Kit(s)', 'Link to Plots',
          'Created with Version', 'Processing Date', 'Progress'
        ]);
      });
    });
  });
});
