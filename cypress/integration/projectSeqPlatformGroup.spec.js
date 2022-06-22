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

describe('Check projectSeqPlatformGroup page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.intercept('/projectSeqPlatformGroup/update*').as('updateProjectSeqPlatformGroup');
      cy.loginAsOperator();
      cy.visit('/alignmentConfigurationOverview/index');
      cy.get('.merging-criteria-table tr td').find('a').first().click();
    });

    it('should visit the index page', () => {
      cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
    });

    it('should update Library Prep. Kit', () => {
      cy.get('input#libPrepKit').should('not.be.checked');
      cy.get('input#libPrepKit').check();

      cy.wait('@updateProjectSeqPlatformGroup').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
        cy.get('input#libPrepKit').should('be.checked');
      });
    });

    it('should update Seq. Platform/Chemical Version', () => {
      cy.get('select#useSeqPlatformGroup').should('have.value', 'USE_OTP_DEFAULT');
      cy.get('select#useSeqPlatformGroup').select('USE_PROJECT_SEQ_TYPE_SPECIFIC', { force: true });

      cy.wait('@updateProjectSeqPlatformGroup').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
        cy.get('select#useSeqPlatformGroup').should('have.value', 'USE_PROJECT_SEQ_TYPE_SPECIFIC');
      });
    });

    it('should add a new seq. platform group', () => {
      cy.intercept('/projectSeqPlatformGroup/createNewGroupAndAddPlatform*').as('createNewGroupAndAddPlatform');
      cy.get('select#new_select_seqPlat').should('have.value', 'null');
      cy.get('select#new_select_seqPlat').select('Illumina HiSeq 2000 v1', { force: true });
      cy.get('.seqPlatformGroupSelector button[type=submit]').click();

      cy.wait('@createNewGroupAndAddPlatform').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
        cy.get('.list-group.list-group-flush').should('contain', 'Illumina HiSeq 2000 v1');
      });
    });

    it('should add seq. platform to the seq. platform group', () => {
      cy.intercept('/projectSeqPlatformGroup/addPlatformToExistingSeqPlatformGroup*')
        .as('addPlatformToExistingSeqPlatformGroup');
      cy.get('.seqPlatformGroups').find('select').select('Illumina HiSeq 2000 v2', { force: true });
      cy.get('.seqPlatformGroups').find('button[type=submit].btn-outline-primary').click();

      cy.wait('@addPlatformToExistingSeqPlatformGroup').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
        cy.get('.list-group.list-group-flush').should('contain', 'Illumina HiSeq 2000 v1');
        cy.get('.list-group.list-group-flush').should('contain', 'Illumina HiSeq 2000 v2');
      });
    });

    it('should remove the seq. platform from the seq. platform group', () => {
      cy.intercept('/projectSeqPlatformGroup/removePlatformFromSeqPlatformGroup*')
        .as('removePlatformFromSeqPlatformGroup');
      cy.get('.seqPlatformGroups div').contains('Illumina HiSeq 2000 v1').next().find('button.close')
        .click();

      cy.wait('@removePlatformFromSeqPlatformGroup').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
        cy.get('.list-group.list-group-flush').should('not.contain', 'Illumina HiSeq 2000 v1');
        cy.get('.list-group.list-group-flush').should('contain', 'Illumina HiSeq 2000 v2');
      });
    });

    it('should delete the seq. platform group', () => {
      cy.intercept('/projectSeqPlatformGroup/emptySeqPlatformGroup*').as('emptySeqPlatformGroup');
      cy.get('.seqPlatformGroupSelector button[type=submit].btn-danger').click();

      cy.wait('@emptySeqPlatformGroup').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
        cy.get('.list-group.list-group-flush').should('not.exist');
      });
    });
  });
});
