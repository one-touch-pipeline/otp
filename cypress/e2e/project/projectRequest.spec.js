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

describe('Check projectRequest page', () => {
  'use strict';

  // Use a regex-safe timestamp (no colons, dots or special chars) so it passes
  // any pattern set via REGEX_PROJECT_NAME_NEW_PROJECT_REQUEST.
  const randomProject = new Date().toISOString().replace(/[:.TZ]/g, '-');

  /**
   * Selects a project request row from the unresolved table by fixture index.
   * Assumes the caller has already navigated to a page that shows the table.
   */
  const selectProjectRequest = (nameIdx) => {
    cy.fixture('projectRequest.json').then((requests) => {
      cy.get('table tbody tr')
        .contains('a.project-request-link', `${requests[nameIdx].projectName} ${randomProject}`).click();
    });
  };

  /**
   * Navigates to the unresolved list, opens the project request at the given
   * fixture index, deletes it via the confirmation modal, and asserts it is gone.
   * The caller is responsible for logging in as operator beforehand.
   */
  const deleteProjectRequestFromView = (nameIdx) => {
    cy.intercept('/projectRequest/delete*').as('deleteProjectRequest');
    cy.visit('/projectRequest/unresolved');
    selectProjectRequest(nameIdx);

    cy.location('pathname').should('match', /^\/projectRequest\/view\//);
    cy.get('#delete-request-btn').click();
    cy.get('div#confirmationModal').should('be.visible').find('button#confirmModal').click();

    cy.wait('@deleteProjectRequest').then((interception) => {
      expect(interception.response.statusCode).to.eq(200);
      cy.location('pathname').should('eq', '/projectRequest/unresolved');
      cy.fixture('projectRequest.json').then((requests) => {
        cy.contains(`${requests[nameIdx].projectName} ${randomProject}`).should('not.exist');
      });
    });
  };


  // Fills the project request form's general fields from a fixture entry.
  // Sample count, species and seq type are only filled when present on the fixture.
  const fillProjectRequestForm = (request) => {
    cy.get('select#projectType').select(request.projectType, { force: true });
    cy.get('input#name').type(`${request.projectName} ${randomProject}`);
    cy.get('textarea#description').type(request.description);
    if (request.approximateNumberOfSamples) {
      cy.get('input#approxNoOfSamples').type(request.approximateNumberOfSamples);
    }
    cy.get('select#keywords').parent().find('input').type(request.keywords, { force: true });
    cy.get('select#storagePeriod').select(request.storagePeriod, { force: true });
    if (request.speciesWithStrainList) {
      cy.get('select#speciesWithStrainList').select(request.speciesWithStrainList, { force: true });
      cy.get('select#seqTypesList').select(0, { force: true });
    }
  };

  // Opens the PI tab and fills the first PI row with the given username (defaults to the standard user).
  const setPrimaryPi = () => {
    cy.get('a#pi-tab').click();
    cy.get('.pi-user-form input.username-input').first().clear().type(Cypress.expose('user_username'));
    cy.get('.pi-user-form select.pi-role-select').first().select('PI', { force: true });
  };

  // Sets the operator as PI (avoids the "user added multiple times" error) and removes the user row
  // pre-populated for the logged-in requester, which would otherwise fail validation for having no role.
  const addPiClearUserTab = () => {
    setPrimaryPi();

    cy.get('a#user-tab').click();
    cy.get('.user-form .user-delete-button').first().click();
  };

  [
    {
      label: 'Sequencing Project',
      draftIdx: 0,
      submitIdx: 2
    },
    {
      label: 'User Management Project',
      draftIdx: 4,
      submitIdx: 5
    }
  ].forEach(({
    label,
    draftIdx,
    submitIdx
  }) => {
    context(`[${label}] draft flow: save as draft, edit, then submit for approval`, () => {
      context('when user is a default user', () => {
        before(() => {
          cy.loginAs('operator');
          cy.setProcessingOption('ENABLE_PROJECT_REQUEST_PI', 'false');
          cy.setProcessingOption('REGEX_PROJECT_NAME_NEW_PROJECT_REQUEST', '');
        });

        beforeEach(() => {
          cy.loginAs('user');
        });

        it('should fail to add project request as draft when users are duplicated across PI and user sections', () => {
          cy.intercept('/projectRequest/index*').as('saveProjectRequest');
          cy.visit('/projectRequest/index');

          cy.fixture('projectRequest.json').then((requests) => {
            const request = requests[draftIdx];
            fillProjectRequestForm(request);

            const operatorUsername = Cypress.expose('operator_username');
            cy.get('a#user-tab').click();
            cy.get('.user-form input.username-input').first().clear().type(operatorUsername);
            cy.get('.user-form select.project-role-select').first().select('BIOINFORMATICIAN', { force: true });
            cy.get('#clone-add-1').click();
            cy.get('.user-form input.username-input').eq(1).clear().type(Cypress.expose('user_username'));
            cy.get('.user-form select.project-role-select').eq(1).select('COORDINATOR', { force: true });

            setPrimaryPi();
            cy.get('#clone-add-2').click();
            cy.get('.pi-user-form input.username-input').eq(1).clear().type(operatorUsername);
            cy.get('.pi-user-form select.pi-role-select').eq(1).select('PI', { force: true });

            cy.get('input[name=_action_saveIndex]').click();

            cy.wait('@saveProjectRequest').then((interception) => {
              expect(interception.response.statusCode).to.eq(200);
              cy.get('#otpToastBox .otpErrorToast').should('exist');
              cy.get('#otpToastBox .otpErrorToast').should('contain', 'The project request could not be stored');
              cy.get('#otpToastBox .otpErrorToast')
                .should('contain', 'A user can not be added multiple times to the same request');
            });
          });
        });

        it('should add project request as draft', () => {
          cy.intercept('/projectRequest/index*').as('saveProjectRequest');
          cy.visit('/projectRequest/index');

          cy.fixture('projectRequest.json').then((requests) => {
            const request = requests[draftIdx];
            fillProjectRequestForm(request);

            const operatorUsername = Cypress.expose('operator_username');
            cy.get('a#user-tab').click();
            cy.get('.user-form input.username-input').first().clear().type(operatorUsername);
            cy.get('.user-form select.project-role-select').first().select('BIOINFORMATICIAN', { force: true });
            cy.get('#clone-add-1').click();
            cy.get('.user-form input.username-input').eq(1).clear().type('dori');
            cy.get('.user-form select.project-role-select').eq(1).select('COORDINATOR', { force: true });

            setPrimaryPi();
            cy.get('#clone-add-2').click();
            cy.get('.pi-user-form input.username-input').eq(1).clear().type('goofy');
            cy.get('.pi-user-form select.pi-role-select').eq(1).select('PI', { force: true });

            cy.get('input[name=_action_saveIndex]').click();

            cy.wait('@saveProjectRequest').then((interception) => {
              expect(interception.response.statusCode).to.eq(200);
              cy.location('pathname').should('match', /^\/projectRequest\/view\//);
              cy.get('#otpToastBox .otpSuccessToast').should('exist').and('contain', 'stored successfully');
              cy.get('h3').should('contain', `${request.projectName} ${randomProject}`);
            });
          });
        });

        it('should find, edit and save unresolved project request', () => {
          cy.intercept('/projectRequest/unresolved*').as('routeToUnresolved');
          cy.intercept('/projectRequest/index*').as('projectRequestIndex');
          cy.visit('/projectRequest/index');

          // navigate to the unresolved tab
          cy.get('.nav-link').contains('Unresolved').click();
          cy.wait('@routeToUnresolved').then((interception) => {
            expect(interception.response.statusCode).to.eq(200);

            // select the project request in the table
            cy.fixture('projectRequest.json').then((requests) => {
              cy.get('table tbody tr')
                .contains('a.project-request-link', `${requests[draftIdx].projectName} ${randomProject}`).click();
            });

            // edit a field of the project request
            cy.get('input[value=Edit]').click();
            cy.wait('@projectRequestIndex').then((intcpt) => {
              expect(intcpt.response.statusCode).to.eq(200);
              cy.location('pathname').should('eq', '/projectRequest/index');
              cy.get('textarea#requesterComment').type('hello world');
              cy.get('input#saveIndex-request-btn').click();
              cy.location('pathname').should('match', /^\/projectRequest\/view\//);
              cy.get('#comments').should('contain', 'hello world');
            });
          });
        });

        it('should submit an unresolved project request', () => {
          cy.intercept('/projectRequest/index*').as('projectRequestIndex');
          cy.visit('/projectRequest/unresolved');

          // select the project request in the table
          cy.fixture('projectRequest.json').then((requests) => {
            cy.get('table tbody tr')
              .contains('a.project-request-link', `${requests[draftIdx].projectName} ${randomProject}`).click();
          });

          cy.get('input#submitView-request-btn').click();
          cy.wait('@projectRequestIndex').then((interception) => {
            expect(interception.response.statusCode).to.eq(302);
            cy.location('pathname').should('eq', '/projectRequest/unresolved');
            cy.fixture('projectRequest.json').then((requests) => {
              cy.get('table tbody tr')
                .contains('a.project-request-link', `${requests[draftIdx].projectName} ${randomProject}`)
                .closest('tr').find('td').should('contain', 'Check by Data Manager needed');
            });
          });
        });
      });

      context('when user is an operator', () => {
        beforeEach(() => {
          cy.loginAs('operator');
        });

        it('should pass on the project request', () => {
          cy.intercept('/projectRequest/index*').as('projectRequestIndex');
          cy.visit('/projectRequest/unresolved');
          selectProjectRequest(draftIdx);

          cy.location('pathname').should('match', /^\/projectRequest\/view\//);
          cy.get('input#passOn-request-btn').click();

          cy.wait('@projectRequestIndex').then((interception) => {
            expect(interception.response.statusCode).to.eq(302);
            cy.location('pathname').should('match', /^\/projectRequest\/view\//);
            cy.get('h3 span.h4').should('contain', 'Approval needed');
            cy.get('h3 span.h4').should('not.contain', 'Check by Data Manager needed');
          });
        });
      });

      context('when user is a PI', () => {
        beforeEach(() => {
          cy.loginAs('user');
        });

        it('should edit and save the project request', () => {
          cy.intercept('/projectRequest/index*').as('projectRequestIndex');
          cy.visit('/projectRequest/unresolved');

          // select the project request in the table
          selectProjectRequest(draftIdx);

          cy.get('input#edit-request-btn').click();
          cy.location('pathname').should('match', /^\/projectRequest\/index/);

          cy.fixture('projectRequest.json').then((requests) => {
            if (requests[draftIdx].speciesWithStrainList) {
              cy.get('select#speciesWithStrainList').parent().find('input')
                .type(requests[1].speciesWithStrainList, { force: true });
            }
          });

          cy.get('input#saveIndex-request-btn').click();
          cy.location('pathname').should('match', /^\/projectRequest\/view\//);
          cy.get('#otpToastBox .otpSuccessToast').should('exist');
        });

        it('should delete a project request being edited', () => {
          cy.intercept('/projectRequest/delete*').as('deleteProjectRequest');
          cy.visit('/projectRequest/unresolved');

          // select the project request in the table
          selectProjectRequest(draftIdx);

          cy.get('input#edit-request-btn').click();

          cy.visit('/projectRequest/unresolved');
          selectProjectRequest(draftIdx);

          cy.get('#delete-request-btn').click();
          cy.get('#confirmModal').should('be.visible').click();

          cy.wait('@deleteProjectRequest').then((interception) => {
            expect(interception.response.statusCode).to.eq(200);
            cy.location('pathname').should('eq', '/projectRequest/unresolved');
            cy.fixture('projectRequest.json').then((requests) => {
              cy.contains(`${requests[draftIdx].projectName} ${randomProject}`).should('not.exist');
            });
          });
        });
      });
    }); // end: draft flow

    context(`[${label}] direct submit flow: submit immediately and progress to approval`, () => {
      context('when user is a default user', () => {
        before(() => {
          cy.loginAs('operator');
          cy.setProcessingOption('ENABLE_PROJECT_REQUEST_PI', 'false');
          cy.setProcessingOption('REGEX_PROJECT_NAME_NEW_PROJECT_REQUEST', '');
        });

        beforeEach(() => {
          cy.loginAs('user');
        });

        it('should submit a project request', () => {
          cy.intercept('/projectRequest/index*').as('saveProjectRequest');
          cy.visit('/projectRequest/index');

          cy.fixture('projectRequest.json').then((requests) => {
            const request = requests[submitIdx];
            fillProjectRequestForm(request);

            setPrimaryPi();
            cy.get('#clone-add-2').click();
            cy.get('.pi-user-form input.username-input').eq(1).clear().type('goofy');
            cy.get('.pi-user-form select.pi-role-select').eq(1).select('PI', { force: true });

            const operatorUsername = Cypress.expose('operator_username');
            cy.get('a#user-tab').click();
            cy.get('.user-form select.project-role-select').first().select('COORDINATOR', { force: true });
            cy.get('.user-form input.username-input').first().clear().type(operatorUsername);
            cy.get('#clone-add-1').click();
            cy.get('.user-form input.username-input').eq(1).clear().type('dori');
            cy.get('.user-form select.project-role-select').eq(1).select('BIOINFORMATICIAN', { force: true });

            cy.get('input#submitIndex-request-btn').click();

            cy.wait('@saveProjectRequest').then((interception) => {
              expect(interception.response.statusCode).to.eq(200);
              cy.location('pathname').should('match', /^\/projectRequest\/unresolved/);
              cy.get('#otpToastBox .otpSuccessToast').should('exist').and('contain', 'stored successfully');
              cy.get('table tbody tr')
                .contains('a.project-request-link', `${request.projectName} ${randomProject}`)
                .closest('tr').find('td').should('contain', 'Check by Data Manager needed');
            });
          });
        });
      });

      context('when user is an operator and passes on the request', () => {
        beforeEach(() => {
          cy.loginAs('operator');
        });

        it('should pass on the project request', () => {
          cy.intercept('/projectRequest/index*').as('projectRequestIndex');
          cy.visit('/projectRequest/unresolved');
          selectProjectRequest(submitIdx);

          cy.location('pathname').should('match', /^\/projectRequest\/view\//);
          cy.get('input#passOn-request-btn').click();

          cy.wait('@projectRequestIndex').then((interception) => {
            expect(interception.response.statusCode).to.eq(302);
            cy.location('pathname').should('match', /^\/projectRequest\/view\//);
            cy.get('h3 span.h4').should('contain', 'Approval needed');
            cy.get('h3 span.h4').should('not.contain', 'Check by Data Manager needed');
          });
        });
      });

      context('when user is a PI', () => {
        beforeEach(() => {
          cy.loginAs('user');
        });

        it('should approve the project request', () => {
          cy.intercept('/projectRequest/index*').as('projectRequestIndex');
          cy.visit('/projectRequest/unresolved');
          selectProjectRequest(submitIdx);

          cy.location('pathname').should('match', /^\/projectRequest\/view\//);
          cy.get('input#confirmConsent').check();
          cy.get('input#confirmRecordOfProcessingActivities').check();
          cy.get('input[name=_action_approve]').click();

          cy.wait('@projectRequestIndex').then((interception) => {
            expect(interception.response.statusCode).to.eq(302);
            cy.location('pathname').should('match', /^\/projectRequest\/view\//);
            cy.get('.col-sm-3').should('contain', 'goofy (goofy Wonderland): Waiting for Approval');
            cy.get('.col-sm-3').should('contain', 'dave (Dave Development): Approved');
          });

          // second PI (goofy) approves
          cy.loginAs('departmentHead');
          cy.intercept('/projectRequest/index*').as('projectRequestIndex2');
          cy.visit('/projectRequest/unresolved');
          selectProjectRequest(submitIdx);

          cy.location('pathname').should('match', /^\/projectRequest\/view\//);
          cy.get('input#confirmConsent').check();
          cy.get('input#confirmRecordOfProcessingActivities').check();
          cy.get('input[name=_action_approve]').click();

          cy.wait('@projectRequestIndex2').then((interception) => {
            expect(interception.response.statusCode).to.eq(302);
            cy.location('pathname').should('match', /^\/projectRequest\/view\//);
            cy.get('h3').contains('Approved').should('exist');
          });
        });
      });

      after(() => {
        // cleanup: remove the request created by this flow
        cy.loginAs('operator');
        deleteProjectRequestFromView(submitIdx);
      });
    }); // end: direct submit flow
  });

  context('when department head feature is enabled and project request progressed until delete', () => {
    before(() => {
      cy.loginAs('operator');
      cy.setProcessingOption('ENABLE_PROJECT_REQUEST_PI', 'true');

      // Create Addition Field
      cy.visit('/projectFields/create');
      cy.intercept('/projectFields/createText*').as('createProjectField');

      cy.get('input#name').type('Organizational Unit');
      cy.get('textarea#descriptionRequest').type('Description');
      cy.get('textarea#descriptionConfig').type('Description');
      cy.get('select#fieldUseForSequencingProjects').select('OPTIONAL', { force: true });
      cy.get('select#fieldUseForDataManagementProjects').select('OPTIONAL', { force: true });
      cy.get('input#create').click();
      cy.wait('@createProjectField').its('response.statusCode').should('eq', 302);
    });

    it('should submit project request with department deputy as a normal user', () => {
      cy.loginAs('user');

      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.intercept('/projectRequest/getPIs*').as('getPIs');
      cy.intercept('/projectRequest/unresolved*').as('routeToUnresolved');

      cy.visit('/projectRequest/index');

      cy.fixture('projectRequest.json').then((request) => {
        cy.get('select#projectType').select(request[3].projectType, { force: true });
        cy.get('input#name').type(`${request[3].projectName} ${randomProject}`);
        cy.get('textarea#description').type(request[3].description);
        cy.get('input#approxNoOfSamples').type(request[3].approximateNumberOfSamples);
        cy.get('select#keywords').parent().find('input').type(request[3].keywords, { force: true });
        cy.get('select#storagePeriod').select(request[3].storagePeriod, { force: true });
        cy.get('select#speciesWithStrainList').select(request[3].speciesWithStrainList, { force: true });
        cy.get('select#seqTypesList').select(0, { force: true });
        cy.get('input#organizationalunit').type(request[3].organizationalUnit);
        cy.wait('@getPIs').its('response.statusCode').should('eq', 200);

        const pi = 'margarett (Margarett Mclaughin)';
        cy.get('a#pi-tab').click();
        cy.get('.pi-user-form select.pi-selector').first().select(pi, { force: true });
        cy.get('.pi-user-form select.pi-role-select').first().select('BIOINFORMATICIAN', { force: true });

        const operatorUsername = Cypress.expose('operator_username');
        cy.get('a#user-tab').click();
        cy.get('.user-form input.username-input').first().clear().type(operatorUsername);
        cy.get('.user-form select.project-role-select').first().select('SUBMITTER', { force: true });

        cy.get('input[value="Save as Draft"]').click();

        // save as draft
        cy.wait('@projectRequestIndex').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        });

        // navigate to the unresolved tab
        cy.visit('/projectRequest/index');
        cy.get('.nav-link').contains('Unresolved').click();
        cy.wait('@routeToUnresolved').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
        });

        // select the project request in the table
        cy.get('table tbody tr')
          .contains('a.project-request-link', `${request[3].projectName} ${randomProject}`).click();

        cy.get('input[value="Edit"]').click();
        cy.wait('@projectRequestIndex').then((interception) => {
          expect(interception.response.statusCode).to.eq(302);
          cy.location('pathname').should('match', /^\/projectRequest\/index/);
        });

        cy.wait('@getPIs').its('response.statusCode').should('eq', 200);

        // wait till fetched pi were added to the form
        cy.get('.pi-user-form select.pi-selector').first().contains(pi);

        cy.get('input[value="Submit"]').click();

        cy.wait('@projectRequestIndex').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          cy.location('pathname').should('match', /^\/projectRequest\/unresolved/);
          cy.get('#otpToastBox .otpSuccessToast').should('exist').and('contain', 'stored successfully');
          cy.get('table tbody tr')
            .contains('a.project-request-link', `${request[3].projectName} ${randomProject}`)
            .closest('tr').find('td').should('contain', 'Check by Data Manager needed');
        });
      });
    });

    it('should pass on the request as operator', () => {
      cy.loginAs('operator');
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/unresolved');
      selectProjectRequest(3);

      cy.location('pathname').should('match', /^\/projectRequest\/view\//);
      cy.get('input#passOn-request-btn').click();

      cy.wait('@projectRequestIndex').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        cy.get('h3 span.h4').should('contain', 'Approval needed');
        cy.get('h3 span.h4').should('not.contain', 'Check by Data Manager needed');
      });
    });

    it('should not be able to approve the request as non department head nor deputy', () => {
      cy.loginAs('operator');
      cy.visit('/projectRequest/unresolved');
      selectProjectRequest(3);

      cy.get('input#approve-request-btn').should('not.exist');
      cy.get('h3').contains('Approved').should('not.exist');
    });

    it('should approve the request as department deputy', () => {
      cy.loginAs('departmentDeputy');
      cy.intercept('/projectRequest/index*').as('projectRequestIndex');
      cy.visit('/projectRequest/unresolved');
      selectProjectRequest(3);

      cy.get('input#confirmConsent').check();
      cy.get('input#confirmRecordOfProcessingActivities').check();
      cy.get('input[name=_action_approve]').click();

      cy.wait('@projectRequestIndex').then((interception) => {
        expect(interception.response.statusCode).to.eq(302);
        cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        cy.get('h3').contains('Approved').should('exist');
      });
    });

    after(() => {
      // cleanup: remove the request created by this suite and reset the feature toggle
      cy.loginAs('operator');
      deleteProjectRequestFromView(3);
      cy.setProcessingOption('ENABLE_PROJECT_REQUEST_PI', 'false');
    });
  });

  context('mandatory fields differ by project type', () => {
    before(() => {
      cy.loginAs('operator');
      cy.setProcessingOption('ENABLE_PROJECT_REQUEST_PI', 'false');
      cy.setProcessingOption('REGEX_PROJECT_NAME_NEW_PROJECT_REQUEST', '');
    });

    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should mark species and approximate number of samples as required for SEQUENCING but not USER_MANAGEMENT', () => {
      cy.visit('/projectRequest/index');

      cy.get('select#projectType').select('SEQUENCING', { force: true });
      cy.get('input#approxNoOfSamples').should('have.attr', 'required');
      cy.get('select#speciesWithStrainList').should('have.attr', 'required');

      cy.get('select#projectType').select('USER_MANAGEMENT', { force: true });
      cy.get('input#approxNoOfSamples').should('not.have.attr', 'required');
      cy.get('select#speciesWithStrainList').should('not.have.attr', 'required');
    });

    it('should save USER_MANAGEMENT request without species and approximate number of samples', () => {
      cy.intercept('/projectRequest/index*').as('saveProjectRequest');
      cy.visit('/projectRequest/index');

      cy.get('select#projectType').select('USER_MANAGEMENT', { force: true });
      cy.get('input#name').type(`validation-um ${randomProject}`);
      cy.get('textarea#description')
        .type('my-description: hello world, this is a description that is longer than 50 chars.');
      cy.get('select#keywords').parent().find('input').type('keyword1{enter}', { force: true });
      cy.get('select#storagePeriod').select('Store data for ten years', { force: true });
      addPiClearUserTab();

      cy.get('input[name=_action_saveIndex]').click();

      cy.wait('@saveProjectRequest').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        cy.get('#otpToastBox .otpSuccessToast').should('exist').and('contain', 'stored successfully');
        cy.get('#otpToastBox .otpErrorToast').should('not.exist');
      });
    });

    it('should require storage end date when USER_DEFINED storage period is selected', () => {
      cy.intercept('/projectRequest/index*').as('saveProjectRequest');
      cy.visit('/projectRequest/index');

      cy.get('select#projectType').select('USER_MANAGEMENT', { force: true });
      cy.get('input#name').type(`validation-storage ${randomProject}`);
      cy.get('textarea#description')
        .type('my-description: hello world, this is a description that is longer than 50 chars.');
      cy.get('select#keywords').parent().find('input').type('keyword1{enter}', { force: true });
      cy.get('select#storagePeriod').select('Store data until given deletion date:', { force: true });
      addPiClearUserTab();

      cy.get('input[name=_action_saveIndex]').click();

      cy.wait('@saveProjectRequest').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('#otpToastBox .otpErrorToast').should('exist').and('contain', 'The project request could not be stored');
        cy.get('#otpToastBox .otpErrorToast').should('contain', 'Storage end date must be filled in');
      });
    });

    it('should reject a past storage end date', () => {
      cy.intercept('/projectRequest/index*').as('saveProjectRequest');
      cy.visit('/projectRequest/index');

      cy.get('select#projectType').select('USER_MANAGEMENT', { force: true });
      cy.get('input#name').type(`validation-storage-past ${randomProject}`);
      cy.get('textarea#description')
        .type('my-description: hello world, this is a description that is longer than 50 chars.');
      cy.get('select#keywords').parent().find('input').type('keyword1{enter}', { force: true });
      cy.get('select#storagePeriod').select('Store data until given deletion date:', { force: true });
      cy.get('input#storageUntil').type('2020-01-01');
      addPiClearUserTab();

      cy.get('input[name=_action_saveIndex]').click();

      cy.wait('@saveProjectRequest').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.get('#otpToastBox .otpErrorToast').should('exist').and('contain', 'The project request could not be stored');
        cy.get('#otpToastBox .otpErrorToast').should('contain', 'The selected date for Storage Until is in the past');
      });
    });
  });

  context('project request with required custom additional field', () => {
    before(() => {
      cy.loginAs('operator');
      cy.setProcessingOption('ENABLE_PROJECT_REQUEST_PI', 'false');
      cy.setProcessingOption('REGEX_PROJECT_NAME_NEW_PROJECT_REQUEST', '');

      cy.visit('/projectFields/index');
      cy.get('body').then(($body) => {
        if ($body.text().includes('Required Field')) {
          // Field already exists — update statuses to the desired values
          cy.intercept('/projectFields/updateField*').as('updateField');

          cy.contains('td', 'Required Field').closest('tr').find('td.seqProject').within(() => {
            cy.get('button.edit').click();
            cy.get('select[name="dropdown"]').select('REQUIRED', { force: true });
            cy.get('button.save').click();
          });
          cy.wait('@updateField');

          cy.contains('td', 'Required Field').closest('tr').find('td.usrManagementProject').within(() => {
            cy.get('button.edit').click();
            cy.get('select[name="dropdown"]').select('OPTIONAL', { force: true });
            cy.get('button.save').click();
          });
          cy.wait('@updateField');
        } else {
          // Field does not exist — create it
          cy.visit('/projectFields/create');
          cy.intercept('/projectFields/createText*').as('createProjectField');

          cy.get('input#name').type('Required Field');
          cy.get('textarea#descriptionRequest').type('A required custom field for testing');
          cy.get('textarea#descriptionConfig').type('A required custom field for testing');
          cy.get('select#fieldUseForSequencingProjects').select('REQUIRED', { force: true });
          cy.get('select#fieldUseForDataManagementProjects').select('OPTIONAL', { force: true });
          cy.get('input#create').click();
          cy.wait('@createProjectField').its('response.statusCode').should('eq', 302);
        }
      });
    });

    it('should mark the custom field as required for SEQUENCING and optional for USER_MANAGEMENT', () => {
      cy.loginAs('user');
      cy.visit('/projectRequest/index');

      cy.get('select#projectType').select('SEQUENCING', { force: true });
      cy.get('input#requiredfield').should('have.attr', 'required');

      cy.get('select#projectType').select('USER_MANAGEMENT', { force: true });
      cy.get('input#requiredfield').should('not.have.attr', 'required');
    });

    it('should save SEQUENCING request with required custom field filled', () => {
      cy.loginAs('user');
      cy.intercept('/projectRequest/index*').as('saveProjectRequest');
      cy.intercept('/projectRequest/delete*').as('deleteProjectRequest');
      cy.visit('/projectRequest/index');

      cy.get('select#projectType').select('SEQUENCING', { force: true });
      cy.get('input#name').type(`custom-field-seq ${randomProject}`);
      cy.get('textarea#description')
        .type('my-description: hello world, this is a description that is longer than 50 chars.');
      cy.get('input#approxNoOfSamples').type('50');
      cy.get('select#keywords').parent().find('input').type('keyword1{enter}', { force: true });
      cy.get('select#storagePeriod').select('Store data for ten years', { force: true });
      cy.get('select#speciesWithStrainList').select('Mouse (Mus caroli) [Unknown]', { force: true });
      cy.get('select#seqTypesList').select(0, { force: true });
      addPiClearUserTab();

      // attempt to save without filling the required custom field — browser should prevent submission
      cy.get('input[name=_action_saveIndex]').click();
      cy.location('pathname').should('eq', '/projectRequest/index');

      // fill the required custom field and save successfully
      cy.get('input#requiredfield').type('custom value');
      cy.get('input[name=_action_saveIndex]').click();

      cy.wait('@saveProjectRequest').then((interception) => {
        expect(interception.response.statusCode).to.eq(200);
        cy.location('pathname').should('match', /^\/projectRequest\/view\//);
        cy.get('#otpToastBox .otpSuccessToast').should('exist').and('contain', 'stored successfully');
        cy.get('table td').should('contain', 'custom value');
      });

      // delete the draft immediately so the field becomes unused for after() cleanup
      cy.get('a#delete-request-btn').click();
      cy.get('div#confirmationModal').should('be.visible').find('button#confirmModal').click();
      cy.wait('@deleteProjectRequest').its('response.statusCode').should('eq', 200);
    });

    after(() => {
      // set field statuses back to NOT_AVAILABLE so the field no longer affects other tests
      cy.loginAs('operator');
      cy.visit('/projectFields/index');
      cy.intercept('/projectFields/updateField*').as('updateField');

      cy.contains('td', 'Required Field').closest('tr').find('td.seqProject').within(() => {
        cy.get('button.edit').click();
        cy.get('select[name="dropdown"]').select('NOT_AVAILABLE', { force: true });
        cy.get('button.save').click();
      });
      cy.wait('@updateField');

      cy.contains('td', 'Required Field').closest('tr').find('td.usrManagementProject').within(() => {
        cy.get('button.edit').click();
        cy.get('select[name="dropdown"]').select('NOT_AVAILABLE', { force: true });
        cy.get('button.save').click();
      });
      cy.wait('@updateField');
    });
  });
});
