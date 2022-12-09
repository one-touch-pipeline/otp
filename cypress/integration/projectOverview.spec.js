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

describe('Check statistics page', () => {
  'use strict';

  context('when user is an operator', () => {
    before(() => {
      cy.clearDownloadsFolder();
    });

    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should visit the index page', () => {
      cy.visit('/projectOverview/index');
    });

    // checks for download files
    it('should download all csv files by clicking the buttons and verify the downloads', () => {
      cy.visit('/projectOverview/index');

      cy.get('div#sampleTypeNameCountBySample_wrapper button')
        .contains('Download').click();
      // eslint-disable-next-line max-len
      cy.checkDownloadByMd5Sum('number_of_samples_by_sample_type-ExampleProject', '.csv', '86c24f467eb02942db7a168c8dfbabed');

      cy.get('div#centerNameRunId_wrapper button')
        .contains('Download').click();
      cy.checkDownloadByMd5Sum('sequencing_center-ExampleProject', '.csv', 'eb2d0de62d478b80deb15da2c2d87857');

      cy.get('div#projectOverviewTable_wrapper button')
        .contains('Download').click();
      cy.checkDownloadByMd5Sum('sequences_of_samples-ExampleProject', '.csv', '5de0472d9144405fcea6e9c8aadc3ef6');

      cy.get('div#patientsAndSamplesGBCountPerProject_wrapper button')
        .contains('Download').click();
      cy.checkDownloadByMd5Sum('sequencing_technologies-ExampleProject', '.csv', '4b2a37ef52e61b2e6459ef5ec23e3d52');
    });

    it('should check the length of the tables', () => {
      cy.visit('/projectOverview/index');

      cy.get('table#sampleTypeNameCountBySample tbody').find('tr').should('have.length', 3);
      cy.get('table#centerNameRunId tbody').find('tr').should('have.length', 1);
      cy.get('table#projectOverviewTable tbody').find('tr').should('have.length', 30);
      cy.get('table#patientsAndSamplesGBCountPerProject tbody').find('tr').should('have.length', 5);
    });
  });
});
