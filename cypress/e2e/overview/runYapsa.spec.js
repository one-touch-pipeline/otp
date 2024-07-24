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

describe('Check run yapsa page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.fixture('bigProject.json').then((config) => {
        cy.visit(`/runYapsa/results?project=${config[0].projectNameUsedForTables}`);
      });
    });

    it('should visit the result page', () => {
      cy.log('Waiting until loading is done.');
      cy.get('table tbody tr').contains('Loading...').should('not.exist');

    });

    it('should download csv, when button is clicked', () => {
      cy.get('table tbody tr').contains('Loading...').should('not.exist');
      cy.get('div#resultsTable_wrapper button').contains('Download').click();

      cy.fixture('bigProject.json').then((config) => {
        cy.checkDownloadByContent(`Mutational_Signatures_Results_(from_runYapsa)-${config[0].projectNameUsedForTables}`, '.csv', [
          'Patient ID', 'Sample Types', 'Seq. Type', 'Library Prep. Kit(s)', 'Created with Version', 'Processing Date', 'Progress'
        ]);
      });
    });
  });
});
