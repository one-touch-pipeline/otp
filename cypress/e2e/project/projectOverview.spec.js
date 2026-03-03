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

  ['operator', 'user'].forEach((role) => {
    context(`when user is ${role === 'operator' ? 'an operator' : 'a normal user'}`, () => {
      before(() => {
        cy.clearDownloadsFolder();
      });

      beforeEach(() => {
        cy.loginAs(role);
      });

      it('should visit the index page', () => {
        cy.intercept('get', '/projectOverview/index*')
          .as('index');

        cy.visit('/projectOverview/index');
        cy.wait('@index')
          .then((interception) => {
            expect(interception.response.statusCode).to.equal(200);
          });
      });

      it('should download and check the sample type name table', () => {
        loadPageAndWaitForTables('projectOverviewSampleType.json');
        cy.get('div#sampleTypeNameCountBySample_wrapper button')
          .contains('Download').click();
        cy.checkDownloadByContentOfFixture('projectOverviewSampleType.json');
      });

      it('should download and check the sequencing center table', () => {
        loadPageAndWaitForTables('projectOverviewSequencingCenter.json');
        cy.get('div#centerNameRunId_wrapper button')
          .contains('Download').click();
        cy.checkDownloadByContentOfFixture('projectOverviewSequencingCenter.json');
      });

      it('should download and check the sequencing samples table', () => {
        loadPageAndWaitForTables('projectOverviewSequences.json');
        cy.get('div#projectOverviewTable_wrapper button')
          .contains('Download').click();
        cy.checkDownloadByContentOfFixture('projectOverviewSequences.json');
      });

      it('should download and check the sequencing type table', () => {
        loadPageAndWaitForTables('projectOverviewSequencingTypes.json');

        cy.get('div#patientsAndSamplesGBCountPerProject_wrapper button')
          .contains('Download').click();
        cy.checkDownloadByContentOfFixture('projectOverviewSequencingTypes.json');
      });

      it('should check the length of the tables', () => {
        cy.visit('/projectOverview/index');

        cy.get('table#sampleTypeNameCountBySample tbody').find('tr').should('have.length', 3);
        cy.get('table#centerNameRunId tbody').find('tr').should('have.length', 1);
        cy.get('table#projectOverviewTable tbody').find('tr').should('have.length', 31);
        cy.get('table#patientsAndSamplesGBCountPerProject tbody').find('tr').should('have.length', 6);
      });
    });
  });
});

const loadPageAndWaitForTables = (fixtureFileName) => {
  'use strict';

  cy.clearDownloadsFolder();

  cy.fixture(`downloadChecks/${fixtureFileName}`).then((config) => {

    cy.intercept('get', '/projectOverview/index*')
      .as('index');
    cy.intercept('POST', `/projectOverview/${config.tableUrl}*`)
      .as('dataTable');

    cy.visit(`/projectOverview/index?project=${config.project}`);

    cy.wait('@index')
      .then((interception) => {
        expect(interception.response.statusCode).to.equal(200);
      });
    cy.wait('@dataTable')
      .then((interception) => {
        expect(interception.response.statusCode).to.equal(200);
      });
  });
};
