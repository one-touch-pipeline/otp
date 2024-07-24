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

describe('Check projectFields page', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.fixture('projectFields.json').as('projectFields');
      cy.loginAs('operator');
    });

    context('when tab of create page is selected', () => {
      beforeEach(() => {
        cy.visit('/projectFields/create');
      });

      it('should create additional project field, when required values are set', () => {
        cy.intercept('/projectFields/createText*').as('createProjectField');
        cy.get('@projectFields').then((fields) => {
          cy.get('#name').clear().type(fields[0].name);
          cy.get('#descriptionConfig').clear().type(fields[0].descriptionConfig);
          cy.get('#descriptionRequest').clear().type(fields[0].descriptionRequest);
          cy.get('input#create').click();

          cy.wait('@createProjectField').then((interception) => {
            expect(interception.response.statusCode).to.eq(302);
            cy.location('pathname').should('eq', '/projectFields/index');
          });
        });
      });
    });

    context('when tab of index page is selected', () => {
      beforeEach(() => {
        cy.intercept('/projectFields/updateField*').as('updateProjectField');
        cy.visit('/projectFields/index');
      });

      it('should update the name', () => {
        cy.get('td.name').first().find('button.edit').click();

        cy.get('@projectFields').then((fields) => {
          cy.get('td.name').first().find('input.edit-switch-input').type(fields[1].name);
        });
        cy.get('td.name').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the config page tooltip', () => {
        cy.get('td.descriptionConfig').first().find('button.js-edit').click();
        cy.get('@projectFields').then((fields) => {
          cy.get('td.descriptionConfig').first().find('textarea.edit-switch-input').type(fields[1].descriptionConfig);
        });
        cy.get('td.descriptionConfig').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the creation request page tooltip', () => {
        cy.get('td.descriptionRequest').first().find('button.js-edit').click();
        cy.get('@projectFields').then((fields) => {
          cy.get('td.descriptionRequest').first().find('textarea').focus()
            .type(fields[1].descriptionRequest);
        });
        cy.get('td.descriptionRequest').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the sequencing project', () => {
        cy.get('td.seqProject').first().scrollIntoView();
        cy.get('td.seqProject').first().find('button.edit').click();
        cy.get('td.seqProject').first().find('select').select(1, { force: true });
        cy.get('td.seqProject').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the user management project', () => {
        cy.get('td.usrManagementProject').first().scrollIntoView();
        cy.get('td.usrManagementProject').first().find('button.edit').click();
        cy.get('td.usrManagementProject').first().find('select').select(1, { force: true });
        cy.get('td.usrManagementProject').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the information provider', () => {
        cy.get('td.sourceOfData').first().scrollIntoView();
        cy.get('td.sourceOfData').first().find('button.edit').click();
        cy.get('td.sourceOfData').first().find('select').select(1, { force: true });
        cy.get('td.sourceOfData').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the display on project config flag', () => {
        cy.get('td.projectDisplayOnConfigPage').first().scrollIntoView();
        cy.get('td.projectDisplayOnConfigPage').first().find('button.edit').click();
        cy.get('td.projectDisplayOnConfigPage').first().find('select').select(1, { force: true });
        cy.get('td.projectDisplayOnConfigPage').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the sorting number', () => {
        cy.get('td.sortingNumber').first().scrollIntoView();
        cy.get('td.sortingNumber').first().find('button.edit').click();
        cy.get('@projectFields').then((fields) => {
          cy.get('td.sortingNumber').first().find('input[type=number]').focus()
            .type(fields[1].sortingNumber);
        });
        cy.get('td.sortingNumber').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the only changeable by operator flag', () => {
        cy.get('td.changeableByOperator').first().scrollIntoView();
        cy.get('td.changeableByOperator').first().find('button.edit').click();
        cy.get('td.changeableByOperator').first().find('button.toggle').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the used externally flag', () => {
        cy.get('td.usedExternally').first().scrollIntoView();
        cy.get('td.usedExternally').first().find('button.edit').click();
        cy.get('td.usedExternally').first().find('button.toggle').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the default value', () => {
        cy.get('td.defaultValue').first().find('button.js-edit').click();
        cy.get('@projectFields').then((fields) => {
          cy.get('td.defaultValue').first().find('textarea.edit-switch-input').type(fields[1].defaultValue);
        });
        cy.get('td.defaultValue').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the allowed values', () => {
        cy.intercept('/projectFields/updateMultiField*').as('updateMultiField');

        cy.get('td.allowedValues').first().find('button.js-edit').click();
        cy.get('@projectFields').then((fields) => {
          cy.get('td.allowedValues').first().find('.field input[type=text]').focus()
            .clear()
            .type(fields[1].defaultValue);
        });
        cy.get('td.allowedValues').first().find('button.save').click();

        cy.wait('@updateMultiField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the predefined verifier', () => {
        cy.get('td.predefinedVerifier').first().scrollIntoView();
        cy.get('td.predefinedVerifier').first().find('button.edit').click();
        cy.get('td.predefinedVerifier').first().find('select').select('MULTI_LINE_TEXT', { force: true });
        cy.get('td.predefinedVerifier').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the regular expression', () => {
        cy.get('td.regularExpression').first().find('button.js-edit').click();
        cy.get('@projectFields').then((fields) => {
          cy.get('td.regularExpression').first().find('input[type=text]').type(fields[1].regularExpression);
        });
        cy.get('td.regularExpression').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the regular expression error message', () => {
        cy.get('td.regularExpressionErrorMsg').first().find('button.js-edit').click();
        cy.get('@projectFields').then((fields) => {
          cy.get('td.regularExpressionErrorMsg').first().find('input[type=text]')
            .type(fields[1].regularExpressionErrorMsg);
        });
        cy.get('td.regularExpressionErrorMsg').first().find('button.save').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should update the legacy flag', () => {
        cy.get('td.legacy').first().scrollIntoView();
        cy.get('td.legacy').first().find('button.edit').click();
        cy.get('td.legacy').first().find('button.toggle').click();

        cy.wait('@updateProjectField').then((interception) => {
          expect(interception.response.statusCode).to.eq(200);
          expect(interception.response.body.success).to.eq(true);
        });
      });

      it('should delete project field definition', () => {
        cy.get('@projectFields').then((fields) => {
          cy.intercept('/projectFields/deleteFieldDefinition*').as('deleteFieldDefinition');

          cy.contains(fields[1].name).should('exist');
          cy.get('input[name=deleteFieldDefinition][type=submit]').first().click();
          cy.on('window:confirm', () => true);

          cy.wait('@deleteFieldDefinition').then((interception) => {
            expect(interception.response.statusCode).to.eq(302);
            cy.location('pathname').should('eq', '/projectFields/index');
            cy.get('.additionalFields').contains(fields[1].name).should('not.exist');
          });
        });
      });
    });
  });

  context('when user is normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/projectFields/index');
      cy.checkAccessDenied('/projectFields/create');
      cy.checkAccessDenied('/projectFields/createText');
      cy.checkAccessDenied('/projectFields/updateField');
      cy.checkAccessDenied('/projectFields/updateMultiField');
      cy.checkAccessDenied('/projectFields/deleteFieldDefinition');
    });
  });
});
