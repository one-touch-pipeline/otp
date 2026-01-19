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

describe('Check sample category page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/sampleCategory/index');
    });

    it('should display the sample category page', () => {
      cy.get('h1').should('have.text', 'Sample Categories for ExampleProject');
      cy.get('a.btn').contains('Edit categories').should('exist');
      cy.get('table tbody').should('exist').should('not.be.empty');

      cy.get('#project.form-select').select('Example project 1', { force: true });
      cy.url().should('include', 'Example+project+1');
      cy.get('h1').should('have.text', 'Sample Categories for Example project 1');
      cy.get('td').should('have.text', 'No sample categories exist for this project.');
      cy.get('.btn').contains('Edit categories').should('not.exist');
    });

    it('should edit the sample categories', () => {
      cy.get('a.btn').contains('Edit categories').click();
      cy.url().should('include', 'edit');

      cy.get('table tbody').should('exist').should('not.be.empty');
      cy.get('button').contains('Submit').should('exist');
      cy.get('.btn-outline-danger').contains('Cancel').should('exist');

      cy.get('tbody tr:nth-child(2)').as('editRow');

      cy.get('@editRow').within(() => {
        cy.get('td:nth-child(2) select').select('DISEASE', { force: true });
      });
      cy.get('.confirm > .btn-primary').click();

      cy.get('#otpToastBox .otpSuccessToast').should('exist')
        .and('contain.text', 'The sample categories were edited successfully');

      cy.get('@editRow').within(() => {
        cy.get('td:nth-child(2)').should('contain.text', 'DISEASE');
      });
    });

    it('should remove the sample categories', () => {
      cy.get('a.btn').contains('Edit categories').click();
      cy.url().should('include', 'edit');

      cy.get('table tbody').should('exist').should('not.be.empty');
      cy.get('button').contains('Submit').should('exist');
      cy.get('.btn-outline-danger').contains('Cancel').should('exist');

      cy.get('tbody tr:nth-child(2)').as('editRow');

      cy.get('@editRow').within(() => {
        cy.get('td:nth-child(2) select').select('IGNORED', { force: true });
      });
      cy.get('.confirm > .btn-primary').click();

      cy.get('#otpToastBox .otpSuccessToast').should('exist')
        .and('contain.text', 'The sample categories were edited successfully');

      cy.get('@editRow').within(() => {
        cy.get('td:nth-child(2)').should('contain.text', 'IGNORED');
      });
    });

    it('should add the sample categories', () => {
      cy.get('a.btn').contains('Edit categories').click();
      cy.url().should('include', 'edit');

      cy.get('table tbody').should('exist').should('not.be.empty');
      cy.get('button').contains('Submit').should('exist');
      cy.get('.btn-outline-danger').contains('Cancel').should('exist');

      cy.get('tbody tr:nth-child(2)').as('editRow');

      cy.get('@editRow').within(() => {
        cy.get('td:nth-child(2) select').select('CONTROL', { force: true });
      });
      cy.get('.confirm > .btn-primary').click();

      cy.get('#otpToastBox .otpSuccessToast').should('exist')
        .and('contain.text', 'The sample categories were edited successfully');

      cy.get('@editRow').within(() => {
        cy.get('td:nth-child(2)').should('contain.text', 'CONTROL');
      });
    });
  });

  context('when user is normal user with project access', () => {
    beforeEach(() => {
      cy.loginAs('user');
      cy.visit('/sampleCategory/index');
    });

    it('should display the sample category page for user', () => {
      cy.get('h1').should('have.text', 'Sample Categories for ExampleProject');
      cy.get('.btn').contains('Edit categories').should('not.exist');
      cy.get('table tbody').should('exist').should('not.be.empty');
    });
  });
});
