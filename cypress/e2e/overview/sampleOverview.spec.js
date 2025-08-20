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

describe('Check sampleOverview page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/sampleOverview/index?project=ExampleProject');
    });

    it('should visit the index page and change projects', () => {
      cy.get('#select2-project-container').should('contain.text', 'ExampleProject');
      cy.get('#laneOverviewId').should('not.be.empty');
      cy.get('table#laneOverviewId td.dataTables_empty').should('not.exist');

      cy.get('.form-select[name="sampleTypeSelection"]').should('contain.text', 'Sample Type');
      cy.get('.form-select[name="seqTypeSelection"]').should('contain.text', 'Seq. Type');

      // Intercept the data table request for the project change
      cy.intercept('POST', '**/dataTableSourceLaneOverview**').as('projectChangeDataTable');

      cy.get('#project.form-select').select('Example project 1', { force: true });
      cy.url().should('include', 'Example+project+1');

      // Wait for data table to reload after project change
      cy.wait('@projectChangeDataTable');

      cy.get('#laneOverviewId').should('not.be.empty');
      cy.get('table#laneOverviewId td.dataTables_empty').should('exist')
        .should('have.text', 'No data available in table');
    });

    it('should filter the table by Sample Type', () => {
      // Note: This filter is applied client-side, no AJAX request is made
      // The DataTable's built-in filtering is used (oTableLaneOverview.fnFilter)
      const sampleTypeName = 'tumor01';
      cy.get('.form-select[name="sampleTypeSelection"]').as('sampleTypeSelection').contains('Sample Type');

      // Select the sample type which will trigger client-side filtering
      cy.get('@sampleTypeSelection').select(sampleTypeName, { force: true });

      // Verify the table is filtered correctly
      cy.get('table#laneOverviewId td.dataTables_empty').should('not.exist');
      cy.get('table#laneOverviewId tbody tr').should('have.length.at.least', 1);
      cy.get('table#laneOverviewId tbody tr td:nth-child(2)').each(($cell) => {
        expect($cell.text()).to.equal(sampleTypeName);
      });
      cy.get('#laneOverviewId_info').should('contain.text', 'filtered from');
    });

    it('should add and remove filters for Seq Type', () => {
      const seqTypeName = 'WGS PAIRED bulk';
      cy.get('.form-select[name="seqTypeSelection"]').as('seqTypeSelection').contains('Seq. Type');
      cy.get('@seqTypeSelection').select(seqTypeName, { force: true });
      cy.get('@seqTypeSelection').find('option:selected').should('have.text', seqTypeName);
      cy.get('td.search input').click();

      // Verify the table is filtered
      cy.get('table#laneOverviewId td.dataTables_empty').should('not.exist');
      cy.get('table#laneOverviewId tbody tr').should('have.length.at.least', 1);
      cy.get('#laneOverviewId_info').should('contain.text', 'filtered from');

      // Add another filter row
      cy.get('td.add input').click();
      cy.get('@seqTypeSelection').should('have.length', 2);
      cy.get('@seqTypeSelection').find('option:selected').should('contain.text', seqTypeName);

      // Remove the filter
      cy.get('table#searchCriteriaTableSeqType').find('tr').eq(0).find('td.remove input')
        .click({ force: true });

      cy.get('@seqTypeSelection').find('option:selected').should('not.have.text', seqTypeName);
      cy.get('@seqTypeSelection').should('have.length', 1);
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
      cy.visit('/sampleOverview/index?project=ExampleProject');
    });

    it('should not be able to select a project user is not part of', () => {
      cy.get('select#project option').should('not.have.text', 'Example project 1');
    });

    it('should load data table on initialization and download CSV file, when button is clicked', () => {
      // Clear downloads folder before test
      cy.clearDownloadsFolder();

      // Intercept the data table request
      cy.intercept('POST', '**/dataTableSourceLaneOverview**').as('loadDataTable');

      cy.visit('/sampleOverview/index?project=ExampleProject');

      cy.wait('@loadDataTable').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      // Verify table has rows
      cy.get('table#laneOverviewId').find('tbody tr').should('have.length.at.least', 1);

      // Download CSV file via Button
      cy.get('button.buttons-csv').should('be.visible').click();

      // Verify the downloaded file
      cy.checkDownloadByContentOfFixture('sampleOverview.json');
    });
  });
});
