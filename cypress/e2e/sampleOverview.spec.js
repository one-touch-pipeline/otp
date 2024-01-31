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

describe('Check sampleOverview page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should filter the table by Sample Type', () => {
      cy.visit('/sampleOverview/index');

      const sampleTypeName = 'tumor01';
      cy.get('span#select2--container').contains('Sample Type').click();
      cy.get('li').contains(sampleTypeName).click();
      cy.get('table#laneOverviewId td.dataTables_empty').should('not.exist');

      cy.get('table#laneOverviewId tbody').find('tr').each((tableRow) => {
        cy.wrap(tableRow).find('td').eq(1).should('contain', 'tumor01');
      });
    });

    it('should add and remove filters for Seq Type', () => {
      cy.visit('/sampleOverview/index');

      const seqTypeName = 'WGS PAIRED bulk';
      cy.get('span#select2--container').contains('Seq. Type').click();
      cy.get('li').contains(seqTypeName).click();
      cy.get('td.add input').click();
      cy.get('span#select2--container').contains(seqTypeName).should('have.length', 1);

      cy.get('table#searchCriteriaTableSeqType').find('tr').eq(0).find('td.remove input')
        .click({ force: true });
      cy.get('span#select2--container').contains('WGS PAIRED bulk').should('not.exist');
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should not be able to select a project user is not part of', () => {
      cy.visit('/sampleOverview/index');
      cy.get('select#project option').should('not.have.text', 'Example project 1');
    });

    it('should load data table on initialization and download csv file, when button is clicked', () => {
      cy.intercept('/sampleOverview/dataTableSourceLaneOverview*').as('loadDataTable');

      cy.visit('/sampleOverview/index');

      cy.wait('@loadDataTable').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });

      cy.get('table#laneOverviewId').find('tbody tr').should('have.length', 8);

      // Download Csv file via Button
      cy.get('button.buttons-csv').click();

      cy.checkDownloadByContent('Sample_Overview-ExampleProject', '.csv', [
        'Patient ID', 'Sample Type', 'Registered Lanes', 'cell ranger',
        '10x_scRNA PAIRED single cell', 'EXAMPLE PAIRED bulk', 'EXOME PAIRED bulk', 'WGS PAIRED bulk',
        'WGBS PAIRED bulk', 'WGBS_TAG PAIRED bulk'
      ]);
    });
  });
});
