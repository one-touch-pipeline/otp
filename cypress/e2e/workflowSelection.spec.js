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

describe('Check workflow selection page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should be able to change workflow versions and merging criteria', () => {
      cy.visit('/workflowSelection/index');

      cy.get('table#alignment .edit-switch').should('exist');
      cy.get('table#alignment .edit-switch').first().find('button.edit').click();
      cy.get('table#alignment .edit-switch').first().find('select').select('1.2.73-1', { force: true });
      cy.get('table#alignment .edit-switch').first().find('button.save').click();
      cy.get('table#alignment .edit-switch').first().find('.edit-switch-label').should('contain.text', '1.2.73-1');

      cy.get('table#mergingCriteria .edit-switch').should('exist');
      cy.get('table#mergingCriteria .edit-switch').first().find('button.edit').click();
      cy.get('table#mergingCriteria .edit-switch').first().find('select').select('No', { force: true });
      cy.get('table#mergingCriteria .edit-switch').first().find('button.save').click();
      cy.get('table#mergingCriteria .edit-switch').first().find('.edit-switch-label').should('contain.text', 'No');
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
    });

    it('should be able to see workflow versions and merging criteria, but no buttons to change them', () => {
      cy.visit('/workflowSelection/index');

      cy.get('table#alignment .edit-switch').should('not.exist');
      cy.get('table#alignment tr:nth-child(2) td:nth-child(3)').should('contain.text', '1.2.73-1');

      cy.get('table#mergingCriteria .edit-switch').should('not.exist');
      cy.get('table#mergingCriteria tr:nth-child(2) td:nth-child(2)').should('contain.text', 'No');
    });
  });
});
