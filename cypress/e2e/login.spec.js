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

describe('Check login', () => {
  'use strict';

  context('when password is wrong', () => {
    it('should redirect to login page and show error message', () => {
      const username = Cypress.env('user_username');
      const password = 'wrong-password';

      cy.clearCookies();

      cy.visit('/');
      cy.get('#account').type(username);
      cy.get('#password').type(password);
      cy.get('#loginButton').click();

      cy.url().should('contain', '/login?target=');

      cy.get('.login_message').contains('Sorry, we were not able to find a user with that username and password.');
    });
  });

  context('when user doesn\'t exist', () => {
    it('should redirect to login page and show error message', () => {
      const username = 'nonexisting-user';
      const password = '*';

      cy.clearCookies();

      cy.visit('/');
      cy.get('#account').type(username);
      cy.get('#password').type(password);
      cy.get('#loginButton').click();

      cy.url().should('contain', '/login?target=');

      cy.get('.login_message').contains('There is a problem with your account. Please contact support@otp.de.');
    });
  });
});
