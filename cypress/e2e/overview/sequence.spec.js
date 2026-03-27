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

describe('Check statistics page', () => {
  'use strict';

  const userRoles = ['operator', 'user'];

  userRoles.forEach((role) => {
    context(`when user is ${role}`, () => {
      before(() => {
        cy.clearDownloadsFolder();
      });

      beforeEach(() => {
        cy.loginAs(role);
      });

      it('should visit the index page', () => {
        cy.visit('/sequence/index');
      });

      it('should download the csv file and verify the download', () => {
        cy.visit('/sequence/index');

        filterForDownload('sequence.json');

        cy.get('#sequenceTable_processing').should('exist');
        cy.get('div#sequenceTable_wrapper button').contains('Download CSV').click();
        cy.checkDownloadByContentOfFixture('sequence.json', '');
      });

      it('should download the sample swap template and verify the download', () => {
        cy.visit('/sequence/index');

        filterForDownload('dataSwapTemplate.json');

        cy.get('div#sequenceTable_wrapper button').contains('Download Sample Swap Template').click();

        cy.checkDownloadByContentOfFixture('dataSwapTemplate.json', '');
      });

      it('should filter the table by sample type', () => {
        cy.intercept('POST', '/sequence/dataTableSource*').as('loadDataTable');

        const filterType = 'Sample Type';
        const filterToSelect = 'tumor01';

        cy.visit('/sequence/index');

        cy.get('#sequenceTable_processing').should('exist');
        cy.get('div#data-table-filter-container').find('span#select2--container')
          .contains('No Search Criteria')
          .click();
        cy.get('ul#select2--results').contains(filterType).click({ force: true });
        cy.get('span#dtf_sampleTypeSelection').click();
        cy.get('li.select2-results__option').contains(filterToSelect).click();

        cy.get('#searchCriteriaTable td.search input[type=button]').click();
        cy.wait('@loadDataTable')
          .then((interception) => {
            expect(interception.response.statusCode).to.equal(200);
            expect(interception.response.body.aaData).to.have.length(171);
            interception.response.body.aaData.forEach((seq) => expect(seq.sampleTypeName).to.equal(filterToSelect));
          });
      });

      it('should filter the table by sample name', () => {
        cy.intercept('POST', '/sequence/dataTableSource*').as('loadDataTable');

        const filterType = 'Sample Name';
        const filterToSelect = 'sample_20';

        cy.visit('/sequence/index');

        cy.get('#sequenceTable_processing').should('exist');
        cy.get('div#data-table-filter-container').find('span#select2--container')
          .contains('No Search Criteria')
          .click();
        cy.get('ul#select2--results').contains(filterType).click({ force: true });
        cy.get('span#dtf_sampleNameSearch').click();
        cy.get('#searchCriteriaTable tr').eq(0).find('input[name=sampleNameSearch]').type(`${filterToSelect}{enter}`);

        cy.get('#searchCriteriaTable td.search input[type=button]').click();
        cy.wait('@loadDataTable')
          .then((interception) => {
            expect(interception.response.statusCode).to.equal(200);
            expect(interception.response.body.aaData).to.have.length(11);
            interception.response.body.aaData.forEach((seq) => expect(seq.sampleName).to.contains(filterToSelect));
          });
      });
    });
  });
});

const filterForDownload = (fixtureFileName) => {
  'use strict';

  cy.intercept('POST', '/sequence/dataTableSource*').as('loadDataTable');

  cy.fixture(`downloadChecks/${fixtureFileName}`).then((config) => {
    cy.get('div#data-table-filter-container').find('span#select2--container')
      .contains('No Search Criteria')
      .click();

    cy.get('ul#select2--results').contains(config.filterType).click({ force: true });
    cy.get('#searchCriteriaTable td.search input[type=button]').click();

    cy.wait('@loadDataTable')
      .then((interception) => {
        expect(interception.response.statusCode).to.equal(200);
      });
  });
};
