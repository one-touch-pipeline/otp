/*
 * Copyright 2011-2023 The OTP authors
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

describe('Check keywords page', () => {
  'use strict';

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });
    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/keyword');
    });
  });

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should add new keyword', () => {
      cy.visit('/keyword');
      cy.get('#value').type('newKeyword');
      cy.get('#Add').click();
      cy.get('#project-keywords ul').contains('newKeyword');
    });

    it('should remove new keyword', () => {
      cy.visit('/keyword');
      cy.get('#project-keywords #remove-newKeyword').click();
      cy.get('#project-keywords ul').should('not.contain', 'newKeyword');
    });

    it('should add existing keyword', () => {
      cy.visit('/keyword');
      cy.get('#all-keywords #add-keyword1').first().click();
      cy.get('#project-keywords ul').contains('keyword1');
    });

    it('should remove keyword, it is still shown', () => {
      cy.visit('/keyword');
      cy.get('#project-keywords #remove-keyword1').click();
      cy.get('#project-keywords ul').should('not.contain', 'keyword1');
      cy.get('#all-keywords ul').contains('keyword1');
    });
  });
});
