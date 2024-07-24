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
      cy.loginAs('operator');
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
        getProjectMemberRow('dori').should('exist');
      });
    });

    it('should deactivate and reactivate a user', () => {
      cy.intercept('/projectUser/setEnabled*').as('setEnabled');

      getProjectMemberRow('dori').find('button.changeProjectAccess').as('projectAccessCell');

      cy.get('@projectAccessCell').contains('Deactivate').click();

      cy.get('#confirmationModal').should('be.visible').find('button#confirmModal').click();
      cy.get('#confirmationModal').should('not.be.visible');

      cy.wait('@setEnabled').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });

      cy.get('#formerProjectMemberTable tbody tr').filter(':contains("dori")')
        .find('button:contains("Reactivate")')
        .click();

      cy.get('#confirmationModal').should('be.visible').find('button#confirmModal').click();

      cy.wait('@setEnabled').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });

    it('should update users mail address', () => {
      const newMail = `${Cypress._.random(0, 1e6)}@example.org`;

      cy.intercept('/projectUser/updateEmail*').as('updateEmail');

      getProjectMemberRow('dori').as('userRow');

      cy.get('@userRow').find('.edit-switch-text button.edit').click();
      cy.get('@userRow').find('input[name=value][type=text]').clear().type(newMail);
      cy.get('@userRow').find('button.save').click();

      cy.wait('@updateEmail').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
        cy.get('@userRow').should('contain.text', newMail);
      });
    });

    it('should add and remove a role', () => {
      const role = 'BIOINFORMATICIAN';
      cy.intercept('/projectUser/addRoleToUserProjectRole*').as('addRoleToUserProjectRole');

      getProjectMemberRow('dori').as('userRow');

      cy.get('@userRow').find('select[name=newRoles]').select(role, { force: true });
      cy.get('@userRow').find('button.addRole').click();

      cy.wait('@addRoleToUserProjectRole').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
        cy.get('.projectUserTable tr').contains('Dori Development').siblings().find(`div[name=${role}]`)
          .should('contain.text', role);
      });

      cy.intercept('/projectUser/deleteProjectRole*').as('deleteProjectRole');

      cy.get('@userRow').find(`div[name=${role}] button.remove`).click();
      cy.get('@userRow').find(`div[name=${role}] button.delete`).click();

      cy.wait('@deleteProjectRole').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
        cy.get('@userRow').find('.edit-switch-label').should('not.contain.text', role);
      });
    });

    it('should toggle access to OTP', () => {
      cy.intercept('/projectUser/setAccessToOtp*').as('setAccessToOtp');

      getProjectMemberRow('dori').find('td.accessToOtp').as('accessToOtpCell');

      cy.get('@accessToOtpCell').find('button.edit').click();
      cy.get('@accessToOtpCell').find('button.toggle').click();

      cy.wait('@setAccessToOtp').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });

      cy.get('@accessToOtpCell').find('.icon-true').should('exist');
    });

    shouldBeAbleToToggleAccessToFiles();

    shouldBeAbleToToggleManageUsers();

    it('should toggle access to delegate management', () => {
      cy.intercept('/projectUser/setManageUsersAndDelegate*').as('setManageUsersAndDelegate');

      getProjectMemberRow('dori').find('td.delegateManagement').as('delegateManagementCell');

      cy.get('@delegateManagementCell').find('button.edit').click();
      cy.get('@delegateManagementCell').find('button.toggle').click();

      cy.wait('@setManageUsersAndDelegate').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });

    shouldBeAbleToToggleNotification('dori');

  });

  context('when user is normal user with user management rights', () => {
    let manageUserRightsWhereGranted = true;

    before('Add Manage User rights, when not already granted', () => {
      cy.loginAs('operator');
      cy.visit('/projectUser/index');

      getProjectMemberRow('dave').as('userRow', { type: 'static' });

      cy.get('@userRow').find('td.manageUsers').then((userRow) => {
        if (userRow.find('.icon-false').length) {
          manageUserRightsWhereGranted = false;
          cy.get('@userRow').find('td.manageUsers button.edit').click();
          cy.get('@userRow').find('td.manageUsers button.toggle').click();
        }
      });
    });

    beforeEach(() => {
      cy.loginAs('user');
      cy.visit('/projectUser/index');
    });

    it('should not be able to add or remove pi role', () => {
      cy.intercept('/projectUser/addRoleToUserProjectRole*').as('addRole');
      cy.intercept('/projectUser/deleteProjectRole*').as('removeRole');

      getProjectMemberRow('dave').find('select[name="newRoles"] option:contains("PI")').should('not.exist');

      getProjectMemberRow('dori').find('div[name="PI"]').as('piEntry');
      cy.get('@piEntry').find('button.remove').click();
      cy.get('@piEntry').find('button.delete').click();

      cy.wait('@removeRole').its('response.body.success').should('eq', false);

      cy.get('div[name="PI"]').should('exist');
      cy.get('div[name="BIOINFORMATICIAN"]').should('exist');
    });

    it('should be able to add and remove non pi role', () => {
      cy.intercept('/projectUser/addRoleToUserProjectRole*').as('addRole');
      cy.intercept('/projectUser/deleteProjectRole*').as('removeRole');

      getProjectMemberRow('dave').as('userRow');

      cy.get('@userRow').find('select[name="newRoles"]').select('COORDINATOR', { force: true });
      cy.get('@userRow').find('button.addRole').click();

      cy.wait('@addRole').its('response.body.success').should('eq', true);

      cy.get('@userRow').find('div[name="COORDINATOR"] button.remove').click();
      cy.get('@userRow').find('div[name="COORDINATOR"] button.delete').click();
      cy.wait('@removeRole').its('response.body.success').should('eq', true);
    });

    after('Remove Manage User rights when not granted before', () => {
      if (!manageUserRightsWhereGranted) {
        cy.loginAs('operator');
        cy.visit('/projectUser/index');

        getProjectMemberRow('dave').as('userRow');
        cy.get('@userRow').find('td.manageUsers button.edit').click();
        cy.get('@userRow').find('td.manageUsers button.toggle').click();
      }
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
      cy.visit('/projectUser/index');
    });

    shouldBeAbleToToggleNotification('dave');

    it('should not be able to add pi role', () => {
      cy.intercept('/projectUser/addRoleToUserProjectRole*').as('addRole');

      cy.get('select[name="newRoles"]').should('not.exist');
    });

    it('should see own deactivate button', () => {
      getProjectMemberRow('dave').find('button.changeProjectAccess:contains("Deactivate")').should('exist');
      getProjectMemberRow('dori').find('button.changeProjectAccess:contains("Deactivate")').should('be.disabled');
    });
  });

  function getProjectMemberRow(name) {
    return cy.get('#projectMemberTable tbody tr').filter(`:contains("${name}")`);
  }

  function shouldBeAbleToToggleManageUsers() {
    return it('should toggle access to manage users', () => {
      cy.intercept('/projectUser/setManageUsers*').as('setManageUsers');

      getProjectMemberRow('dori').find('td.manageUsers').as('manageUsersCell');

      cy.get('@manageUsersCell').find('button.edit').click();
      cy.get('@manageUsersCell').find('button.toggle').click();

      cy.wait('@setManageUsers').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });
  }

  function shouldBeAbleToToggleAccessToFiles() {
    return it('should toggle access to files', () => {
      cy.intercept('/projectUser/setAccessToFiles*').as('setAccessToFiles');

      getProjectMemberRow('dori').find('td.accessToFiles').as('accessToFilesCell');

      cy.get('@accessToFilesCell').find('button.edit').click();
      cy.get('@accessToFilesCell').find('button:contains("Toggle")').click();

      cy.get('#confirmationModal').should('be.visible').find('button#confirmModal').click();

      cy.wait('@setAccessToFiles').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });
  }

  function shouldBeAbleToToggleNotification(user) {
    return it(`should be able to toggle notification of user ${user}`, () => {
      cy.intercept('/projectUser/setReceivesNotifications*').as('setReceivesNotifications');

      getProjectMemberRow(user).find('td.receivesNotifications').as('notificationCell');

      cy.get('@notificationCell').find('button.edit').click();
      cy.get('@notificationCell').find('button.toggle').click();
      cy.wait('@setReceivesNotifications').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });

      cy.get('@notificationCell').find('button.edit').click();
      cy.get('@notificationCell').find('button.toggle').click();
      cy.wait('@setReceivesNotifications').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        expect(interception.response.body.success).to.eq(true);
      });
    });
  }
});
