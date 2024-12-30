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

describe('Check projectSeqPlatformGroup page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/defaultSeqPlatformGroup/index');
    });


    it('should visit the index page', () => {
      cy.location('pathname').should('match', /^\/defaultSeqPlatformGroup\/index/);

      cy.get('h4').contains('Create new Seq. Platform Group').should('exist');
      cy.get('h4').contains('Current configuration').should('exist');

      cy.get('#createNewSeqPlatformGroup > .card.seqPlatformGroupSelector').should('exist')
        .and('not.be.empty');
      cy.get('#currentSeqPlatformGroup > .card.seqPlatformGroupSelector').find('.list-group')
        .should('exist')
        .and('not.be.empty')
        .and('contain.text', 'ExampleSeqPlatform ExampleModel ExampleKit');
    });


    it('should add a seqPlatform to an existing group', () => {
      cy.intercept('/defaultSeqPlatformGroup/addPlatformToExistingSeqPlatformGroup*').as('addPlatformToExistingSeqPlatformGroup');
      cy.get('#currentSeqPlatformGroup > .card.seqPlatformGroupSelector').first().as('currentSeqPlatformGroup');

      cy.get('@currentSeqPlatformGroup')
        .find('select')
        .select('Illumina HiSeq 2000 unknown kit', { force: true });
      cy.get('@currentSeqPlatformGroup').find('button.btn-primary').should('be.enabled');
      cy.get('@currentSeqPlatformGroup').find('button.btn-primary').click();

      cy.wait('@addPlatformToExistingSeqPlatformGroup').its('response.statusCode').should('eq', 302);
      cy.get('@currentSeqPlatformGroup').find('.list-group').should('exist')
        .and('contain.text', 'ExampleSeqPlatform ExampleModel ExampleKit')
        .and('contain.text', 'Illumina HiSeq 2000 unknown kit');
    });


    it('should remove a seqPlatform from an existing group', () => {
      cy.intercept('/defaultSeqPlatformGroup/removePlatformFromSeqPlatformGroup*').as('removePlatformFromSeqPlatformGroup');
      cy.get('#currentSeqPlatformGroup > .card.seqPlatformGroupSelector').first().as('currentSeqPlatformGroup');

      cy.get('@currentSeqPlatformGroup')
        .contains('Illumina HiSeq 2000 unknown kit').parent()
        .find('button.btn-outline-danger')
        .click();

      cy.wait('@removePlatformFromSeqPlatformGroup').its('response.statusCode').should('eq', 302);
      cy.get('@currentSeqPlatformGroup').find('.list-group')
        .should('contain.text', 'ExampleSeqPlatform ExampleModel ExampleKit')
        .and('not.contain.text', 'Illumina HiSeq 2000 unknown kit');
    });


    it('should create a new seqPlatform group', () => {
      cy.intercept('/defaultSeqPlatformGroup/createNewGroupAndAddPlatform*').as('createNewGroupAndAddPlatform');
      cy.get('#createNewSeqPlatformGroup > .card.seqPlatformGroupSelector').as('createNewSeqPlatformGroup');

      cy.get('@createNewSeqPlatformGroup')
        .find('select')
        .select('Illumina HiSeq 2000 unknown kit', { force: true });
      cy.get('@createNewSeqPlatformGroup').find('button.btn-primary').should('be.enabled');
      cy.get('@createNewSeqPlatformGroup').find('button.btn-primary').click();

      cy.wait('@createNewGroupAndAddPlatform').its('response.statusCode').should('eq', 302);
      cy.get('#currentSeqPlatformGroup > .card').should('have.length', 2);
      cy.get('#currentSeqPlatformGroup > .card.seqPlatformGroupSelector')
        .contains('Illumina HiSeq 2000 unknown kit')
        .should('exist')
        .and('not.contain.text', 'ExampleSeqPlatform ExampleModel ExampleKit');
    });


    it('should delete the seqPlatform group', () => {
      cy.intercept('/defaultSeqPlatformGroup/emptySeqPlatformGroup*').as('emptySeqPlatformGroup');
      cy.get('#currentSeqPlatformGroup > .card.seqPlatformGroupSelector').last().as('currentSeqPlatformGroup');

      cy.get('@currentSeqPlatformGroup').find('button.btn-danger').click();

      cy.wait('@emptySeqPlatformGroup').its('response.statusCode').should('eq', 302);
      cy.get('#currentSeqPlatformGroup > .card').should('have.length', 1);
      cy.get('@currentSeqPlatformGroup').find('.list-group')
        .should('not.contain.text', 'Illumina HiSeq 2000 unknown kit');
    });


    it('should delete last seqPlatform group and create a new one', () => {
      cy.intercept('/defaultSeqPlatformGroup/emptySeqPlatformGroup*').as('emptySeqPlatformGroup');
      cy.intercept('/defaultSeqPlatformGroup/createNewGroupAndAddPlatform*').as('createNewGroupAndAddPlatform');
      cy.get('#currentSeqPlatformGroup > .card.seqPlatformGroupSelector').first().as('currentSeqPlatformGroup');
      cy.get('#createNewSeqPlatformGroup > .card.seqPlatformGroupSelector').as('createNewSeqPlatformGroup');

      // delete the last group
      cy.get('#currentSeqPlatformGroup > .card').should('have.length', 1);
      cy.get('@currentSeqPlatformGroup').find('button.btn-danger').click();

      cy.wait('@emptySeqPlatformGroup').its('response.statusCode').should('eq', 302);
      cy.get('#currentSeqPlatformGroup > .card').should('have.length', 0);
      cy.get('#currentSeqPlatformGroup > .alert.alert-info')
        .contains('No default Seq. Platform Groups defined')
        .should('exist');


      // recreate the group
      cy.get('@createNewSeqPlatformGroup').find('select')
        .select('ExampleSeqPlatform ExampleModel ExampleKit', { force: true });
      cy.get('@createNewSeqPlatformGroup').find('button.btn-primary').should('be.enabled');
      cy.get('@createNewSeqPlatformGroup').find('button.btn-primary').click();

      cy.wait('@createNewGroupAndAddPlatform').its('response.statusCode').should('eq', 302);
      cy.get('#currentSeqPlatformGroup > .card').should('have.length', 1);
      cy.get('#currentSeqPlatformGroup > .card.seqPlatformGroupSelector')
        .contains('ExampleSeqPlatform ExampleModel ExampleKit')
        .should('exist');
    });
  });


  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/defaultSeqPlatformGroup/index');
    });
  });
});
