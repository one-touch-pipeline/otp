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

describe('Check processing priority page', () => {
  'use strict';

  const randomDate = new Date().toISOString();
  const randomNumber = new Date().getMilliseconds();
  const randomNumberString = randomNumber.toString();

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.intercept('/processingPriority/index*').as('index');
      cy.visit('/processingPriority/index');
      cy.wait('@index').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
      });
    });

    it('should display the processing priority page', () => {
      cy.get('h3').should('have.text', 'Processing Priority');
      cy.get('button.btn').contains('Create').should('exist');
      cy.get('table tbody').should('exist').should('not.be.empty');

      cy.get('button#edit-row').should('exist');
      cy.get('button#delete-row').should('exist');
    });

    it('should create a processing priority', () => {
      cy.get('button.btn').contains('Create').click();

      cy.get('button.btn-primary').contains('Save').should('exist');
      cy.get('button.btn-secondary').contains('Cancel').should('exist');
      cy.intercept('/ProcessingPriority/save*').as('createProcessingPriority');

      cy.get('input#pp-name').clear().type(`prio ${randomDate}`, { force: true });
      cy.get('input#pp-priority').clear().type(randomNumberString, { force: true });
      cy.get('input#pp-allowedParallelWorkflowRuns').clear().type('2', { force: true });
      cy.get('input#pp-queue').clear().type(`queue ${randomDate}`, { force: true });
      cy.get('input#pp-roddyConfigSuffix').clear().type(`suffix ${randomDate}`, { force: true });
      cy.get('input#pp-errorMailPrefix').clear().type(`mail ${randomDate}`, { force: true });
      cy.get('button#bt-save').click();
      cy.wait('@createProcessingPriority')
        .then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });
      cy.get('.otpSuccessToast').should('exist')
        .and('contain.text', `Processing Priority prio ${randomDate} has been saved.`);
      cy.get('table tbody tr td:nth-child(1)').contains(`prio ${randomDate}`).siblings()
        .should('contain.text', randomNumberString)
        .should('contain.text', '2')
        .should('contain.text', `queue ${randomDate}`)
        .should('contain.text', `suffix ${randomDate}`)
        .should('contain.text', `mail ${randomDate}`);
    });

    it('should update a processing priority', () => {
      cy.get('table tbody tr td:nth-child(1)').contains(`prio ${randomDate}`).siblings().last()
        .find('button#edit-row')
        .click();
      cy.get('button.btn-primary').contains('Save').should('exist');
      cy.get('button.btn-secondary').contains('Cancel').should('exist');
      cy.get('input#pp-name').clear().type(`prio2 ${randomDate}`, { force: true });
      cy.get('input#pp-priority').clear().type((randomNumber + 1).toString(), { force: true });
      cy.get('input#pp-allowedParallelWorkflowRuns').clear().type('4', { force: true });
      cy.get('input#pp-queue').clear().type(`queue2 ${randomDate}`, { force: true });
      cy.get('input#pp-roddyConfigSuffix').clear().type(`suffix2 ${randomDate}`, { force: true });
      cy.get('input#pp-errorMailPrefix').clear().type(`mail2 ${randomDate}`, { force: true });
      cy.get('button#bt-save').click();

      cy.get('.otpSuccessToast').should('exist')
        .and('contain.text', `Processing Priority prio2 ${randomDate} has been saved.`);
      cy.get('table tbody tr td:nth-child(1)')
        .should('not.contains.text', `prio ${randomDate}`);
      cy.get('table tbody tr td:nth-child(1)')
        .contains(`prio2 ${randomDate}`).siblings()
        .should('contain.text', (randomNumber + 1).toString())
        .should('contain.text', '4')
        .should('contain.text', `queue2 ${randomDate}`)
        .should('contain.text', `suffix2 ${randomDate}`)
        .should('contain.text', `mail2 ${randomDate}`);
    });

    it('should not be able to create a processing priority with invalid inputs', () => {
      cy.get('button.btn').contains('Create').click();
      cy.get('input#pp-name').clear().type(`prio2 ${randomDate}`, { force: true });
      cy.get('input#pp-priority').clear().type('55');
      cy.get('input#pp-allowedParallelWorkflowRuns').clear().type('2', { force: true });
      cy.get('input#pp-queue').clear().type('queue', { force: true });
      cy.get('input#pp-roddyConfigSuffix').clear().type('suffix', { force: true });
      cy.get('input#pp-errorMailPrefix').clear().type('mail', { force: true });
      cy.get('button#bt-save').should('be.disabled');
    });

    it('should delete a processing priority', () => {
      cy.get('table tbody tr td:nth-child(1)').contains(`prio2 ${randomDate}`).siblings().last()
        .find('button#delete-row')
        .click();
      cy.get('button#bt-delete').click();
      cy.get('.otpSuccessToast').should('exist')
        .and('contain.text', `Processing Priority prio2 ${randomDate} has been deleted.`);
      cy.get('table tbody tr td:nth-child(1)')
        .should('not.contains.text', `prio2 ${randomDate}`);
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/processingPriority/index', 403);
    });
  });
});
