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

import { describe, it } from 'node:test';
import { RuleTester } from 'eslint';

import rule from './require-cypress-response-status.mjs';

RuleTester.describe = describe;
RuleTester.it = it;

const ruleTester = new RuleTester();

ruleTester.run('require-cypress-response-status', rule, {
  valid: [
    "cy.wait('@data').its('response.statusCode').should('eq', 200);",
    "cy.wait('@kill').its('response.statusCode').should('eq', 302);",
    "cy.wait('@denied').its('response.statusCode').should('eq', 405);",
    "cy.wait('@data').then((request) => { expect(request.response.statusCode).to.eq(200); });",
    "cy.wait(['@first', '@second']).each((request) => { expect(request.response.statusCode).to.eq(200); });",
    "cy.wait('@data').should((request) => { expect(request.response.statusCode).to.eq(200); });",
    "cy.wait('@data').its('response').should('have.property', 'statusCode', 200);",
    "cy.wait('@data').should('have.nested.property', 'response.statusCode', 200);",
    "cy.wait('@data').its('response.statusCode').then((status) => { expect(status).to.eq(200); });",
    "cy.wait('@data').then(({ response }) => { expect(response.statusCode).to.eq(200); });",
    "cy.wait('@data').then((request) => { assert.equal(request.response.statusCode, 200); });",
    "cy.wait(['@first', '@second']).spread((a, b) => { expect(a.response.statusCode).to.eq(200); " +
      "expect(b.response.statusCode).to.eq(302); });",
    "cy.wait(['@first', '@second']).then((requests) => { expect(requests[0].response.statusCode).to.eq(200); " +
      "expect(requests[1].response.statusCode).to.eq(302); });",
    // Intentional network-error cases can use a documented ESLint exception.
    "// eslint-disable-next-line rule-to-test/require-cypress-response-status -- No response status\n" +
      "cy.wait('@data').its('error').should('exist');",
    // A helper may return the interception for its caller to assert on.
    "function waitForData() { return cy.wait('@data'); }",
    'cy.wait(1000);',
    'cy.wait(alias);',
    'cy.wait(`@${name}`);',
    "other.wait('@data');",
    'cy.wait();'
  ],
  invalid: [
    "cy.wait('@workflowRunListData');",
    "cy.wait('@data', { timeout: 10000 });",
    "cy.wait(['@first', '@second']);",
    "cy.wait(['@first', dynamicAlias]);",
    'cy.wait(`@data`);',
    "cy['wait']('@data');",
    "it('loads data', () => { cy.wait('@data'); cy.get('table').should('exist'); });",
    "cy.wait('@data'); cy.wait('@data').its('response.statusCode').should('eq', 200);",
    "cy.wait('@data').then(() => {});",
    "cy.wait('@data').then(() => { cy.get('table').should('exist'); });",
    "cy.wait('@data').then((request) => { expect(request.response.body).to.exist; });",
    "cy.wait('@data').then((request) => { expect(other.response.statusCode).to.eq(200); });",
    "cy.wait('@data').then(() => { cy.wait('@other').then((request) => { " +
      "expect(request.response.statusCode).to.eq(200); }); });",
    "cy.wait('@data').then((request) => { expect(request.response.statusCode); });",
    "cy.wait('@data').its('response.statusCode');",
    "cy.wait('@data').its('response.statusCode').should('exist');",
    "cy.wait('@data').its('response').should('exist');",
    "cy.wait('@data').its('response.body.success').should('eq', true);",
    "cy.wait('@data').then((request) => { const check = () => expect(request.response.statusCode).to.eq(200); });",
    "cy.wait('@data').then((request) => { { const request = other; " +
      "expect(request.response.statusCode).to.eq(200); } });",
    "cy.wait(['@first', '@second']).spread((a, b) => { expect(a.response.statusCode).to.eq(200); });",
    "cy.wait(['@first', '@second']).then((requests) => { expect(requests[0].response.statusCode).to.eq(200); });"
  ].map((code) => ({
    code,
    errors: [{ messageId: 'missingStatusAssertion' }]
  }))
});
