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

describe('Check errors pages', () => {
  'use strict';

  context('when user is an operator', () => {
    beforeEach(() => {
      cy.loginAsOperator();
    });

    it('should visit the error403 page and expect a 403 HTTP Error', () => {
      const url = '/errors/error403';
      cy.request({
        url,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.equal(403);
      });
      cy.visit(url, { failOnStatusCode: false });
      cy.get('h1').should('contain', 'Access Denied');
    });

    it('should visit the error404 page and expect a 404 HTTP Error', () => {
      const url = '/errors/error404';
      cy.request({
        url,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.equal(404);
      });
      cy.visit(url, { failOnStatusCode: false });
      cy.get('h1').should('contain', 'Resource not found');
    });

    it('should visit the error405 page and expect a 405 HTTP Error', () => {
      const url = '/errors/error405';
      cy.request({
        url,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.equal(405);
      });
      cy.visit(url, { failOnStatusCode: false });
      cy.get('h1').should('contain', 'Method Not Allowed');
    });

    it('should visit the error500 page and expect a 500 HTTP Error', () => {
      const url = '/errors/error500';
      cy.request({
        url,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.equal(500);
      });
      cy.visit(url, { failOnStatusCode: false });
      cy.get('h1').should('contain', 'Unexpected error occurred');
    });

    it('should visit the noProject page and expect a 404 HTTP Error', () => {
      const url = '/errors/noProject';
      cy.request({
        url,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.equal(404);
      });
      cy.visit(url, { failOnStatusCode: false });
      cy.get('p').should('contain', 'No project exists');
    });

    it('should visit the switchedUserDeniedException page and expect a 401 HTTP Error', () => {
      const url = '/errors/switchedUserDeniedException';
      cy.request({
        url,
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.be.equal(401);
      });
      cy.visit(url, { failOnStatusCode: false });
      cy.get('h1').should('contain', 'Action not permitted as switched user');
    });
  });
});
