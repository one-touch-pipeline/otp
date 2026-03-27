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

describe('Check alignment quality overview page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should change warning status to accepted', () => {
      cy.visit('/alignmentQualityOverview/index');

      cy.get('#seqType')
        .select('EXOME PAIRED bulk', { force: true });

      cy.get('table#overviewTableProcessedMergedBMF tbody')
        .find('tr')
        .eq(0)
        .find('select.qcDropdown')
        .select('ACCEPTED');

      cy.get('input#modalInput')
        .type('test-comment');

      cy.get('button#confirmModal')
        .click();

      cy.get('.toast-body').should('contain.text', 'success');

      cy.get('table#overviewTableProcessedMergedBMF tbody')
        .find('tr')
        .eq(0)
        .should('contain.text', 'test-comment')
        .should('not.contain.html', 'select');
    });

    // Checks for download files
    it('should download and check the alignment quality control with EXOME', () => {
      loadAlignmentPage('ExampleProject', 'EXOME PAIRED bulk');
      cy.get('div#overviewTableProcessedMergedBMF_wrapper button')
        .contains('Download').click();
      cy.checkDownloadByContentOfFixture('alignmentQualityControlExome.json');
    });

    // For the other Seq types the projectUsingFastQ was used
    // The ExampleProject doesn't have data for other Seq types
    it('should download and check the alignment quality control with 10x_scRNA', () => {
      loadAlignmentPage('projectUsingFastq', '10x_scRNA PAIRED single cell');
      cy.get('div#overviewTableProcessedMergedBMF_wrapper button')
        .contains('Download').click();
      cy.checkDownloadByContentOfFixture('alignmentQualityControl10xscRNA.json');
    });

    it('should download and check the alignment quality control with ChIP paired', () => {
      loadAlignmentPage('projectUsingFastq', 'ChIP PAIRED bulk');
      cy.get('div#overviewTableProcessedMergedBMF_wrapper button')
        .contains('Download').click();
      cy.checkDownloadByContentOfFixture('alignmentQualityControlChIP.json');
    });

    it('should download and check the alignment quality control with RNA SINGLE bulk', () => {
      loadAlignmentPage('projectUsingFastq', 'RNA SINGLE bulk');
      cy.get('div#overviewTableProcessedMergedBMF_wrapper button')
        .contains('Download').click();
      cy.checkDownloadByContentOfFixture('alignmentQualityControlRNA.json');
    });

    it('should download and check the alignment quality control with WGS PAIRED bulk', () => {
      loadAlignmentPage('projectUsingFastq', 'WGS PAIRED bulk');
      cy.get('div#overviewTableProcessedMergedBMF_wrapper button')
        .contains('Download').click();
      cy.checkDownloadByContentOfFixture('alignmentQualityControlWGS.json');
    });
  });

  context('when user is a normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    // Checks for download files
    // Normal User has access only to ExampleProject
    it('should download and check the alignment quality control with EXOME', () => {
      loadAlignmentPage('ExampleProject', 'EXOME PAIRED bulk');
      cy.get('div#overviewTableProcessedMergedBMF_wrapper button')
        .contains('Download').click();
      cy.checkDownloadByContentOfFixture('alignmentQualityControlExome.json');
    });
  });
});

const loadAlignmentPage = (project, seqType) => {
  'use strict';

  cy.clearDownloadsFolder();
  cy.intercept('POST', '/alignmentQualityOverview/dataTableSource*').as('dataTable');
  cy.visit(`/alignmentQualityOverview/index?project=${project}`);
  cy.get('#seqType')
    .select(seqType, { force: true });
  cy.wait('@dataTable').then((interception) => {
    expect(interception.response.statusCode).to.equal(200);
  });
};
