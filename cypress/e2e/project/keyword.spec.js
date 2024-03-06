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

describe('test for keyword page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should visit the keyword page and add a custom keyword', () => {
      cy.visit('/keyword/index');
      cy.intercept('/keyword/createOrAdd*').as('createKeyword');
      const keyword = 'custom keyword';

      cy.get('div#project-keywords input#value').type(keyword);
      cy.get('div#project-keywords input#Add').click();

      cy.wait('@createKeyword');

      cy.get('div#project-keywords').contains(keyword);
    });

    it('should add an existing keyword', () => {
      cy.visit('/keyword/index');
      cy.intercept('/keyword/add*').as('addKeyword');

      cy.get('input#add-keyword2').click();
      cy.wait('@addKeyword');

      cy.get('div#project-keywords').contains('keyword2');
    });

    it('should add an existing keyword', () => {
      cy.visit('/keyword/index');
      cy.intercept('/keyword/createOrAdd*').as('addKeyword');
      const keyword = 'keyword1';

      cy.get('div#project-keywords input#value').type(keyword);
      cy.get('div#project-keywords input#Add').click();

      cy.wait('@addKeyword');

      cy.get('div#project-keywords').contains(keyword);
    });

    it('should remove all keywords', () => {
      cy.visit('/keyword/index');
      cy.intercept('//keyword/remove*').as('removeKeyword');

      cy.get('input[id="remove-custom keyword"]').click();
      cy.wait('@removeKeyword');

      cy.get('input[id="remove-keyword1"]').click();
      cy.wait('@removeKeyword');

      cy.get('input[id="remove-keyword2"]').click();
      cy.wait('@removeKeyword');

      cy.get('input[id="remove-keyword2"]').should('not.exist');
      cy.get('input[id="remove-custom keyword"]').should('not.exist');
    });
  });

  context('when user is a user with project access', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should deny access to keyword page', () => {
      cy.checkAccessDenied('/keyword/index');
    });
  });

  context('when user is a user without project access', () => {
    beforeEach(() => {
      cy.loginAsDepartmentHeadUser();
    });

    it('should deny access to keyword page', () => {
      cy.checkAccessDenied('/keyword/index', 404);
    });
  });
});
