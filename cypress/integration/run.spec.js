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

    it.skip('should download csv files and check if the files exist', () => {
      cy.visit('/run/list');

      cy.get('button').contains('Download').click();
      cy.checkDownloadByByteSize('list_of_runs', '.csv', 2484);
    });

    it('visits some show pages by clicking on runs', () => {
      cy.visit('/run/list');

      cy.get('table#runTable tbody').find('tr').eq(3)
        .find('td')
        .eq(0)
        .find('a')
        .click();
      cy.checkPage('/run/show');

      cy.visit('/run/list');
      cy.get('table#runTable tbody').find('tr').eq(12)
        .find('td')
        .eq(0)
        .find('a')
        .click();
      cy.checkPage('/run/show');

      cy.visit('/run/list');
      cy.get('table#runTable tbody').find('tr').eq(47)
        .find('td')
        .eq(0)
        .find('a')
        .click();
      cy.checkPage('/run/show');
    });
  });
});
