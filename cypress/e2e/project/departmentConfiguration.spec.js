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

describe('test for department configuration page', () => {
  'use strict';

  context('when user is a user which is a department head', () => {
    beforeEach(() => {
      cy.loginAs('departmentHead');
    });

    it('should redirect to index when deputy username doesnt exist and show errors message', () => {
      cy.intercept('/departmentConfiguration/addDeputy*').as('addDeputy');
      cy.intercept('/departmentConfiguration/index*').as('loadIndex');

      cy.visit('/departmentConfiguration/index');

      cy.get('input#deputyUsername').type('nonExistentUser');
      cy.get('button#addDeputyBtn').click();

      cy.wait('@addDeputy').its('response.statusCode').should('eq', 302);
      cy.wait('@loadIndex').its('response.statusCode').should('eq', 200);

      cy.get('#otpToastBox').should('exist').contains('can not be resolved');
      cy.get('table#deputyTable tbody tr').should('have.length', 2);
      cy.checkPage('/departmentConfiguration/index');
    });

    it('should add deputy and delete it afterwards', () => {
      cy.intercept('/departmentConfiguration/addDeputy*').as('addDeputy');
      cy.intercept('/departmentConfiguration/index*').as('loadIndex');
      cy.intercept('/departmentConfiguration/removeDeputy*').as('removeDeputy');
      cy.visit('/departmentConfiguration/index');

      cy.get('input#deputyUsername').type('dave');
      cy.get('button#addDeputyBtn').click();

      cy.wait('@addDeputy').its('response.statusCode').should('eq', 302);
      cy.wait('@loadIndex').its('response.statusCode').should('eq', 200);

      cy.get('table#deputyTable tbody tr').should('have.length', 3);
      cy.checkPage('/departmentConfiguration/index');

      cy.get('table#deputyTable').contains('dave').parent().find('button.deputy-remove-btn')
        .click();

      cy.wait('@removeDeputy').its('response.statusCode').should('eq', 302);
      cy.wait('@loadIndex').its('response.statusCode').should('eq', 200);

      cy.get('table#deputyTable tbody tr').should('have.length', 2);
      cy.checkPage('/departmentConfiguration/index');
    });
  });

  context('when user is a normal user which is not a department head', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should deny access to department configuration pages', () => {
      cy.checkAccessDenied('/departmentConfiguration');
      cy.checkAccessDenied('/departmentConfiguration/addDeputy');
      cy.checkAccessDenied('/departmentConfiguration/removeDeputy');
    });
  });

  context('when user is operator which is not a department head', () => {
    beforeEach(() => {
      cy.loginAs('operator');
    });

    it('should deny access to department configuration pages', () => {
      cy.checkAccessDenied('/departmentConfiguration');
      cy.checkAccessDenied('/departmentConfiguration/addDeputy');
      cy.checkAccessDenied('/departmentConfiguration/removeDeputy');
    });
  });
});
