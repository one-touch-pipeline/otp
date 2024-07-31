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

describe('Check processing threshold page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/processingThreshold/index');
    });


    it('should display the processing threshold page', () => {
      cy.get('h1').should('have.text', 'Processing Thresholds for ExampleProject');
      cy.get('a.btn').contains('Edit thresholds').should('exist');
      cy.get('table tbody').should('exist').should('not.be.empty');

      cy.get('#project.form-select').select('Example project 1', { force: true });
      cy.url().should('include', 'Example+project+1');
      cy.get('h1').should('have.text', 'Processing Thresholds for Example project 1');
      cy.get('td').should('have.text', 'No processing thresholds exist for this project.');
      cy.get('.btn').contains('Edit thresholds').should('not.exist');
    });


    it('should edit the processing thresholds', () => {
      cy.get('a.btn').contains('Edit thresholds').click();
      cy.url().should('include', 'edit');

      cy.get('table tbody').should('exist').should('not.be.empty');
      cy.get('button').contains('Submit').should('exist');
      cy.get('.btn-outline-danger').contains('Cancel').should('exist');

      cy.get('tbody tr:nth-child(2)').as('editRow');

      cy.get('@editRow').within(() => {
        cy.get('td:nth-child(2) select').select('DISEASE', { force: true });
        cy.get('input[name="sampleTypes[0].seqTypes[0].minNumberOfLanes"]').clear().type('2');
        cy.get('input[name="sampleTypes[0].seqTypes[0].minCoverage"]').clear().type('10');
        cy.get('input[name="sampleTypes[0].seqTypes[1].minNumberOfLanes"]').clear().type('2');
        cy.get('input[name="sampleTypes[0].seqTypes[1].minCoverage"]').clear().type('10');
      });
      cy.get('.confirm > .btn-primary').click();

      cy.get('#otpToastBox .otpSuccessToast').should('exist')
        .and('contain.text', 'The processing thresholds were edited successfully');

      cy.get('@editRow').within(() => {
        cy.get('td:nth-child(2)').should('contain.text', 'DISEASE');
        cy.get('td:nth-child(3)').should('contain.text', '2');
        cy.get('td:nth-child(4)').should('contain.text', '10');
        cy.get('td:nth-child(5)').should('contain.text', '2');
        cy.get('td:nth-child(6)').should('contain.text', '10');
      });
    });


    it('should remove the processing thresholds', () => {
      cy.get('a.btn').contains('Edit thresholds').click();
      cy.url().should('include', 'edit');

      cy.get('table tbody').should('exist').should('not.be.empty');
      cy.get('button').contains('Submit').should('exist');
      cy.get('.btn-outline-danger').contains('Cancel').should('exist');

      cy.get('tbody tr:nth-child(2)').as('editRow');

      cy.get('@editRow').within(() => {
        cy.get('td:nth-child(2) select').select('CONTROL', { force: true });
        cy.get('input[name="sampleTypes[0].seqTypes[0].minNumberOfLanes"]').clear();
        cy.get('input[name="sampleTypes[0].seqTypes[0].minCoverage"]').clear();
        cy.get('input[name="sampleTypes[0].seqTypes[1].minNumberOfLanes"]').clear();
        cy.get('input[name="sampleTypes[0].seqTypes[1].minCoverage"]').clear();
      });
      cy.get('.confirm > .btn-primary').click();

      cy.get('#otpToastBox .otpSuccessToast').should('exist')
        .and('contain.text', 'The processing thresholds were edited successfully');

      cy.get('@editRow').within(() => {
        cy.get('td:nth-child(2)').should('contain.text', 'CONTROL');
        cy.get('td:nth-child(3)').should('contain.text', '');
        cy.get('td:nth-child(4)').should('contain.text', '');
        cy.get('td:nth-child(5)').should('contain.text', '');
        cy.get('td:nth-child(6)').should('contain.text', '');
      });
    });
  });


  context('when user is normal user with project access', () => {
    beforeEach(() => {
      cy.loginAs('user');
      cy.visit('/processingThreshold/index');
    });

    it('should display the processing threshold page for user', () => {
      cy.get('h1').should('have.text', 'Processing Thresholds for ExampleProject');
      cy.get('.btn').contains('Edit thresholds').should('not.exist');
      cy.get('table tbody').should('exist').should('not.be.empty');
    });
  });
});
