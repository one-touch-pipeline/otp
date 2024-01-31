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

describe('Check projectUser page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
      cy.visit('/projectUser/index');
    });

    it('should add an user with LDAP Account', () => {
      cy.intercept('/projectUser/addUserToProject*').as('addUserToProject');

      cy.get('form .ldap-user').find('input#username').type('dori');
      cy.get('form .ldap-user').find('#projectRoles').select('PI', { force: true });
      cy.get('.submit-container').find('input[type=submit]').click();

      cy.wait('@addUserToProject').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectUser\/index/);
        cy.get('.projectUserTable').filter(':contains("Project Members of")').parent()
          .should('contain.text', 'Dori Development');
      });
    });

    it('should deactivate an user', () => {
      cy.intercept('/projectUser/setEnabled*').as('setEnabled');

      cy.get('.projectUserTable tr').filter(':contains("Dori Development")')
        .find('button.changeProjectAccess').contains('Deactivate')
        .click();

      cy.get('#confirmationModal').should('be.visible');
      cy.get('button#confirmModal').click();
      cy.get('#confirmationModal').should('not.be.visible');

      cy.wait('@setEnabled').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });

    it('should reactivate an user', () => {
      cy.intercept('/projectUser/setEnabled*').as('setEnabled');

      cy.get('.projectUserTable tr').filter(':contains("Dori Development")')
        .find('button.changeProjectAccess').contains('Reactivate')
        .click();

      cy.wait('@setEnabled').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });

    it('should update users mail address', () => {
      const newMail = `${Cypress._.random(0, 1e6)}@example.org`;

      cy.intercept('/projectUser/updateEmail*').as('updateEmail');

      cy.get('.projectUserTable tr').contains('Dori Development').siblings().find('.edit-switch-text button.edit')
        .click();
      cy.get('.projectUserTable tr').contains('Dori Development').siblings().find('input[name=value][type=text]')
        .clear()
        .type(newMail);

      cy.get('.projectUserTable tr').contains('Dori Development').siblings().find('input[name=value][type=text]')
        .parent()
        .find('button.save')
        .click();

      cy.wait('@updateEmail').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
        cy.get('.projectUserTable').filter(':contains("Project Members of")').parent()
          .should('contain.text', newMail);
      });
    });

    it('should add a role', () => {
      cy.intercept('/projectUser/addRoleToUserProjectRole*').as('addRoleToUserProjectRole');

      cy.get('.projectUserTable tr').contains('Dori Development').siblings().find('select[name=newRoles]')
        .select('BIOINFORMATICIAN', { force: true });
      cy.get('.projectUserTable tr').contains('Dori Development').siblings().find('button.addRole')
        .click();

      cy.wait('@addRoleToUserProjectRole').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
        cy.get('.projectUserTable tr').contains('Dori Development').siblings().find('div[name=BIOINFORMATICIAN]')
          .should('contain.text', 'BIOINFORMATICIAN');
      });
    });

    it('should remove a role', () => {
      cy.intercept('/projectUser/deleteProjectRole*').as('deleteProjectRole');

      cy.get('.projectUserTable tr').contains('Dori Development').siblings()
        .find('div[name=BIOINFORMATICIAN]')
        .find('button.remove')
        .click();

      cy.get('.projectUserTable tr').contains('Dori Development').siblings()
        .find('div[name=BIOINFORMATICIAN]')
        .find('button.delete')
        .click();

      cy.wait('@deleteProjectRole').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
        cy.get('.projectUserTable tr').contains('Dori Development').siblings().find('.edit-switch-label')
          .should('not.contain.text', 'BIOINFORMATICIAN');
      });
    });

    it('should toggle access to OTP', () => {
      cy.intercept('/projectUser/setAccessToOtp*').as('setAccessToOtp');

      cy.get('.projectUserTable tr').contains('Dori Development').parent()
        .find('td.accessToOtp')
        .find('button.edit')
        .click();

      cy.get('.projectUserTable tr').contains('Dori Development').parent()
        .find('td.accessToOtp')
        .find('button.toggle')
        .click();

      cy.wait('@setAccessToOtp').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });

    it('should toggle access to files', () => {
      cy.intercept('/projectUser/setAccessToFiles*').as('setAccessToFiles');

      cy.get('.projectUserTable tr').contains('Dori Development').parent()
        .find('td.accessToFiles')
        .find('button.edit')
        .click();

      cy.get('#confirmationModal').then((modal) => {
        if (modal.is(':visible')) {
          cy.get('.modal-footer #confirmModal').click();
        } else {
          cy.get('.projectUserTable tr').contains('Dori Development').parent()
            .find('td.accessToFiles')
            .find('button')
            .contains('Toggle')
            .click();
        }
      });

      cy.wait('@setAccessToFiles').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });

    it('should toggle access to manage users', () => {
      cy.intercept('/projectUser/setManageUsers*').as('setManageUsers');

      cy.get('.projectUserTable tr').contains('Dori Development').parent()
        .find('td.manageUsers')
        .find('button.edit')
        .click();

      cy.get('.projectUserTable tr').contains('Dori Development').parent()
        .find('td.manageUsers')
        .find('button.toggle')
        .click();

      cy.wait('@setManageUsers').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });

    it('should toggle access to delegate management', () => {
      cy.intercept('/projectUser/setManageUsersAndDelegate*').as('setManageUsersAndDelegate');

      cy.get('.projectUserTable tr').contains('Dori Development').parent()
        .find('td.delegateManagement')
        .find('button.edit')
        .click();

      cy.get('.projectUserTable tr').contains('Dori Development').parent()
        .find('td.delegateManagement')
        .find('button.toggle')
        .click();

      cy.wait('@setManageUsersAndDelegate').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });

    it('should toggle access to receive notifications', () => {
      cy.intercept('/projectUser/setReceivesNotifications*').as('setReceivesNotifications');

      cy.get('.projectUserTable tr').contains('Dori Development').parent()
        .find('td.receivesNotifications')
        .find('button.edit')
        .click();

      cy.get('.projectUserTable tr').contains('Dori Development').parent()
        .find('td.receivesNotifications')
        .find('button.toggle')
        .click();

      cy.wait('@setReceivesNotifications').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAsUser();
      cy.visit('/projectUser/index');
    });

    it('should toggle notification', () => {
      cy.get('.projectUserTable tr').filter(':contains("Dave Development")')
        .find('td.receivesNotifications .icon-true').should('exist');
      cy.get('.projectUserTable tr').filter(':contains("Dave Development")')
        .find('td.receivesNotifications button.edit').first()
        .click();
      cy.get('.projectUserTable tr').filter(':contains("Dave Development")')
        .find('td.receivesNotifications button.toggle').first()
        .click();
      cy.get('.projectUserTable tr').filter(':contains("Dave Development")')
        .find('td.receivesNotifications .icon-false').should('exist');
    });

    it('should see deactivate button', () => {
      cy.get('.projectUserTable tr').filter(':contains("Dave Development")')
        .find('button.changeProjectAccess').contains('Deactivate');
    });
  });
});
