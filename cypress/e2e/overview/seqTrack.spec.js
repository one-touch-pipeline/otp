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

describe('Check seq track pages', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should visit the seq track set page by starting on project overview page', () => {
      cy.visit('/projectOverview/index');
      cy.get('table#projectOverviewTable tbody').find('tr').eq(5)
        .find('td')
        .eq(0)
        .find('a')
        .click();

      cy.get('table tbody tr').find('a').eq(2)
        .click();
      cy.checkPage('seqTrack/seqTrackSet');

      cy.visit('/projectOverview/index');
      cy.get('table#projectOverviewTable tbody').find('tr').eq(23)
        .find('td')
        .eq(0)
        .find('a')
        .click();
      cy.get('table tbody tr').find('a').eq(10).click();
      cy.checkPage('seqTrack/seqTrackSet');
    });

    it('should visit the run show page by starting on project overview page via the run page', () => {
      cy.visit('/projectOverview/index');
      cy.get('table#projectOverviewTable tbody').find('tr').eq(12)
        .find('td')
        .eq(0)
        .find('a')
        .click();

      cy.get('table tbody tr').find('a').eq(2)
        .click();
      cy.checkPage('seqTrack/seqTrackSet');

      cy.get('div.run-information').contains('run').click();
      cy.checkPage('run/show');
    });
  });
});
