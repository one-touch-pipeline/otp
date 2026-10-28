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

describe('Check projectSeqPlatformGroup page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.intercept('/projectSeqPlatformGroup/update*').as('updateProjectSeqPlatformGroup');
      cy.loginAs('operator');
      cy.visit('/workflowSelection/index');
      cy.get('table#mergingCriteriaTable tr td').find('a').eq(1).click({ force: true });
    });


    it('should visit the index page', () => {
      cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
    });


    it('should update Library Prep. Kit', () => {
      cy.get('input#libPrepKit').should('be.checked');
      cy.get('input#libPrepKit').uncheck();

      cy.wait('@updateProjectSeqPlatformGroup').its('response.statusCode').should('eq', 302);

      cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
      cy.get('input#libPrepKit').should('not.be.checked');
    });


    it('should update Seq. Platform/Chemical Version', () => {
      cy.get('select#useSeqPlatformGroup').should('have.value', 'USE_OTP_DEFAULT');
      cy.get('select#useSeqPlatformGroup').select('USE_PROJECT_SEQ_TYPE_SPECIFIC', { force: true });

      cy.wait('@updateProjectSeqPlatformGroup').its('response.statusCode').should('eq', 302);

      cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
      cy.get('select#useSeqPlatformGroup').should('have.value', 'USE_PROJECT_SEQ_TYPE_SPECIFIC');
    });


    it('should add a new seq. platform group', () => {
      cy.intercept('/projectSeqPlatformGroup/createNewGroupAndAddPlatform*').as('createNewGroupAndAddPlatform');
      cy.get('select#new_select_seqPlat').should('have.value', 'null');
      cy.get('select#new_select_seqPlat').select('Illumina HiSeq 2000 v1', { force: true });
      cy.get('#addSeqPlatformGroup').click();

      cy.wait('@createNewGroupAndAddPlatform').its('response.statusCode').should('eq', 302);

      cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
      cy.get('.list-group.list-group-flush').should('contain', 'Illumina HiSeq 2000 v1');
    });


    it('should copy seq. platform group into another merging criteria', () => {
      cy.intercept('/projectSeqPlatformGroup/index*').as('projectSeqPlatformGroupIndex');
      cy.intercept('/projectSeqPlatformGroup/searchForSeqPlatformGroups*').as('searchForSeqPlatformGroups');
      cy.intercept('/projectSeqPlatformGroup/copySeqPlatformGroup*').as('copySeqPlatformGroup');
      cy.visit('/workflowSelection/index');
      cy.get('table#mergingCriteriaTable tbody tr').last().find('a').click({ force: true });

      cy.wait('@projectSeqPlatformGroupIndex').its('response.statusCode').should('eq', 200);

      // Changing the merging criteria submits the form, and the copy selection has to wait for the reloaded page.
      // Selecting the value that is already set doesn't trigger a reload, so no new request would be sent.
      cy.get('select#useSeqPlatformGroup').then(($select) => {
        if ($select.val() !== 'USE_PROJECT_SEQ_TYPE_SPECIFIC') {
          cy.get('select#useSeqPlatformGroup').select('USE_PROJECT_SEQ_TYPE_SPECIFIC', { force: true });
          cy.wait('@updateProjectSeqPlatformGroup').its('response.statusCode').should('eq', 302);
        }
      });
      cy.get('select#useSeqPlatformGroup').should('have.value', 'USE_PROJECT_SEQ_TYPE_SPECIFIC');

      cy.get('select#selectedProjectToCopyFrom').select('ExampleProject', { force: true });
      // the seq. type selection is only enabled after the project is selected
      cy.get('select#selectedSeqTypeToCopyFrom').should('not.be.disabled')
        .select('ChIP PAIRED bulk', { force: true });

      cy.wait('@searchForSeqPlatformGroups').its('response.statusCode').should('eq', 302);

      cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
      cy.get('button.copySeqPlatformGroup').first().click();

      cy.wait('@copySeqPlatformGroup').its('response.statusCode').should('eq', 302);

      cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
      cy.get('.list-group.list-group-flush').should('contain', 'Illumina HiSeq 2000 v1');
    });


    it('should add seq. platform to the seq. platform group', () => {
      cy.intercept('/projectSeqPlatformGroup/addPlatformToExistingSeqPlatformGroup*')
        .as('addPlatformToExistingSeqPlatformGroup');
      cy.get('.seqPlatformGroups').find('select').select('Illumina HiSeq 2000 v2', { force: true });
      cy.get('.seqPlatformGroups').find('button[type=submit].btn-primary').click();

      cy.wait('@addPlatformToExistingSeqPlatformGroup').its('response.statusCode').should('eq', 302);

      cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
      cy.get('.list-group.list-group-flush').should('contain', 'Illumina HiSeq 2000 v1');
      cy.get('.list-group.list-group-flush').should('contain', 'Illumina HiSeq 2000 v2');
    });


    it('should remove the seq. platform from the seq. platform group', () => {
      cy.intercept('/projectSeqPlatformGroup/removePlatformFromSeqPlatformGroup*')
        .as('removePlatformFromSeqPlatformGroup');
      cy.get('.seqPlatformGroups div').contains('Illumina HiSeq 2000 v1').next().find('button.btn-outline-danger')
        .click();

      cy.wait('@removePlatformFromSeqPlatformGroup').its('response.statusCode').should('eq', 302);

      cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
      cy.get('.list-group.list-group-flush').should('not.contain', 'Illumina HiSeq 2000 v1');
      cy.get('.list-group.list-group-flush').should('contain', 'Illumina HiSeq 2000 v2');
    });


    it('should delete the seq. platform group', () => {
      cy.intercept('/projectSeqPlatformGroup/emptySeqPlatformGroup*').as('emptySeqPlatformGroup');
      cy.get('.seqPlatformGroupSelector').find('button[type=submit].btn-danger').click();

      cy.wait('@emptySeqPlatformGroup').its('response.statusCode').should('eq', 302);

      cy.location('pathname').should('match', /^\/projectSeqPlatformGroup\/index/);
      cy.get('.seqPlatformGroups').should('not.contain', 'Illumina HiSeq 2000 v2');
      cy.get('.alert.alert-info')
        .contains('No group configured for ExampleProject and ChIP PAIRED bulk!')
        .should('exist');
    });


    it('should change seq platform version back to default', () => {
      cy.intercept('/projectSeqPlatformGroup/emptySeqPlatformGroup*').as('emptySeqPlatformGroup');

      setMergingCriteriaToDefault();
      checkLibPrepKit();

      cy.visit('/workflowSelection/index');
      cy.get('#headingCriteria').click();
      // registered just before the navigation, the changes above reload the index page as well
      cy.intercept('/projectSeqPlatformGroup/index*').as('projectSeqPlatformGroupIndex');
      cy.get('table#mergingCriteriaTable tbody tr').last().find('a').click();

      cy.wait('@projectSeqPlatformGroupIndex').its('response.statusCode').should('eq', 200);

      cy.get('.seqPlatformGroupSelector button[type=submit].btn-danger').click();
      cy.wait('@emptySeqPlatformGroup').its('response.statusCode').should('eq', 302);
      setMergingCriteriaToDefault();
    });
  });
});

/*
 * Every change of the merging criteria submits the form, so each one has to be saved before the next one or a
 * navigation. A value that is already set sends no request.
 */
const setMergingCriteriaToDefault = () => {
  'use strict';

  cy.get('select#useSeqPlatformGroup').then(($select) => {
    if ($select.val() !== 'USE_OTP_DEFAULT') {
      cy.get('select#useSeqPlatformGroup').select('USE_OTP_DEFAULT', { force: true });
      cy.wait('@updateProjectSeqPlatformGroup').its('response.statusCode').should('eq', 302);
    }
  });
  cy.get('select#useSeqPlatformGroup').should('have.value', 'USE_OTP_DEFAULT');
};

const checkLibPrepKit = () => {
  'use strict';

  cy.get('input#libPrepKit').then(($checkbox) => {
    if (!$checkbox.is(':checked')) {
      cy.get('input#libPrepKit').check();
      cy.wait('@updateProjectSeqPlatformGroup').its('response.statusCode').should('eq', 302);
    }
  });
  cy.get('input#libPrepKit').should('be.checked');
};
