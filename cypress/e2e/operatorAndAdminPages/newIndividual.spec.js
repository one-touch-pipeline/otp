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

describe('Check create individual tests', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAs('operator');
      cy.visit('/individual/insert');
    });

    it('should be able to create a new individual without samples successfully', () => {
      const pid = `test_ind_nosample_${Date.now()}`;

      // Fill in PID / Identifier
      cy.get('#identifier').type(pid);

      // Select project (using the custom select helper)
      cy.selectAvailableOptionFromDropdown('individualProject.id', 1);

      // Select individual type
      cy.selectAvailableOptionFromDropdown('type', 1);

      // Submit the form
      cy.get('input[type="submit"]').click();

      // Check success flash message
      cy.get('#infoBox .message').should('exist')
        .and('contain.text', 'Individual was successfully saved');
    });

    it('should be able to create a new individual with a sample successfully', () => {
      const pid = `test_ind_sample_${Date.now()}`;

      // Fill in PID / Identifier
      cy.get('#identifier').type(pid);

      // Select project
      cy.selectAvailableOptionFromDropdown('individualProject.id', 1);

      // Select individual type
      cy.selectAvailableOptionFromDropdown('type', 1);

      // Add a sample
      cy.get('button.add-button').click();

      // Select sample type and enter sample identifier/name
      cy.selectAvailableOptionFromDropdown('samples[0].sampleType', 1);
      cy.get('input[name="samples[0].sampleIdentifiers"]').type(`sample_${Date.now()}`);

      // Submit the form
      cy.get('input[type="submit"]').click();

      // Check success flash message
      cy.get('#infoBox .message').should('exist')
        .and('contain.text', 'Individual was successfully saved');
    });

    it('should be able to create a new individual and redirect to the overview page', () => {
      const pid = `test_ind_redirect_${Date.now()}`;

      // Fill in PID / Identifier
      cy.get('#identifier').type(pid);

      // Select project
      cy.selectAvailableOptionFromDropdown('individualProject.id', 1);

      // Select individual type
      cy.selectAvailableOptionFromDropdown('type', 1);

      // Check the redirect checkbox
      cy.get('#checkRedirect').check();

      // Submit the form
      cy.get('input[type="submit"]').click();

      // Verify redirection to the show/overview page
      cy.url().should('include', '/individual/show/');
      cy.get('.body').should('contain.text', pid);
    });
  });

  context('when user is a normal user', () => {
    beforeEach(() => {
      cy.loginAs('user');
    });

    it('should not be able to visit the page', () => {
      cy.checkAccessDenied('/individual/insert');
    });
  });
});
