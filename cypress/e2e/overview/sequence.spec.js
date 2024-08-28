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

describe('Check statistics page', () => {
  'use strict';

  context('when user is an operator', () => {
    before(() => {
      cy.clearDownloadsFolder();
    });

    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should visit the index page', () => {
      cy.visit('/sequence/index');
    });

    it('should download the csv file and verify the download', () => {
      cy.visit('/sequence/index');

      cy.get('#sequenceTable_processing').should('exist');
      cy.get('div#sequenceTable_wrapper button').contains('Download CSV').click();
      cy.checkDownloadByContent('Sequence_Export', '.csv', [
        'Project', 'Patient ID', 'Sample Type', 'Seq. Type', 'Sequencing Read Type', 'Single Cell', 'Sample Name',
        'Seq. Center', 'Library Prep. Kit', 'Antibody Target,', 'Run,', 'Lane,', 'Library,',
        'Single Cell Well Label', 'ILSe,', 'Known Issues', 'File Exists', 'File Archived', 'Date,',
        'Withdrawn,', 'Species Common Name', 'Scientific Name', 'Strain,', 'Mixed In Species']);
    });

    it('should download the sample swap template and verify the download', () => {
      cy.visit('/sequence/index');

      cy.get('#sequenceTable_processing').should('exist');
      cy.get('div#sequenceTable_wrapper button').contains('Download Sample Swap Template').click();
      cy.checkDownloadByContent('Sample_Swap_Template', '.csv', [
        'Seq. Track ID', 'ILSe', 'Run', 'Lane', 'Single Cell Well Label',
        'Project Old', 'Project New', 'Patient ID Old', 'Patient ID New',
        'Sample Type Old', 'Sample Type New', 'Seq. Type Old', 'Seq. Type New',
        'Sequencing Read Type', 'Single Cell',
        'Sample Name Old', 'Sample Name New', 'Antibody Target Old', 'Antibody Target New'
      ]);
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
